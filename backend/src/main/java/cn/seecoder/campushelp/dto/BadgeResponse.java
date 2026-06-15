package cn.seecoder.campushelp.dto;

import cn.seecoder.campushelp.entity.enums.BadgeDefinition;

/**
 * A single badge's state for the current user, including earned status,
 * progress toward the target, and whether it is currently worn.
 */
public class BadgeResponse {

    private String badgeKey;
    private String displayName;
    private String description;
    private String emoji;
    private boolean earned;
    private String progress;
    private boolean wearing;
    private boolean hiddenRequirement;

    /**
     * Build a response for an unearned badge with progress info.
     *
     * @param def      badge metadata
     * @param current  current progress count
     * @param wearing  whether this badge is worn (always false if unearned)
     */
    public static BadgeResponse unearned(BadgeDefinition def, int current, boolean wearing) {
        BadgeResponse rsp = new BadgeResponse();
        rsp.badgeKey = def.getKey();
        rsp.displayName = def.getDisplayName();
        rsp.emoji = def.getEmoji();
        rsp.earned = false;
        rsp.wearing = wearing;
        rsp.hiddenRequirement = def.isHiddenRequirement();

        if (def.isHiddenRequirement()) {
            rsp.description = def.getDescription(); // "触发一个神秘的隐藏彩蛋"
            rsp.progress = "???";
        } else {
            rsp.description = def.getDescription();
            rsp.progress = current + "/" + def.getTargetValue();
        }
        return rsp;
    }

    /**
     * Build a response for an earned badge.
     */
    public static BadgeResponse earned(BadgeDefinition def, boolean wearing) {
        BadgeResponse rsp = new BadgeResponse();
        rsp.badgeKey = def.getKey();
        rsp.displayName = def.getDisplayName();
        rsp.emoji = def.getEmoji();
        rsp.description = def.getDescription();
        rsp.earned = true;
        rsp.wearing = wearing;
        rsp.hiddenRequirement = def.isHiddenRequirement();
        rsp.progress = null; // earned = no progress needed
        return rsp;
    }

    // ── Getters & Setters ──

    public String getBadgeKey() { return badgeKey; }
    public void setBadgeKey(String badgeKey) { this.badgeKey = badgeKey; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public boolean isEarned() { return earned; }
    public void setEarned(boolean earned) { this.earned = earned; }

    public String getProgress() { return progress; }
    public void setProgress(String progress) { this.progress = progress; }

    public boolean isWearing() { return wearing; }
    public void setWearing(boolean wearing) { this.wearing = wearing; }

    public boolean isHiddenRequirement() { return hiddenRequirement; }
    public void setHiddenRequirement(boolean hiddenRequirement) { this.hiddenRequirement = hiddenRequirement; }
}
