package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EvaluationServiceTest {

    @Autowired private EvaluationService evaluationService;
    @Autowired private DemandService demandService;
    @Autowired private UserService userService;
    @Autowired private UserMapper userMapper;
    @Autowired private UserAccountMapper userAccountMapper;
    @Autowired private DemandMapper demandMapper;
    @Autowired private EvaluationMapper evaluationMapper;

    private Long publisherId;
    private Long acceptorId;
    private Long completedDemandId;

    @BeforeEach
    void setUp() {
        evaluationMapper.delete(new LambdaQueryWrapper<>());
        demandMapper.delete(new LambdaQueryWrapper<>());
        userMapper.delete(new LambdaQueryWrapper<>());

        // Publisher
        RegisterRequest r1 = new RegisterRequest();
        r1.setStudentId("pub001"); r1.setPassword("pass123"); r1.setName("Publisher");
        userService.register(r1);
        publisherId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "pub001")).getUserId();

        // Acceptor
        RegisterRequest r2 = new RegisterRequest();
        r2.setStudentId("acc001"); r2.setPassword("pass123"); r2.setName("Acceptor");
        userService.register(r2);
        acceptorId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "acc001")).getUserId();

        // Create and complete a demand
        CreateDemandRequest dReq = new CreateDemandRequest();
        dReq.setType("errand"); dReq.setTitle("Test Eval"); dReq.setDescription("test");
        DemandResponse created = demandService.publish(publisherId, dReq);
        demandService.accept(created.getDemandId(), acceptorId);
        demandService.complete(created.getDemandId(), publisherId);
        completedDemandId = created.getDemandId();
    }

    @Test
    @DisplayName("Publisher rates acceptor after completion")
    void publisherRateAcceptor_shouldPersist() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId);
        req.setRating(4);
        req.setComment("Reliable");

        EvaluationResponse rsp = evaluationService.create(publisherId, req);
        assertNotNull(rsp.getEvaluationId());
        assertEquals(4, rsp.getRating());
        assertEquals(acceptorId, rsp.getTargetUserId());
        assertEquals("Publisher", rsp.getEvaluatorName());
    }

    @Test
    @DisplayName("Acceptor rates publisher after completion")
    void acceptorRatePublisher_shouldPersist() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId);
        req.setRating(5);
        req.setComment("Great publisher");

        EvaluationResponse rsp = evaluationService.create(acceptorId, req);
        assertEquals(5, rsp.getRating());
        assertEquals(publisherId, rsp.getTargetUserId());
        assertEquals("Acceptor", rsp.getEvaluatorName());
    }

    @Test
    @DisplayName("Duplicate evaluation throws CONFLICT with helpful message")
    void duplicateEvaluation_shouldThrowConflict() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId);
        req.setRating(4);
        evaluationService.create(publisherId, req);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.create(publisherId, req));
        assertEquals(409, ex.getCode());
        assertTrue(ex.getMessage().contains("修改评价"));
    }

    @Test
    @DisplayName("Evaluating non-completed demand throws")
    void evaluateOpenDemand_shouldThrow() {
        CreateDemandRequest dReq = new CreateDemandRequest();
        dReq.setType("errand"); dReq.setTitle("Open"); dReq.setDescription("test");
        DemandResponse open = demandService.publish(publisherId, dReq);

        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(open.getDemandId());
        req.setRating(3);

        assertThrows(BusinessException.class,
                () -> evaluationService.create(publisherId, req));
    }

    @Test
    @DisplayName("Non-participant cannot evaluate")
    void nonParticipant_shouldThrow() {
        RegisterRequest r3 = new RegisterRequest();
        r3.setStudentId("outsider"); r3.setPassword("pass123"); r3.setName("Outsider");
        userService.register(r3);
        Long outsiderId = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "outsider")).getUserId();

        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId);
        req.setRating(3);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.create(outsiderId, req));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Rating updates target user reputation score")
    void create_updatesReputationScore() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId);
        req.setRating(5);
        evaluationService.create(publisherId, req);

        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        assertEquals(5.0, account.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("Multiple ratings produce average reputation")
    void multipleRatings_shouldAverage() {
        // Create and complete second demand
        CreateDemandRequest d2 = new CreateDemandRequest();
        d2.setType("trade"); d2.setTitle("Second"); d2.setDescription("test");
        DemandResponse created2 = demandService.publish(publisherId, d2);
        demandService.accept(created2.getDemandId(), acceptorId);
        demandService.complete(created2.getDemandId(), publisherId);

        CreateEvaluationRequest r1 = new CreateEvaluationRequest();
        r1.setDemandId(completedDemandId); r1.setRating(2);
        evaluationService.create(publisherId, r1);

        CreateEvaluationRequest r2 = new CreateEvaluationRequest();
        r2.setDemandId(created2.getDemandId()); r2.setRating(4);
        evaluationService.create(publisherId, r2);

        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        // Formula: 0.6*3.0 + 0.4*(1.0*5) = 1.8 + 2.0 = 3.8
        assertEquals(3.8, account.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("getByDemand returns both evaluations ordered by time")
    void getByDemand_shouldReturnBoth() {
        CreateEvaluationRequest r1 = new CreateEvaluationRequest();
        r1.setDemandId(completedDemandId); r1.setRating(5);
        evaluationService.create(publisherId, r1);

        CreateEvaluationRequest r2 = new CreateEvaluationRequest();
        r2.setDemandId(completedDemandId); r2.setRating(4);
        evaluationService.create(acceptorId, r2);

        List<EvaluationResponse> evals = evaluationService.getByDemand(completedDemandId);
        assertEquals(2, evals.size());
        // Ordered by create time ascending
        assertTrue(evals.get(0).getCreateTime().compareTo(evals.get(1).getCreateTime()) <= 0);
    }

    @Test
    @DisplayName("getByUser returns evaluations received by a user")
    void getByUser_shouldReturnReceived() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId); req.setRating(5);
        evaluationService.create(publisherId, req);

        List<EvaluationResponse> evals = evaluationService.getByUser(acceptorId);
        assertEquals(1, evals.size());
        assertEquals(5, evals.get(0).getRating());
    }

    @Test
    @DisplayName("getByUser with no ratings returns empty")
    void getByUser_noRatings_shouldReturnEmpty() {
        List<EvaluationResponse> evals = evaluationService.getByUser(publisherId);
        assertTrue(evals.isEmpty());
    }

    @Test
    @DisplayName("getMine returns existing evaluation for demand")
    void getMine_shouldReturnExisting() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId); req.setRating(4);
        evaluationService.create(publisherId, req);

        EvaluationResponse mine = evaluationService.getMine(completedDemandId, publisherId);
        assertNotNull(mine);
        assertEquals(4, mine.getRating());
        assertEquals(publisherId, mine.getEvaluatorId());
    }

    @Test
    @DisplayName("getMine returns null when not rated")
    void getMine_notRated_shouldReturnNull() {
        EvaluationResponse mine = evaluationService.getMine(completedDemandId, publisherId);
        assertNull(mine);
    }

    @Test
    @DisplayName("Update evaluation changes rating and comment")
    void updateEvaluation_shouldModify() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId); req.setRating(3); req.setComment("ok");
        EvaluationResponse created = evaluationService.create(publisherId, req);

        CreateEvaluationRequest update = new CreateEvaluationRequest();
        update.setDemandId(completedDemandId); update.setRating(5); update.setComment("excellent");
        EvaluationResponse updated = evaluationService.update(created.getEvaluationId(), publisherId, update);

        assertEquals(5, updated.getRating());
        assertEquals("excellent", updated.getComment());

        // Verify reputation was recalculated
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        assertEquals(5.0, account.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("Update by non-owner throws FORBIDDEN")
    void updateByNonOwner_shouldThrow() {
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId); req.setRating(3);
        EvaluationResponse created = evaluationService.create(publisherId, req);

        CreateEvaluationRequest update = new CreateEvaluationRequest();
        update.setDemandId(completedDemandId); update.setRating(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> evaluationService.update(created.getEvaluationId(), acceptorId, update));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("Reputation score defaults to 5.0 for user with no ratings")
    void reputation_noRatings_shouldDefault() {
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, publisherId));
        assertEquals(5.0, account.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("Reputation score recalculated after each evaluation")
    void reputation_recalc_afterEachEval() {
        CreateEvaluationRequest r1 = new CreateEvaluationRequest();
        r1.setDemandId(completedDemandId); r1.setRating(3);
        evaluationService.create(publisherId, r1);

        UserAccount acc = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        // 0.6*3.0 + 0.4*(1.0*5) = 1.8 + 2.0 = 3.8
        assertEquals(3.8, acc.getReputationScore(), 0.1);

        // Complete another demand and rate again
        CreateDemandRequest d2 = new CreateDemandRequest();
        d2.setType("trade"); d2.setTitle("Third"); d2.setDescription("test");
        DemandResponse c2 = demandService.publish(publisherId, d2);
        demandService.accept(c2.getDemandId(), acceptorId);
        demandService.complete(c2.getDemandId(), publisherId);

        CreateEvaluationRequest r2 = new CreateEvaluationRequest();
        r2.setDemandId(c2.getDemandId()); r2.setRating(5);
        evaluationService.create(publisherId, r2);

        acc = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        // 0.6*4.0 + 0.4*(1.0*5) = 2.4 + 2.0 = 4.4
        assertEquals(4.4, acc.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("Low completion rate reduces credit score")
    void lowCompletionRate_reducesScore() {
        // Register a new acceptor with mixed track record
        RegisterRequest r = new RegisterRequest();
        r.setStudentId("spotty001"); r.setPassword("pass123"); r.setName("Spotty");
        userService.register(r);
        User spotty = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "spotty001"));

        // Demand 1: accepted and completed
        CreateDemandRequest d1 = new CreateDemandRequest();
        d1.setType("errand"); d1.setTitle("Task A"); d1.setDescription("test");
        DemandResponse rsp1 = demandService.publish(publisherId, d1);
        demandService.accept(rsp1.getDemandId(), spotty.getUserId());
        demandService.complete(rsp1.getDemandId(), publisherId);

        // Demand 2: accepted then cancelled by publisher
        CreateDemandRequest d2 = new CreateDemandRequest();
        d2.setType("errand"); d2.setTitle("Task B"); d2.setDescription("test");
        DemandResponse rsp2 = demandService.publish(publisherId, d2);
        demandService.accept(rsp2.getDemandId(), spotty.getUserId());
        demandService.cancel(rsp2.getDemandId(), publisherId);

        // Rate spotty 5 stars
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(rsp1.getDemandId()); req.setRating(5);
        evaluationService.create(publisherId, req);

        // completionRate = 1/(1+1) = 0.5
        // score = 0.6*5.0 + 0.4*(0.5*5) = 3.0 + 1.0 = 4.0
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, spotty.getUserId()));
        assertEquals(4.0, account.getReputationScore(), 0.1);
    }

    @Test
    @DisplayName("User with zero concluded demands defaults to completion rate 1.0")
    void zeroConcludedDemands_defaultsToPerfectCompletion() {
        // Fresh user with no accepted demands — rate them
        CreateEvaluationRequest req = new CreateEvaluationRequest();
        req.setDemandId(completedDemandId); req.setRating(4);
        evaluationService.create(publisherId, req);

        // acceptor has 1 completed, 0 cancelled → rate = 1.0
        // score = 0.6*4.0 + 0.4*5.0 = 2.4 + 2.0 = 4.4
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, acceptorId));
        assertEquals(4.4, account.getReputationScore(), 0.1);
    }
}
