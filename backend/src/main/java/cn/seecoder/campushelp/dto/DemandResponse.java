package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.User;

import java.time.LocalDateTime;

/**
 * Demand response including publisher and acceptor display info.
 * When isAnonymous is true, the publisher name is replaced with "匿名用户".
 */
public class DemandResponse {

    private Long demandId;
    private Long publisherId;
    private String publisherName;
    private String publisherAvatar;
    private Long acceptorId;
    private String acceptorName;
    private String acceptorAvatar;
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

    /** Lightweight constructor used by list queries (only publisher loaded). */
    public static DemandResponse from(Demand demand, User publisher) {
        return from(demand, publisher, null);
    }

    /** Full constructor with optional acceptor info for detail views. */
    public static DemandResponse from(Demand demand, User publisher, User acceptor) {
        DemandResponse rsp = new DemandResponse();
        rsp.demandId = demand.getDemandId();
        rsp.publisherId = demand.getPublisherId();
        rsp.acceptorId = demand.getAcceptorId();
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

        if (demand.isAnonymous()) {
            rsp.publisherName = "匿名用户";
        } else if (publisher != null) {
            rsp.publisherName = publisher.getName();
            rsp.publisherAvatar = publisher.getAvatar();
        } else {
            rsp.publisherName = "未知用户";
        }

        if (acceptor != null) {
            rsp.acceptorName = acceptor.getName();
            rsp.acceptorAvatar = acceptor.getAvatar();
        }
        return rsp;
    }

    public Long getDemandId() { return demandId; }
    public Long getPublisherId() { return publisherId; }
    public String getPublisherName() { return publisherName; }
    public String getPublisherAvatar() { return publisherAvatar; }
    public Long getAcceptorId() { return acceptorId; }
    public String getAcceptorName() { return acceptorName; }
    public String getAcceptorAvatar() { return acceptorAvatar; }
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
