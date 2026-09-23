/**
 * Copyright (C) 2018-2024
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.service.dto;

import com.mailvor.annotation.Query;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
* @author shenji
* @date 2022-09-06
*/
@Data
public class MailvorPddOrderQueryCriteria extends MailvorOrderQueryCriteria{
    @Query
    private Integer orderStatus;

    /** 状态集合筛选（订单Tab服务端过滤用），如待结算传 0,1,2,3 */
    @Query(propName = "orderStatus", type = Query.Type.IN)
    private List<Integer> orderStatusIn;

    @Query(blurry = "orderSn,goodsName,goodsSign,uid")
    private String value;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> orderCreateTime;
}
