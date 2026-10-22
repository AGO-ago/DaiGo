package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单评价
 */
@Data
@TableName("order_comment")
public class OrderComment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 子订单 id */
    private Long orderId;

    /** 评价用户 id */
    private Long userId;

    /** 档口 id */
    private Long shopId;

    /** 评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 评价图，逗号分隔 URL */
    private String images;

    /** 商家回复 */
    private String replyContent;

    /** 商家回复时间 */
    private LocalDateTime replyTime;

    /** 平台回复 */
    private String adminReply;

    /** 是否隐藏：0 否 1 是 */
    private Integer hidden;

    /** 隐藏原因 */
    private String hiddenReason;

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
