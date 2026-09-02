package com.mailvor.modules.wechat.rest.controller;

import com.mailvor.api.ApiResult;
import com.mailvor.api.MshopException;
import com.mailvor.common.util.JwtToken;
import com.mailvor.modules.services.AuthService;
import com.mailvor.modules.user.domain.MwUser;
import com.mailvor.modules.user.service.MwUserService;
import com.mailvor.modules.wechat.rest.param.WxLoginParam;
import com.mailvor.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * WxMaUserController#login (POST /wxapp/login) 单元测试。
 * 覆盖契约锁定的响应形状(token / userInfo / needBindPhone)、绑定手机号两种分支、
 * 单点登录踢人、以及微信登录失败时的异常传播。
 * 采用与 HaodankuControllerTest 一致的纯 Mockito + ReflectionTestUtils 方式，不启动 Spring 上下文。
 */
class WxMaUserControllerTest {

    private AuthService authService;
    private WxMaUserController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        MwUserService userService = mock(MwUserService.class);
        RedisUtils redisUtils = mock(RedisUtils.class);
        controller = new WxMaUserController(userService, redisUtils, authService);
        ReflectionTestUtils.setField(controller, "singleLogin", false);

        // JwtToken 的 jwtKey/expiredTimeIn 正常由 @Value 注入，单测手动初始化其静态字段
        JwtToken jwtToken = new JwtToken();
        jwtToken.setJwtKey("unit-test-jwt-key-0123456789");
        jwtToken.setExpiredTimeIn(3600);
    }

    private WxLoginParam param(String code) {
        WxLoginParam p = new WxLoginParam();
        p.setCode(code);
        return p;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> userInfoOf(ApiResult<Map<String, Object>> result) {
        return (Map<String, Object>) result.getData().get("userInfo");
    }

    @Test
    @DisplayName("未绑定手机号登录：返回契约形状且 needBindPhone=true")
    void loginShouldReturnContractShapeAndNeedBindPhoneTrue() {
        MwUser user = MwUser.builder()
                .uid(1001L).username("openid-abc").nickname("微信用户")
                .avatar("http://a/avatar.png").phone(null).build();
        when(authService.wechatMaLogin(anyString())).thenReturn(user);

        ApiResult<Map<String, Object>> result =
                controller.login(param("code-123"), new MockHttpServletRequest());

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatus());

        Map<String, Object> data = result.getData();
        assertNotNull(data);
        assertTrue(data.get("token") instanceof String);
        assertFalse(((String) data.get("token")).isEmpty());
        assertEquals(Boolean.TRUE, data.get("needBindPhone"));

        Map<String, Object> userInfo = userInfoOf(result);
        assertNotNull(userInfo);
        assertEquals(1001L, userInfo.get("uid"));
        assertEquals("微信用户", userInfo.get("nickname"));
        assertEquals("http://a/avatar.png", userInfo.get("avatar"));
        assertNull(userInfo.get("phone"));

        // 登录成功后必须保存在线用户信息
        verify(authService).save(eq(user), anyString(), any());
    }

    @Test
    @DisplayName("已绑定手机号登录：needBindPhone=false 且回显手机号")
    void loginShouldReturnNeedBindPhoneFalseWhenPhonePresent() {
        MwUser user = MwUser.builder()
                .uid(2002L).username("openid-xyz").nickname("老用户")
                .avatar("http://a/2.png").phone("13800001111").build();
        when(authService.wechatMaLogin(anyString())).thenReturn(user);

        ApiResult<Map<String, Object>> result =
                controller.login(param("code-999"), new MockHttpServletRequest());

        Map<String, Object> data = result.getData();
        assertEquals(Boolean.FALSE, data.get("needBindPhone"));

        Map<String, Object> userInfo = userInfoOf(result);
        assertEquals("13800001111", userInfo.get("phone"));
        assertEquals(2002L, userInfo.get("uid"));
    }

    @Test
    @DisplayName("开启单点登录时踢除历史会话")
    void loginShouldKickOtherSessionsWhenSingleLoginEnabled() {
        ReflectionTestUtils.setField(controller, "singleLogin", true);
        MwUser user = MwUser.builder().uid(3003L).username("openid-s").nickname("n")
                .avatar("a").phone("13900002222").build();
        when(authService.wechatMaLogin(anyString())).thenReturn(user);

        controller.login(param("code-s"), new MockHttpServletRequest());

        verify(authService).checkLoginOnUser(eq("openid-s"), anyString());
    }

    @Test
    @DisplayName("微信登录失败(如配置缺失)时异常向上传播且不签发token")
    void loginShouldPropagateExceptionWhenWechatLoginFails() {
        when(authService.wechatMaLogin(anyString()))
                .thenThrow(new MshopException("请先配置小程序"));

        MshopException ex = assertThrows(MshopException.class,
                () -> controller.login(param("bad-code"), new MockHttpServletRequest()));
        assertEquals("请先配置小程序", ex.getMessage());
        verify(authService, never()).save(any(), anyString(), any());
    }
}
