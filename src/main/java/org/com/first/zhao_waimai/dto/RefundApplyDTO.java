package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户申请退款
 */
@Data
public class RefundApplyDTO {

    @NotBlank(message = "退款原因不能为空")
    @Size(max = 200, message = "退款原因不能超过 200 字")
    private String reason;
}
