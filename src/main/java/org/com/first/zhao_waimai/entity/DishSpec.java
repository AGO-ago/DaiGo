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
 * 菜品规格/口味
 */
@Data
@TableName("dish_spec")
public class DishSpec {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属菜品 id */
    private Long dishId;

    /** 规格名，如 微辣/大份 */
    private String name;

    /** 加价（可空为不加价） */
    private BigDecimal price;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 状态：0 停用 1 启用 */
    private Integer status;

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
