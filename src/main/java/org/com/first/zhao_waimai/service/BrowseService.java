package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.DishTreeVO;
import org.com.first.zhao_waimai.dto.MallVO;
import org.com.first.zhao_waimai.dto.ShopVO;

import java.util.List;

/**
 * 主数据浏览服务（美食城/档口/菜品只读）
 */
public interface BrowseService {

    /**
     * 营业中的美食城列表
     */
    List<MallVO> listMalls();

    /**
     * 美食城下档口列表
     */
    List<ShopVO> listShops(Long mallId);

    /**
     * 档口菜品树（按分类分组，仅上架）
     */
    List<DishTreeVO> getDishTree(Long shopId);
}
