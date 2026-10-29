package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地址保存请求（新增/修改共用）
 */
@Data
public class AddressRequest {

    /** 联系人姓名 */
    @NotBlank(message = "联系人不能为空")
    private String contact;

    /** 公司名称 */
    @NotBlank(message = "公司不能为空")
    private String company;

    /** 楼栋 */
    @NotBlank(message = "楼栋不能为空")
    private String building;

    /** 楼层 */
    private String floor;

    /** 工位/房号 */
    private String office;

    /** 地图选点详细地址 */
    private String address;

    /** 纬度（地图选点） */
    private BigDecimal latitude;

    /** 经度（地图选点） */
    private BigDecimal longitude;

    /** 联系电话 */
    @NotBlank(message = "联系电话不能为空")
    private String phone;

    /** 是否设为默认 */
    private Boolean isDefault = Boolean.FALSE;
}
