package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家入驻申请提交
 */
@Data
public class MerchantApplySaveDTO {

    @NotBlank(message = "档口名称不能为空")
    @Size(max = 64, message = "档口名称不能超过 64 字")
    private String shopName;

    @NotNull(message = "请选择所属美食城")
    private Long mallId;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 32, message = "联系人不能超过 32 字")
    private String contact;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    private String licenseImg;

    @Size(max = 64, message = "营业时间不能超过 64 字")
    private String businessHours;
}
