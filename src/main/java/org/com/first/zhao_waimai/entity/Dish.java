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
 * 菜品
 */
@Data
@TableName("dish")
public class Dish {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属档口 id */
    private Long shopId;

    /** 所属分类 id */
    private Long categoryId;

    /** 菜品名称 */
    private String name;

    /** 菜品图 URL */
    private String image;

    /** 售价 */
    private BigDecimal price;

    /** 原价（划线价，可空） */
    private BigDecimal originalPrice;

    /** 菜品描述 */
    private String description;

    /** 状态：0 下架 1 上架 */
    private Integer status;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 累计销量 */
    private Integer soldCount;

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
