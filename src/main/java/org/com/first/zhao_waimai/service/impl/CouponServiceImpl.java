package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CouponVO;
import org.com.first.zhao_waimai.dto.UserCouponVO;
import org.com.first.zhao_waimai.entity.Coupon;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.CouponMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.service.CouponService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 优惠券服务实现
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final ShopMapper shopMapper;

    @Override
    public List<CouponVO> listAvailable(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Coupon> coupons = couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                .le(Coupon::getStartTime, now)
                .ge(Coupon::getEndTime, now)
                .gt(Coupon::getRemainCount, 0)
                .orderByAsc(Coupon::getThreshold));
        if (coupons.isEmpty()) {
            return List.of();
        }
        Set<Long> received = new HashSet<>();
        for (UserCoupon uc : userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId))) {
            received.add(uc.getCouponId());
        }
        Map<Long, String> shopNames = loadShopNames(coupons);
        return coupons.stream().map(c -> CouponVO.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType())
                .threshold(c.getThreshold())
                .amount(c.getAmount())
                .remainCount(c.getRemainCount())
                .startTime(c.getStartTime())
                .endTime(c.getEndTime())
                .shopId(c.getShopId())
                .shopName(c.getShopId() == null ? "平台通用" : shopNames.getOrDefault(c.getShopId(), ""))
                .received(received.contains(c.getId()))
                .build()).toList();
    }

    @Override
    public void receive(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券不在领取时间内");
        }
        long received = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (received > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "您已领取过该优惠券");
        }
        // 条件更新防超发
        int updated = couponMapper.update(null, Wrappers.<Coupon>lambdaUpdate()
                .eq(Coupon::getId, couponId)
                .gt(Coupon::getRemainCount, 0)
                .setSql("remain_count = remain_count - 1"));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券已抢光");
        }
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(userId);
        userCoupon.setCouponId(couponId);
        userCoupon.setStatus(UserCoupon.STATUS_UNUSED);
        userCouponMapper.insert(userCoupon);
    }

    @Override
    public List<UserCouponVO> myCoupons(Long userId, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        List<UserCoupon> list = userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getUserId, userId)
                .eq(status != null, UserCoupon::getStatus, status)
                .orderByDesc(UserCoupon::getId));
        if (list.isEmpty()) {
            return List.of();
        }
        List<Long> couponIds = list.stream().map(UserCoupon::getCouponId).distinct().toList();
        Map<Long, Coupon> couponMap = new HashMap<>();
        couponMapper.selectList(Wrappers.<Coupon>lambdaQuery().in(Coupon::getId, couponIds))
                .forEach(c -> couponMap.put(c.getId(), c));
        Map<Long, String> shopNames = loadShopNames(couponMap.values().stream().toList());

        return list.stream()
                .map(uc -> {
                    Coupon c = couponMap.get(uc.getCouponId());
                    if (c == null) {
                        return null;
                    }
                    // 未使用但已过期的券不展示在"可用"列表
                    if (status != null && status == UserCoupon.STATUS_UNUSED && c.getEndTime().isBefore(now)) {
                        return null;
                    }
                    return UserCouponVO.builder()
                            .id(uc.getId())
                            .couponId(c.getId())
                            .name(c.getName())
                            .type(c.getType())
                            .threshold(c.getThreshold())
                            .amount(c.getAmount())
                            .shopId(c.getShopId())
                            .shopName(c.getShopId() == null ? "平台通用" : shopNames.getOrDefault(c.getShopId(), ""))
                            .status(uc.getStatus())
                            .endTime(c.getEndTime())
                            .build();
                })
                .filter(v -> v != null)
                .toList();
    }

    private Map<Long, String> loadShopNames(List<Coupon> coupons) {
        Map<Long, String> map = new HashMap<>();
        List<Long> shopIds = coupons.stream().map(Coupon::getShopId).filter(id -> id != null).distinct().toList();
        if (!shopIds.isEmpty()) {
            for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
                map.put(shop.getId(), shop.getName());
            }
        }
        return map;
    }
}
