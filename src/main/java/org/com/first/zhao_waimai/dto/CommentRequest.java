package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交评价请求
 */
@Data
public class CommentRequest {

    /** 子订单 id */
    @NotNull(message = "订单不能为空")
    private Long orderId;

    /** 评分 1-5 */
    @NotNull(message = "请选择评分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;

    /** 评价内容 */
    @Size(max = 500, message = "评价内容最多 500 字")
    private String content;
}
