package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品响应
 */
@Data
@Builder
public class DishVO {

    /** 菜品 id */
    private Long id;

    /** 菜品名称 */
    private String name;

    /** 菜品图 URL */
    private String image;

    /** 售价 */
    private BigDecimal price;

    /** 原价（划线价） */
    private BigDecimal originalPrice;

    /** 描述 */
    private String description;

    /** 累计销量 */
    private Integer soldCount;

    /** 状态：0 下架 1 上架 */
    private Integer status;

    /** 启用中的规格列表（无规格为空数组） */
    private List<DishSpecVO> specs;
}
