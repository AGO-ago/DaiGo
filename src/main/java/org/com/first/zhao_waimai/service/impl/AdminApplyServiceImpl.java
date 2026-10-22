package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.ApplyApproveDTO;
import org.com.first.zhao_waimai.dto.MerchantApplyVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.MerchantApply;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.MerchantApplyMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AdminApplyService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台端入驻审核实现
 */
@Service
@RequiredArgsConstructor
public class AdminApplyServiceImpl implements AdminApplyService {

    private final MerchantApplyMapper merchantApplyMapper;
    private final ShopMapper shopMapper;
    private final MallMapper mallMapper;
    private final UserMapper userMapper;

    @Override
    public PageDTO<MerchantApplyVO> list(Integer status, long page, long size) {
        AdminGuard.requireAdmin();
        Page<MerchantApply> result = merchantApplyMapper.selectPage(new Page<>(page, size),
                Wrappers.<MerchantApply>lambdaQuery()
                        .eq(status != null, MerchantApply::getStatus, status)
                        .orderByAsc(MerchantApply::getStatus)
                        .orderByDesc(MerchantApply::getId));
        List<MerchantApply> applies = result.getRecords();
        Map<Long, String> mallNames = loadMallNames(applies);
        Map<Long, String> userNames = loadUserNames(applies);
        PageDTO<MerchantApplyVO> dto = new PageDTO<>();
        dto.setList(applies.stream()
                .map(a -> toVO(a, mallNames.get(a.getMallId()), userNames.get(a.getUserId())))
                .toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long adminId, Long applyId, ApplyApproveDTO dto) {
        AdminGuard.requireAdmin();
        MerchantApply apply = requirePending(applyId);

        Shop bound = shopMapper.selectOne(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMerchantUserId, apply.getUserId())
                .last("LIMIT 1"));
        if (bound != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该账号已绑定档口：" + bound.getName());
        }

        Long shopId;
        if (dto != null && dto.getShopId() != null) {
            Shop shop = shopMapper.selectById(dto.getShopId());
            if (shop == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "档口不存在");
            }
            if (shop.getMerchantUserId() != null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该档口已被其他商家绑定");
            }
            shopId = shop.getId();
        } else {
            Shop shop = new Shop();
            shop.setMallId(apply.getMallId());
            shop.setName(apply.getShopName());
            shop.setPhone(apply.getPhone());
            shop.setBusinessHours(apply.getBusinessHours());
            shop.setDeliveryFee(dto != null && dto.getDeliveryFee() != null
                    ? dto.getDeliveryFee() : new BigDecimal("2.00"));
            shop.setMinOrderAmount(dto != null && dto.getMinOrderAmount() != null
                    ? dto.getMinOrderAmount() : new BigDecimal("15.00"));
            shop.setAverageDeliveryTime(dto != null && dto.getAverageDeliveryTime() != null
                    ? dto.getAverageDeliveryTime() : 30);
            shop.setStatus(1);
            shop.setAutoAccept(0);
            shopMapper.insert(shop);
            shopId = shop.getId();
        }

        int updated = shopMapper.update(null, Wrappers.<Shop>lambdaUpdate()
                .eq(Shop::getId, shopId)
                .isNull(Shop::getMerchantUserId)
                .set(Shop::getMerchantUserId, apply.getUserId()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "档口绑定失败，请刷新后重试");
        }

        MerchantApply update = new MerchantApply();
        update.setId(apply.getId());
        update.setStatus(MerchantApply.STATUS_APPROVED);
        update.setShopId(shopId);
        update.setAuditAdminId(adminId);
        update.setAuditTime(LocalDateTime.now());
        merchantApplyMapper.updateById(update);
    }

    @Override
    public void reject(Long adminId, Long applyId, String reason) {
        AdminGuard.requireAdmin();
        MerchantApply apply = requirePending(applyId);
        MerchantApply update = new MerchantApply();
        update.setId(apply.getId());
        update.setStatus(MerchantApply.STATUS_REJECTED);
        update.setRejectReason(reason);
        update.setAuditAdminId(adminId);
        update.setAuditTime(LocalDateTime.now());
        merchantApplyMapper.updateById(update);
    }

    private MerchantApply requirePending(Long applyId) {
        MerchantApply apply = merchantApplyMapper.selectById(applyId);
        if (apply == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "入驻申请不存在");
        }
        if (apply.getStatus() == null || apply.getStatus() != MerchantApply.STATUS_PENDING) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该申请已处理，不能重复审核");
        }
        return apply;
    }

    private Map<Long, String> loadMallNames(List<MerchantApply> applies) {
        List<Long> ids = applies.stream().map(MerchantApply::getMallId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Mall mall : mallMapper.selectList(Wrappers.<Mall>lambdaQuery().in(Mall::getId, ids))) {
            map.put(mall.getId(), mall.getName());
        }
        return map;
    }

    private Map<Long, String> loadUserNames(List<MerchantApply> applies) {
        List<Long> ids = applies.stream().map(MerchantApply::getUserId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids))) {
            map.put(user.getId(), user.getNickname());
        }
        return map;
    }

    private MerchantApplyVO toVO(MerchantApply apply, String mallName, String userName) {
        MerchantApplyVO vo = new MerchantApplyVO();
        vo.setId(apply.getId());
        vo.setUserId(apply.getUserId());
        vo.setUserName(userName);
        vo.setShopName(apply.getShopName());
        vo.setMallId(apply.getMallId());
        vo.setMallName(mallName);
        vo.setContact(apply.getContact());
        vo.setPhone(apply.getPhone());
        vo.setLicenseImg(apply.getLicenseImg());
        vo.setBusinessHours(apply.getBusinessHours());
        vo.setStatus(apply.getStatus());
        vo.setRejectReason(apply.getRejectReason());
        vo.setShopId(apply.getShopId());
        vo.setAuditTime(apply.getAuditTime());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }
}
