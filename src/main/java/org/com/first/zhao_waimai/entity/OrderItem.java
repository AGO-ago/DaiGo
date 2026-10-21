package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单项（下单时快照）
 */
@Data
@TableName("order_item")
public class OrderItem {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属子订单 id */
    private Long orderId;

    /** 菜品 id */
    private Long dishId;

    /** 菜品名称快照 */
    private String dishName;

    /** 规格名快照 */
    private String specName;

    /** 菜品图快照 */
    private String dishImage;

    /** 成交单价快照 */
    private BigDecimal price;

    /** 数量 */
    private Integer count;

    /** 小计 = 单价 * 数量 */
    private BigDecimal amount;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
