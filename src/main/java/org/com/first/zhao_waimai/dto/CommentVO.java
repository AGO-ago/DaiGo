package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价展示
 */
@Data
@Builder
public class CommentVO {

    /** 评价 id */
    private Long id;

    /** 档口 id */
    private Long shopId;

    /** 评分 1-5 */
    private Integer score;

    /** 评价内容 */
    private String content;

    /** 评价图，逗号分隔 */
    private String images;

    /** 评价人展示名（脱敏） */
    private String userName;

    /** 评价时间 */
    private LocalDateTime createTime;

    /** 商家回复 */
    private String replyContent;

    /** 商家回复时间 */
    private LocalDateTime replyTime;
}
