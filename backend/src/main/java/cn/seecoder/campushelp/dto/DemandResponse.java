package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.Demand;
import cn.seecoder.campushelp.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private String images;
    private String status;
    private Map<String, Object> attributes;
    private List<TeamMemberResponse> teamMembers;
    private Integer joinedCount;
    private Boolean favorited;
    private LocalDateTime createTime;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Lightweight constructor used by list queries (only publisher loaded). */
    public static DemandResponse from(Demand demand, User publisher) {
        return from(demand, publisher, null, null, null);
    }

    /** Full constructor with optional acceptor info for detail views. */
    public static DemandResponse from(Demand demand, User publisher, User acceptor) {
        return from(demand, publisher, acceptor, null, null);
    }

    /** Full constructor with optional team member info for team-type demands. */
    public static DemandResponse from(Demand demand, User publisher, User acceptor,
                                       List<TeamMemberResponse> teamMembers) {
        return from(demand, publisher, acceptor, teamMembers, null);
    }

    /** Master constructor with optional favoritedIds for batch favorited status. */
    public static DemandResponse from(Demand demand, User publisher, User acceptor,
                                       List<TeamMemberResponse> teamMembers,
                                       Set<Long> favoritedIds) {
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
        rsp.images = demand.getImages();
        rsp.status = demand.getStatus();
        rsp.attributes = parseAttributes(demand.getAttributes());
        rsp.teamMembers = teamMembers;
        rsp.favorited = favoritedIds != null && favoritedIds.contains(demand.getDemandId());
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

    private static Map<String, Object> parseAttributes(String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(attributesJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
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
    public String getImages() { return images; }
    public String getStatus() { return status; }
    public Map<String, Object> getAttributes() { return attributes; }
    public void setAttributes(Map<String, Object> attributes) { this.attributes = attributes; }
    public List<TeamMemberResponse> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<TeamMemberResponse> teamMembers) { this.teamMembers = teamMembers; }
    public Integer getJoinedCount() { return joinedCount; }
    public void setJoinedCount(Integer joinedCount) { this.joinedCount = joinedCount; }
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public LocalDateTime getCreateTime() { return createTime; }
}
