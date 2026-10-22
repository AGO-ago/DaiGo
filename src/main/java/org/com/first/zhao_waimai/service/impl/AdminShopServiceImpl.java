package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminShopVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AdminShopService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台端档口管理实现
 */
@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopMapper shopMapper;
    private final MallMapper mallMapper;
    private final UserMapper userMapper;

    @Override
    public PageDTO<AdminShopVO> list(String keyword, Integer status, Long mallId, long page, long size) {
        AdminGuard.requireAdmin();
        Page<Shop> result = shopMapper.selectPage(new Page<>(page, size), Wrappers.<Shop>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), Shop::getName, keyword)
                .eq(status != null, Shop::getStatus, status)
                .eq(mallId != null, Shop::getMallId, mallId)
                .orderByAsc(Shop::getId));
        List<Shop> shops = result.getRecords();
        Map<Long, String> mallNames = loadMallNames(shops);
        Map<Long, String> userNames = loadUserNames(shops);
        PageDTO<AdminShopVO> dto = new PageDTO<>();
        dto.setList(shops.stream().map(s -> toVO(s, mallNames.get(s.getMallId()),
                userNames.get(s.getMerchantUserId()))).toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public void updateStatus(Long shopId, Integer status) {
        AdminGuard.requireAdmin();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "档口状态不合法");
        }
        requireShop(shopId);
        Shop update = new Shop();
        update.setId(shopId);
        update.setStatus(status);
        shopMapper.updateById(update);
    }

    @Override
    public void updateCommission(Long shopId, BigDecimal commissionRate) {
        AdminGuard.requireAdmin();
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0
                || commissionRate.compareTo(new BigDecimal("0.30")) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "抽佣比例需在 0-0.30 之间");
        }
        requireShop(shopId);
        Shop update = new Shop();
        update.setId(shopId);
        update.setCommissionRate(commissionRate.setScale(4, RoundingMode.HALF_UP));
        shopMapper.updateById(update);
    }

    private Shop requireShop(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档口不存在");
        }
        return shop;
    }

    private Map<Long, String> loadMallNames(List<Shop> shops) {
        List<Long> ids = shops.stream().map(Shop::getMallId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Mall mall : mallMapper.selectList(Wrappers.<Mall>lambdaQuery().in(Mall::getId, ids))) {
            map.put(mall.getId(), mall.getName());
        }
        return map;
    }

    private Map<Long, String> loadUserNames(List<Shop> shops) {
        List<Long> ids = shops.stream().map(Shop::getMerchantUserId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids))) {
            map.put(user.getId(), user.getNickname());
        }
        return map;
    }

    private AdminShopVO toVO(Shop shop, String mallName, String merchantName) {
        AdminShopVO vo = new AdminShopVO();
        vo.setId(shop.getId());
        vo.setMallId(shop.getMallId());
        vo.setMallName(mallName);
        vo.setName(shop.getName());
        vo.setLogo(shop.getLogo());
        vo.setPhone(shop.getPhone());
        vo.setDeliveryFee(shop.getDeliveryFee());
        vo.setMinOrderAmount(shop.getMinOrderAmount());
        vo.setAverageDeliveryTime(shop.getAverageDeliveryTime());
        vo.setBusinessHours(shop.getBusinessHours());
        vo.setStatus(shop.getStatus());
        vo.setAnnouncement(shop.getAnnouncement());
        vo.setAutoAccept(shop.getAutoAccept());
        vo.setCommissionRate(shop.getCommissionRate());
        vo.setMerchantUserId(shop.getMerchantUserId());
        vo.setMerchantName(merchantName);
        vo.setCreateTime(shop.getCreateTime());
        return vo;
    }
}
