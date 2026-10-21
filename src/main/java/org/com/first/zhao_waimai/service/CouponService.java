package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CouponVO;
import org.com.first.zhao_waimai.dto.UserCouponVO;

import java.util.List;

/**
 * 优惠券服务
 */
public interface CouponService {

    /**
     * 领券中心（有效期内的可领券）
     */
    List<CouponVO> listAvailable(Long userId);

    /**
     * 领取优惠券（每人每券限领一张）
     */
    void receive(Long userId, Long couponId);

    /**
     * 我的优惠券（status=0 未使用且未过期）
     */
    List<UserCouponVO> myCoupons(Long userId, Integer status);
}
