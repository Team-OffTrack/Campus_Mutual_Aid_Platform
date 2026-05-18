package cn.seecoder.campushelp.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * Request body for publishing a new demand.
 * Only type, title, and description are required; other fields use sensible defaults.
 */
public class CreateDemandRequest {

    @NotBlank(message = "类型不能为空")
    private String type;

    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "描述不能为空")
    private String description;

    private String location;
    private LocalDateTime deadline;
    private String rewardType;
    private Integer rewardAmount;
    private Boolean isAnonymous;
    private String images;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }

    public String getRewardType() { return rewardType; }
    public void setRewardType(String rewardType) { this.rewardType = rewardType; }

    public Integer getRewardAmount() { return rewardAmount; }
    public void setRewardAmount(Integer rewardAmount) { this.rewardAmount = rewardAmount; }

    public Boolean getIsAnonymous() { return isAnonymous; }
    public void setIsAnonymous(Boolean isAnonymous) { this.isAnonymous = isAnonymous; }

    public String getImages() { return images; }
    public void setImages(String images) { this.images = images; }
}
