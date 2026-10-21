package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.DishTreeVO;
import org.com.first.zhao_waimai.dto.HotDishVO;
import org.com.first.zhao_waimai.dto.MallVO;
import org.com.first.zhao_waimai.dto.ShopVO;

import java.util.List;

/**
 * 主数据浏览服务（美食城/档口/菜品只读）
 */
public interface BrowseService {

    /**
     * 营业中的美食城列表（含档口数与缩略图；lat/lng 非空时计算距离）
     */
    List<MallVO> listMalls(Double lat, Double lng);

    /**
     * 美食城详情（含档口数与缩略图）
     */
    MallVO getMall(Long id, Double lat, Double lng);

    /**
     * 美食城下档口列表
     */
    List<ShopVO> listShops(Long mallId);

    /**
     * 档口菜品树（按分类分组，仅上架）
     */
    List<DishTreeVO> getDishTree(Long shopId);

    /**
     * 热销菜品（首页今日抢购，按销量倒序）
     */
    List<HotDishVO> listHotDishes(int limit, Double lat, Double lng);
}
