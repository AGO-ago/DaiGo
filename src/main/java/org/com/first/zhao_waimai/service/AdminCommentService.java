package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminCommentVO;
import org.com.first.zhao_waimai.dto.CommentHideDTO;
import org.com.first.zhao_waimai.dto.PageDTO;

/**
 * 平台端评价处理
 */
public interface AdminCommentService {

    /**
     * 评价分页查询（评分区间，便于筛选差评）
     */
    PageDTO<AdminCommentVO> list(Integer minScore, Integer maxScore, Integer hidden,
                                 String keyword, long page, long size);

    /**
     * 平台回复
     */
    void reply(Long commentId, String content);

    /**
     * 隐藏 / 取消隐藏评价
     */
    void hide(Long commentId, CommentHideDTO dto);
}
