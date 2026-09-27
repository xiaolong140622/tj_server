package com.mailvor.modules.dataoke.rest;

import com.alibaba.fastjson.JSONObject;
import com.mailvor.api.ApiResult;
import com.mailvor.modules.tk.param.jd.GoodsListJDParam;
import com.mailvor.modules.tk.service.DataokeService;
import com.mailvor.modules.tk.service.JdService;
import com.mailvor.modules.tk.service.KuService;
import com.mailvor.modules.tk.vo.jd.JdKuGoodsDetailVO;
import com.mailvor.modules.tk.vo.jd.JdKuSearchListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsListVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataokeJDController 通道编排单测：官方为主/聚合为补的降级链与异常收口
 */
class DataokeJDControllerTest {

    private DataokeJDController controller;
    private DataokeService dtkService;
    private JdService jdService;
    private KuService kuService;

    @BeforeEach
    void setUp() {
        controller = new DataokeJDController();
        dtkService = mock(DataokeService.class);
        jdService = mock(JdService.class);
        kuService = mock(KuService.class);
        ReflectionTestUtils.setField(controller, "service", dtkService);
        ReflectionTestUtils.setField(controller, "jdService", jdService);
        ReflectionTestUtils.setField(controller, "kuService", kuService);
    }

    @Test
    void detailShouldReturnNormalizedViewWhenOfficialSucceeds() {
        JdKuGoodsDetailVO item = new JdKuGoodsDetailVO();
        item.setGoodsId("5001234");
        item.setTitle("官方通道商品");
        item.setStartPrice(9.9);
        item.setEndPrice(19.9);
        item.setCoupon(5.0);
        item.setFee(1.23);
        item.setCommissionRate(8.5);
        item.setImg("https://main.jpg");
        item.setDetails("https://d1.jpg,https://d2.jpg");
        JdKuSearchListVO official = new JdKuSearchListVO();
        official.setCode(200);
        official.setMsg("success");
        official.setData(Collections.singletonList(item));
        when(jdService.goodsDetailOfficial("5001234", null)).thenReturn(official);

        JSONObject res = controller.getGoodsDetail("5001234", null);

        assertEquals(200, res.getIntValue("code"));
        JSONObject data = res.getJSONObject("data");
        assertEquals("官方通道商品", data.getString("title"));
        assertEquals(9.9, data.getDoubleValue("price"), 0.0001);
        assertEquals(19.9, data.getDoubleValue("originalPrice"), 0.0001);
        assertEquals(5.0, data.getDoubleValue("couponAmount"), 0.0001);
        assertEquals(1.23, data.getDoubleValue("commission"), 0.0001);
        assertEquals("https://main.jpg|https://d1.jpg|https://d2.jpg", data.getString("images"));
        // B-2 冻结契约：官方通道不返回补贴口径时 subsidyRate/subSideRate 必须保持 null（禁 0 填充）
        org.junit.jupiter.api.Assertions.assertNull(data.get("subsidyRate"));
        org.junit.jupiter.api.Assertions.assertNull(data.get("subSideRate"));
        verify(dtkService, never()).goodsDetailJD(anyString(), any());
    }

    @Test
    void detailShouldPassThroughDtkWhenOfficialFails() {
        JdKuSearchListVO official = new JdKuSearchListVO();
        official.setCode(-1);
        official.setMsg("京东联盟详情上游异常");
        when(jdService.goodsDetailOfficial(any(), any())).thenReturn(official);
        JSONObject dtk = new JSONObject();
        dtk.put("code", -1);
        dtk.put("msg", "京东详情DTK通道上游异常");
        when(dtkService.goodsDetailJD("encId", null)).thenReturn(dtk);

        JSONObject res = controller.getGoodsDetail("encId", null);

        assertSame(dtk, res);
    }

    @Test
    void searchShouldFallBackToKuAndBackfillCommissionRate() {
        JdKuSearchListVO official = new JdKuSearchListVO();
        official.setCode(500);
        official.setMsg("官方失败");
        when(jdService.searchGoodsOfficial(any())).thenReturn(official);

        JdKuGoodsDetailVO kuItem = new JdKuGoodsDetailVO();
        kuItem.setFeeRatio(7.7);
        List<JdKuGoodsDetailVO> list = new ArrayList<>();
        list.add(kuItem);
        JdKuSearchListVO ku = new JdKuSearchListVO();
        ku.setCode(200);
        ku.setData(list);
        when(kuService.searchJD(any())).thenReturn(ku);

        JdKuSearchListVO res = controller.goodsSearch(new GoodsListJDParam());

        assertSame(ku, res);
        assertEquals(Double.valueOf(7.7), res.getData().get(0).getCommissionRate());
    }

    @Test
    void rankShouldReturnFailEnvelopeWhenUpstreamThrows() {
        when(jdService.listRank(any())).thenThrow(new RuntimeException("sdk boom"));

        JdUnionCommonGoodsListVO res = controller.getRankList(new GoodsListJDParam());

        assertEquals(Integer.valueOf(-1), res.getCode());
        assertTrue(res.getMsg().contains("异常"));
    }

    @Test
    void apiResultFailEnvelopeShape() {
        // 守护测试：确认 ApiCode.FAIL 信封 status/msg 口径与前端 request.js 判定兼容（code!=200 即失败）
        ApiResult<Object> r = ApiResult.result(com.mailvor.api.ApiCode.FAIL, "x", null);
        assertEquals(500, r.getStatus());
        assertFalse(r.isSuccess());
    }
}
