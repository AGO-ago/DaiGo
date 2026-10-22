package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 退款/拒单原因
 */
@Data
public class ReasonDTO {

    @NotBlank(message = "原因不能为空")
    @Size(max = 200, message = "原因不能超过 200 字")
    private String reason;
}
