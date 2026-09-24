/**
 * Copyright (C) 2018-2025
 * All rights reserved, Designed By www.mailvor.com
 */
package com.mailvor.modules.tk.vo.jd;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * 统一数据结构
 * */
@Schema(description = "京东转链vo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdUnionCommonGoodsWordVO {

    @Schema(description = "转链码：200成功，其余为上游/通道失败（详见msg）")
    private Integer code;
    @Schema(description = "描述")
    private String msg;

    @Schema(description = "转链url")
    private String link;
    @Schema(description = "口令")
    private String pwd;

}
