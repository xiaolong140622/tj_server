package com.mailvor.modules.tk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mailvor.enums.CommonEnum;
import com.mailvor.modules.tk.domain.MailvorDyOrder;
import com.mailvor.modules.tk.domain.MailvorJdOrder;
import com.mailvor.modules.tk.domain.MailvorPddOrder;
import com.mailvor.modules.tk.domain.MailvorTbOrder;
import com.mailvor.modules.tk.domain.MailvorVipOrder;
import com.mailvor.modules.tk.service.UserRewardSummaryService;
import com.mailvor.modules.tk.service.mapper.MailvorDyOrderMapper;
import com.mailvor.modules.tk.service.mapper.MailvorJdOrderMapper;
import com.mailvor.modules.tk.service.mapper.MailvorPddOrderMapper;
import com.mailvor.modules.tk.service.mapper.MailvorTbOrderMapper;
import com.mailvor.modules.tk.service.mapper.MailvorVipOrderMapper;
import com.mailvor.modules.tk.vo.RewardSummaryVo;
import com.mailvor.modules.user.service.mapper.UserBillMapper;
import com.mailvor.utils.OrderUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * 用户奖励汇总：累计奖励/待结算/已到账
 * 口径见 GET /user/reward/summary 契约广播
 */
@Slf4j
@Service
public class UserRewardSummaryServiceImpl implements UserRewardSummaryService {

    /** 淘宝订单结算状态：3=订单结算 */
    private static final Integer TB_SETTLED_ORDER_STATUS = 3;
    /** 京东有效码：16=已付款(待结算)，17=已完成(已结算) */
    private static final Long JD_PENDING_VALID_CODE = 16L;
    /** 拼多多：0已支付/1已成团/2确认收货/3审核成功=待结算，5=已结算，4/10=无效 */
    private static final List<Integer> PDD_PENDING_ORDER_STATUS = Arrays.asList(0, 1, 2, 3);

    @Resource
    private UserBillMapper userBillMapper;
    @Resource
    private MailvorTbOrderMapper tbOrderMapper;
    @Resource
    private MailvorJdOrderMapper jdOrderMapper;
    @Resource
    private MailvorPddOrderMapper pddOrderMapper;
    @Resource
    private MailvorVipOrderMapper vipOrderMapper;
    @Resource
    private MailvorDyOrderMapper dyOrderMapper;

    @Override
    public RewardSummaryVo getSummary(Long uid, BigDecimal nowMoney) {
        RewardSummaryVo vo = new RewardSummaryVo();
        vo.setTotalReward(round2(userBillMapper.sumBrokeragePrice(uid)));
        vo.setPendingReward(round2(sumPendingFee(uid)));
        vo.setSettledReward(nowMoney == null ? BigDecimal.ZERO : round2(nowMoney.doubleValue()));
        return vo;
    }

    /**
     * 各平台有效且未结算订单的预估佣金总和（元）
     */
    private Double sumPendingFee(Long uid) {
        double fee = 0D;

        LambdaQueryWrapper<MailvorTbOrder> tbWrapper = new LambdaQueryWrapper<>();
        tbWrapper.eq(MailvorTbOrder::getUid, uid)
                .eq(MailvorTbOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue())
                .eq(MailvorTbOrder::getInnerType, 0)
                // NOT IN 在 SQL 三值逻辑下会排除 NULL，需显式放行（NULL 视为未结算）
                .and(w -> w.isNull(MailvorTbOrder::getTkStatus)
                        .or().notIn(MailvorTbOrder::getTkStatus, TB_SETTLED_ORDER_STATUS, OrderUtil.TB_NOT_VALID_ORDER_STATUS));
        fee += nullToZero(tbOrderMapper.sumFee(tbWrapper));

        LambdaQueryWrapper<MailvorJdOrder> jdWrapper = new LambdaQueryWrapper<>();
        jdWrapper.eq(MailvorJdOrder::getUid, uid)
                .eq(MailvorJdOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue())
                .eq(MailvorJdOrder::getInnerType, 0)
                .eq(MailvorJdOrder::getValidCode, JD_PENDING_VALID_CODE);
        fee += nullToZero(jdOrderMapper.sumFee(jdWrapper));

        LambdaQueryWrapper<MailvorPddOrder> pddWrapper = new LambdaQueryWrapper<>();
        pddWrapper.eq(MailvorPddOrder::getUid, uid)
                .eq(MailvorPddOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue())
                .eq(MailvorPddOrder::getInnerType, 0)
                .in(MailvorPddOrder::getOrderStatus, PDD_PENDING_ORDER_STATUS);
        // promotion_amount 单位为分
        fee += nullToZero(pddOrderMapper.sumFee(pddWrapper)) / 100;

        LambdaQueryWrapper<MailvorVipOrder> vipWrapper = new LambdaQueryWrapper<>();
        vipWrapper.eq(MailvorVipOrder::getUid, uid)
                .eq(MailvorVipOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue())
                .eq(MailvorVipOrder::getInnerType, 0)
                .eq(MailvorVipOrder::getSettled, 0)
                // <> 在 SQL 三值逻辑下会排除 NULL，NULL 子状态名视为未失效
                .and(w -> w.isNull(MailvorVipOrder::getOrderSubStatusName)
                        .or().ne(MailvorVipOrder::getOrderSubStatusName, OrderUtil.VIP_NOT_VALID_ORDER_STATUS));
        fee += nullToZero(vipOrderMapper.sumFee(vipWrapper));

        LambdaQueryWrapper<MailvorDyOrder> dyWrapper = new LambdaQueryWrapper<>();
        dyWrapper.eq(MailvorDyOrder::getUid, uid)
                .eq(MailvorDyOrder::getIsDel, CommonEnum.DEL_STATUS_0.getValue())
                .eq(MailvorDyOrder::getInnerType, 0)
                // flow_point 可能为 NULL（API 缺字段/历史数据），SQL 三值逻辑下需显式放行
                .and(w -> w.isNull(MailvorDyOrder::getFlowPoint)
                        .or().ne(MailvorDyOrder::getFlowPoint, OrderUtil.DY_NOT_VALID_ORDER_STATUS))
                .isNull(MailvorDyOrder::getSettleTime);
        fee += nullToZero(dyOrderMapper.sumFee(dyWrapper));

        return fee;
    }


    private Double nullToZero(Double value) {
        return value == null ? 0D : value;
    }

    private BigDecimal round2(Double value) {
        return BigDecimal.valueOf(value == null ? 0D : value).setScale(2, RoundingMode.HALF_UP);
    }
}
