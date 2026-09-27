package com.mailvor.modules.dataoke.rest;

import com.mailvor.api.ApiResult;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.modules.tk.service.PddService;
import com.mailvor.modules.user.domain.MwUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * DataokePDDController 异常收口单测：官方通道失败不再返回 ok(null) 误导前端
 */
class DataokePDDControllerTest {

    private DataokePDDController controller;
    private PddService pddService;

    @BeforeEach
    void setUp() {
        controller = new DataokePDDController();
        pddService = mock(PddService.class);
        ReflectionTestUtils.setField(controller, "pddService", pddService);
        MwUser user = new MwUser();
        user.setUid(15555L);
        LocalUser.set(user, 1);
    }

    @AfterEach
    void tearDown() {
        LocalUser.clear();
    }

    @Test
    void authShouldReturnFailEnvelopeWhenUpstreamFails() {
        when(pddService.auth(anyLong())).thenReturn(null);

        ApiResult res = controller.auth();

        assertFalse(res.isSuccess());
        assertTrue(res.getMsg().contains("上游异常"));
    }

    @Test
    void authShouldReturnOkWhenUpstreamSucceeds() {
        when(pddService.auth(anyLong())).thenReturn(Collections.emptyList());

        ApiResult res = controller.auth();

        assertTrue(res.isSuccess());
        assertEquals(200, res.getStatus());
    }
}
