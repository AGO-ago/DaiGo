package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家菜品管理列表（分类 + 菜品）
 */
@Data
public class MerchantDishListVO {

    /** 分类 */
    private List<Category> categories = new ArrayList<>();

    /** 菜品 */
    private List<DishItem> dishes = new ArrayList<>();

    @Data
    public static class Category {
        private Long id;
        private String name;
        private Integer sort;
    }

    @Data
    public static class DishItem {
        private Long id;
        private Long categoryId;
        private String categoryName;
        private String name;
        private String image;
        private BigDecimal price;
        private BigDecimal originalPrice;
        private String description;
        private Integer status;
        private Integer sort;
        private Integer soldCount;
    }
}
