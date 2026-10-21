package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家修改档口信息
 */
@Data
public class ShopUpdateDTO {

    @NotBlank(message = "档口名称不能为空")
    @Size(max = 64, message = "档口名称最长 64 字")
    private String name;

    @Size(max = 500, message = "logo 地址过长")
    private String logo;

    @Size(max = 20, message = "联系电话过长")
    private String phone;

    @NotNull(message = "配送费不能为空")
    @DecimalMin(value = "0", message = "配送费不能为负")
    private BigDecimal deliveryFee;

    @NotNull(message = "起送价不能为空")
    @DecimalMin(value = "0", message = "起送价不能为负")
    private BigDecimal minOrderAmount;

    @NotNull(message = "预计送达时间不能为空")
    private Integer averageDeliveryTime;

    @Size(max = 64, message = "营业时间过长")
    private String businessHours;

    /** 状态：0 休息 1 营业 */
    @NotNull(message = "营业状态不能为空")
    private Integer status;
}
