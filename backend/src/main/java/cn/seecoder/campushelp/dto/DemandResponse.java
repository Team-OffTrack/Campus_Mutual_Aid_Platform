package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.User;

import java.time.LocalDateTime;

/**
 * Demand response including publisher display info.
 * When isAnonymous is true, the publisher name is replaced with the mask name or "匿名用户".
 */
public class DemandResponse {

    private Long demandId;
    private Long publisherId;
    private String publisherName;
    private String type;
    private String title;
    private String description;
    private String location;
    private LocalDateTime deadline;
    private String rewardType;
    private Integer rewardAmount;
    private Boolean isAnonymous;
    private String status;
    private LocalDateTime createTime;

    public static DemandResponse from(Demand demand, User publisher) {
        DemandResponse rsp = new DemandResponse();
        rsp.demandId = demand.getDemandId();
        rsp.publisherId = demand.getPublisherId();
        rsp.type = demand.getType();
        rsp.title = demand.getTitle();
        rsp.description = demand.getDescription();
        rsp.location = demand.getLocation();
        rsp.deadline = demand.getDeadline();
        rsp.rewardType = demand.getRewardType();
        rsp.rewardAmount = demand.getRewardAmount();
        rsp.isAnonymous = demand.isAnonymous();
        rsp.status = demand.getStatus();
        rsp.createTime = demand.getCreateTime();

        // Respect anonymous preference — hide real name
        if (demand.isAnonymous()) {
            rsp.publisherName = "匿名用户";
        } else if (publisher != null) {
            rsp.publisherName = publisher.getName();
        } else {
            rsp.publisherName = "未知用户";
        }
        return rsp;
    }

    public Long getDemandId() { return demandId; }
    public Long getPublisherId() { return publisherId; }
    public String getPublisherName() { return publisherName; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public LocalDateTime getDeadline() { return deadline; }
    public String getRewardType() { return rewardType; }
    public Integer getRewardAmount() { return rewardAmount; }
    public Boolean getIsAnonymous() { return isAnonymous; }
    public String getStatus() { return status; }
    public LocalDateTime getCreateTime() { return createTime; }
}
