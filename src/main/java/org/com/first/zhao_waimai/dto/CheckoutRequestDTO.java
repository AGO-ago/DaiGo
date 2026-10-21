package org.com.first.zhao_waimai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 结算请求（购物车提交）
 */
@Data
public class CheckoutRequestDTO {

    /** 收货地址 id */
    @NotNull(message = "请选择收货地址")
    private Long addressId;

    /** 订单备注 */
    @Size(max = 200, message = "备注最长 200 字")
    private String remark;

    /** 购物车条目 */
    @NotEmpty(message = "购物车为空")
    @Valid
    private List<CheckoutItemDTO> items;
}
