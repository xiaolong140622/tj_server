package com.mailvor.modules.tk.service;

import com.mailvor.modules.tk.vo.RewardSummaryVo;
import com.mailvor.modules.tk.vo.SpreadSummaryVo;

import java.math.BigDecimal;

/**
 * 用户奖励汇总服务
 */
public interface UserRewardSummaryService {

    /**
     * 获取用户奖励三值汇总
     *
     * @param uid       用户id
     * @param nowMoney  当前可用余额（已到账）
     * @return RewardSummaryVo
     */
    RewardSummaryVo getSummary(Long uid, BigDecimal nowMoney);

    /**
     * 推广中心三值汇总：有效好友数/归因订单数/累计佣金
     *
     * @param uid 用户id
     * @return SpreadSummaryVo
     */
    SpreadSummaryVo getSpreadSummary(Long uid);
}
