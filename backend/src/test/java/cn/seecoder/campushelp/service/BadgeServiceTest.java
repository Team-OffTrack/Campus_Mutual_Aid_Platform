package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.*;
import cn.seecoder.campushelp.entity.enums.BadgeDefinition;
import cn.seecoder.campushelp.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BadgeServiceTest {

    @Autowired private BadgeService badgeService;
    @Autowired private UserService userService;
    @Autowired private DemandService demandService;
    @Autowired private UserMapper userMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private EvaluationMapper evaluationMapper;
    @Autowired private DailyCheckinMapper dailyCheckinMapper;
    @Autowired private ReportMapper reportMapper;
    @Autowired private UserBadgeMapper userBadgeMapper;
    @Autowired private WornBadgeMapper wornBadgeMapper;

    private Long publisherId;
    private Long acceptorId;
    private Long demandId;

    @BeforeEach
    void setUp() {
        // Clean in FK-safe order
        userBadgeMapper.delete(new LambdaQueryWrapper<>());
        wornBadgeMapper.delete(new LambdaQueryWrapper<>());
        evaluationMapper.delete(new LambdaQueryWrapper<>());
        reportMapper.delete(new LambdaQueryWrapper<>());
        dailyCheckinMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Create publisher
        RegisterRequest pubReg = new RegisterRequest();
        pubReg.setStudentId("badgepub");
        pubReg.setPassword("pass123");
        pubReg.setName("徽章发布者");
        userService.register(pubReg);
        publisherId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "badgepub")).getUserId();

        // Create acceptor
        RegisterRequest accReg = new RegisterRequest();
        accReg.setStudentId("badgeacc");
        accReg.setPassword("pass123");
        accReg.setName("徽章接单人");
        userService.register(accReg);
        acceptorId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "badgeacc")).getUserId();
    }

    // ── Helpers ──

    private boolean hasBadge(Long userId, String badgeKey) {
        return userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, userId)
                .eq(UserBadge::getBadgeKey, badgeKey)) > 0;
    }

    private DemandResponse publishAndAccept() {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("test demand");
        req.setDescription("test");
        req.setRewardType("point");
        req.setRewardAmount(0); // 0 avoids freezing points, allowing many publishes
        DemandResponse r = demandService.publish(publisherId, req);
        demandService.accept(r.getDemandId(), acceptorId);
        return r;
    }

    private void publishAcceptComplete() {
        DemandResponse r = publishAndAccept();
        demandService.complete(r.getDemandId(), publisherId);
    }

    /** Publish, accept, complete and return the demand ID for evaluation referencing. */
    private Long publishAcceptCompleteAndGetId() {
        DemandResponse r = publishAndAccept();
        demandService.complete(r.getDemandId(), publisherId);
        return r.getDemandId();
    }

    // ── FIRST_PUBLISH ──

    @Test
    @DisplayName("FIRST_PUBLISH — auto-awarded by publish()")
    void FIRST_PUBLISH_autoAwardedByPublish() {
        // Publish triggers the hook in DemandServiceImpl
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle("my first demand");
        req.setDescription("test");
        req.setRewardType("point");
        req.setRewardAmount(0);
        demandService.publish(publisherId, req);

        assertTrue(hasBadge(publisherId, "FIRST_PUBLISH"));
    }

    @Test
    @DisplayName("FIRST_PUBLISH — checkAndAward returns false when already earned")
    void FIRST_PUBLISH_alreadyEarned_returnsFalse() {
        demandService.publish(publisherId, createDemandReq("d1"));
        boolean result = badgeService.checkAndAward(publisherId, "FIRST_PUBLISH");
        assertFalse(result);
    }

    // ── FIRST_ACCEPT ──

    @Test
    @DisplayName("FIRST_ACCEPT — auto-awarded by accept()")
    void FIRST_ACCEPT_autoAwardedByAccept() {
        CreateDemandRequest req = createDemandReq("accept test");
        DemandResponse r = demandService.publish(publisherId, req);
        demandService.accept(r.getDemandId(), acceptorId);

        assertTrue(hasBadge(acceptorId, "FIRST_ACCEPT"));
    }

    @Test
    @DisplayName("FIRST_ACCEPT — not awarded when no accepts")
    void FIRST_ACCEPT_notAwarded() {
        CreateDemandRequest req = createDemandReq("no accept");
        demandService.publish(publisherId, req);

        assertFalse(hasBadge(acceptorId, "FIRST_ACCEPT"));
    }

    // ── TEN_COMPLETES ──

    @Test
    @DisplayName("TEN_COMPLETES — not awarded with 9 completions")
    void TEN_COMPLETES_notAwardedWith9() {
        for (int i = 0; i < 9; i++) {
            publishAcceptComplete();
        }
        assertFalse(hasBadge(acceptorId, "TEN_COMPLETES"));
        boolean awarded = badgeService.checkAndAward(acceptorId, "TEN_COMPLETES");
        assertFalse(awarded);
    }

    @Test
    @DisplayName("TEN_COMPLETES — auto-awarded on 10th completion")
    void TEN_COMPLETES_autoAwardedOn10th() {
        for (int i = 0; i < 10; i++) {
            publishAcceptComplete();
        }
        // complete() hook auto-awards when count reaches 10
        assertTrue(hasBadge(acceptorId, "TEN_COMPLETES"));
    }

    // ── FIRST_FIVE_STAR ──

    @Test
    @DisplayName("FIRST_FIVE_STAR — awarded via checkAndAward after 5-star eval")
    void FIRST_FIVE_STAR_awardedAfter5Star() {
        Long did = publishAcceptCompleteAndGetId();
        insertEvaluation(did, acceptorId, 5);
        boolean awarded = badgeService.checkAndAward(acceptorId, "FIRST_FIVE_STAR");
        assertTrue(awarded);
    }

    @Test
    @DisplayName("FIRST_FIVE_STAR — not awarded with 4-star")
    void FIRST_FIVE_STAR_notAwardedWith4() {
        Long did = publishAcceptCompleteAndGetId();
        insertEvaluation(did, acceptorId, 4);
        boolean awarded = badgeService.checkAndAward(acceptorId, "FIRST_FIVE_STAR");
        assertFalse(awarded);
    }

    // ── HUNDRED_STARS ──

    @Test
    @DisplayName("HUNDRED_STARS — awarded when sum >= 100")
    void HUNDRED_STARS_awardedWhenSum100() {
        // Create 20 completed demands, one evaluation each (5 stars × 20 = 100)
        for (int i = 0; i < 20; i++) {
            Long did = publishAcceptCompleteAndGetId();
            insertEvaluation(did, acceptorId, 5);
        }
        boolean awarded = badgeService.checkAndAward(acceptorId, "HUNDRED_STARS");
        assertTrue(awarded);
    }

    @Test
    @DisplayName("HUNDRED_STARS — not awarded when sum < 100")
    void HUNDRED_STARS_notAwardedUnder100() {
        Long did = publishAcceptCompleteAndGetId();
        insertEvaluation(did, acceptorId, 4);
        boolean awarded = badgeService.checkAndAward(acceptorId, "HUNDRED_STARS");
        assertFalse(awarded);
    }

    // ── CHECKIN_30 ──

    @Test
    @DisplayName("CHECKIN_30 — awarded with max streak >= 30")
    void CHECKIN_30_awardedWithMaxStreak30() {
        for (int i = 1; i <= 30; i++) {
            DailyCheckin dc = new DailyCheckin();
            dc.setUserId(publisherId);
            dc.setCheckinDate(LocalDate.now().minusDays(30 - i));
            dc.setPointsAwarded(5);
            dc.setStreak(i);
            dailyCheckinMapper.insert(dc);
        }
        boolean awarded = badgeService.checkAndAward(publisherId, "CHECKIN_30");
        assertTrue(awarded);
    }

    @Test
    @DisplayName("CHECKIN_30 — not awarded under streak 30")
    void CHECKIN_30_notAwardedUnder30() {
        DailyCheckin dc = new DailyCheckin();
        dc.setUserId(publisherId);
        dc.setCheckinDate(LocalDate.now());
        dc.setPointsAwarded(5);
        dc.setStreak(5);
        dailyCheckinMapper.insert(dc);

        boolean awarded = badgeService.checkAndAward(publisherId, "CHECKIN_30");
        assertFalse(awarded);
    }

    // ── HELPER ──

    @Test
    @DisplayName("HELPER — auto-awarded on 5th donation completion")
    void HELPER_autoAwardedOn5th() {
        for (int i = 0; i < 5; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("donation " + i);
            req.setDescription("test");
            req.setRewardType("donation");
            req.setRewardAmount(0);
            DemandResponse r = demandService.publish(publisherId, req);
            demandService.accept(r.getDemandId(), acceptorId);
            demandService.complete(r.getDemandId(), publisherId);
        }
        // complete() hook auto-awards HELPER when 5 donation completes reached
        assertTrue(hasBadge(acceptorId, "HELPER"));
    }

    @Test
    @DisplayName("HELPER — not awarded with only 4 donations")
    void HELPER_notAwardedWith4() {
        for (int i = 0; i < 4; i++) {
            CreateDemandRequest req = new CreateDemandRequest();
            req.setType("errand");
            req.setTitle("donation " + i);
            req.setDescription("test");
            req.setRewardType("donation");
            req.setRewardAmount(0);
            DemandResponse r = demandService.publish(publisherId, req);
            demandService.accept(r.getDemandId(), acceptorId);
            demandService.complete(r.getDemandId(), publisherId);
        }
        assertFalse(hasBadge(acceptorId, "HELPER"));
    }

    // ── FIRST_REPORT_SUCCESS ──

    @Test
    @DisplayName("FIRST_REPORT_SUCCESS — awarded for RESOLVED report")
    void FIRST_REPORT_SUCCESS_awardedForResolved() {
        Report report = new Report();
        report.setReporterId(publisherId);
        report.setTargetType("USER");
        report.setTargetId(acceptorId);
        report.setReason("SPAM");
        report.setStatus("RESOLVED");
        reportMapper.insert(report);

        boolean awarded = badgeService.checkAndAward(publisherId, "FIRST_REPORT_SUCCESS");
        assertTrue(awarded);
    }

    @Test
    @DisplayName("FIRST_REPORT_SUCCESS — not awarded for DISMISSED")
    void FIRST_REPORT_SUCCESS_notAwardedForDismissed() {
        Report report = new Report();
        report.setReporterId(publisherId);
        report.setTargetType("USER");
        report.setTargetId(acceptorId);
        report.setReason("SPAM");
        report.setStatus("DISMISSED");
        reportMapper.insert(report);

        boolean awarded = badgeService.checkAndAward(publisherId, "FIRST_REPORT_SUCCESS");
        assertFalse(awarded);
    }

    // ── EASTER_EGG ──

    @Test
    @DisplayName("EASTER_EGG — awarded via awardEasterEgg")
    void EASTER_EGG_awarded() {
        badgeService.awardEasterEgg(publisherId);
        assertTrue(hasBadge(publisherId, "EASTER_EGG"));
    }

    @Test
    @DisplayName("EASTER_EGG — idempotent")
    void EASTER_EGG_idempotent() {
        badgeService.awardEasterEgg(publisherId);
        badgeService.awardEasterEgg(publisherId);
        long count = userBadgeMapper.selectCount(new LambdaQueryWrapper<UserBadge>()
                .eq(UserBadge::getUserId, publisherId)
                .eq(UserBadge::getBadgeKey, "EASTER_EGG"));
        assertEquals(1, count);
    }

    // ── getUserBadges ──

    @Test
    @DisplayName("getUserBadges — returns all 9 badges")
    void getUserBadges_returnsAll9() {
        List<BadgeResponse> badges = badgeService.getUserBadges(publisherId);
        assertEquals(9, badges.size());
        for (BadgeDefinition def : BadgeDefinition.values()) {
            assertTrue(badges.stream().anyMatch(b -> b.getBadgeKey().equals(def.getKey())));
        }
    }

    @Test
    @DisplayName("getUserBadges — earned badge has progress=null")
    void getUserBadges_earnedHasNullProgress() {
        demandService.publish(publisherId, createDemandReq("progress test"));
        // FIRST_PUBLISH was auto-awarded by publish()
        List<BadgeResponse> badges = badgeService.getUserBadges(publisherId);
        BadgeResponse first = badges.stream()
                .filter(b -> b.getBadgeKey().equals("FIRST_PUBLISH")).findFirst().orElseThrow();
        assertTrue(first.isEarned());
        assertNull(first.getProgress());
    }

    @Test
    @DisplayName("getUserBadges — unearned shows progress")
    void getUserBadges_unearnedShowsProgress() {
        // Publisher has 0 published demands in this test (setUp doesn't publish)
        List<BadgeResponse> badges = badgeService.getUserBadges(publisherId);
        BadgeResponse first = badges.stream()
                .filter(b -> b.getBadgeKey().equals("FIRST_PUBLISH")).findFirst().orElseThrow();
        assertFalse(first.isEarned());
        assertEquals("0/1", first.getProgress());
    }

    @Test
    @DisplayName("getUserBadges — EASTER_EGG shows ??? progress when unearned")
    void getUserBadges_easterEggHidden() {
        List<BadgeResponse> badges = badgeService.getUserBadges(publisherId);
        BadgeResponse egg = badges.stream()
                .filter(b -> b.getBadgeKey().equals("EASTER_EGG")).findFirst().orElseThrow();
        assertTrue(egg.isHiddenRequirement());
        assertFalse(egg.isEarned());
        assertEquals("???", egg.getProgress());
    }

    // ── Wear / Unwear ──

    @Test
    @DisplayName("wearBadge — sets worn badge via service")
    void wearBadge_setsWorn() {
        demandService.publish(publisherId, createDemandReq("wear1"));
        // FIRST_PUBLISH auto-awarded; wear it
        badgeService.wearBadge(publisherId, "FIRST_PUBLISH");
        assertEquals("FIRST_PUBLISH", badgeService.getWornBadgeKey(publisherId));
    }

    @Test
    @DisplayName("wearBadge — auto-unwears previous")
    void wearBadge_autoUnwearsPrevious() {
        // Award FIRST_PUBLISH by publishing
        demandService.publish(publisherId, createDemandReq("wear-unwear1"));
        badgeService.wearBadge(publisherId, "FIRST_PUBLISH");
        assertEquals("FIRST_PUBLISH", badgeService.getWornBadgeKey(publisherId));

        // Award EASTER_EGG and wear it — should replace FIRST_PUBLISH
        badgeService.awardEasterEgg(publisherId);
        badgeService.wearBadge(publisherId, "EASTER_EGG");
        assertEquals("EASTER_EGG", badgeService.getWornBadgeKey(publisherId));
    }

    @Test
    @DisplayName("wearBadge — throws for unearned badge")
    void wearBadge_throwsForUnearned() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> badgeService.wearBadge(publisherId, "FIRST_ACCEPT"));
        assertEquals(400, ex.getCode());
    }

    @Test
    @DisplayName("unwearBadge — removes worn badge")
    void unwearBadge_removes() {
        demandService.publish(publisherId, createDemandReq("wear-remove"));
        badgeService.wearBadge(publisherId, "FIRST_PUBLISH");
        badgeService.unwearBadge(publisherId);
        assertNull(badgeService.getWornBadgeKey(publisherId));
    }

    @Test
    @DisplayName("unwearBadge — idempotent when nothing worn")
    void unwearBadge_idempotent() {
        badgeService.unwearBadge(publisherId);
        assertNull(badgeService.getWornBadgeKey(publisherId));
    }

    // ── Utility helpers ──

    private CreateDemandRequest createDemandReq(String title) {
        CreateDemandRequest req = new CreateDemandRequest();
        req.setType("errand");
        req.setTitle(title);
        req.setDescription("test");
        req.setRewardType("point");
        req.setRewardAmount(0);
        return req;
    }

    private void insertEvaluation(Long demandId, Long targetUserId, int rating) {
        Evaluation e = new Evaluation();
        e.setDemandId(demandId);
        e.setEvaluatorId(publisherId);
        e.setTargetUserId(targetUserId);
        e.setRating(rating);
        evaluationMapper.insert(e);
    }
}
