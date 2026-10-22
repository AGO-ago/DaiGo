package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台端评价展示
 */
@Data
public class AdminCommentVO {

    /** 评价 id */
    private Long id;

    /** 子订单 id */
    private Long orderId;

    /** 档口 id */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

    /** 评价用户 id */
    private Long userId;

    /** 评价用户展示名 */
    private String userName;

    /** 评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 评价图，逗号分隔 */
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

    /** 评价时间 */
    private LocalDateTime createTime;
}
