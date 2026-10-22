package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价隐藏入参
 */
@Data
public class CommentHideDTO {

    /** 隐藏：1 隐藏 0 取消隐藏 */
    @NotNull(message = "请选择操作类型")
    private Integer hidden;

    /** 隐藏原因（隐藏时必填） */
    @Size(max = 200, message = "隐藏原因不能超过 200 字")
    private String reason;
}
