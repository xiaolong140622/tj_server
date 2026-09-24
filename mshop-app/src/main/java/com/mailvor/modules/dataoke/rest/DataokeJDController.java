package com.mailvor.modules.dataoke.rest;

import com.alibaba.fastjson.JSONObject;
import com.mailvor.api.ApiResult;
import com.mailvor.common.bean.LocalUser;
import com.mailvor.common.interceptor.UserCheck;
import com.mailvor.modules.tk.param.GoodsJdWordParam;
import com.mailvor.modules.tk.param.jd.GoodsListJDParam;
import com.mailvor.modules.tk.service.DataokeService;
import com.mailvor.modules.tk.service.JdService;
import com.mailvor.modules.tk.service.KuService;
import com.mailvor.modules.tk.vo.jd.JdKuSearchListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsListVO;
import com.mailvor.modules.tk.vo.jd.JdUnionCommonGoodsWordVO;
import com.mailvor.modules.user.domain.MwUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 *
 * @author shenji
 * @date 2023/3/20
 */
@RestController
@RequestMapping("/jd")
@Slf4j
public class DataokeJDController {
    @Resource
    private DataokeService service;

    @Resource
    private JdService jdService;

    @Resource
    private KuService kuService;

    /**
     * 京东商品关键词搜索（C-1：联盟官方通道为主，好单库 jd_goods_search 为补充降级）
     * 入参：keyword 必填，pageId 页码(从1)、pageSize 页大小(默认10)、sortName/sort 排序、cid1-3 等
     * 返回统一 JdKuSearchListVO：{code,msg,data:[...]}；code!=200 视为失败（前端错误态，不得回落 mock）
     * 降级规则：官方通道非200/异常 → 自动回退 ku 通道；ku 条目 commissionRate 以 feeRatio 同值回填
     */
    @GetMapping(value = "/goods/search")
    public JdKuSearchListVO goodsSearch(GoodsListJDParam param) {
        JdKuSearchListVO official = jdService.searchGoodsOfficial(param);
        if (official.getCode() != null && official.getCode() == 200) {
            return official;
        }
        log.warn("JD联盟官方搜索通道失败(code={}, msg={})，降级好单库通道", official.getCode(), official.getMsg());
        JdKuSearchListVO ku = kuService.searchJD(param);
        if (ku.getData() != null) {
            ku.getData().forEach(item -> {
                if (item.getCommissionRate() == null) {
                    item.setCommissionRate(item.getFeeRatio());
                }
            });
        }
        return ku;
    }

    /**
     * 获取京东商品详情
     * @param goodsId
     * @param itemId
     * @return
     */
    @GetMapping(value = "/goods/detail")
    public JSONObject getGoodsDetail(@RequestParam(required = false) String goodsId,
                                     @RequestParam(required = false) String itemId) {

        return service.goodsDetailJD(goodsId, itemId);
    }

    /**
     * 获取京东商品转链口令（C-2 收口：上游失败返回 200+ApiResult.fail 信封，不再 500）
     * 成功：{status:true,data:{code:200,msg:success,link,pwd}}
     * 失败：{status:false,msg:"京东转链上游异常"|上游message,data:null}
     * @param param
     * @return
     */
    @UserCheck
    @GetMapping(value = "/goods/word")
    public ApiResult<JdUnionCommonGoodsWordVO> goodsWord2(@Valid GoodsJdWordParam param) {
        String positionId;

        MwUser user = LocalUser.getUser();
        if(user != null) {
            positionId = user.getUid().toString();
        } else {
            positionId = "0";
        }
        JdUnionCommonGoodsWordVO daRes = jdService.goodsWord(param.getGoodsId(), param.getCouponLink(), positionId);
        if (daRes == null || daRes.getCode() == null || daRes.getCode() != 200) {
            String msg = daRes == null || daRes.getMsg() == null ? "京东转链上游异常" : daRes.getMsg();
            return ApiResult.result(com.mailvor.api.ApiCode.FAIL, msg, null);
        }
        return ApiResult.ok(daRes);

    }
    /**
     * 获取京东商品榜单
     * @param param
     * @return
     * @throws Exception
     */
    @GetMapping(value = "/rank/list")
    public JdUnionCommonGoodsListVO getRankList(GoodsListJDParam param) throws Exception {
        return jdService.listRank(param);
    }
}
