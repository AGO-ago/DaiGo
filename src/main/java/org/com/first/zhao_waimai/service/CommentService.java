package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CommentRequest;
import org.com.first.zhao_waimai.dto.CommentVO;

import java.util.List;

/**
 * 订单评价服务
 */
public interface CommentService {

    /**
     * 提交评价（仅本人已完成订单，且每单只能评价一次）
     */
    void create(Long userId, CommentRequest request);

    /**
     * 档口评价列表（公开）
     */
    List<CommentVO> listByShop(Long shopId);
}
