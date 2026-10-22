package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评价回复请求
 */
@Data
public class ReplyDTO {

    /** 回复内容 */
    @NotBlank(message = "回复内容不能为空")
    @Size(max = 200, message = "回复内容不能超过 200 个字符")
    private String content;
}
