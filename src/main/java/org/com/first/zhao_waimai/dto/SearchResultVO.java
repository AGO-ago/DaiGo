package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 搜索结果（档口 + 菜品）
 */
@Data
@Builder
public class SearchResultVO {

    /** 匹配的档口 */
    private List<ShopVO> shops;

    /** 匹配的菜品 */
    private List<HotDishVO> dishes;
}
