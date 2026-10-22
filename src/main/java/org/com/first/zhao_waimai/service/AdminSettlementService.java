package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SettlementVO;

import java.time.LocalDate;

/**
 * 平台端结算管理
 */
public interface AdminSettlementService {

    /**
     * 按日期生成结算单（幂等，已存在则跳过）
     */
    int generate(LocalDate date);

    /**
     * 定时任务调用：与 generate 一致，但不校验管理员身份
     */
    int generateByTask(LocalDate date);

    /**
     * 结算单分页查询
     */
    PageDTO<SettlementVO> list(Long shopId, Integer status, long page, long size);

    /**
     * 标记已结算
     */
    void settle(Long settlementId);
}
