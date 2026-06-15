package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.BadgeResponse;

import java.util.List;

/**
 * Achievement badge service.
 * Handles badge award logic, progress queries, and badge wearing.
 */
public interface BadgeService {

    /**
     * Check the condition for a badge and award it if met.
     * Safe to call repeatedly — no-ops if already earned.
     *
     * @return true if the badge was newly awarded, false if already owned
     */
    boolean checkAndAward(Long userId, String badgeKey);

    /**
     * Get all 9 badges with earned status, progress, and wearing info.
     */
    List<BadgeResponse> getUserBadges(Long userId);

    /**
     * Wear an earned badge. Auto-unwears any previously worn badge.
     *
     * @throws cn.seecoder.campushelp.common.BusinessException if badge not earned
     */
    void wearBadge(Long userId, String badgeKey);

    /** Remove worn badge (idempotent). */
    void unwearBadge(Long userId);

    /** Get the badge key currently worn by this user, or null. */
    String getWornBadgeKey(Long userId);

    /** Award EASTER_EGG badge (idempotent). Called from the frontend easter egg trigger. */
    void awardEasterEgg(Long userId);
}
