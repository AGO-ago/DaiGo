package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.dto.SeckillVO;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.SeckillActivity;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.SeckillActivityMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.SeckillService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户端秒杀实现
 */
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final DishMapper dishMapper;
    private final ShopMapper shopMapper;

    @Override
    public List<SeckillVO> listActive() {
        LocalDateTime now = LocalDateTime.now();
        List<SeckillActivity> activities = seckillActivityMapper.selectList(
                Wrappers.<SeckillActivity>lambdaQuery()
                        .eq(SeckillActivity::getStatus, SeckillActivity.STATUS_ONLINE)
                        .le(SeckillActivity::getStartTime, now)
                        .ge(SeckillActivity::getEndTime, now)
                        .apply("sold_count < stock")
                        .orderByAsc(SeckillActivity::getEndTime));
        if (activities.isEmpty()) {
            return List.of();
        }
        List<Long> dishIds = activities.stream().map(SeckillActivity::getDishId).distinct().toList();
        Map<Long, Dish> dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                        .in(Dish::getId, dishIds)
                        .eq(Dish::getStatus, 1))
                .stream().collect(Collectors.toMap(Dish::getId, Function.identity()));
        List<Long> shopIds = activities.stream().map(SeckillActivity::getShopId).distinct().toList();
        Map<Long, Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                        .in(Shop::getId, shopIds)
                        .eq(Shop::getStatus, 1))
                .stream().collect(Collectors.toMap(Shop::getId, Function.identity()));
        return activities.stream()
                .filter(a -> dishes.containsKey(a.getDishId()) && shops.containsKey(a.getShopId()))
                .map(a -> toVO(a, dishes.get(a.getDishId()), shops.get(a.getShopId())))
                .toList();
    }

    private SeckillVO toVO(SeckillActivity activity, Dish dish, Shop shop) {
        SeckillVO vo = new SeckillVO();
        vo.setId(activity.getId());
        vo.setDishId(dish.getId());
        vo.setName(dish.getName());
        vo.setImage(dish.getImage());
        vo.setPrice(activity.getSeckillPrice());
        vo.setOriginalPrice(dish.getPrice());
        vo.setShopId(shop.getId());
        vo.setShopName(shop.getName());
        vo.setMallId(shop.getMallId());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setRemainStock(Math.max(0, activity.getStock() - activity.getSoldCount()));
        vo.setLimitPerUser(activity.getLimitPerUser());
        return vo;
    }
}
