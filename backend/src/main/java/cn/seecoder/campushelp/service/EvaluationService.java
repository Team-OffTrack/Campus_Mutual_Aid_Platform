package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.CreateEvaluationRequest;
import cn.seecoder.campushelp.dto.EvaluationResponse;

import java.util.List;

/**
 * Bidirectional evaluation system for completed demands.
 * Each participant (publisher & acceptor) can submit one rating per demand.
 * Submitting an evaluation notifies the other party and recalculates their reputation score.
 */
public interface EvaluationService {

    /** Create a new evaluation. The other participant is notified. */
    EvaluationResponse create(Long evaluatorId, CreateEvaluationRequest request);

    /** Update an existing evaluation (rating and/or comment). */
    EvaluationResponse update(Long evaluationId, Long evaluatorId, CreateEvaluationRequest request);

    /** Get all evaluations for a demand (ordered by create time ascending, like comments). */
    List<EvaluationResponse> getByDemand(Long demandId);

    /** Get the current user's evaluation for a specific demand, or null. */
    EvaluationResponse getMine(Long demandId, Long evaluatorId);

    /** Get all evaluations received by a user. */
    List<EvaluationResponse> getByUser(Long userId);
}
