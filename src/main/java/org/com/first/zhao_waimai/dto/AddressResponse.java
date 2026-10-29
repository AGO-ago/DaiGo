package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地址响应
 */
@Data
@Builder
public class AddressResponse {

    /** 地址 id */
    private Long id;

    /** 联系人姓名 */
    private String contact;

    /** 公司名称 */
    private String company;

    /** 楼栋 */
    private String building;

    /** 楼层 */
    private String floor;

    /** 工位/房号 */
    private String office;

    /** 地图选点详细地址 */
    private String address;

    /** 纬度 */
    private BigDecimal latitude;

    /** 经度 */
    private BigDecimal longitude;

    /** 联系电话 */
    private String phone;

    /** 是否默认：0 否 1 是 */
    private Integer isDefault;
}
