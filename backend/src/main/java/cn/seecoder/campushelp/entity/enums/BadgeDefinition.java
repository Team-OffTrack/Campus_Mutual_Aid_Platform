package cn.seecoder.campushelp.entity.enums;

/**
 * Badge metadata — single source of truth for all 9 achievement badges.
 * Each badge defines its key, display name, emoji icon, description,
 * target value for progress tracking, and whether the requirement
 * should be hidden from the user (only EASTER_EGG).
 */
public enum BadgeDefinition {

    FIRST_PUBLISH("FIRST_PUBLISH", "首次发布", "🎉",
            "发布第一个需求", 1, false),

    FIRST_ACCEPT("FIRST_ACCEPT", "首次接单", "🤝",
            "接受第一个需求", 1, false),

    TEN_COMPLETES("TEN_COMPLETES", "十全十美", "🏆",
            "完成10个需求（作为发布者或接单人）", 10, false),

    FIRST_FIVE_STAR("FIRST_FIVE_STAR", "五星好评", "⭐",
            "首次获得5星评价", 1, false),

    HUNDRED_STARS("HUNDRED_STARS", "百星好评", "💯",
            "累计获得100颗星的总评分", 100, false),

    CHECKIN_30("CHECKIN_30", "签到达人", "🔥",
            "连续签到30天", 30, false),

    HELPER("HELPER", "乐于助人", "💝",
            "完成5个捐赠类型需求（作为发布者或接单人）", 5, false),

    FIRST_REPORT_SUCCESS("FIRST_REPORT_SUCCESS", "正义使者", "🛡️",
            "首次举报被管理员确认处理", 1, false),

    EASTER_EGG("EASTER_EGG", "彩蛋猎人", "🐱",
            "触发一个神秘的隐藏彩蛋", 1, true);

    private final String key;
    private final String displayName;
    private final String emoji;
    private final String description;
    private final int targetValue;
    private final boolean hiddenRequirement;

    BadgeDefinition(String key, String displayName, String emoji,
                    String description, int targetValue, boolean hiddenRequirement) {
        this.key = key;
        this.displayName = displayName;
        this.emoji = emoji;
        this.description = description;
        this.targetValue = targetValue;
        this.hiddenRequirement = hiddenRequirement;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public String getEmoji() { return emoji; }
    public String getDescription() { return description; }
    public int getTargetValue() { return targetValue; }
    public boolean isHiddenRequirement() { return hiddenRequirement; }

    /** Look up a definition by its key, or null if not found. */
    public static BadgeDefinition fromKey(String key) {
        for (BadgeDefinition def : values()) {
            if (def.key.equals(key)) return def;
        }
        return null;
    }
}
