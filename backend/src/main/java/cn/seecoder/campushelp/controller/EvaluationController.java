package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.CreateEvaluationRequest;
import cn.seecoder.campushelp.dto.EvaluationResponse;
import cn.seecoder.campushelp.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Evaluation endpoints for the bidirectional rating system.
 * Participants rate each other after demand completion; ratings affect reputation scores.
 */
@RestController
@RequestMapping("/api/v1/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    /** Submit a new evaluation. Returns 409 if the user already rated this demand. */
    @PostMapping
    public ApiResult<EvaluationResponse> create(Authentication auth,
                                                 @Valid @RequestBody CreateEvaluationRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(evaluationService.create(userId, request));
    }

    /** Update an existing evaluation (rating and/or comment). */
    @PutMapping("/{evaluationId}")
    public ApiResult<EvaluationResponse> update(Authentication auth,
                                                 @PathVariable Long evaluationId,
                                                 @Valid @RequestBody CreateEvaluationRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(evaluationService.update(evaluationId, userId, request));
    }

    /** Get all evaluations for a demand (ordered like comments). */
    @GetMapping("/demand/{demandId}")
    public ApiResult<List<EvaluationResponse>> getByDemand(@PathVariable Long demandId) {
        return ApiResult.success(evaluationService.getByDemand(demandId));
    }

    /** Get the current user's own evaluation for a demand (null if not rated yet). */
    @GetMapping("/mine")
    public ApiResult<EvaluationResponse> getMine(Authentication auth,
                                                  @RequestParam Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(evaluationService.getMine(demandId, userId));
    }

    /** Get all evaluations received by a user. */
    @GetMapping("/user/{userId}")
    public ApiResult<List<EvaluationResponse>> getByUser(@PathVariable Long userId) {
        return ApiResult.success(evaluationService.getByUser(userId));
    }
}
