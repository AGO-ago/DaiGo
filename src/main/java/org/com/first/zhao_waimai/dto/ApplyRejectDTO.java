package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 入驻申请驳回
 */
@Data
public class ApplyRejectDTO {

    @NotBlank(message = "驳回原因不能为空")
    private String reason;
}
