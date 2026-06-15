package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.DailyCheckinResponse;
import cn.seecoder.campushelp.dto.DailyCheckinStatus;
import cn.seecoder.campushelp.entity.DailyCheckin;
import cn.seecoder.campushelp.entity.PointsTransaction;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.entity.enums.BadgeDefinition;
import cn.seecoder.campushelp.entity.enums.PointsTransactionType;
import cn.seecoder.campushelp.mapper.DailyCheckinMapper;
import cn.seecoder.campushelp.mapper.PointsTransactionMapper;
import cn.seecoder.campushelp.mapper.UserAccountMapper;
import cn.seecoder.campushelp.service.BadgeService;
import cn.seecoder.campushelp.service.PointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class PointsServiceImpl implements PointsService {

    private static final int CHECKIN_BASE_POINTS = 5;
    private static final int CHECKIN_MAX_POINTS = 15;
    private static final int SIGNUP_BONUS_POINTS = 100;

    private final UserAccountMapper userAccountMapper;
    private final PointsTransactionMapper pointsTransactionMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final BadgeService badgeService;

    public PointsServiceImpl(UserAccountMapper userAccountMapper,
                             PointsTransactionMapper pointsTransactionMapper,
                             DailyCheckinMapper dailyCheckinMapper,
                             BadgeService badgeService) {
        this.userAccountMapper = userAccountMapper;
        this.pointsTransactionMapper = pointsTransactionMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.badgeService = badgeService;
    }

    @Override
    @Transactional
    public DailyCheckinResponse checkin(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. Check if already checked in today
        DailyCheckin todayCheckin = dailyCheckinMapper.selectOne(new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, today));
        if (todayCheckin != null) {
            throw new BusinessException(ResultCode.CONFLICT, "今日已签到");
        }

        // 2. Calculate streak
        DailyCheckin lastCheckin = dailyCheckinMapper.selectOne(new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckinDate)
                .last("LIMIT 1"));
        int streak;
        LocalDate yesterday = today.minusDays(1);
        if (lastCheckin != null && yesterday.equals(lastCheckin.getCheckinDate())) {
            streak = lastCheckin.getStreak() + 1;
        } else {
            streak = 1;
        }

        // 3. Calculate points: 5 base + bonus based on streak
        int points = calculateCheckinPoints(streak);

        // 4. Insert daily_checkin record
        DailyCheckin checkin = new DailyCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setPointsAwarded(points);
        checkin.setStreak(streak);
        dailyCheckinMapper.insert(checkin);

        // Badge: CHECKIN_30
        if (streak >= 30) {
            badgeService.checkAndAward(userId, BadgeDefinition.CHECKIN_30.getKey());
        }

        // 5. Update user_account: add points to available
        UserAccount account = getUserAccountForUpdate(userId);
        account.setAvailablePoints(account.getAvailablePoints() + points);
        userAccountMapper.updateById(account);

        // 6. Insert points_transaction
        insertTransaction(userId, points, account.getAvailablePoints(),
                PointsTransactionType.DAILY_CHECKIN, null, "每日签到 (连续" + streak + "天)");

        return new DailyCheckinResponse(points, streak, today);
    }

    @Override
    public DailyCheckinStatus getCheckinStatus(Long userId) {
        LocalDate today = LocalDate.now();

        DailyCheckin todayCheckin = dailyCheckinMapper.selectOne(new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckinDate, today));

        if (todayCheckin != null) {
            return new DailyCheckinStatus(true, todayCheckin.getStreak(), todayCheckin.getCheckinDate());
        }

        // Not checked in today — return the last streak for display
        DailyCheckin lastCheckin = dailyCheckinMapper.selectOne(new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckinDate)
                .last("LIMIT 1"));
        if (lastCheckin != null && lastCheckin.getCheckinDate().equals(LocalDate.now().minusDays(1))) {
            return new DailyCheckinStatus(false, lastCheckin.getStreak(), lastCheckin.getCheckinDate());
        }

        return new DailyCheckinStatus(false, 0, lastCheckin != null ? lastCheckin.getCheckinDate() : null);
    }

    @Override
    @Transactional
    public void freezeOnPublish(Long userId, int amount, Long demandId) {
        UserAccount account = getUserAccountForUpdate(userId);
        if (account.getAvailablePoints() < amount) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "可用积分不足，需要 " + amount + " 积分");
        }
        account.setAvailablePoints(account.getAvailablePoints() - amount);
        account.setFrozenPoints(account.getFrozenPoints() + amount);
        userAccountMapper.updateById(account);

        insertTransaction(userId, -amount, account.getAvailablePoints(),
                PointsTransactionType.PUBLISH, demandId, "发布需求冻结积分");
    }

    @Override
    @Transactional
    public void unfreezeOnCancel(Long userId, int amount, Long demandId) {
        UserAccount account = getUserAccountForUpdate(userId);
        if (account.getFrozenPoints() < amount) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "冻结积分不足，数据异常");
        }
        account.setFrozenPoints(account.getFrozenPoints() - amount);
        account.setAvailablePoints(account.getAvailablePoints() + amount);
        userAccountMapper.updateById(account);

        insertTransaction(userId, amount, account.getAvailablePoints(),
                PointsTransactionType.CANCEL_REFUND, demandId, "取消需求解冻积分");
    }

    @Override
    @Transactional
    public void transferOnComplete(Long publisherId, Long acceptorId, int amount, Long demandId) {
        // Publisher: frozen points decrease
        UserAccount publisherAccount = getUserAccountForUpdate(publisherId);
        if (publisherAccount.getFrozenPoints() < amount) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "发布者冻结积分不足，数据异常");
        }
        publisherAccount.setFrozenPoints(publisherAccount.getFrozenPoints() - amount);
        userAccountMapper.updateById(publisherAccount);

        insertTransaction(publisherId, 0, publisherAccount.getAvailablePoints(),
                PointsTransactionType.COMPLETE_EARN, demandId, "需求完成，积分转给接单人");

        // Acceptor: available points increase
        UserAccount acceptorAccount = getUserAccountForUpdate(acceptorId);
        acceptorAccount.setAvailablePoints(acceptorAccount.getAvailablePoints() + amount);
        userAccountMapper.updateById(acceptorAccount);

        insertTransaction(acceptorId, amount, acceptorAccount.getAvailablePoints(),
                PointsTransactionType.COMPLETE_EARN, demandId, "需求完成，获得积分");
    }

    @Override
    public Page<PointsTransaction> getTransactions(Long userId, int pageNum, int pageSize, String type) {
        LambdaQueryWrapper<PointsTransaction> wrapper = new LambdaQueryWrapper<PointsTransaction>()
                .eq(PointsTransaction::getUserId, userId);
        if (type != null && !type.isBlank()) {
            wrapper.eq(PointsTransaction::getType, type);
        }
        wrapper.orderByDesc(PointsTransaction::getCreateTime);
        return pointsTransactionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void adjustFrozenPoints(Long userId, int oldAmount, int newAmount, Long demandId) {
        if (oldAmount == newAmount) return;

        UserAccount account = getUserAccountForUpdate(userId);
        int diff = newAmount - oldAmount;

        if (diff > 0) {
            // Increasing reward: freeze extra points
            if (account.getAvailablePoints() < diff) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "可用积分不足，需要 " + diff + " 积分");
            }
            account.setAvailablePoints(account.getAvailablePoints() - diff);
            account.setFrozenPoints(account.getFrozenPoints() + diff);
            userAccountMapper.updateById(account);
            insertTransaction(userId, -diff, account.getAvailablePoints(),
                    PointsTransactionType.EDIT_ADJUST, demandId, "编辑需求增加冻结积分");
        } else {
            // Decreasing reward: unfreeze points
            int unfreezeAmount = -diff;
            if (account.getFrozenPoints() < unfreezeAmount) {
                throw new BusinessException(ResultCode.INTERNAL_ERROR, "冻结积分不足，数据异常");
            }
            account.setFrozenPoints(account.getFrozenPoints() - unfreezeAmount);
            account.setAvailablePoints(account.getAvailablePoints() + unfreezeAmount);
            userAccountMapper.updateById(account);
            insertTransaction(userId, unfreezeAmount, account.getAvailablePoints(),
                    PointsTransactionType.EDIT_ADJUST, demandId, "编辑需求减少冻结积分");
        }
    }

    @Override
    @Transactional
    public void awardSignupBonus(Long userId) {
        UserAccount account = getUserAccountForUpdate(userId);
        account.setAvailablePoints(account.getAvailablePoints() + SIGNUP_BONUS_POINTS);
        userAccountMapper.updateById(account);

        insertTransaction(userId, SIGNUP_BONUS_POINTS, account.getAvailablePoints(),
                PointsTransactionType.SIGNUP_BONUS, null, "注册赠送积分");
    }

    // ─── private helpers ───

    private UserAccount getUserAccountForUpdate(Long userId) {
        UserAccount account = userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccount>()
                .eq(UserAccount::getUserId, userId)
                .last("FOR UPDATE"));
        if (account == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户账户不存在");
        }
        return account;
    }

    private void insertTransaction(Long userId, int amount, int balanceAfter, String type, Long referenceId, String description) {
        PointsTransaction tx = new PointsTransaction();
        tx.setUserId(userId);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setType(type);
        tx.setReferenceId(referenceId);
        tx.setDescription(description);
        pointsTransactionMapper.insert(tx);
    }

    static int calculateCheckinPoints(int streak) {
        int bonus;
        if (streak >= 7) {
            bonus = 10;
        } else if (streak >= 5) {
            bonus = 5;
        } else if (streak >= 3) {
            bonus = 3;
        } else {
            bonus = 0;
        }
        return Math.min(CHECKIN_BASE_POINTS + bonus, CHECKIN_MAX_POINTS);
    }
}
