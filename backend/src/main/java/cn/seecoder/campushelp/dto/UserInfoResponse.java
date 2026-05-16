package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.PrivacyProfile;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;

public class UserInfoResponse {

    private Long userId;
    private String studentId;
    private String name;
    private String avatar;
    private String role;
    private Integer status;
    private Boolean isAnonymous;
    private String maskName;
    private Integer availablePoints;
    private Integer frozenPoints;
    private Double reputationScore;

    public static UserInfoResponse from(User user, PrivacyProfile profile, UserAccount account) {
        UserInfoResponse rsp = new UserInfoResponse();
        rsp.userId = user.getUserId();
        rsp.studentId = user.getStudentId();
        rsp.name = user.getName();
        rsp.avatar = user.getAvatar();
        rsp.role = user.getRole();
        rsp.status = user.getStatus();
        if (profile != null) {
            rsp.isAnonymous = profile.isAnonymous();
            rsp.maskName = profile.getMaskName();
        }
        if (account != null) {
            rsp.availablePoints = account.getAvailablePoints();
            rsp.frozenPoints = account.getFrozenPoints();
            rsp.reputationScore = account.getReputationScore();
        }
        return rsp;
    }

    public Long getUserId() { return userId; }
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public String getRole() { return role; }
    public Integer getStatus() { return status; }
    public Boolean getIsAnonymous() { return isAnonymous; }
    public String getMaskName() { return maskName; }
    public Integer getAvailablePoints() { return availablePoints; }
    public Integer getFrozenPoints() { return frozenPoints; }
    public Double getReputationScore() { return reputationScore; }
}
