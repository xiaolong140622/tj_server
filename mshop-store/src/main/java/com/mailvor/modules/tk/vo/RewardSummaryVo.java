package com.mailvor.modules.tk.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户奖励汇总：累计奖励/待结算/已到账
 */
@Data
public class RewardSummaryVo {

    /** 累计奖励：有效佣金流水总和（含已提现） */
    private BigDecimal totalReward;

    /** 待结算：各平台有效且未结算订单预估佣金总和 */
    private BigDecimal pendingReward;

    /** 已到账：用户当前可用余额 */
    private BigDecimal settledReward;
}
