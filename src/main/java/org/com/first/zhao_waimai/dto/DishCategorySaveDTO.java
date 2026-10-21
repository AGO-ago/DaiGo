package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家新增/编辑菜品分类
 */
@Data
public class DishCategorySaveDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 32, message = "分类名称最长 32 字")
    private String name;

    /** 排序值，越小越靠前 */
    private Integer sort;
}
