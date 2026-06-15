package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.BadgeResponse;
import cn.seecoder.campushelp.service.BadgeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Achievement badge endpoints.
 * All require authentication.
 */
@RestController
@RequestMapping("/api/v1/badges")
public class BadgeController {

    private final BadgeService badgeService;

    public BadgeController(BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    /** Get all 9 badges with earned status and progress for the current user. */
    @GetMapping
    public ApiResult<List<BadgeResponse>> getUserBadges(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(badgeService.getUserBadges(userId));
    }

    /** Wear an earned badge. Replaces any previously worn badge. */
    @PostMapping("/wear/{badgeKey}")
    public ApiResult<Void> wearBadge(Authentication auth, @PathVariable String badgeKey) {
        Long userId = (Long) auth.getPrincipal();
        badgeService.wearBadge(userId, badgeKey);
        return ApiResult.success();
    }

    /** Remove the currently worn badge (idempotent). */
    @DeleteMapping("/wear")
    public ApiResult<Void> unwearBadge(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        badgeService.unwearBadge(userId);
        return ApiResult.success();
    }

    /** Award the EASTER_EGG badge. Called when the user triggers the easter egg. */
    @PostMapping("/easter-egg")
    public ApiResult<Void> awardEasterEgg(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        badgeService.awardEasterEgg(userId);
        return ApiResult.success();
    }
}
