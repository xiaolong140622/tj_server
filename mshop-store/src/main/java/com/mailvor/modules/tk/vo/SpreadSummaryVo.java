package com.mailvor.modules.tk.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 推广中心汇总：好友数/归因订单数/累计佣金
 */
@Data
public class SpreadSummaryVo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 有效好友数（直推一级人数） */
    private Long peopleCount;

    /** 归因订单数（本人+直推下级有效订单，跨6平台） */
    private Long orderCount;

    /** 累计佣金（与 /user/reward/summary totalReward 同源） */
    private BigDecimal commission;
}
