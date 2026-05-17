package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.Evaluation;

import java.time.LocalDateTime;

/**
 * Evaluation response with evaluator name for display in comment sections.
 */
public class EvaluationResponse {

    private Long evaluationId;
    private Long demandId;
    private Long evaluatorId;
    private String evaluatorName;
    private String evaluatorAvatar;
    private Long targetUserId;
    private Integer rating;
    private String comment;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static EvaluationResponse from(Evaluation e, String evaluatorName, String evaluatorAvatar) {
        EvaluationResponse r = new EvaluationResponse();
        r.evaluationId = e.getEvaluationId();
        r.demandId = e.getDemandId();
        r.evaluatorId = e.getEvaluatorId();
        r.evaluatorName = evaluatorName;
        r.evaluatorAvatar = evaluatorAvatar;
        r.targetUserId = e.getTargetUserId();
        r.rating = e.getRating();
        r.comment = e.getComment();
        r.createTime = e.getCreateTime();
        r.updateTime = e.getUpdateTime();
        return r;
    }

    public Long getEvaluationId() { return evaluationId; }
    public Long getDemandId() { return demandId; }
    public Long getEvaluatorId() { return evaluatorId; }
    public String getEvaluatorName() { return evaluatorName; }
    public String getEvaluatorAvatar() { return evaluatorAvatar; }
    public Long getTargetUserId() { return targetUserId; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
}
