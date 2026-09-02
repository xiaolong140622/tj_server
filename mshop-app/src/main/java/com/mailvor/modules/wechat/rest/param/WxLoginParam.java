package com.mailvor.modules.wechat.rest.param;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * @ClassName WxLoginParam
 * 小程序登录入参
 **/
@Getter
@Setter
public class WxLoginParam {

    @NotBlank(message = "code不能为空")
    @ApiModelProperty(value = "wx.login 返回的登录凭证 code", required = true)
    private String code;

    @ApiModelProperty(value = "分销绑定关系的ID(邀请码)")
    private String spread;
}
