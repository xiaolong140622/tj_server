/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.user.rest;

import cn.binarywang.wx.miniapp.api.WxMaService;
import com.google.common.collect.Maps;
import com.mailvor.api.ApiResult;
import com.mailvor.api.MshopException;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.common.interceptor.AuthCheck;
import com.mailvor.modules.mp.config.WxMaConfiguration;
import com.mailvor.modules.tk.service.UserRewardSummaryService;
import com.mailvor.modules.tk.vo.SpreadSummaryVo;
import com.mailvor.modules.user.domain.MwUser;
import com.mailvor.utils.RedisUtils;
import com.mailvor.utils.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

/**
 * 推广中心/邀请好友 汇总接口（契约见群内广播 2026-09-23）
 */
@Slf4j
@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Api(value = "用户推广汇总", tags = "用户:推广汇总")
public class SpreadController {

    private static final String LANDING_PAGE = "pages/index/index";

    private final UserRewardSummaryService userRewardSummaryService;
    private final RedisUtils redisUtil;

    /**
     * 推广中心三值汇总：有效好友数/归因订单数/累计佣金
     */
    @AuthCheck
    @GetMapping("/spread/summary")
    @ApiOperation(value = "推广中心汇总", notes = "peopleCount/orderCount/commission")
    public ApiResult<SpreadSummaryVo> spreadSummary() {
        MwUser mwUser = LocalUser.getUser();
        return ApiResult.ok(userRewardSummaryService.getSpreadSummary(mwUser.getUid()));
    }

    /**
     * 邀请好友：邀请码 + 无限制小程序码
     * scene=c=<邀请码>，落地页 pages/index/index；qrBase64 为 png base64，
     * 生成失败（未发布/配额等）时为 null，前端按「点击刷新」处理
     */
    @AuthCheck
    @GetMapping("/spread/code")
    @ApiOperation(value = "邀请码与小程序码", notes = "code/scene/page/qrBase64")
    public ApiResult<Map<String, Object>> spreadCode() {
        MwUser mwUser = LocalUser.getUser();
        String code = mwUser.getCode();
        if (StringUtils.isBlank(code)) {
            throw new MshopException("邀请码不存在");
        }
        String scene = "c=" + code;
        Map<String, Object> map = Maps.newHashMap();
        map.put("code", code);
        map.put("scene", scene);
        map.put("page", LANDING_PAGE);
        String cacheKey = "spread:wxacode:" + code;
        Object cached = redisUtil.get(cacheKey);
        String qrBase64 = cached == null ? null : cached.toString();
        if (qrBase64 == null) {
            try {
                WxMaService wxMaService = WxMaConfiguration.getWxMaService();
                byte[] bytes = wxMaService.getQrcodeService()
                        .createWxaCodeUnlimitBytes(scene, LANDING_PAGE, false, null, 430, false, null, false);
                qrBase64 = Base64.getEncoder().encodeToString(bytes);
                redisUtil.set(cacheKey, qrBase64, 7 * 24 * 3600L);
            } catch (Exception e) {
                log.warn("生成小程序码失败 uid={}", mwUser.getUid(), e);
            }
        }
        map.put("qrBase64", qrBase64);
        return ApiResult.ok(map);
    }
}
