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
* @date 2022-09-07
*/
@Data
public class MailvorDyOrderQueryCriteria extends MailvorOrderQueryCriteria{
    @Query
    private String orderId;

    /** 订单状态流筛选：PAY_SUCC=待结算，CONFIRM=已结算，REFUND=无效 */
    @Query
    private String flowPoint;

    @Query(blurry = "productName,orderId,uid")
    private String value;

    @Query(type = Query.Type.BETWEEN)
    private List<Timestamp> paySuccessTime;
}
