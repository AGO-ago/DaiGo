package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CommentVO;
import org.com.first.zhao_waimai.dto.CouponSaveDTO;
import org.com.first.zhao_waimai.dto.DishCategorySaveDTO;
import org.com.first.zhao_waimai.dto.DishSaveDTO;
import org.com.first.zhao_waimai.dto.DishSpecSaveDTO;
import org.com.first.zhao_waimai.dto.DishSpecVO;
import org.com.first.zhao_waimai.dto.MerchantDishListVO;
import org.com.first.zhao_waimai.dto.MerchantCouponVO;
import org.com.first.zhao_waimai.dto.MerchantOrderVO;
import org.com.first.zhao_waimai.dto.MerchantStatVO;
import org.com.first.zhao_waimai.dto.MerchantApplySaveDTO;
import org.com.first.zhao_waimai.dto.MerchantApplyVO;
import org.com.first.zhao_waimai.dto.OrderItemVO;
import org.com.first.zhao_waimai.dto.ShopUpdateDTO;
import org.com.first.zhao_waimai.entity.Coupon;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.DishCategory;
import org.com.first.zhao_waimai.entity.DishSpec;
import org.com.first.zhao_waimai.entity.Mall;
import org.com.first.zhao_waimai.entity.MerchantApply;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.OrderItem;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.CouponMapper;
import org.com.first.zhao_waimai.mapper.DishCategoryMapper;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.DishSpecMapper;
import org.com.first.zhao_waimai.mapper.MallMapper;
import org.com.first.zhao_waimai.mapper.MerchantApplyMapper;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.OrderItemMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.service.CommentService;
import org.com.first.zhao_waimai.service.MerchantService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家端服务实现
 * 权限：商家令牌（role=2）且已绑定档口，所有数据仅限本档口
 */
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl implements MerchantService {

    /** 订单状态：待接单 / 备餐中 / 配送中 / 已完成 */
    private static final int STATUS_WAIT_ACCEPT = 1;
    private static final int STATUS_PREPARING = 2;
    private static final int STATUS_DELIVERING = 3;
    private static final int STATUS_FINISHED = 4;
    /** 商家端订单列表最多返回条数 */
    private static final int ORDER_LIST_LIMIT = 100;

    /** 状态推进动作：动作 → [原状态, 新状态] */
    private static final Map<String, int[]> ACTIONS = Map.of(
            "accept", new int[]{STATUS_WAIT_ACCEPT, STATUS_PREPARING},
            "prepare", new int[]{STATUS_PREPARING, STATUS_DELIVERING},
            "deliver", new int[]{STATUS_DELIVERING, STATUS_FINISHED},
            "complete", new int[]{STATUS_DELIVERING, STATUS_FINISHED}
    );

    private final ShopMapper shopMapper;
    private final DishMapper dishMapper;
    private final DishCategoryMapper dishCategoryMapper;
    private final DishSpecMapper dishSpecMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final CommentService commentService;
    private final MerchantApplyMapper merchantApplyMapper;
    private final MallMapper mallMapper;
    private final OrderCommentMapper orderCommentMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    @Override
    public Shop myShop(Long userId) {
        requireMerchantRole();
        return shopMapper.selectOne(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMerchantUserId, userId)
                .last("LIMIT 1"));
    }

    @Override
    public void updateShop(Long userId, ShopUpdateDTO dto) {
        Shop shop = requireShop(userId);
        if (dto.getAutoAccept() != null && dto.getAutoAccept() != 0 && dto.getAutoAccept() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "自动接单开关不合法");
        }
        Shop update = new Shop();
        update.setId(shop.getId());
        update.setName(dto.getName());
        update.setLogo(dto.getLogo());
        update.setPhone(dto.getPhone());
        update.setDeliveryFee(dto.getDeliveryFee());
        update.setMinOrderAmount(dto.getMinOrderAmount());
        update.setAverageDeliveryTime(dto.getAverageDeliveryTime());
        update.setBusinessHours(dto.getBusinessHours());
        update.setStatus(dto.getStatus());
        update.setAutoAccept(dto.getAutoAccept());
        shopMapper.updateById(update);
    }

    @Override
    public void updateShopStatus(Long userId, Integer status) {
        Shop shop = requireShop(userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "营业状态不合法");
        }
        Shop update = new Shop();
        update.setId(shop.getId());
        update.setStatus(status);
        shopMapper.updateById(update);
    }

    @Override
    public List<MerchantOrderVO> listOrders(Long userId, Integer status) {
        Shop shop = requireShop(userId);
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getShopId, shop.getId())
                .eq(status != null, Orders::getStatus, status)
                .orderByDesc(Orders::getId)
                .last("LIMIT " + ORDER_LIST_LIMIT));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, List<OrderItemVO>> itemMap = loadItems(orders.stream().map(Orders::getId).toList());
        List<MerchantOrderVO> list = new ArrayList<>();
        for (Orders o : orders) {
            MerchantOrderVO vo = new MerchantOrderVO();
            vo.setOrderId(o.getId());
            vo.setOrderNo(o.getOrderNo());
            vo.setStatus(o.getStatus());
            vo.setTotalAmount(o.getTotalAmount());
            vo.setDeliveryFee(o.getDeliveryFee());
            vo.setDiscountAmount(o.getDiscountAmount());
            vo.setPayAmount(o.getPayAmount());
            vo.setAddressSnapshot(o.getAddressSnapshot());
            vo.setRemark(o.getRemark());
            vo.setCreateTime(o.getCreateTime());
            vo.setItems(itemMap.getOrDefault(o.getId(), List.of()));
            list.add(vo);
        }
        return list;
    }

    @Override
    public void updateOrderStatus(Long userId, Long orderId, String action) {
        Shop shop = requireShop(userId);
        int[] transition = ACTIONS.get(action);
        if (transition == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的操作");
        }
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !shop.getId().equals(order.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        // 条件更新，防止重复点击导致状态越级；同步记录状态时间轴
        LocalDateTime now = LocalDateTime.now();
        var update = Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, transition[0])
                .set(Orders::getStatus, transition[1]);
        switch (action) {
            case "accept" -> update.set(Orders::getAcceptTime, now).set(Orders::getPrepareTime, now);
            case "prepare" -> update.set(Orders::getDeliveryTime, now);
            default -> update.set(Orders::getFinishTime, now);
        }
        int updated = ordersMapper.update(null, update);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
        }
    }

    @Override
    public MerchantDishListVO listDishes(Long userId) {
        Shop shop = requireShop(userId);
        MerchantDishListVO vo = new MerchantDishListVO();
        Map<Long, String> categoryNames = new HashMap<>();
        for (DishCategory category : dishCategoryMapper.selectList(Wrappers.<DishCategory>lambdaQuery()
                .eq(DishCategory::getShopId, shop.getId())
                .orderByAsc(DishCategory::getSort)
                .orderByAsc(DishCategory::getId))) {
            MerchantDishListVO.Category c = new MerchantDishListVO.Category();
            c.setId(category.getId());
            c.setName(category.getName());
            c.setSort(category.getSort());
            vo.getCategories().add(c);
            categoryNames.put(category.getId(), category.getName());
        }
        for (Dish dish : dishMapper.selectList(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getShopId, shop.getId())
                .orderByAsc(Dish::getSort)
                .orderByAsc(Dish::getId))) {
            MerchantDishListVO.DishItem item = new MerchantDishListVO.DishItem();
            item.setId(dish.getId());
            item.setCategoryId(dish.getCategoryId());
            item.setCategoryName(categoryNames.getOrDefault(dish.getCategoryId(), ""));
            item.setName(dish.getName());
            item.setImage(dish.getImage());
            item.setPrice(dish.getPrice());
            item.setOriginalPrice(dish.getOriginalPrice());
            item.setDescription(dish.getDescription());
            item.setStatus(dish.getStatus());
            item.setSort(dish.getSort());
            item.setSoldCount(dish.getSoldCount());
            vo.getDishes().add(item);
        }
        return vo;
    }

    @Override
    public Long saveDish(Long userId, Long dishId, DishSaveDTO dto) {
        Shop shop = requireShop(userId);
        DishCategory category = dishCategoryMapper.selectById(dto.getCategoryId());
        if (category == null || !shop.getId().equals(category.getShopId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "菜品分类不存在");
        }
        if (dishId == null) {
            Dish dish = new Dish();
            dish.setShopId(shop.getId());
            dish.setCategoryId(dto.getCategoryId());
            dish.setName(dto.getName());
            dish.setImage(dto.getImage());
            dish.setPrice(dto.getPrice());
            dish.setOriginalPrice(dto.getOriginalPrice());
            dish.setDescription(dto.getDescription());
            dish.setStatus(dto.getStatus());
            dish.setSort(dto.getSort() == null ? 0 : dto.getSort());
            dish.setSoldCount(0);
            dishMapper.insert(dish);
            return dish.getId();
        }
        Dish exists = dishMapper.selectById(dishId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        Dish update = new Dish();
        update.setId(dishId);
        update.setCategoryId(dto.getCategoryId());
        update.setName(dto.getName());
        update.setImage(dto.getImage());
        update.setPrice(dto.getPrice());
        update.setOriginalPrice(dto.getOriginalPrice());
        update.setDescription(dto.getDescription());
        update.setStatus(dto.getStatus());
        update.setSort(dto.getSort() == null ? exists.getSort() : dto.getSort());
        dishMapper.updateById(update);
        return dishId;
    }

    @Override
    public void deleteDish(Long userId, Long dishId) {
        Shop shop = requireShop(userId);
        Dish exists = dishMapper.selectById(dishId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        dishMapper.deleteById(dishId);
    }

    @Override
    public void updateDishStatus(Long userId, Long dishId, Integer status) {
        Shop shop = requireShop(userId);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上架状态不合法");
        }
        Dish exists = dishMapper.selectById(dishId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        Dish update = new Dish();
        update.setId(dishId);
        update.setStatus(status);
        dishMapper.updateById(update);
    }

    @Override
    public Long saveCategory(Long userId, Long categoryId, DishCategorySaveDTO dto) {
        Shop shop = requireShop(userId);
        if (categoryId == null) {
            DishCategory category = new DishCategory();
            category.setShopId(shop.getId());
            category.setName(dto.getName());
            category.setSort(dto.getSort() == null ? 0 : dto.getSort());
            dishCategoryMapper.insert(category);
            return category.getId();
        }
        DishCategory exists = dishCategoryMapper.selectById(categoryId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        DishCategory update = new DishCategory();
        update.setId(categoryId);
        update.setName(dto.getName());
        update.setSort(dto.getSort() == null ? exists.getSort() : dto.getSort());
        dishCategoryMapper.updateById(update);
        return categoryId;
    }

    @Override
    public void deleteCategory(Long userId, Long categoryId) {
        Shop shop = requireShop(userId);
        DishCategory exists = dishCategoryMapper.selectById(categoryId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        long dishCount = dishMapper.selectCount(Wrappers.<Dish>lambdaQuery()
                .eq(Dish::getCategoryId, categoryId));
        if (dishCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该分类下还有菜品，无法删除");
        }
        dishCategoryMapper.deleteById(categoryId);
    }

    @Override
    public List<DishSpecVO> listSpecs(Long userId, Long dishId) {
        requireOwnDish(userId, dishId);
        return dishSpecMapper.selectList(Wrappers.<DishSpec>lambdaQuery()
                        .eq(DishSpec::getDishId, dishId)
                        .orderByAsc(DishSpec::getSort)
                        .orderByAsc(DishSpec::getId))
                .stream().map(this::toSpecVO).toList();
    }

    @Override
    public Long saveSpec(Long userId, Long dishId, Long specId, DishSpecSaveDTO dto) {
        requireOwnDish(userId, dishId);
        if (specId == null) {
            DishSpec spec = new DishSpec();
            spec.setDishId(dishId);
            spec.setName(dto.getName().trim());
            spec.setPrice(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice());
            spec.setSort(dto.getSort() == null ? 0 : dto.getSort());
            spec.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            dishSpecMapper.insert(spec);
            return spec.getId();
        }
        DishSpec exists = dishSpecMapper.selectById(specId);
        if (exists == null || !dishId.equals(exists.getDishId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规格不存在");
        }
        DishSpec update = new DishSpec();
        update.setId(specId);
        update.setName(dto.getName().trim());
        update.setPrice(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice());
        update.setSort(dto.getSort() == null ? exists.getSort() : dto.getSort());
        update.setStatus(dto.getStatus() == null ? exists.getStatus() : dto.getStatus());
        dishSpecMapper.updateById(update);
        return specId;
    }

    @Override
    public void deleteSpec(Long userId, Long specId) {
        DishSpec exists = dishSpecMapper.selectById(specId);
        if (exists == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "规格不存在");
        }
        requireOwnDish(userId, exists.getDishId());
        dishSpecMapper.deleteById(specId);
    }

    /**
     * 校验菜品归属当前商家档口，返回菜品
     */
    private Dish requireOwnDish(Long userId, Long dishId) {
        Shop shop = requireShop(userId);
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || !shop.getId().equals(dish.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在");
        }
        return dish;
    }

    private DishSpecVO toSpecVO(DishSpec spec) {
        return DishSpecVO.builder()
                .id(spec.getId())
                .dishId(spec.getDishId())
                .name(spec.getName())
                .price(spec.getPrice())
                .sort(spec.getSort())
                .status(spec.getStatus())
                .build();
    }

    @Override
    public List<CommentVO> listComments(Long userId) {
        Shop shop = requireShop(userId);
        return commentService.listByShop(shop.getId());
    }

    @Override
    public void replyComment(Long userId, Long commentId, String content) {
        Shop shop = requireShop(userId);
        OrderComment comment = orderCommentMapper.selectById(commentId);
        if (comment == null || !shop.getId().equals(comment.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        OrderComment update = new OrderComment();
        update.setId(commentId);
        update.setReplyContent(content.trim());
        update.setReplyTime(LocalDateTime.now());
        orderCommentMapper.updateById(update);
    }

    @Override
    public List<MerchantCouponVO> listCoupons(Long userId) {
        Shop shop = requireShop(userId);
        return couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                        .eq(Coupon::getShopId, shop.getId())
                        .orderByDesc(Coupon::getId))
                .stream().map(c -> MerchantCouponVO.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .type(c.getType())
                        .threshold(c.getThreshold())
                        .amount(c.getAmount())
                        .totalCount(c.getTotalCount())
                        .remainCount(c.getRemainCount())
                        .receivedCount(userCouponMapper.selectCount(Wrappers.<UserCoupon>lambdaQuery()
                                .eq(UserCoupon::getCouponId, c.getId())).intValue())
                        .startTime(c.getStartTime())
                        .endTime(c.getEndTime())
                        .status(c.getStatus())
                        .build())
                .toList();
    }

    @Override
    public Long saveCoupon(Long userId, Long couponId, CouponSaveDTO dto) {
        Shop shop = requireShop(userId);
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "失效时间必须晚于生效时间");
        }
        if (dto.getType() == Coupon.TYPE_DISCOUNT
                && dto.getAmount().compareTo(BigDecimal.ONE) >= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "折扣率需在 0-1 之间，如 0.85 表示 85 折");
        }
        BigDecimal threshold = dto.getThreshold() == null ? BigDecimal.ZERO : dto.getThreshold();
        if (couponId == null) {
            Coupon coupon = new Coupon();
            coupon.setName(dto.getName().trim());
            coupon.setType(dto.getType());
            coupon.setThreshold(threshold);
            coupon.setAmount(dto.getAmount());
            coupon.setTotalCount(dto.getTotalCount());
            coupon.setRemainCount(dto.getTotalCount());
            coupon.setStartTime(dto.getStartTime());
            coupon.setEndTime(dto.getEndTime());
            coupon.setShopId(shop.getId());
            coupon.setCreatorType(Coupon.CREATOR_SHOP);
            coupon.setStatus(dto.getStatus() == null ? Coupon.STATUS_ENABLED : dto.getStatus());
            couponMapper.insert(coupon);
            return coupon.getId();
        }
        Coupon exists = couponMapper.selectById(couponId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        // 已领取数量固定，调整总量时同步剩余数量，避免超发或负数
        int used = Math.max(0, exists.getTotalCount() - exists.getRemainCount());
        int remain = Math.max(0, dto.getTotalCount() - used);
        Coupon update = new Coupon();
        update.setId(couponId);
        update.setName(dto.getName().trim());
        update.setType(dto.getType());
        update.setThreshold(threshold);
        update.setAmount(dto.getAmount());
        update.setTotalCount(dto.getTotalCount());
        update.setRemainCount(remain);
        update.setStartTime(dto.getStartTime());
        update.setEndTime(dto.getEndTime());
        update.setStatus(dto.getStatus() == null ? exists.getStatus() : dto.getStatus());
        couponMapper.updateById(update);
        return couponId;
    }

    @Override
    public void deleteCoupon(Long userId, Long couponId) {
        Shop shop = requireShop(userId);
        Coupon exists = couponMapper.selectById(couponId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        couponMapper.deleteById(couponId);
    }

    @Override
    public void updateCouponStatus(Long userId, Long couponId, Integer status) {
        Shop shop = requireShop(userId);
        if (status == null || (status != Coupon.STATUS_DISABLED && status != Coupon.STATUS_ENABLED)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态不合法");
        }
        Coupon exists = couponMapper.selectById(couponId);
        if (exists == null || !shop.getId().equals(exists.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        Coupon update = new Coupon();
        update.setId(couponId);
        update.setStatus(status);
        couponMapper.updateById(update);
    }

    @Override
    public MerchantStatVO statToday(Long userId) {
        Shop shop = requireShop(userId);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<Orders> todayOrders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getShopId, shop.getId())
                .ge(Orders::getCreateTime, start)
                .in(Orders::getStatus, STATUS_WAIT_ACCEPT, STATUS_PREPARING, STATUS_DELIVERING, STATUS_FINISHED));
        BigDecimal amount = todayOrders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int pending = (int) todayOrders.stream().filter(o -> o.getStatus() == STATUS_WAIT_ACCEPT).count();
        int finished = (int) todayOrders.stream().filter(o -> o.getStatus() == STATUS_FINISHED).count();
        return MerchantStatVO.builder()
                .todayAmount(amount)
                .todayOrderCount(todayOrders.size())
                .pendingCount(pending)
                .finishedCount(finished)
                .build();
    }

    @Override
    public Long submitApply(Long userId, MerchantApplySaveDTO dto) {
        requireMerchantRole();
        if (myShop(userId) != null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前账号已绑定档口，无需重复申请");
        }
        long pending = merchantApplyMapper.selectCount(Wrappers.<MerchantApply>lambdaQuery()
                .eq(MerchantApply::getUserId, userId)
                .eq(MerchantApply::getStatus, MerchantApply.STATUS_PENDING));
        if (pending > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "已有待审核的入驻申请，请耐心等待");
        }
        Mall mall = mallMapper.selectById(dto.getMallId());
        if (mall == null || mall.getStatus() == null || mall.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所选美食城不可用");
        }
        MerchantApply apply = new MerchantApply();
        apply.setUserId(userId);
        apply.setShopName(dto.getShopName());
        apply.setMallId(dto.getMallId());
        apply.setContact(dto.getContact());
        apply.setPhone(dto.getPhone());
        apply.setLicenseImg(dto.getLicenseImg());
        apply.setBusinessHours(dto.getBusinessHours());
        apply.setStatus(MerchantApply.STATUS_PENDING);
        merchantApplyMapper.insert(apply);
        return apply.getId();
    }

    @Override
    public MerchantApplyVO myLatestApply(Long userId) {
        requireMerchantRole();
        MerchantApply apply = merchantApplyMapper.selectOne(Wrappers.<MerchantApply>lambdaQuery()
                .eq(MerchantApply::getUserId, userId)
                .orderByDesc(MerchantApply::getId)
                .last("LIMIT 1"));
        if (apply == null) {
            return null;
        }
        MerchantApplyVO vo = new MerchantApplyVO();
        vo.setId(apply.getId());
        vo.setUserId(apply.getUserId());
        vo.setShopName(apply.getShopName());
        vo.setMallId(apply.getMallId());
        Mall mall = apply.getMallId() == null ? null : mallMapper.selectById(apply.getMallId());
        vo.setMallName(mall == null ? null : mall.getName());
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

    /**
     * 校验商家角色并返回绑定档口
     */
    private Shop requireShop(Long userId) {
        Shop shop = myShop(userId);
        if (shop == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号未绑定档口，请先提交入驻申请");
        }
        return shop;
    }

    private void requireMerchantRole() {
        Integer role = UserContext.getRole();
        if (role == null || role != CommonConstants.ROLE_MERCHANT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用商家账号登录");
        }
    }

    private Map<Long, List<OrderItemVO>> loadItems(List<Long> orderIds) {
        Map<Long, List<OrderItemVO>> map = new HashMap<>();
        for (OrderItem item : orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds))) {
            OrderItemVO vo = new OrderItemVO();
            vo.setDishId(item.getDishId());
            vo.setDishName(item.getDishName());
            vo.setSpecId(item.getSpecId());
            vo.setSpecName(item.getSpecName());
            vo.setSpecPrice(item.getSpecPrice());
            vo.setDishImage(item.getDishImage());
            vo.setPrice(item.getPrice());
            vo.setCount(item.getCount());
            vo.setAmount(item.getAmount());
            map.computeIfAbsent(item.getOrderId(), k -> new ArrayList<>()).add(vo);
        }
        return map;
    }
}
