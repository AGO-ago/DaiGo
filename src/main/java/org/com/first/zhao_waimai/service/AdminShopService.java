package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminShopVO;
import org.com.first.zhao_waimai.dto.PageDTO;

/**
 * 平台端档口管理
 */
public interface AdminShopService {

    /** 档口分页查询 */
    PageDTO<AdminShopVO> list(String keyword, Integer status, Long mallId, long page, long size);

    /** 上/下线（强制下架后不可接单、用户端不展示） */
    void updateStatus(Long shopId, Integer status);

    /** 设置抽佣比例（0-0.30） */
    void updateCommission(Long shopId, java.math.BigDecimal commissionRate);
}
