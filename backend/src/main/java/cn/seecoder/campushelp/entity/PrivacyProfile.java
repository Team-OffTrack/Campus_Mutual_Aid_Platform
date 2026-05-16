package cn.seecoder.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("privacy_profile")
public class PrivacyProfile {

    @TableId(type = IdType.AUTO)
    private Long privacyId;

    private Long userId;
    private Integer isAnonymous;
    private String maskName;

    public Long getPrivacyId() { return privacyId; }
    public void setPrivacyId(Long privacyId) { this.privacyId = privacyId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Integer isAnonymous) { this.isAnonymous = isAnonymous; }

    public boolean isAnonymous() { return isAnonymous != null && isAnonymous == 1; }

    public String getMaskName() { return maskName; }
    public void setMaskName(String maskName) { this.maskName = maskName; }
}
