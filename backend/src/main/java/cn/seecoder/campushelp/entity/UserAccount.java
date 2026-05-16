package cn.seecoder.campushelp.entity;

import com.baomidou.mybatisplus.annotation.*;

@TableName("user_account")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long accountId;

    private Long userId;
    private Integer availablePoints;
    private Integer frozenPoints;
    private Double reputationScore;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Integer getAvailablePoints() { return availablePoints; }
    public void setAvailablePoints(Integer availablePoints) { this.availablePoints = availablePoints; }

    public Integer getFrozenPoints() { return frozenPoints; }
    public void setFrozenPoints(Integer frozenPoints) { this.frozenPoints = frozenPoints; }

    public Double getReputationScore() { return reputationScore; }
    public void setReputationScore(Double reputationScore) { this.reputationScore = reputationScore; }
}
