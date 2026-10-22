package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单项响应
 */
@Data
public class OrderItemVO {

    /** 菜品 id */
    private Long dishId;

    /** 菜品名快照 */
    private String dishName;

    /** 规格 id */
    private Long specId;

    /** 规格名快照 */
    private String specName;

    /** 规格加价快照 */
    private BigDecimal specPrice;

    /** 菜品图快照 */
    private String dishImage;

    /** 单价快照 */
    private BigDecimal price;

    /** 数量 */
    private Integer count;

    /** 小计 */
    private BigDecimal amount;
}
