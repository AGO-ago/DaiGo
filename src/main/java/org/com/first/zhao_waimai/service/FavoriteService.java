package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.ShopVO;

import java.util.List;

/**
 * 收藏服务
 */
public interface FavoriteService {

    /**
     * 我的收藏（档口）
     */
    List<ShopVO> list(Long userId);

    /**
     * 已收藏的档口 id 列表（用于详情页状态回显）
     */
    List<Long> shopIds(Long userId);

    /**
     * 收藏档口
     */
    void add(Long userId, Long shopId);

    /**
     * 取消收藏
     */
    void remove(Long userId, Long shopId);
}
