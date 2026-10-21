package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.util.List;

/**
 * 菜品树节点（按分类分组）
 */
@Data
public class DishTreeVO {

    /** 分类 id */
    private Long categoryId;

    /** 分类名称 */
    private String categoryName;

    /** 分类下菜品 */
    private List<DishVO> dishes;
}
