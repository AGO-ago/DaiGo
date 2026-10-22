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
 * 秒杀活动
 */
@Data
@TableName("seckill_activity")
public class SeckillActivity {

    /** 状态：下线 */
    public static final int STATUS_OFFLINE = 0;
    /** 状态：上线 */
    public static final int STATUS_ONLINE = 1;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品 id */
    private Long dishId;

    /** 档口 id */
    private Long shopId;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer soldCount;

    /** 每人限购份数 */
    private Integer limitPerUser;

    /** 状态：0 下线 1 上线 */
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
