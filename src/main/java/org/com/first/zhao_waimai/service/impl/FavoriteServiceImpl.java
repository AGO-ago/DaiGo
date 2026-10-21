package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.ShopVO;
import org.com.first.zhao_waimai.entity.Favorite;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.FavoriteMapper;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.FavoriteService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务实现
 */
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ShopMapper shopMapper;
    private final MallMapper mallMapper;

    @Override
    public List<ShopVO> list(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getType, 1)
                .orderByDesc(Favorite::getId));
        if (favorites.isEmpty()) {
            return List.of();
        }
        List<Long> shopIds = favorites.stream().map(Favorite::getShopId).distinct().toList();
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds));
        Map<Long, Shop> shopMap = new HashMap<>();
        shops.forEach(s -> shopMap.put(s.getId(), s));

        Map<Long, Mall> mallMap = new HashMap<>();
        List<Long> mallIds = shops.stream().map(Shop::getMallId).distinct().toList();
        if (!mallIds.isEmpty()) {
            mallMapper.selectList(Wrappers.<Mall>lambdaQuery().in(Mall::getId, mallIds))
                    .forEach(m -> mallMap.put(m.getId(), m));
        }

        // 按收藏时间倒序输出
        return favorites.stream()
                .map(f -> shopMap.get(f.getShopId()))
                .filter(s -> s != null)
                .map(s -> {
                    Mall mall = mallMap.get(s.getMallId());
                    return ShopVO.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .logo(s.getLogo())
                            .deliveryFee(s.getDeliveryFee())
                            .minOrderAmount(s.getMinOrderAmount())
                            .averageDeliveryTime(s.getAverageDeliveryTime())
                            .businessHours(s.getBusinessHours())
                            .status(s.getStatus())
                            .mallId(s.getMallId())
                            .mallName(mall == null ? "" : mall.getName())
                            .build();
                })
                .toList();
    }

    @Override
    public List<Long> shopIds(Long userId) {
        return favoriteMapper.selectList(Wrappers.<Favorite>lambdaQuery()
                        .eq(Favorite::getUserId, userId)
                        .eq(Favorite::getType, 1))
                .stream().map(Favorite::getShopId).toList();
    }

    @Override
    public void add(Long userId, Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档口不存在");
        }
        favoriteMapper.upsert(userId, shopId);
    }

    @Override
    public void remove(Long userId, Long shopId) {
        favoriteMapper.delete(Wrappers.<Favorite>lambdaQuery()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getShopId, shopId)
                .eq(Favorite::getType, 1));
    }
}
