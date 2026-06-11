package cn.seecoder.campushelp.entity.enums;

/**
 * Reward type constants — replaces scattered string literals.
 */
public final class RewardType {
    private RewardType() {}

    public static final String POINT = "point";

    /** @deprecated Cash is being phased out; historical data only. */
    @Deprecated
    public static final String CASH = "cash";

    public static final String DONATION = "donation";
}
