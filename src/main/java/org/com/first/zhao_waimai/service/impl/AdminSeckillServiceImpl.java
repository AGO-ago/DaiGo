package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminSeckillVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SeckillSaveDTO;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.SeckillActivity;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.SeckillActivityMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.AdminSeckillService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 平台端秒杀活动管理实现
 */
@Service
@RequiredArgsConstructor
public class AdminSeckillServiceImpl implements AdminSeckillService {

    private final SeckillActivityMapper seckillActivityMapper;
    private final DishMapper dishMapper;
    private final ShopMapper shopMapper;

    @Override
    public PageDTO<AdminSeckillVO> list(String keyword, Integer status, long page, long size) {
        AdminGuard.requireAdmin();
        // 关键词先匹配菜品名，再落到活动筛选
        List<Long> dishIds = null;
        if (keyword != null && !keyword.isBlank()) {
            dishIds = dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                            .like(Dish::getName, keyword))
                    .stream().map(Dish::getId).toList();
            if (dishIds.isEmpty()) {
                PageDTO<AdminSeckillVO> empty = new PageDTO<>();
                empty.setList(List.of());
                empty.setTotal(0L);
                empty.setPage(page);
                empty.setSize(size);
                return empty;
            }
        }
        Page<SeckillActivity> result = seckillActivityMapper.selectPage(new Page<>(page, size),
                Wrappers.<SeckillActivity>lambdaQuery()
                        .in(dishIds != null, SeckillActivity::getDishId, dishIds)
                        .eq(status != null, SeckillActivity::getStatus, status)
                        .orderByDesc(SeckillActivity::getId));
        List<SeckillActivity> activities = result.getRecords();
        Map<Long, Dish> dishes = loadDishes(activities);
        Map<Long, String> shopNames = loadShopNames(activities);
        PageDTO<AdminSeckillVO> dto = new PageDTO<>();
        dto.setList(activities.stream().map(a -> toVO(a, dishes.get(a.getDishId()),
                shopNames.get(a.getShopId()))).toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public Long save(Long activityId, SeckillSaveDTO dto) {
        AdminGuard.requireAdmin();
        Dish dish = dishMapper.selectById(dto.getDishId());
        if (dish == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "结束时间需晚于开始时间");
        }
        if (dto.getSeckillPrice().compareTo(dish.getPrice()) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "秒杀价需低于菜品原价");
        }
        long overlap = seckillActivityMapper.selectCount(Wrappers.<SeckillActivity>lambdaQuery()
                .eq(SeckillActivity::getDishId, dto.getDishId())
                .ne(activityId != null, SeckillActivity::getId, activityId)
                .lt(SeckillActivity::getStartTime, dto.getEndTime())
                .gt(SeckillActivity::getEndTime, dto.getStartTime()));
        if (overlap > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该菜品在所选时段已存在秒杀活动");
        }

        SeckillActivity activity;
        if (activityId == null) {
            activity = new SeckillActivity();
            activity.setSoldCount(0);
        } else {
            activity = requireActivity(activityId);
            activity.setSoldCount(activity.getSoldCount() == null ? 0 : activity.getSoldCount());
        }
        activity.setDishId(dish.getId());
        activity.setShopId(dish.getShopId());
        activity.setSeckillPrice(dto.getSeckillPrice());
        activity.setStartTime(dto.getStartTime());
        activity.setEndTime(dto.getEndTime());
        activity.setStock(dto.getStock());
        activity.setLimitPerUser(dto.getLimitPerUser());
        activity.setStatus(dto.getStatus() == null ? SeckillActivity.STATUS_ONLINE : dto.getStatus());
        if (activityId == null) {
            seckillActivityMapper.insert(activity);
        } else {
            seckillActivityMapper.updateById(activity);
        }
        return activity.getId();
    }

    @Override
    public void delete(Long activityId) {
        AdminGuard.requireAdmin();
        requireActivity(activityId);
        seckillActivityMapper.deleteById(activityId);
    }

    @Override
    public void updateStatus(Long activityId, Integer status) {
        AdminGuard.requireAdmin();
        if (status == null || (status != SeckillActivity.STATUS_ONLINE
                && status != SeckillActivity.STATUS_OFFLINE)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "活动状态不合法");
        }
        requireActivity(activityId);
        SeckillActivity update = new SeckillActivity();
        update.setId(activityId);
        update.setStatus(status);
        seckillActivityMapper.updateById(update);
    }

    private SeckillActivity requireActivity(Long activityId) {
        SeckillActivity activity = seckillActivityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "秒杀活动不存在");
        }
        return activity;
    }

    private Map<Long, Dish> loadDishes(List<SeckillActivity> activities) {
        List<Long> ids = activities.stream().map(SeckillActivity::getDishId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, Dish> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Dish dish : dishMapper.selectList(Wrappers.<Dish>lambdaQuery().in(Dish::getId, ids))) {
            map.put(dish.getId(), dish);
        }
        return map;
    }

    private Map<Long, String> loadShopNames(List<SeckillActivity> activities) {
        List<Long> ids = activities.stream().map(SeckillActivity::getShopId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, ids))) {
            map.put(shop.getId(), shop.getName());
        }
        return map;
    }

    private AdminSeckillVO toVO(SeckillActivity activity, Dish dish, String shopName) {
        AdminSeckillVO vo = new AdminSeckillVO();
        vo.setId(activity.getId());
        vo.setDishId(activity.getDishId());
        vo.setDishName(dish == null ? null : dish.getName());
        vo.setDishImage(dish == null ? null : dish.getImage());
        vo.setDishPrice(dish == null ? null : dish.getPrice());
        vo.setShopId(activity.getShopId());
        vo.setShopName(shopName);
        vo.setSeckillPrice(activity.getSeckillPrice());
        vo.setStartTime(activity.getStartTime());
        vo.setEndTime(activity.getEndTime());
        vo.setStock(activity.getStock());
        vo.setSoldCount(activity.getSoldCount());
        vo.setLimitPerUser(activity.getLimitPerUser());
        vo.setStatus(activity.getStatus());
        vo.setCreateTime(activity.getCreateTime());
        return vo;
    }
}
