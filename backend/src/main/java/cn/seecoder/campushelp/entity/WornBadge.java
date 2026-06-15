package cn.seecoder.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * Tracks the badge a user is currently wearing on their avatar.
 * One badge per user (unique constraint on user_id).
 */
@TableName("worn_badge")
public class WornBadge {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String badgeKey;

    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime wornAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getBadgeKey() { return badgeKey; }
    public void setBadgeKey(String badgeKey) { this.badgeKey = badgeKey; }

    public LocalDateTime getWornAt() { return wornAt; }
    public void setWornAt(LocalDateTime wornAt) { this.wornAt = wornAt; }
}
