package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminSeckillVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SeckillSaveDTO;

/**
 * 平台端秒杀活动管理
 */
public interface AdminSeckillService {

    /**
     * 活动分页查询
     */
    PageDTO<AdminSeckillVO> list(String keyword, Integer status, long page, long size);

    /**
     * 新建或编辑活动
     */
    Long save(Long activityId, SeckillSaveDTO dto);

    /**
     * 删除活动
     */
    void delete(Long activityId);

    /**
     * 上下线
     */
    void updateStatus(Long activityId, Integer status);
}
