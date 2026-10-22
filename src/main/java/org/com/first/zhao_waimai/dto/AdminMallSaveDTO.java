package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 美食城新增/编辑
 */
@Data
public class AdminMallSaveDTO {

    @NotBlank(message = "美食城名称不能为空")
    @Size(max = 64, message = "名称不能超过 64 字")
    private String name;

    @NotBlank(message = "地址不能为空")
    @Size(max = 200, message = "地址不能超过 200 字")
    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String coverImg;

    private String businessHours;

    /** 状态：0 关闭 1 营业 */
    private Integer status;
}
