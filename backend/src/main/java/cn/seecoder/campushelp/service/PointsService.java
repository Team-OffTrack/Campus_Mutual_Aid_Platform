package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.DailyCheckinResponse;
import cn.seecoder.campushelp.dto.DailyCheckinStatus;
import cn.seecoder.campushelp.entity.PointsTransaction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface PointsService {

    /** Check in for today. Returns points awarded and current streak. */
    DailyCheckinResponse checkin(Long userId);

    /** Get today's check-in status without mutating anything. */
    DailyCheckinStatus getCheckinStatus(Long userId);

    /** Freeze points from available to frozen when a point-reward demand is published. */
    void freezeOnPublish(Long userId, int amount, Long demandId);

    /** Unfreeze (refund) points back to available when a demand is cancelled. */
    void unfreezeOnCancel(Long userId, int amount, Long demandId);

    /** Transfer frozen points from publisher to acceptor's available balance on completion. */
    void transferOnComplete(Long publisherId, Long acceptorId, int amount, Long demandId);

    /** Paginated transaction history for a user, with optional type filter. */
    Page<PointsTransaction> getTransactions(Long userId, int pageNum, int pageSize, String type);

    /**
     * Adjust frozen points when the rewardAmount of an existing demand changes.
     * If newAmount > oldAmount: freeze the difference (checks available balance).
     * If newAmount < oldAmount: unfreeze the difference.
     * If equal: no-op.
     * Creates an EDIT_ADJUST transaction record.
     */
    void adjustFrozenPoints(Long userId, int oldAmount, int newAmount, Long demandId);

    /** Award signup bonus points to a newly registered user. */
    void awardSignupBonus(Long userId);
}
