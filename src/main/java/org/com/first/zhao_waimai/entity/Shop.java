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
 * 档口/商家
 */
@Data
@TableName("shop")
public class Shop {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属美食城 id */
    private Long mallId;

    /** 档口名称 */
    private String name;

    /** 档口 logo URL */
    private String logo;

    /** 联系电话 */
    private String phone;

    /** 固定配送费 */
    private BigDecimal deliveryFee;

    /** 起送价 */
    private BigDecimal minOrderAmount;

    /** 预计送达分钟数 */
    private Integer averageDeliveryTime;

    /** 营业时间 */
    private String businessHours;

    /** 状态：0 休息 1 营业 */
    private Integer status;

    /** 档口公告 */
    private String announcement;

    /** 自动接单：0 否 1 是 */
    private Integer autoAccept;

    /** 抽佣比例，空取平台默认 */
    private BigDecimal commissionRate;

    /** 绑定商家账号 id（平台开通后写入，空表示未绑定） */
    private Long merchantUserId;

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
