package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.CreateEvaluationRequest;
import cn.seecoder.campushelp.dto.EvaluationResponse;
import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.Evaluation;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.mapper.DemandMapper;
import cn.seecoder.campushelp.mapper.EvaluationMapper;
import cn.seecoder.campushelp.mapper.UserAccountMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.service.EvaluationService;
import cn.seecoder.campushelp.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    private final UserAccountMapper userAccountMapper;
    private final NotificationService notificationService;

    public EvaluationServiceImpl(EvaluationMapper evaluationMapper, DemandMapper demandMapper,
                                  UserMapper userMapper, UserAccountMapper userAccountMapper,
                                  NotificationService notificationService) {
        this.evaluationMapper = evaluationMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.userAccountMapper = userAccountMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public EvaluationResponse create(Long evaluatorId, CreateEvaluationRequest request) {
        Demand demand = demandMapper.selectById(request.getDemandId());
        if (demand == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "需求不存在");
        }
        if (!"COMPLETED".equals(demand.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "只能评价已完成的需求");
        }

        Long publisherId = demand.getPublisherId();
        Long acceptorId = demand.getAcceptorId();
        if (!evaluatorId.equals(publisherId) && !evaluatorId.equals(acceptorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只有需求参与方可以评价");
        }

        // Check duplicate: one evaluation per evaluator per demand
        Long count = evaluationMapper.selectCount(new LambdaQueryWrapper<Evaluation>()
                .eq(Evaluation::getDemandId, request.getDemandId())
                .eq(Evaluation::getEvaluatorId, evaluatorId));
        if (count > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "您已经评价过该需求，请通过修改评价更新评分");
        }

        // Target is the other participant
        Long targetId = evaluatorId.equals(publisherId) ? acceptorId : publisherId;

        Evaluation e = new Evaluation();
        e.setDemandId(request.getDemandId());
        e.setEvaluatorId(evaluatorId);
        e.setTargetUserId(targetId);
        e.setRating(request.getRating());
        e.setComment(request.getComment());
        evaluationMapper.insert(e);

        User evaluator = userMapper.selectById(evaluatorId);
        if (evaluator == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        notificationService.notifyEvaluationReceived(
                targetId, demand.getTitle(), demand.getDemandId(),
                evaluator.getName(), request.getRating());

        // Recalculate target's reputation
        updateReputationScore(targetId);

        return EvaluationResponse.from(e, evaluator.getName(), evaluator.getAvatar());
    }

    @Override
    @Transactional
    public EvaluationResponse update(Long evaluationId, Long evaluatorId, CreateEvaluationRequest request) {
        Evaluation e = evaluationMapper.selectById(evaluationId);
        if (e == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "评价不存在");
        }
        if (!e.getEvaluatorId().equals(evaluatorId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "只能修改自己的评价");
        }

        e.setRating(request.getRating());
        e.setComment(request.getComment());
        evaluationMapper.updateById(e);

        // Re-notify the other party
        Demand demand = demandMapper.selectById(e.getDemandId());
        User evaluator = userMapper.selectById(evaluatorId);
        if (demand != null && evaluator != null) {
            notificationService.notifyEvaluationUpdated(
                    e.getTargetUserId(), demand.getTitle(), demand.getDemandId(),
                    evaluator.getName(), request.getRating());
        }

        // Recalculate target's reputation
        updateReputationScore(e.getTargetUserId());

        return EvaluationResponse.from(e, evaluator.getName(), evaluator.getAvatar());
    }

    @Override
    public List<EvaluationResponse> getByDemand(Long demandId) {
        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDemandId, demandId)
                        .orderByAsc(Evaluation::getCreateTime));
        return evals.stream()
                .map(e -> {
                    User evaluator = userMapper.selectById(e.getEvaluatorId());
                    return EvaluationResponse.from(e,
                            evaluator != null ? evaluator.getName() : "未知用户",
                            evaluator != null ? evaluator.getAvatar() : null);
                })
                .toList();
    }

    @Override
    public EvaluationResponse getMine(Long demandId, Long evaluatorId) {
        Evaluation e = evaluationMapper.selectOne(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getDemandId, demandId)
                        .eq(Evaluation::getEvaluatorId, evaluatorId));
        if (e == null) return null;
        User evaluator = userMapper.selectById(evaluatorId);
        return EvaluationResponse.from(e,
                evaluator != null ? evaluator.getName() : "未知用户",
                evaluator != null ? evaluator.getAvatar() : null);
    }

    @Override
    public List<EvaluationResponse> getByUser(Long userId) {
        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getTargetUserId, userId)
                        .orderByDesc(Evaluation::getCreateTime));
        return evals.stream()
                .map(e -> {
                    User evaluator = userMapper.selectById(e.getEvaluatorId());
                    return EvaluationResponse.from(e,
                            evaluator != null ? evaluator.getName() : "未知用户",
                            evaluator != null ? evaluator.getAvatar() : null);
                })
                .toList();
    }

    /**
     * Recalculate the user's credit score.
     *
     * Formula: score = 0.6 × avgRating + 0.4 × (completionRate × 5)
     * - avgRating: average of all received evaluation ratings (1–5), defaults to 5.0
     * - completionRate: COMPLETED / (COMPLETED + CANCELLED) as an acceptor, defaults to 1.0
     * - Result rounded to 1 decimal place.
     */
    private void updateReputationScore(Long userId) {
        // 1) Average rating from evaluations
        List<Evaluation> evals = evaluationMapper.selectList(
                new LambdaQueryWrapper<Evaluation>()
                        .eq(Evaluation::getTargetUserId, userId));
        double avgRating = evals.isEmpty() ? 5.0
                : evals.stream().mapToInt(Evaluation::getRating).average().orElse(5.0);

        // 2) Completion rate as acceptor
        long completed = demandMapper.selectCount(
                new LambdaQueryWrapper<Demand>()
                        .eq(Demand::getAcceptorId, userId)
                        .eq(Demand::getStatus, "COMPLETED"));
        long cancelled = demandMapper.selectCount(
                new LambdaQueryWrapper<Demand>()
                        .eq(Demand::getAcceptorId, userId)
                        .eq(Demand::getStatus, "CANCELLED"));
        long concluded = completed + cancelled;
        double completionRate = concluded > 0 ? (double) completed / concluded : 1.0;

        // 3) Weighted credit score
        double score = 0.6 * avgRating + 0.4 * (completionRate * 5.0);
        score = Math.round(score * 10.0) / 10.0;

        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId));
        if (account != null) {
            account.setReputationScore(score);
            userAccountMapper.updateById(account);
        }
    }
}
