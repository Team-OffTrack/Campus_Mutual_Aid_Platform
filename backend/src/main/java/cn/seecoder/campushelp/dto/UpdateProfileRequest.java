package cn.seecoder.campushelp.dto;

public class UpdateProfileRequest {

    private String name;
    private String avatar;
    private Boolean isAnonymous;
    private String maskName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Boolean getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }

    public String getMaskName() { return maskName; }
    public void setMaskName(String maskName) { this.maskName = maskName; }
}
