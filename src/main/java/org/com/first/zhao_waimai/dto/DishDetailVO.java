package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品详情（用户端规格选择）
 */
@Data
@Builder
public class DishDetailVO {

    /** 菜品 id */
    private Long id;

    /** 所属档口 id */
    private Long shopId;

    /** 所属档口名 */
    private String shopName;

    /** 菜品名称 */
    private String name;

    /** 菜品图 */
    private String image;

    /** 售价（不含规格加价） */
    private BigDecimal price;

    /** 原价（划线价） */
    private BigDecimal originalPrice;

    /** 描述 */
    private String description;

    /** 累计销量 */
    private Integer soldCount;

    /** 状态：0 下架 1 上架 */
    private Integer status;

    /** 启用中的规格列表 */
    private List<DishSpecVO> specs;
}
