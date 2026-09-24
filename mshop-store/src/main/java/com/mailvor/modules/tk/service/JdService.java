/**
 * Copyright (C) 2018-2025
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.service;

import com.alibaba.fastjson.JSON;
import com.jd.open.api.sdk.DefaultJdClient;
import com.jd.open.api.sdk.JdClient;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.BigFieldGoodsReq;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.GoodsReq;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.request.query.RankGoodsReq;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.BigfieldQueryResult;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.GoodsQueryResult;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.GoodsResp;
import com.jd.open.api.sdk.domain.kplunion.GoodsService.response.query.RankGoodsQueryResult;
import com.jd.open.api.sdk.domain.kplunion.OrderService.request.query.OrderRowReq;
import com.jd.open.api.sdk.domain.kplunion.OrderService.response.query.OrderRowQueryResult;
import com.jd.open.api.sdk.domain.kplunion.promotioncommon.PromotionService.request.get.PromotionCodeReq;
import com.jd.open.api.sdk.domain.kplunion.promotioncommon.PromotionService.response.get.GetResult;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsBigfieldQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenGoodsRankQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenOrderRowQueryRequest;
import com.jd.open.api.sdk.request.kplunion.UnionOpenPromotionCommonGetRequest;
import com.jd.open.api.sdk.response.kplunion.UnionOpenOrderRowQueryResponse;
import com.mailvor.modules.tk.config.JdConfig;
import com.mailvor.modules.tk.param.QueryJdParam;
import com.mailvor.modules.tk.param.jd.GoodsListJDParam;
import com.mailvor.modules.tk.vo.jd.JdKuGoodsDetailVO;
import com.mailvor.modules.tk.vo.jd.JdKuSearchListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsWordVO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author shenji
 * @date 2022/10/27
 */
@Slf4j
@Component
public class JdService {

    @Resource
    private JdConfig jdConfig;

    @SneakyThrows
    private RankGoodsQueryResult listRankUnion(GoodsListJDParam param){
        JdClient client = getJdClient();
        UnionOpenGoodsRankQueryRequest request=new UnionOpenGoodsRankQueryRequest();
        RankGoodsReq rankGoodsReq=new RankGoodsReq();
        rankGoodsReq.setRankId(200000L);
        rankGoodsReq.setSortType(3);
        rankGoodsReq.setPageIndex(param.getPageId());
        rankGoodsReq.setPageSize(param.getPageSize());
        request.setRankGoodsReq(rankGoodsReq);
        request.setVersion("1.0");
        return client.execute(request).getQueryResult();
    }

    public JdUnionCommonGoodsListVO listRank(GoodsListJDParam param) {
        RankGoodsQueryResult queryResult = listRankUnion(param);
        return JdUnionCommonGoodsListVO.convert(queryResult);

    }

    /**
     * C-1 京东联盟官方搜索通道（union.open.goods.query）。
     * 返回与好单库通道同构的统一 VO（JdKuSearchListVO），code==200 为成功；
     * 任何上游异常/无权限/非200 一律收口为 {code,msg} 信封（不抛出、不冒泡500），
     * 由调用方（DataokeJDController /jd/goods/search）按「联盟为主/第三方为补」降级到 kuService.searchJD。
     */
    public JdKuSearchListVO searchGoodsOfficial(GoodsListJDParam param) {
        JdKuSearchListVO vo = new JdKuSearchListVO();
        try {
            JdClient client = getJdClient();
            UnionOpenGoodsQueryRequest request = new UnionOpenGoodsQueryRequest();
            GoodsReq goodsReq = new GoodsReq();
            goodsReq.setKeyword(param.getKeyword());
            goodsReq.setPageIndex(param.getPageId() == null ? 1 : param.getPageId());
            goodsReq.setPageSize(param.getPageSize() == null ? 10 : param.getPageSize());
            if (StringUtils.isNotBlank(param.getCid1())) {
                goodsReq.setCid1(Long.valueOf(param.getCid1()));
            }
            if (StringUtils.isNotBlank(param.getCid2())) {
                goodsReq.setCid2(Long.valueOf(param.getCid2()));
            }
            if (StringUtils.isNotBlank(param.getCid3())) {
                goodsReq.setCid3(Long.valueOf(param.getCid3()));
            }
            if (StringUtils.isNotBlank(param.getSortName())) {
                goodsReq.setSortName(param.getSortName());
            }
            if (StringUtils.isNotBlank(param.getSort())) {
                goodsReq.setSort(param.getSort());
            }
            if (param.getIsCoupon() != null) {
                goodsReq.setIsCoupon(param.getIsCoupon());
            }
            if (param.getCommissionShareStart() != null) {
                goodsReq.setCommissionShareStart(param.getCommissionShareStart());
            }
            if (param.getCommissionShareEnd() != null) {
                goodsReq.setCommissionShareEnd(param.getCommissionShareEnd());
            }
            if (param.getPriceFrom() != null) {
                goodsReq.setPricefrom(param.getPriceFrom().doubleValue());
            }
            if (param.getPriceTo() != null) {
                goodsReq.setPriceto(param.getPriceTo().doubleValue());
            }
            if (StringUtils.isNotBlank(param.getOwner())) {
                goodsReq.setOwner(param.getOwner());
            }
            if (StringUtils.isNotBlank(param.getSkuIds())) {
                goodsReq.setSkuIds(Arrays.stream(param.getSkuIds().split(","))
                        .filter(StringUtils::isNotBlank)
                        .map(s -> Long.valueOf(s.trim())).toArray(Long[]::new));
            }
            request.setGoodsReqDTO(goodsReq);
            request.setVersion("1.0");
            GoodsQueryResult result = client.execute(request).getQueryResult();
            if (result == null) {
                vo.setCode(-1);
                vo.setMsg("京东联盟搜索上游无响应");
                return vo;
            }
            vo.setCode(result.getCode());
            vo.setMsg(result.getMessage());
            if (result.getCode() != 200 || result.getData() == null) {
                return vo;
            }
            List<JdKuGoodsDetailVO> list = Arrays.stream(result.getData())
                    .map(this::toUnifiedItem).collect(Collectors.toList());
            vo.setData(list);
            return vo;
        } catch (Exception e) {
            log.warn("京东联盟官方搜索通道调用失败: {}", e.getMessage());
            vo.setCode(-1);
            vo.setMsg("京东联盟搜索上游异常");
            return vo;
        }
    }

    /**
     * 官方 goods.query 条目 → 统一商品VO（字段口径以 shared/jd-channel-contract-v1.md 冻结契约为准）
     */
    private JdKuGoodsDetailVO toUnifiedItem(GoodsResp g) {
        JdKuGoodsDetailVO item = new JdKuGoodsDetailVO();
        item.setGoodsId(StringUtils.isNotBlank(g.getItemId()) ? g.getItemId() : String.valueOf(g.getSkuId()));
        item.setTitle(g.getSkuName());
        item.setOwner("g".equals(g.getOwner()) ? "1" : "0");
        if (g.getComments() != null) {
            item.setComments(g.getComments().toString());
        }
        if (g.getInOrderCount30Days() != null) {
            item.setSales(g.getInOrderCount30Days().toString());
        }
        if (g.getGoodCommentsShare() != null) {
            item.setGoodsCommentShare(g.getGoodCommentsShare().toString());
        }
        if (g.getShopInfo() != null) {
            item.setShopName(g.getShopInfo().getShopName());
        }
        if (g.getImageInfo() != null) {
            if (g.getImageInfo().getImageList() != null && g.getImageInfo().getImageList().length > 0) {
                item.setImg(g.getImageInfo().getImageList()[0].getUrl());
                item.setDetails(item.getImg());
            }
        }
        if (g.getPriceInfo() != null) {
            item.setStartPrice(g.getPriceInfo().getPrice());
            Double end = g.getPriceInfo().getLowestCouponPrice() != null
                    ? g.getPriceInfo().getLowestCouponPrice() : g.getPriceInfo().getLowestPrice();
            item.setEndPrice(end != null ? end : g.getPriceInfo().getPrice());
        }
        if (g.getCommissionInfo() != null) {
            item.setFee(g.getCommissionInfo().getCommission());
            item.setFeeRatio(g.getCommissionInfo().getCommissionShare());
            item.setCommissionRate(g.getCommissionInfo().getCommissionShare());
        }
        if (g.getCouponInfo() != null && g.getCouponInfo().getCouponList() != null
                && g.getCouponInfo().getCouponList().length > 0) {
            item.setCoupon(g.getCouponInfo().getCouponList()[0].getDiscount());
            item.setCouponLink(g.getCouponInfo().getCouponList()[0].getLink());
        }
        // 补贴三字段：goods.query 上游（SDK jd-api-sdk-java-20260810）不返回 subsidy 口径，
        // subsidyRate/subSideRate 保持 null，前端按契约隐藏处理；订单侧官方口径在 OrderRowResp
        return item;
    }

    @SneakyThrows
    private BigfieldQueryResult goodsDetailUnion(String itemId) {
        JdClient client = getJdClient();
        UnionOpenGoodsBigfieldQueryRequest request=new UnionOpenGoodsBigfieldQueryRequest();
        BigFieldGoodsReq goodsReq=new BigFieldGoodsReq();
        goodsReq.setItemIds(Arrays.asList(itemId).toArray(new String[0]));
        request.setGoodsReq(goodsReq);
        request.setVersion("1.0");
        return client.execute(request).getQueryResult();
    }

    //    public JdKuCommonSearchListVO goodsDetail(String itemId) {
//        BigfieldQueryResult queryResult = goodsDetailUnion(itemId);
//        //todo 后续实现
//    }
    /**
     * 获取商品短链接
     * @param itemId
     * @param couponUrl
     * @param uid
     * @return
     */
    @SneakyThrows
    private GetResult goodsWordUnion(String itemId, String couponUrl, String uid) {        //无权限无法调用
        JdClient client=new DefaultJdClient(jdConfig.getServer(),
                null,
                jdConfig.getAppKey(),jdConfig.getAppSecret());
        UnionOpenPromotionCommonGetRequest request=new UnionOpenPromotionCommonGetRequest();
        PromotionCodeReq promotionCodeReq=new PromotionCodeReq();
        promotionCodeReq.setMaterialId(itemId);
        promotionCodeReq.setSiteId(jdConfig.getSiteId());
        promotionCodeReq.setSubUnionId(uid);
        if(StringUtils.isNotBlank(couponUrl)){
            promotionCodeReq.setCouponUrl(couponUrl);
        }
        //生成短链接
        promotionCodeReq.setCommand(1);
        promotionCodeReq.setSceneId(1);
        request.setPromotionCodeReq(promotionCodeReq);
        request.setVersion("1.0");
        return client.execute(request).getGetResult();
    }

    /**
     * 获取商品短链接
     * @param itemId
     * @param couponUrl
     * @param uid
     * @return
     */
    /**
     * C-2 转链收口：联盟上游异常/无响应/数据缺失统一返回 {code,msg} 失败信封（code!=200），
     * 不再 @SneakyThrows 冒泡成 500；成功时 code=200 且 link/pwd 齐全。
     * 错误语义与调用方见 shared/jd-channel-contract-v1.md。
     */
    public JdUnionCommonGoodsWordVO goodsWord(String itemId, String couponUrl, String uid) {
        GetResult getResult;
        try {
            getResult = goodsWordUnion(itemId, couponUrl, uid);
        } catch (Exception e) {
            log.warn("京东联盟转链通道调用失败: {}", e.getMessage());
            return JdUnionCommonGoodsWordVO.builder().code(-1).msg("京东转链上游异常").build();
        }
        if (getResult == null) {
            return JdUnionCommonGoodsWordVO.builder().code(-1).msg("京东转链上游无响应").build();
        }
        if (getResult.getCode() != 200 || getResult.getData() == null) {
            log.warn("京东联盟转链上游返回失败: code={}, msg={}", getResult.getCode(), getResult.getMessage());
            return JdUnionCommonGoodsWordVO.builder()
                    .code(getResult.getCode())
                    .msg(StringUtils.isNotBlank(getResult.getMessage()) ? getResult.getMessage() : "京东转链上游失败")
                    .build();
        }
        return JdUnionCommonGoodsWordVO.builder()
                .code(200)
                .msg("success")
                .link(getResult.getData().getClickURL())
                .pwd(getResult.getData().getJCommand())
                .build();
    }

    public OrderRowQueryResult order(QueryJdParam param) {
        JdClient client = getJdClient();
        UnionOpenOrderRowQueryRequest request=new UnionOpenOrderRowQueryRequest();
        OrderRowReq orderReq=new OrderRowReq();
        orderReq.setStartTime(param.getStartTime());
        orderReq.setEndTime(param.getEndTime());
        orderReq.setType(param.getType());
        orderReq.setFields("goodsInfo");
        orderReq.setPageIndex(param.getPageNo());
        orderReq.setPageSize(param.getPageSize());
        request.setOrderReq(orderReq);
        request.setVersion("1.0");
        UnionOpenOrderRowQueryResponse response= null;
        try {
            response = client.execute(request);
            return response.getQueryResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private JdClient getJdClient() {
        JdClient client=new DefaultJdClient(jdConfig.getServer(),
                null,
                jdConfig.getAppKey(),jdConfig.getAppSecret());
        return client;
    }
    public static void main(String[] args) throws Exception {

        String url = "https://api.jd.com/routerjson";
        String clientId = "a5a67f64a5491868c8436301";
        String clientSecret = "d22224c5a8bb6e638cb06";
//        PopClient client = new PopHttpClient(clientId, clientSecret);
//
//        PddDdkOrderListRangeGetRequest request = new PddDdkOrderListRangeGetRequest();
//        request.setEndTime("2022-06-23 23:00:00");
//        request.setPageSize(300);
//        request.setStartTime("2022-06-23 00:00:00");
//        PddDdkOrderListRangeGetResponse response = client.syncInvoke(request);
//        System.out.println(JsonUtil.transferToJson(response));

        JdClient client=new DefaultJdClient(url, null, clientId,clientSecret);
//        UnionOpenGoodsRankQueryRequest request=new UnionOpenGoodsRankQueryRequest();
//        RankGoodsReq rankGoodsReq=new RankGoodsReq();
//        rankGoodsReq.setRankId(200000L);
//        rankGoodsReq.setSortType(3);
//        rankGoodsReq.setPageIndex(1);
//        rankGoodsReq.setPageSize(10);
//        request.setRankGoodsReq(rankGoodsReq);
//        request.setVersion("1.0");
//        UnionOpenGoodsRankQueryResponse response = client.execute(request);
//        System.out.println(JSON.toJSONString(response.getQueryResult()));

//        UnionOpenGoodsBigfieldQueryRequest request=new UnionOpenGoodsBigfieldQueryRequest();
//        BigFieldGoodsReq goodsReq=new BigFieldGoodsReq();
//        goodsReq.setItemIds(Arrays.asList("28HYqoPfcmj38vCmWNacVosZ_3bnREOCd6cyii0Eb7l").toArray(new String[0]));
//        request.setGoodsReq(goodsReq);
//        request.setVersion("1.0");
//        System.out.println(JSON.toJSONString(client.execute(request).getQueryResult()));

//        UnionOpenPromotionCommonGetRequest request=new UnionOpenPromotionCommonGetRequest();
//        PromotionCodeReq promotionCodeReq=new PromotionCodeReq();
//        promotionCodeReq.setMaterialId("CGVm8tBjeQAy37yXTeYN9Nw6_3GHvBWMeRevlvCwYT8");
//        promotionCodeReq.setSiteId("4100889962");
//        promotionCodeReq.setSubUnionId("1");
//        promotionCodeReq.setCouponUrl("https://coupon.m.jd.com/coupons/show.action?linkKey=AAROH_xIpeffAs_-naABEFoex3wz1P2XHbwb2i6uQXWVbRKXorkR6Rf_siHaV3y2-SKueMvRcbdaBaAD7Tk4dKAIm65O_w");
//        promotionCodeReq.setSceneId(1);
//        request.setPromotionCodeReq(promotionCodeReq);
//        request.setVersion("1.0");
//        System.out.println(JSON.toJSONString(client.execute(request).getGetResult()));

        UnionOpenOrderRowQueryRequest request=new UnionOpenOrderRowQueryRequest();
        OrderRowReq orderReq=new OrderRowReq();
        orderReq.setStartTime("2024-12-07 13:08:00");
        orderReq.setEndTime("2024-12-07 13:10:00");
        orderReq.setType(3);
        orderReq.setPageIndex(1);
        orderReq.setPageSize(10);
        request.setOrderReq(orderReq);
        request.setVersion("1.0");
        UnionOpenOrderRowQueryResponse response=client.execute(request);
        System.out.println(JSON.toJSONString(response.getQueryResult()));
    }
}
