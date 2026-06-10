package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.TeamMember;
import cn.seecoder.campushelp.entity.User;

import java.time.LocalDateTime;

public class TeamMemberResponse {

    private Long id;
    private Long demandId;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String role;
    private String status;
    private String message;
    private LocalDateTime createTime;

    public static TeamMemberResponse from(TeamMember member, User user) {
        TeamMemberResponse rsp = new TeamMemberResponse();
        rsp.id = member.getId();
        rsp.demandId = member.getDemandId();
        rsp.userId = member.getUserId();
        rsp.userName = user != null ? user.getName() : "未知用户";
        rsp.userAvatar = user != null ? user.getAvatar() : null;
        rsp.role = member.getRole();
        rsp.status = member.getStatus();
        rsp.message = member.getMessage();
        rsp.createTime = member.getCreateTime();
        return rsp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandId() { return demandId; }
    public void setDemandId(Long demandId) { this.demandId = demandId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatar() { return userAvatar; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
