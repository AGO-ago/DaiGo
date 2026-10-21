package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.DishTreeVO;
import org.com.first.zhao_waimai.dto.DishVO;
import org.com.first.zhao_waimai.dto.MallVO;
import org.com.first.zhao_waimai.dto.ShopVO;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.DishCategory;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.DishCategoryMapper;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.BrowseService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 主数据浏览服务实现
 */
@Service
@RequiredArgsConstructor
public class BrowseServiceImpl implements BrowseService {

    private final MallMapper mallMapper;
    private final ShopMapper shopMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishMapper dishMapper;

    @Override
    public List<MallVO> listMalls() {
        List<Mall> malls = mallMapper.selectList(Wrappers.<Mall>lambdaQuery()
                .eq(Mall::getStatus, 1)
                .orderByAsc(Mall::getId));
        return malls.stream().map(m -> MallVO.builder()
                .id(m.getId())
                .name(m.getName())
                .address(m.getAddress())
                .coverImg(m.getCoverImg())
                .businessHours(m.getBusinessHours())
                .build()).toList();
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

        return categories.stream().map(c -> {
            DishTreeVO node = new DishTreeVO();
            node.setCategoryId(c.getId());
            node.setCategoryName(c.getName());
            node.setDishes(dishes.stream()
                    .filter(d -> c.getId().equals(d.getCategoryId()))
                    .map(this::toDishVO)
                    .toList());
            return node;
        }).toList();
    }

    private DishVO toDishVO(Dish d) {
        return DishVO.builder()
                .id(d.getId())
                .name(d.getName())
                .image(d.getImage())
                .price(d.getPrice())
                .originalPrice(d.getOriginalPrice())
                .description(d.getDescription())
                .soldCount(d.getSoldCount())
                .status(d.getStatus())
                .build();
    }
}
