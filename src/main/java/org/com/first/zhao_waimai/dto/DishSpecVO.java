package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品规格响应
 */
@Data
@Builder
public class DishSpecVO {

    /** 规格 id */
    private Long id;

    /** 所属菜品 id */
    private Long dishId;

    /** 规格名，如 微辣/大份 */
    private String name;

    /** 加价（可空为不加价） */
    private BigDecimal price;

    /** 排序值 */
    private Integer sort;

    /** 状态：0 停用 1 启用 */
    private Integer status;
}
