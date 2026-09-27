package com.mailvor.modules.tk.service;

import com.mailvor.modules.tk.config.PddConfig;
import com.mailvor.modules.tk.param.QueryPddParam;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkMemberAuthorityQueryRequest;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkOrderListRangeGetRequest;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkRpPromUrlGenerateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * PDD 官方通道异常收口单测：上游异常不抛出、按既定失败语义返回（null/0），异常原因落日志
 */
class PddServiceTest {

    private PddService service;
    private PopClient client;

    @BeforeEach
    void setUp() {
        service = spy(new PddService());
        PddConfig pddConfig = mock(PddConfig.class);
        doReturn("{}").when(pddConfig).getParam(anyLong());
        doReturn("1784892_test").when(pddConfig).getPid();
        ReflectionTestUtils.setField(service, "pddConfig", pddConfig);
        client = mock(PopClient.class);
        doReturn(client).when(service).getClient();
    }

    @Test
    void orderListUpstreamFailShouldReturnNullWithoutThrow() throws Exception {
        doThrow(new RuntimeException("pdd timeout")).when(client).syncInvoke(any(PddDdkOrderListRangeGetRequest.class));

        assertNull(service.queryPddOrderList(new QueryPddParam()));
    }

    @Test
    void authQueryUpstreamFailShouldReturnZeroWithoutThrow() throws Exception {
        doThrow(new RuntimeException("pdd timeout")).when(client).syncInvoke(any(PddDdkMemberAuthorityQueryRequest.class));

        assertEquals(0, service.authQuery(1L));
    }

    @Test
    void authUpstreamFailShouldReturnNullWithoutThrow() throws Exception {
        doThrow(new RuntimeException("pdd timeout")).when(client).syncInvoke(any(PddDdkRpPromUrlGenerateRequest.class));

        assertNull(service.auth(1L));
    }
}
