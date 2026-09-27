package com.mailvor.modules.tk.service;

import com.mailvor.modules.tk.config.PddConfig;
import com.mailvor.modules.tk.param.QueryPddParam;
import com.pdd.pop.sdk.http.PopClient;
import com.pdd.pop.sdk.http.PopHttpClient;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkMemberAuthorityQueryRequest;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkOrderListRangeGetRequest;
import com.pdd.pop.sdk.http.api.pop.request.PddDdkRpPromUrlGenerateRequest;
import com.pdd.pop.sdk.http.api.pop.response.PddDdkMemberAuthorityQueryResponse;
import com.pdd.pop.sdk.http.api.pop.response.PddDdkOrderListRangeGetResponse;
import com.pdd.pop.sdk.http.api.pop.response.PddDdkRpPromUrlGenerateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @projectName:openapi
 * @author:
 * @createTime: 2019/04/24 14:55
 * @description:
 */
@Slf4j
@Component
public class PddService {
    @Resource
    private PddConfig pddConfig;

    private PopClient client;

    protected PopClient getClient() {
        if(client == null) {
            client = new PopHttpClient(pddConfig.getClientId(), pddConfig.getClientSecret());
        }
        return client;
    }

    /**
     * 查询拼多多订单
     *
     * @param param the param
     * @return the pdd ddk order list range get response
     */
    public PddDdkOrderListRangeGetResponse queryPddOrderList(QueryPddParam param) {
        try {
            PddDdkOrderListRangeGetRequest request = new PddDdkOrderListRangeGetRequest();
            if(param.getLastOrderId() != null) {
                request.setLastOrderId(param.getLastOrderId());
            }
            request.setEndTime(param.getEndTime());
            request.setPageSize(param.getPageSize());
            request.setStartTime(param.getStartTime());

            return getClient().syncInvoke(request);
        } catch (Exception e) {
            log.warn("拼多多官方订单通道调用失败: {}", e.getMessage());
        }
        return null;
    }


    /**
     * 查询是否授权
     *
     * @param uid the uid
     * @return the int
     */
    public int authQuery(Long uid) {
        try {
            PddDdkMemberAuthorityQueryRequest request = new PddDdkMemberAuthorityQueryRequest();
            request.setCustomParameters(pddConfig.getParam(uid));
            request.setPid(pddConfig.getPid());
            PddDdkMemberAuthorityQueryResponse response = getClient().syncInvoke(request);
            return response.getAuthorityQueryResponse().getBind();
        } catch (Exception e) {
            // 上游异常按未授权(0)口径收口，与「未绑定」同语义；真实原因记日志便于排障
            log.warn("拼多多官方授权查询通道调用失败(按未授权口径返回0): {}", e.getMessage());
        }
        return 0;
    }

    public List<PddDdkRpPromUrlGenerateResponse.RpPromotionUrlGenerateResponseUrlListItem> auth(Long uid) {
        try {
            PddDdkRpPromUrlGenerateRequest request = new PddDdkRpPromUrlGenerateRequest();
            request.setChannelType(10);
            request.setCustomParameters(pddConfig.getParam(uid));
            request.setPIdList(Collections.singletonList(pddConfig.getPid()));

            return getClient().syncInvoke(request).getRpPromotionUrlGenerateResponse().getUrlList();
        } catch (Exception e) {
            log.warn("拼多多官方授权链接通道调用失败: {}", e.getMessage());
        }
        return null;
    }
}
