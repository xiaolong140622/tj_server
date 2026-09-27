package com.mailvor.modules.tk.service;

import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.BigfieldQueryResult;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.GoodsQueryResult;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.GoodsResp;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.PriceInfo;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsBigfieldQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsQueryRequest;
import com.jd.open.api.sdk.response.kplunion.UnionOpenGoodsBigfieldQueryResponse;
import com.jd.open.api.sdk.response.kplunion.UnionOpenGoodsQueryResponse;
import com.mailvor.modules.tk.vo.jd.JdKuGoodsDetailVO;
import com.mailvor.modules.tk.vo.jd.JdKuSearchListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsWordVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * C-3 京东联盟官方详情通道单测（goods.query 双通道 + bigfield 尽力而为 + 异常收口）
 */
class JdServiceOfficialChannelTest {

    private JdService service;
    private JdClient client;

    @BeforeEach
    void setUp() {
        service = spy(new JdService());
        client = mock(JdClient.class);
        doReturn(client).when(service).getJdClient();
    }

    private void stubBigfieldEmpty() throws Exception {
        UnionOpenGoodsBigfieldQueryResponse bigResp = new UnionOpenGoodsBigfieldQueryResponse();
        bigResp.setQueryResult(null);
        doReturn(bigResp).when(client).execute(any(UnionOpenGoodsBigfieldQueryRequest.class));
    }

    private void stubGoodsQuery(int code, GoodsResp... data) throws Exception {
        GoodsQueryResult result = new GoodsQueryResult();
        result.setCode(code);
        result.setMessage(code == 200 ? "success" : "上游失败");
        result.setData(data);
        UnionOpenGoodsQueryResponse resp = new UnionOpenGoodsQueryResponse();
        resp.setQueryResult(result);
        doReturn(resp).when(client).execute(any(UnionOpenGoodsQueryRequest.class));
    }

    @Test
    void blankIdShouldReturnFailEnvelope() {
        JdKuSearchListVO vo = service.goodsDetailOfficial(null, " ");
        assertEquals(-1, vo.getCode());
        assertNotNull(vo.getMsg());
        assertNull(vo.getData());
    }

    @Test
    void numericIdShouldUseSkuChannelAndMapUnifiedItem() throws Exception {
        GoodsResp g = new GoodsResp();
        g.setSkuId(5001234L);
        g.setSkuName("测试商品");
        g.setItemId("5001234");
        PriceInfo price = new PriceInfo();
        price.setPrice(99.0);
        g.setPriceInfo(price);
        stubGoodsQuery(200, g);
        stubBigfieldEmpty();

        JdKuSearchListVO vo = service.goodsDetailOfficial("5001234", null);

        assertEquals(200, vo.getCode().intValue());
        JdKuGoodsDetailVO item = vo.getData().get(0);
        assertEquals("测试商品", item.getTitle());
        assertEquals(Double.valueOf(99.0), item.getStartPrice());

        UnionOpenGoodsQueryRequest sent = captureGoodsQueryRequest();
        assertNotNull(sent.getGoodsReqDTO().getSkuIds());
        assertNull(sent.getGoodsReqDTO().getItemIds());
    }

    @Test
    void encryptedIdShouldUseItemIdChannel() throws Exception {
        GoodsResp g = new GoodsResp();
        g.setSkuName("加密品");
        g.setCallerItemId("28HYqoPfcmj38vCmWNacVosZ_3bnREOCd6cyii0Eb7l");
        stubGoodsQuery(200, g);
        stubBigfieldEmpty();

        JdKuSearchListVO vo = service.goodsDetailOfficial(null, "28HYqoPfcmj38vCmWNacVosZ_3bnREOCd6cyii0Eb7l");

        assertEquals(200, vo.getCode().intValue());
        UnionOpenGoodsQueryRequest sent = captureGoodsQueryRequest();
        assertEquals("28HYqoPfcmj38vCmWNacVosZ_3bnREOCd6cyii0Eb7l",
                sent.getGoodsReqDTO().getItemIds()[0]);
    }

    /**
     * 详情主链路会先后调用 goods.query 与 bigfield 两次 execute，
     * 按类型过滤捕获 union 商品查询请求，验证入参通道选择。
     */
    @SuppressWarnings("unchecked")
    private UnionOpenGoodsQueryRequest captureGoodsQueryRequest() throws Exception {
        ArgumentCaptor<com.jd.open.api.sdk.request.JdRequest> captor =
                ArgumentCaptor.forClass(com.jd.open.api.sdk.request.JdRequest.class);
        verify(client, org.mockito.Mockito.atLeastOnce()).execute(captor.capture());
        for (com.jd.open.api.sdk.request.JdRequest req : captor.getAllValues()) {
            if (req instanceof UnionOpenGoodsQueryRequest) {
                return (UnionOpenGoodsQueryRequest) req;
            }
        }
        throw new AssertionError("未捕获到 goods.query 请求");
    }

    @Test
    void bigfieldDetailImagesShouldFillDetails() throws Exception {
        GoodsResp g = new GoodsResp();
        g.setSkuName("带详情图");
        g.setCallerItemId("encId123");
        stubGoodsQuery(200, g);

        BigfieldQueryResult bf = new BigfieldQueryResult();
        bf.setCode(200);
        com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.BigFieldGoodsResp bfr =
                new com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.BigFieldGoodsResp();
        bfr.setDetailImages("https://img1.jpg,https://img2.jpg");
        bf.setData(new com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.BigFieldGoodsResp[]{bfr});
        UnionOpenGoodsBigfieldQueryResponse bigResp = new UnionOpenGoodsBigfieldQueryResponse();
        bigResp.setQueryResult(bf);
        doReturn(bigResp).when(client).execute(any(UnionOpenGoodsBigfieldQueryRequest.class));

        JdKuSearchListVO vo = service.goodsDetailOfficial(null, "encId123");

        assertEquals("https://img1.jpg,https://img2.jpg", vo.getData().get(0).getDetails());
    }

    @Test
    void upstreamThrowShouldReturnFailEnvelopeNotException() throws Exception {
        doThrow(new RuntimeException("connect timeout")).when(client).execute(any(UnionOpenGoodsQueryRequest.class));

        JdKuSearchListVO vo = service.goodsDetailOfficial("123", null);

        assertEquals(-1, vo.getCode().intValue());
        assertTrue(vo.getMsg().contains("异常"));
    }

    @Test
    void upstreamNon200ShouldCarryFailCode() throws Exception {
        GoodsResp g = new GoodsResp();
        stubGoodsQuery(2000, (GoodsResp[]) null);

        JdKuSearchListVO vo = service.goodsDetailOfficial("123", null);

        assertEquals(2000, vo.getCode().intValue());
        assertNull(vo.getData());
    }

    @Test
    void goodsWordUpstreamThrowShouldReturnFailEnvelope() throws Exception {
        doThrow(new RuntimeException("boom")).when(client)
                .execute(any(com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionCommonGetRequest.class));

        JdUnionCommonGoodsWordVO vo = service.goodsWord("matId", null, "1");

        assertEquals(Integer.valueOf(-1), vo.getCode());
        assertEquals("京东转链上游异常", vo.getMsg());
    }
}
