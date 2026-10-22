package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 结算响应中的订单项
 */
@Data
public class CheckoutItemVO {

    /** 菜品 id */
    private Long dishId;

    /** 规格 id（无规格为空） */
    private Long specId;

    /** 规格名 */
    private String specName;

    /** 规格加价 */
    private BigDecimal specPrice;

    /** 菜品名 */
    private String dishName;

    /** 菜品图 */
    private String dishImage;

    /** 单价 */
    private BigDecimal price;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal amount;
}
