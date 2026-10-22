package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CouponSaveDTO;
import org.com.first.zhao_waimai.dto.MerchantCouponVO;
import org.com.first.zhao_waimai.entity.Coupon;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.CouponMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.service.AdminCouponService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 平台券管理实现
 */
@Service
@RequiredArgsConstructor
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public List<MerchantCouponVO> list() {
        AdminGuard.requireAdmin();
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getCreatorType, Coupon.CREATOR_PLATFORM)
                        .orderByDesc(Coupon::getId))
                .stream().map(this::toVO).toList();
    }

    @Override
    public Long save(Long couponId, CouponSaveDTO dto) {
        AdminGuard.requireAdmin();
        validate(dto);
        Coupon coupon;
        if (couponId == null) {
            coupon = new Coupon();
            coupon.setShopId(null);
            coupon.setCreatorType(Coupon.CREATOR_PLATFORM);
            coupon.setRemainCount(dto.getTotalCount());
            fill(coupon, dto);
            couponMapper.insert(coupon);
        } else {
            coupon = requirePlatformCoupon(couponId);
            int used = coupon.getTotalCount() - coupon.getRemainCount();
            fill(coupon, dto);
            coupon.setRemainCount(Math.max(0, dto.getTotalCount() - used));
            couponMapper.updateById(coupon);
        }
        return coupon.getId();
    }

    @Override
    public void delete(Long couponId) {
        AdminGuard.requireAdmin();
        requirePlatformCoupon(couponId);
        couponMapper.deleteById(couponId);
    }

    @Override
    public void updateStatus(Long couponId, Integer status) {
        AdminGuard.requireAdmin();
        if (status == null || (status != Coupon.STATUS_ENABLED && status != Coupon.STATUS_DISABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "券状态不合法");
        }
        requirePlatformCoupon(couponId);
        Coupon update = new Coupon();
        update.setId(couponId);
        update.setStatus(status);
        couponMapper.updateById(update);
    }

    private void validate(CouponSaveDTO dto) {
        if (dto.getEndTime().isBefore(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间需晚于开始时间");
        }
        if (dto.getType() == Coupon.TYPE_DISCOUNT
                && dto.getAmount().compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "折扣率需小于 1，如 0.85");
        }
    }

    private void fill(Coupon coupon, CouponSaveDTO dto) {
        coupon.setName(dto.getName());
        coupon.setType(dto.getType());
        coupon.setThreshold(dto.getThreshold() == null ? BigDecimal.ZERO : dto.getThreshold());
        coupon.setAmount(dto.getAmount());
        coupon.setTotalCount(dto.getTotalCount());
        coupon.setStartTime(dto.getStartTime());
        coupon.setEndTime(dto.getEndTime());
        coupon.setStatus(dto.getStatus() == null ? Coupon.STATUS_ENABLED : dto.getStatus());
    }

    private Coupon requirePlatformCoupon(Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getCreatorType() == null
                || coupon.getCreatorType() != Coupon.CREATOR_PLATFORM) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "平台券不存在");
        }
        return coupon;
    }

    private MerchantCouponVO toVO(Coupon coupon) {
        Long received = userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                .eq(UserCoupon::getCouponId, coupon.getId()));
        return MerchantCouponVO.builder()
                .id(coupon.getId())
                .name(coupon.getName())
                .type(coupon.getType())
                .threshold(coupon.getThreshold())
                .amount(coupon.getAmount())
                .totalCount(coupon.getTotalCount())
                .remainCount(coupon.getRemainCount())
                .receivedCount(received == null ? 0 : received.intValue())
                .startTime(coupon.getStartTime())
                .endTime(coupon.getEndTime())
                .status(coupon.getStatus())
                .build();
    }
}
