package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 热销菜品（首页今日抢购）
 */
@Data
@Builder
public class HotDishVO {

    /** 菜品 id */
    private Long id;

    /** 档口 id */
    private Long shopId;

    /** 档口名 */
    private String shopName;

    /** 菜品名 */
    private String name;

    /** 图片 */
    private String image;

    /** 售价 */
    private BigDecimal price;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 销量 */
    private Integer soldCount;
}
