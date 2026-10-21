package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家新增/编辑菜品
 */
@Data
public class DishSaveDTO {

    @NotNull(message = "请选择菜品分类")
    private Long categoryId;

    @NotBlank(message = "菜品名称不能为空")
    @Size(max = 64, message = "菜品名称最长 64 字")
    private String name;

    @Size(max = 500, message = "图片地址过长")
    private String image;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0.01", message = "售价必须大于 0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "原价不能为负")
    private BigDecimal originalPrice;

    @Size(max = 200, message = "描述最长 200 字")
    private String description;

    /** 状态：0 下架 1 上架 */
    @NotNull(message = "上架状态不能为空")
    private Integer status;

    /** 排序值，越小越靠前 */
    private Integer sort;
}
