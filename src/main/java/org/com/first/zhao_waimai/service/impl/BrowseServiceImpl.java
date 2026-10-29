package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.DishDetailVO;
import org.com.first.zhao_waimai.dto.DishSpecVO;
import org.com.first.zhao_waimai.dto.DishTreeVO;
import org.com.first.zhao_waimai.dto.DishVO;
import org.com.first.zhao_waimai.dto.HotDishVO;
import org.com.first.zhao_waimai.dto.MallVO;
import org.com.first.zhao_waimai.dto.SearchResultVO;
import org.com.first.zhao_waimai.dto.ShopBriefVO;
import org.com.first.zhao_waimai.dto.ShopVO;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.DishCategory;
import org.com.first.zhao_waimai.entity.DishSpec;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.DishCategoryMapper;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.DishSpecMapper;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.BrowseService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主数据浏览服务实现
 */
@Service
@RequiredArgsConstructor
public class BrowseServiceImpl implements BrowseService {

    /** 首页卡片最多展示的档口缩略个数 */
    private static final int SHOP_PREVIEW_LIMIT = 6;
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final MallMapper mallMapper;
    private final ShopMapper shopMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;

    @Override
    public List<MallVO> listMalls(Double lat, Double lng) {
        List<Mall> malls = mallMapper.selectList(Wrappers.<Mall>lambdaQuery()
                .eq(Mall::getStatus, 1)
                .orderByAsc(Mall::getId));
        if (malls.isEmpty()) {
            return List.of();
        }

        // 一次查出全部档口，按美食城分组，避免 N+1
        List<Long> mallIds = malls.stream().map(Mall::getId).toList();
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .in(Shop::getMallId, mallIds)
                .orderByAsc(Shop::getId));
        Map<Long, List<Shop>> shopMap = new HashMap<>();
        for (Shop shop : shops) {
            shopMap.computeIfAbsent(shop.getMallId(), k -> new ArrayList<>()).add(shop);
        }

        List<MallVO> result = malls.stream()
                .map(m -> buildMallVO(m, shopMap.getOrDefault(m.getId(), List.of()), lat, lng))
                .collect(Collectors.toCollection(ArrayList::new));
        // 传了定位时按距离由近到远排序，未维护坐标的排最后
        if (lat != null && lng != null) {
            result.sort(Comparator.comparing(MallVO::getDistance, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return result;
    }

    @Override
    public MallVO getMall(Long id, Double lat, Double lng) {
        Mall mall = mallMapper.selectById(id);
        if (mall == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "美食城不存在");
        }
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMallId, id)
                .orderByAsc(Shop::getId));
        return buildMallVO(mall, shops, lat, lng);
    }

    private MallVO buildMallVO(Mall m, List<Shop> mallShops, Double lat, Double lng) {
        List<ShopBriefVO> previews = mallShops.stream()
                .limit(SHOP_PREVIEW_LIMIT)
                .map(s -> ShopBriefVO.builder().id(s.getId()).name(s.getName()).logo(s.getLogo()).build())
                .toList();
        return MallVO.builder()
                .id(m.getId())
                .name(m.getName())
                .address(m.getAddress())
                .latitude(m.getLatitude() == null ? null : m.getLatitude().doubleValue())
                .longitude(m.getLongitude() == null ? null : m.getLongitude().doubleValue())
                .coverImg(m.getCoverImg())
                .businessHours(m.getBusinessHours())
                .shopCount(mallShops.size())
                .distance(distanceKm(lat, lng, m.getLatitude(), m.getLongitude()))
                .shops(previews)
                .build();
    }

    @Override
    public List<ShopVO> listShops(Long mallId) {
        Mall mall = mallMapper.selectById(mallId);
        if (mall == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "美食城不存在");
        }
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMallId, mallId)
                .orderByAsc(Shop::getId));
        return shops.stream().map(s -> ShopVO.builder()
                .id(s.getId())
                .name(s.getName())
                .logo(s.getLogo())
                .deliveryFee(s.getDeliveryFee())
                .minOrderAmount(s.getMinOrderAmount())
                .averageDeliveryTime(s.getAverageDeliveryTime())
                .businessHours(s.getBusinessHours())
                .status(s.getStatus())
                .build()).toList();
    }

    @Override
    public List<DishTreeVO> getDishTree(Long shopId) {
        Shop shop = shopMapper.selectById(shopId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "档口不存在");
        }
        List<DishCategory> categories = dishCategoryMapper.selectList(Wrappers.<DishCategory>lambdaQuery()
                .eq(DishCategory::getShopId, shopId)
                .orderByAsc(DishCategory::getSort));
        List<Dish> dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getShopId, shopId)
                .eq(Dish::getStatus, 1)
                .orderByAsc(Dish::getSort));
        Map<Long, List<DishSpecVO>> specMap = loadSpecs(dishes.stream().map(Dish::getId).toList());

        return categories.stream().map(c -> {
            DishTreeVO node = new DishTreeVO();
            node.setCategoryId(c.getId());
            node.setCategoryName(c.getName());
            node.setDishes(dishes.stream()
                    .filter(d -> c.getId().equals(d.getCategoryId()))
                    .map(d -> toDishVO(d, specMap.getOrDefault(d.getId(), List.of())))
                    .toList());
            return node;
        }).toList();
    }

    @Override
    public DishDetailVO getDish(Long dishId) {
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || dish.getStatus() == null || dish.getStatus() == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在或已下架");
        }
        Shop shop = shopMapper.selectById(dish.getShopId());
        return DishDetailVO.builder()
                .id(dish.getId())
                .shopId(dish.getShopId())
                .shopName(shop == null ? "" : shop.getName())
                .name(dish.getName())
                .image(dish.getImage())
                .price(dish.getPrice())
                .originalPrice(dish.getOriginalPrice())
                .description(dish.getDescription())
                .soldCount(dish.getSoldCount())
                .status(dish.getStatus())
                .specs(loadSpecs(List.of(dishId)).getOrDefault(dishId, List.of()))
                .build();
    }

    /**
     * 批量加载启用中的规格，按菜品 id 分组（避免 N+1）
     */
    private Map<Long, List<DishSpecVO>> loadSpecs(List<Long> dishIds) {
        Map<Long, List<DishSpecVO>> map = new HashMap<>();
        if (dishIds.isEmpty()) {
            return map;
        }
        List<DishSpec> specs = dishSpecMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                .in(DishSpec::getDishId, dishIds)
                .eq(DishSpec::getStatus, 1)
                .orderByAsc(DishSpec::getSort)
                .orderByAsc(DishSpec::getId));
        for (DishSpec spec : specs) {
            map.computeIfAbsent(spec.getDishId(), k -> new ArrayList<>()).add(DishSpecVO.builder()
                    .id(spec.getId())
                    .dishId(spec.getDishId())
                    .name(spec.getName())
                    .price(spec.getPrice())
                    .sort(spec.getSort())
                    .status(spec.getStatus())
                    .build());
        }
        return map;
    }

    @Override
    public SearchResultVO search(String keyword, Double lat, Double lng) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return SearchResultVO.builder().shops(List.of()).dishes(List.of()).build();
        }

        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getStatus, 1)
                .like(Shop::getName, kw)
                .orderByAsc(Shop::getId)
                .last("LIMIT 10"));
        Map<Long, Mall> mallMap = new HashMap<>();
        List<Long> mallIds = shops.stream().map(Shop::getMallId).distinct().toList();
        if (!mallIds.isEmpty()) {
            for (Mall mall : mallMapper.selectList(Wrappers.<Mall>lambdaQuery().in(Mall::getId, mallIds))) {
                mallMap.put(mall.getId(), mall);
            }
        }
        List<ShopVO> shopVOS = shops.stream().map(s -> {
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
                    .distance(mall == null ? null : distanceKm(lat, lng, mall.getLatitude(), mall.getLongitude()))
                    .build();
        }).toList();

        List<Dish> dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getStatus, 1)
                .like(Dish::getName, kw)
                .orderByDesc(Dish::getSoldCount)
                .last("LIMIT 20"));

        return SearchResultVO.builder()
                .shops(shopVOS)
                .dishes(buildHotDishVOs(dishes, lat, lng))
                .build();
    }

    /**
     * 菜品列表转展示 VO（补充档口名、美食城与距离）
     */
    private List<HotDishVO> buildHotDishVOs(List<Dish> dishes, Double lat, Double lng) {
        if (dishes.isEmpty()) {
            return List.of();
        }
        List<Long> shopIds = dishes.stream().map(Dish::getShopId).distinct().toList();
        Map<Long, Shop> shopMap = new HashMap<>();
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
            shopMap.put(shop.getId(), shop);
        }
        List<Long> mallIds = shopMap.values().stream().map(Shop::getMallId).distinct().toList();
        Map<Long, Mall> mallMap = new HashMap<>();
        if (!mallIds.isEmpty()) {
            for (Mall mall : mallMapper.selectList(Wrappers.<Mall>lambdaQuery().in(Mall::getId, mallIds))) {
                mallMap.put(mall.getId(), mall);
            }
        }
        return dishes.stream().map(d -> {
            Shop shop = shopMap.get(d.getShopId());
            Mall mall = shop == null ? null : mallMap.get(shop.getMallId());
            return HotDishVO.builder()
                    .id(d.getId())
                    .shopId(d.getShopId())
                    .mallId(shop == null ? null : shop.getMallId())
                    .shopName(shop == null ? "" : shop.getName())
                    .name(d.getName())
                    .image(d.getImage())
                    .price(d.getPrice())
                    .originalPrice(d.getOriginalPrice())
                    .soldCount(d.getSoldCount())
                    .distance(mall == null ? null : distanceKm(lat, lng, mall.getLatitude(), mall.getLongitude()))
                    .build();
        }).toList();
    }

    @Override
    public List<HotDishVO> listHotDishes(int limit, Double lat, Double lng) {
        int size = Math.max(1, Math.min(limit, 20));
        List<Dish> dishes = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getStatus, 1)
                .orderByDesc(Dish::getSoldCount)
                .last("LIMIT " + size));
        return buildHotDishVOs(dishes, lat, lng);
    }

    private DishVO toDishVO(Dish d, List<DishSpecVO> specs) {
        return DishVO.builder()
                .id(d.getId())
                .name(d.getName())
                .image(d.getImage())
                .price(d.getPrice())
                .originalPrice(d.getOriginalPrice())
                .description(d.getDescription())
                .soldCount(d.getSoldCount())
                .status(d.getStatus())
                .specs(specs)
                .build();
    }

    /**
     * Haversine 计算球面距离（公里），任一坐标缺失返回 null
     */
    private Double distanceKm(Double lat1, Double lng1, BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) {
            return null;
        }
        double dLat = Math.toRadians(lat2.doubleValue() - lat1);
        double dLng = Math.toRadians(lng2.doubleValue() - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(EARTH_RADIUS_KM * c * 100) / 100.0;
    }
}
