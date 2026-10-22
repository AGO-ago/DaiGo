package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CouponSaveDTO;
import org.com.first.zhao_waimai.dto.MerchantCouponVO;

import java.util.List;

/**
 * 平台券管理
 */
public interface AdminCouponService {

    /**
     * 平台券列表
     */
    List<MerchantCouponVO> list();

    /**
     * 新建或编辑平台券
     */
    Long save(Long couponId, CouponSaveDTO dto);

    /**
     * 删除平台券
     */
    void delete(Long couponId);

    /**
     * 启停平台券
     */
    void updateStatus(Long couponId, Integer status);
}
