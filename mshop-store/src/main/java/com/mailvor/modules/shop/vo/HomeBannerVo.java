package com.mailvor.modules.shop.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 首页轮播图统一契约（GET /home/banner）
 * type: product|category|search|page|h5
 * target: product=商品id / category=分类名(搜索用) / search=关键词 / page=小程序页面路径 / h5=链接
 */
@Data
public class HomeBannerVo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;

    private String imageUrl;

    private String title;

    private String type;

    private String target;
}
