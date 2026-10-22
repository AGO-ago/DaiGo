package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.ApplyApproveDTO;
import org.com.first.zhao_waimai.dto.MerchantApplyVO;
import org.com.first.zhao_waimai.dto.PageDTO;

/**
 * 平台端入驻审核
 */
public interface AdminApplyService {

    /** 入驻申请分页（status 为空查全部） */
    PageDTO<MerchantApplyVO> list(Integer status, long page, long size);

    /** 审核通过：绑定已有空档口或按申请信息新建档口 */
    void approve(Long adminId, Long applyId, ApplyApproveDTO dto);

    /** 审核驳回 */
    void reject(Long adminId, Long applyId, String reason);
}
