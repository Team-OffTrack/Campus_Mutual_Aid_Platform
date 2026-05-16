package cn.seecoder.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

@TableName("notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long notificationId;

    private Long userId;
    private String type;
    private String title;
    private String content;
    private Integer isRead;
    private Long relatedDemandId;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getIsRead() { return isRead; }
    public void setIsRead(Integer isRead) { this.isRead = isRead; }

    public boolean isRead() { return isRead != null && isRead == 1; }

    public Long getRelatedDemandId() { return relatedDemandId; }
    public void setRelatedDemandId(Long relatedDemandId) { this.relatedDemandId = relatedDemandId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
