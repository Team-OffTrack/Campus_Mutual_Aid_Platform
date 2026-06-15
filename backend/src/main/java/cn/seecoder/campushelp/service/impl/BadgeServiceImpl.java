package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.BadgeResponse;
import cn.seecoder.campushelp.entity.*;
import cn.seecoder.campushelp.entity.enums.BadgeDefinition;
import cn.seecoder.campushelp.entity.enums.DemandStatus;
import cn.seecoder.campushelp.mapper.*;
import cn.seecoder.campushelp.service.BadgeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BadgeServiceImpl implements BadgeService {

    private final UserBadgeMapper userBadgeMapper;
    private final WornBadgeMapper wornBadgeMapper;
    private final DemandMapper demandMapper;
    private final EvaluationMapper evaluationMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ReportMapper reportMapper;

    public BadgeServiceImpl(UserBadgeMapper userBadgeMapper,
                            WornBadgeMapper wornBadgeMapper,
                            DemandMapper demandMapper,
                            EvaluationMapper evaluationMapper,
                            DailyCheckinMapper dailyCheckinMapper,
                            ReportMapper reportMapper) {
        this.userBadgeMapper = userBadgeMapper;
        this.wornBadgeMapper = wornBadgeMapper;
        this.demandMapper = demandMapper;
        this.evaluationMapper = evaluationMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.reportMapper = reportMapper;
    }

    @Override
    @Transactional
    public boolean checkAndAward(Long userId, String badgeKey) {
        BadgeDefinition def = BadgeDefinition.fromKey(badgeKey);
        if (def == null) return false;

        // Already earned — no-op
        if (hasBadge(userId, badgeKey)) return false;

        // Check condition (EASTER_EGG is awarded only via awardEasterEgg)
        boolean met = switch (badgeKey) {
            case "FIRST_PUBLISH"       -> countPublished(userId) >= 1;
            case "FIRST_ACCEPT"        -> countAccepted(userId) >= 1;
            case "TEN_COMPLETES"       -> countCompletedAsParticipant(userId) >= 10;
            case "FIRST_FIVE_STAR"     -> countFiveStarRatings(userId) >= 1;
            case "HUNDRED_STARS"       -> totalReceivedStars(userId) >= 100;
            case "CHECKIN_30"          -> maxCheckinStreak(userId) >= 30;
            case "HELPER"              -> countDonationCompletes(userId) >= 5;
            case "FIRST_REPORT_SUCCESS" -> countSuccessfulReports(userId) >= 1;
            default                    -> false;
        };

        if (!met) return false;

        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeKey(badgeKey);
        userBadgeMapper.insert(badge);
        return true;
    }

    @Override
    public List<BadgeResponse> getUserBadges(Long userId) {
        // Load earned badge keys
        Set<String> earnedKeys = userBadgeMapper.selectList(
                new LambdaQueryWrapper<UserBadge>().eq(UserBadge::getUserId, userId))
                .stream().map(UserBadge::getBadgeKey).collect(Collectors.toSet());

        // Load worn badge key
        WornBadge worn = wornBadgeMapper.selectOne(
                new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));
        String wornKey = worn != null ? worn.getBadgeKey() : null;

        // Pre-compute all progress counts in one pass where possible
        int published = countPublished(userId);
        int accepted = countAccepted(userId);
        int completedAsParticipant = countCompletedAsParticipant(userId);
        int fiveStarCount = countFiveStarRatings(userId);
        int totalStars = totalReceivedStars(userId);
        int maxStreak = maxCheckinStreak(userId);
        int donationCompletes = countDonationCompletes(userId);
        int successfulReports = countSuccessfulReports(userId);

        Map<String, Integer> progressMap = Map.of(
                "FIRST_PUBLISH", published,
                "FIRST_ACCEPT", accepted,
                "TEN_COMPLETES", completedAsParticipant,
                "FIRST_FIVE_STAR", fiveStarCount,
                "HUNDRED_STARS", totalStars,
                "CHECKIN_30", maxStreak,
                "HELPER", donationCompletes,
                "FIRST_REPORT_SUCCESS", successfulReports
        );

        List<BadgeResponse> result = new ArrayList<>();
        for (BadgeDefinition def : BadgeDefinition.values()) {
            boolean earned = earnedKeys.contains(def.getKey());
            boolean wearing = def.getKey().equals(wornKey);
            if (earned) {
                result.add(BadgeResponse.earned(def, wearing));
            } else {
                int current = progressMap.getOrDefault(def.getKey(), 0);
                result.add(BadgeResponse.unearned(def, current, wearing));
            }
        }
        return result;
    }

    @Override
    @Transactional
    public void wearBadge(Long userId, String badgeKey) {
        if (!hasBadge(userId, badgeKey)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "尚未获得该徽章，无法佩戴");
        }

        // Remove any previously worn badge
        wornBadgeMapper.delete(new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));

        WornBadge wb = new WornBadge();
        wb.setUserId(userId);
        wb.setBadgeKey(badgeKey);
        wornBadgeMapper.insert(wb);
    }

    @Override
    @Transactional
    public void unwearBadge(Long userId) {
        wornBadgeMapper.delete(new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));
    }

    @Override
    public String getWornBadgeKey(Long userId) {
        WornBadge worn = wornBadgeMapper.selectOne(
                new LambdaQueryWrapper<WornBadge>().eq(WornBadge::getUserId, userId));
        return worn != null ? worn.getBadgeKey() : null;
    }

    @Override
    @Transactional
    public void awardEasterEgg(Long userId) {
        if (hasBadge(userId, "EASTER_EGG")) return;

        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeKey("EASTER_EGG");
        userBadgeMapper.insert(badge);
    }

    // ── Private helpers ──

    private boolean hasBadge(Long userId, String badgeKey) {
        return userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeKey, badgeKey)) > 0;
    }

    // ── Progress query methods (package-visible for testing) ──

    int countPublished(Long userId) {
        return demandMapper.selectCount(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getPublisherId, userId)).intValue();
    }

    int countAccepted(Long userId) {
        return demandMapper.selectCount(new LambdaQueryWrapper<Demand>()
                .eq(Demand::getAcceptorId, userId)).intValue();
    }

    int countCompletedAsParticipant(Long userId) {
        return demandMapper.selectCount(new LambdaQueryWrapper<Demand>()
                .and(w -> w.eq(Demand::getPublisherId, userId).or().eq(Demand::getAcceptorId, userId))
                .eq(Demand::getStatus, DemandStatus.COMPLETED)).intValue();
    }

    int countFiveStarRatings(Long userId) {
        return evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getTargetUserId, userId)
                .eq(Evaluation::getRating, 5)).intValue();
    }

    int totalReceivedStars(Long userId) {
        List<Object> ratings = evaluationMapper.selectObjs(
                new LambdaQueryWrapper<Evaluation>()
                        .select(Evaluation::getRating)
                        .eq(Evaluation::getTargetUserId, userId));
        return ratings.stream().mapToInt(o -> (Integer) o).sum();
    }

    int maxCheckinStreak(Long userId) {
        DailyCheckin top = dailyCheckinMapper.selectOne(
                new LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, userId)
                        .orderByDesc(DailyCheckin::getStreak)
                        .last("LIMIT 1"));
        return top != null ? top.getStreak() : 0;
    }

    int countDonationCompletes(Long userId) {
        return demandMapper.selectCount(new LambdaQueryWrapper<Demand>()
                .and(w -> w.eq(Demand::getPublisherId, userId).or().eq(Demand::getAcceptorId, userId))
                .eq(Demand::getStatus, DemandStatus.COMPLETED)
                .eq(Demand::getRewardType, "donation")).intValue();
    }

    int countSuccessfulReports(Long userId) {
        return reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .eq(Report::getStatus, "RESOLVED")).intValue();
    }
}
