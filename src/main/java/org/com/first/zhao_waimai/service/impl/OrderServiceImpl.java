package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.CheckoutGroupDTO;
import org.com.first.zhao_waimai.dto.CheckoutItemDTO;
import org.com.first.zhao_waimai.dto.CheckoutItemVO;
import org.com.first.zhao_waimai.dto.CheckoutRequestDTO;
import org.com.first.zhao_waimai.dto.CheckoutResultDTO;
import org.com.first.zhao_waimai.dto.OrderDetailVO;
import org.com.first.zhao_waimai.dto.OrderItemVO;
import org.com.first.zhao_waimai.dto.OrderListVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.entity.Coupon;
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.DishSpec;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.OrderCoupon;
import org.com.first.zhao_waimai.entity.OrderItem;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.OrderRefund;
import org.com.first.zhao_waimai.entity.Payment;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.entity.SeckillActivity;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.entity.UserAddress;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.CouponMapper;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.DishSpecMapper;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.OrderCouponMapper;
import org.com.first.zhao_waimai.mapper.OrderItemMapper;
import org.com.first.zhao_waimai.mapper.OrderRefundMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PaymentMapper;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.mapper.SeckillActivityMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserAddressMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单服务实现：拆单核心逻辑
 * 规则：购物车跨档口时，按档口拆为多个子订单，共享一个支付单；
 *      金额一律以后端数据库为准，客户端传的数量/金额不参与计算
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserAddressMapper userAddressMapper;
    private final UserMapper userMapper;
    private final DishMapper dishMapper;
    private final DishSpecMapper dishSpecMapper;
    private final ShopMapper shopMapper;
    private final PaymentMapper paymentMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderCommentMapper orderCommentMapper;
    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final SeckillActivityMapper seckillActivityMapper;
    private final PlatformConfigMapper platformConfigMapper;

    @Override
    public CheckoutResultDTO checkout(Long userId, CheckoutRequestDTO request) {
        UserAddress address = loadAddress(userId, request.getAddressId());
        List<CheckoutGroupDTO> groups = buildGroups(userId, request.getItems());
        AppliedCoupon applied = applyCoupon(userId, request.getUserCouponId(), groups);
        return assemble(null, null, address, groups, applied);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultDTO submit(Long userId, CheckoutRequestDTO request) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先绑定手机号后再下单");
        }
        UserAddress address = loadAddress(userId, request.getAddressId());
        List<CheckoutGroupDTO> groups = buildGroups(userId, request.getItems());
        AppliedCoupon applied = applyCoupon(userId, request.getUserCouponId(), groups);
        String snapshot = buildAddressSnapshot(address);
        // 秒杀库存扣减（条件更新防超卖）
        deductSeckillStock(groups);

        // 1. 支付单：一次结算一个
        Payment payment = new Payment();
        payment.setPaymentNo(genNo("P"));
        payment.setUserId(userId);
        payment.setTotalAmount(BigDecimal.ZERO);
        payment.setChannel(1); // 模拟支付
        payment.setStatus(0);  // 待支付
        paymentMapper.insert(payment);

        // 2. 按档口拆子单 + 订单项快照
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CheckoutGroupDTO group : groups) {
            Orders order = new Orders();
            order.setOrderNo(genNo("O"));
            order.setPaymentId(payment.getId());
            order.setUserId(userId);
            order.setShopId(group.getShopId());
            order.setStatus(0); // 待支付
            order.setTotalAmount(group.getTotalAmount());
            order.setDeliveryFee(group.getDeliveryFee());
            order.setDiscountAmount(group.getDiscountAmount());
            order.setPayAmount(group.getPayAmount());
            order.setAddressSnapshot(snapshot);
            order.setRemark(request.getRemark());
            ordersMapper.insert(order);

            for (CheckoutItemVO item : group.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setDishId(item.getDishId());
                orderItem.setSpecId(item.getSpecId());
                orderItem.setSpecName(item.getSpecName());
                orderItem.setSpecPrice(item.getSpecPrice());
                orderItem.setDishName(item.getDishName());
                orderItem.setDishImage(item.getDishImage());
                orderItem.setPrice(item.getPrice());
                orderItem.setCount(item.getQuantity());
                orderItem.setAmount(item.getAmount());
                orderItemMapper.insert(orderItem);
            }
            group.setOrderId(order.getId());
            totalAmount = totalAmount.add(group.getPayAmount());

            // 用券分摊记录（每个子单一行，便于整单取消时退券）
            if (applied != null && group.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                OrderCoupon orderCoupon = new OrderCoupon();
                orderCoupon.setOrderId(order.getId());
                orderCoupon.setUserCouponId(applied.userCouponId());
                orderCoupon.setAmount(group.getDiscountAmount());
                orderCouponMapper.insert(orderCoupon);
            }
        }

        // 3. 核销用户券（条件更新，防并发重复使用）
        if (applied != null) {
            int used = userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .eq(UserCoupon::getId, applied.userCouponId())
                    .eq(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                    .set(UserCoupon::getStatus, UserCoupon.STATUS_USED)
                    .set(UserCoupon::getUseTime, LocalDateTime.now()));
            if (used == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券已被使用，请重新选择");
            }
        }

        // 4. 回填支付单总额
        payment.setTotalAmount(totalAmount);
        paymentMapper.updateById(payment);

        return assemble(payment.getId(), totalAmount, address, groups, applied);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long paymentId) {
        Payment payment = paymentMapper.selectById(paymentId);
        if (payment == null || !userId.equals(payment.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "支付单不存在");
        }
        if (payment.getStatus() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单已支付，请勿重复支付");
        }
        if (payment.getStatus() == 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单已取消，无法支付");
        }
        // 条件更新防并发重复支付（只有待支付状态才可更新）
        int updated = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                .eq(Payment::getId, paymentId)
                .eq(Payment::getStatus, 0)
                .set(Payment::getStatus, 1)
                .set(Payment::getPayTime, LocalDateTime.now())
                .set(Payment::getTransactionId, "MOCK-" + System.currentTimeMillis()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
        }
        // 子单：待支付 → 待接单
        ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getPaymentId, paymentId)
                .eq(Orders::getStatus, 0)
                .set(Orders::getStatus, 1));
        // 平台与档口均开启自动接单时，直接进入备餐中
        autoAcceptOrders(paymentId);
    }

    /**
     * 自动接单：全局开关开启且档口自身开启时，子单 待接单 → 备餐中
     */
    private void autoAcceptOrders(Long paymentId) {
        PlatformConfig config = platformConfigMapper.selectOne(Wrappers.<PlatformConfig>lambdaQuery()
                .eq(PlatformConfig::getConfigKey, PlatformConfig.KEY_AUTO_ACCEPT)
                .last("LIMIT 1"));
        if (config == null || !"1".equals(config.getConfigValue())) {
            return;
        }
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getPaymentId, paymentId)
                .eq(Orders::getStatus, 1));
        if (orders.isEmpty()) {
            return;
        }
        Map<Long, Shop> shopMap = loadShops(orders.stream().map(Orders::getShopId).distinct().toList());
        LocalDateTime now = LocalDateTime.now();
        for (Orders order : orders) {
            Shop shop = shopMap.get(order.getShopId());
            if (shop == null || !Integer.valueOf(1).equals(shop.getAutoAccept())) {
                continue;
            }
            ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                    .eq(Orders::getId, order.getId())
                    .eq(Orders::getStatus, 1)
                    .set(Orders::getStatus, 2)
                    .set(Orders::getAcceptTime, now)
                    .set(Orders::getPrepareTime, now));
        }
    }

    @Override
    public PageDTO<OrderListVO> listOrders(Long userId, Integer status, long page, long size) {
        Page<Orders> pageParam = new Page<>(page, size);
        Page<Orders> result = ordersMapper.selectPage(pageParam, Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getUserId, userId)
                .eq(status != null, Orders::getStatus, status)
                .orderByDesc(Orders::getId));

        List<Orders> records = result.getRecords();
        List<OrderListVO> list = new ArrayList<>();
        if (!records.isEmpty()) {
            Map<Long, List<OrderItemVO>> itemMap = loadItems(records.stream().map(Orders::getId).toList());
            Map<Long, Shop> shopMap = loadShops(records.stream().map(Orders::getShopId).distinct().toList());
            for (Orders o : records) {
                OrderListVO vo = new OrderListVO();
                fillBase(vo, o, shopMap.get(o.getShopId()));
                vo.setItems(itemMap.getOrDefault(o.getId(), List.of()));
                list.add(vo);
            }
        }
        PageDTO<OrderListVO> dto = new PageDTO<>();
        dto.setList(list);
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public OrderDetailVO getDetail(Long userId, Long orderId) {
        Orders order = loadOwnOrder(userId, orderId);
        OrderDetailVO vo = new OrderDetailVO();
        fillBase(vo, order, shopMapper.selectById(order.getShopId()));
        vo.setItems(loadItems(List.of(orderId)).getOrDefault(orderId, List.of()));
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setRemark(order.getRemark());
        vo.setPaymentId(order.getPaymentId());
        vo.setCommented(orderCommentMapper.selectCount(Wrappers.<OrderComment>lambdaQuery()
                .eq(OrderComment::getOrderId, orderId)) > 0);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Orders order = loadOwnOrder(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前状态不可取消，仅待支付订单可取消");
        }
        int updated = ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 0)
                .set(Orders::getStatus, 5)
                .set(Orders::getCancelTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
        }
        // 同一支付单下若已无待支付子单，则支付单关闭
        Long pending = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getPaymentId, order.getPaymentId())
                .eq(Orders::getStatus, 0));
        if (pending == 0) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .eq(Payment::getId, order.getPaymentId())
                    .eq(Payment::getStatus, 0)
                    .set(Payment::getStatus, 2));
            // 整单全部取消：退回已用的优惠券（券在待支付阶段核销）
            restoreCoupon(order.getPaymentId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Long userId, Long orderId, String reason) {
        Orders order = loadOwnOrder(userId, orderId);
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前状态不可申请退款，仅待接单或备餐中订单可申请");
        }
        Long applying = orderRefundMapper.selectCount(Wrappers.<OrderRefund>lambdaQuery()
                .eq(OrderRefund::getOrderId, orderId)
                .eq(OrderRefund::getStatus, OrderRefund.STATUS_APPLYING));
        if (applying > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单已提交退款申请，请等待商家处理");
        }
        OrderRefund refund = new OrderRefund();
        refund.setRefundNo(genNo("R"));
        refund.setOrderId(order.getId());
        refund.setPaymentId(order.getPaymentId());
        refund.setUserId(userId);
        refund.setShopId(order.getShopId());
        refund.setAmount(order.getPayAmount());
        refund.setReason(reason);
        refund.setStatus(OrderRefund.STATUS_APPLYING);
        refund.setPrevStatus(order.getStatus());
        orderRefundMapper.insert(refund);

        int updated = ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, order.getStatus())
                .set(Orders::getStatus, 6));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态已变更，请刷新后重试");
        }
    }

    /**
     * 支付单下子单全部取消后，退回本单使用的优惠券
     */
    private void restoreCoupon(Long paymentId) {
        List<Long> orderIds = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getPaymentId, paymentId))
                .stream().map(Orders::getId).toList();
        if (orderIds.isEmpty()) {
            return;
        }
        List<OrderCoupon> records = orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .in(OrderCoupon::getOrderId, orderIds));
        for (OrderCoupon record : records) {
            userCouponMapper.update(null, Wrappers.<UserCoupon>lambdaUpdate()
                    .eq(UserCoupon::getId, record.getUserCouponId())
                    .eq(UserCoupon::getStatus, UserCoupon.STATUS_USED)
                    .set(UserCoupon::getStatus, UserCoupon.STATUS_UNUSED)
                    .set(UserCoupon::getUseTime, null));
            orderCouponMapper.deleteById(record.getId());
        }
    }

    private Orders loadOwnOrder(Long userId, Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    /**
     * 批量加载订单项，按 orderId 分组
     */
    private Map<Long, List<OrderItemVO>> loadItems(List<Long> orderIds) {
        List<OrderItem> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .in(OrderItem::getOrderId, orderIds));
        Map<Long, List<OrderItemVO>> map = new HashMap<>();
        for (OrderItem item : items) {
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

    private Map<Long, Shop> loadShops(List<Long> shopIds) {
        Map<Long, Shop> map = new HashMap<>();
        if (shopIds.isEmpty()) {
            return map;
        }
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, shopIds))) {
            map.put(shop.getId(), shop);
        }
        return map;
    }

    private void fillBase(OrderListVO vo, Orders o, Shop shop) {
        vo.setOrderId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setShopId(o.getShopId());
        vo.setShopName(shop == null ? "" : shop.getName());
        vo.setMallId(shop == null ? null : shop.getMallId());
        vo.setStatus(o.getStatus());
        vo.setPayAmount(o.getPayAmount());
        vo.setCreateTime(o.getCreateTime());
    }

    /**
     * 购物车校验 + 按档口分组：
     * 菜品存在性/上架状态 → 档口营业状态 → 起送价；相同菜品+规格自动合并数量
     */
    private List<CheckoutGroupDTO> buildGroups(Long userId, List<CheckoutItemDTO> items) {
        // 合并相同菜品+规格的数量，保持提交顺序
        Map<ItemKey, Integer> quantityMap = new LinkedHashMap<>();
        for (CheckoutItemDTO item : items) {
            ItemKey key = new ItemKey(item.getDishId(), item.getSpecId());
            int merged = quantityMap.merge(key, item.getQuantity(), Integer::sum);
            if (merged > 99) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单品数量不能超过 99");
            }
        }

        Map<Long, CheckoutGroupDTO> groupMap = new LinkedHashMap<>();
        Map<Long, Shop> shopCache = new HashMap<>();
        Map<Long, SeckillActivity> seckillCache = new HashMap<>();
        for (Map.Entry<ItemKey, Integer> entry : quantityMap.entrySet()) {
            Long dishId = entry.getKey().dishId();
            Long specId = entry.getKey().specId();
            Dish dish = dishMapper.selectById(dishId);
            if (dish == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在（可能已删除，请刷新购物车）");
            }
            if (dish.getStatus() == null || dish.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "菜品「" + dish.getName() + "」已下架，请移除后再结算");
            }
            // 规格校验：必须属于该菜品且启用中，价格以后端为准
            DishSpec spec = null;
            BigDecimal specPrice = BigDecimal.ZERO;
            if (specId != null) {
                spec = dishSpecMapper.selectById(specId);
                if (spec == null || !dishId.equals(spec.getDishId())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "所选规格不存在，请重新选择");
                }
                if (spec.getStatus() == null || spec.getStatus() == 0) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "菜品「" + dish.getName() + "」的规格「" + spec.getName() + "」已停用，请重新选择");
                }
                specPrice = spec.getPrice() == null ? BigDecimal.ZERO : spec.getPrice();
            }
            Shop shop = shopCache.computeIfAbsent(dish.getShopId(), shopMapper::selectById);
            if (shop == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "菜品所属档口不存在");
            }
            if (shop.getStatus() == null || shop.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "档口「" + shop.getName() + "」休息中，暂不接单");
            }

            // 秒杀：命中进行中的活动时按活动价计价，并校验每人限购
            BigDecimal basePrice = dish.getPrice();
            SeckillActivity seckill = findActiveSeckill(dish.getId(), seckillCache);
            if (seckill != null) {
                int limit = seckill.getLimitPerUser() == null ? 1 : seckill.getLimitPerUser();
                int bought = orderItemMapper.sumUserDishQuantity(userId, dish.getId(),
                        seckill.getStartTime(), seckill.getEndTime());
                if (bought + entry.getValue() > limit) {
                    throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                            "秒杀商品「" + dish.getName() + "」每人限购 " + limit + " 份");
                }
                basePrice = seckill.getSeckillPrice();
            }
            BigDecimal unitPrice = basePrice.add(specPrice);
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()));
            CheckoutGroupDTO group = groupMap.computeIfAbsent(shop.getId(), k -> {
                CheckoutGroupDTO g = new CheckoutGroupDTO();
                g.setShopId(shop.getId());
                g.setShopName(shop.getName());
                g.setDeliveryFee(shop.getDeliveryFee());
                g.setAverageDeliveryTime(shop.getAverageDeliveryTime());
                g.setTotalAmount(BigDecimal.ZERO);
                g.setDiscountAmount(BigDecimal.ZERO);
                g.setItems(new ArrayList<>());
                return g;
            });
            group.setTotalAmount(group.getTotalAmount().add(amount));
            group.getItems().add(toItemVO(dish, spec, specPrice, entry.getValue(), unitPrice, amount));
        }

        // 起送价校验 + 实付计算（放在分组完成后统一处理）
        for (CheckoutGroupDTO group : groupMap.values()) {
            Shop shop = shopCache.get(group.getShopId());
            if (group.getTotalAmount().compareTo(shop.getMinOrderAmount()) < 0) {
                BigDecimal gap = shop.getMinOrderAmount().subtract(group.getTotalAmount());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "档口「" + shop.getName() + "」满 " + shop.getMinOrderAmount() + " 元起送，还差 " + gap + " 元");
            }
            group.setPayAmount(group.getTotalAmount().add(group.getDeliveryFee())
                    .subtract(group.getDiscountAmount()));
        }
        return new ArrayList<>(groupMap.values());
    }

    /**
     * 查找菜品当前进行中的秒杀活动（带本次请求内缓存）
     */
    private SeckillActivity findActiveSeckill(Long dishId, Map<Long, SeckillActivity> cache) {
        if (cache.containsKey(dishId)) {
            return cache.get(dishId);
        }
        LocalDateTime now = LocalDateTime.now();
        SeckillActivity activity = seckillActivityMapper.selectOne(Wrappers.<SeckillActivity>lambdaQuery()
                .eq(SeckillActivity::getDishId, dishId)
                .eq(SeckillActivity::getStatus, SeckillActivity.STATUS_ONLINE)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now)
                .orderByDesc(SeckillActivity::getId)
                .last("LIMIT 1"));
        cache.put(dishId, activity);
        return activity;
    }

    /**
     * 秒杀库存扣减：条件更新保证并发下不超卖
     */
    private void deductSeckillStock(List<CheckoutGroupDTO> groups) {
        Map<Long, Integer> quantityByDish = new LinkedHashMap<>();
        for (CheckoutGroupDTO group : groups) {
            for (CheckoutItemVO item : group.getItems()) {
                quantityByDish.merge(item.getDishId(), item.getQuantity(), Integer::sum);
            }
        }
        Map<Long, SeckillActivity> cache = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : quantityByDish.entrySet()) {
            SeckillActivity activity = findActiveSeckill(entry.getKey(), cache);
            if (activity == null) {
                continue;
            }
            int quantity = entry.getValue();
            int updated = seckillActivityMapper.update(null, Wrappers.<SeckillActivity>lambdaUpdate()
                    .eq(SeckillActivity::getId, activity.getId())
                    .apply("sold_count + {0} <= stock", quantity)
                    .setSql("sold_count = sold_count + " + quantity));
            if (updated == 0) {
                Dish dish = dishMapper.selectById(entry.getKey());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "秒杀商品「" + (dish == null ? "" : dish.getName()) + "」库存不足");
            }
        }
    }

    /**
     * 优惠券校验与分摊：
     * 平台券按各档口商品金额比例分摊，档口券仅抵扣该档口；抵扣额不超过商品总额
     */
    private AppliedCoupon applyCoupon(Long userId, Long userCouponId, List<CheckoutGroupDTO> groups) {
        for (CheckoutGroupDTO group : groups) {
            group.setDiscountAmount(BigDecimal.ZERO);
            group.setPayAmount(group.getTotalAmount().add(group.getDeliveryFee()));
        }
        if (userCouponId == null) {
            return null;
        }
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !userId.equals(userCoupon.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券不存在");
        }
        if (userCoupon.getStatus() == null || userCoupon.getStatus() != UserCoupon.STATUS_UNUSED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券已使用或已失效");
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "优惠券模板不存在");
        }
        if (coupon.getStatus() == null || coupon.getStatus() != Coupon.STATUS_ENABLED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券已停用");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartTime()) || now.isAfter(coupon.getEndTime())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券不在有效期内");
        }

        List<CheckoutGroupDTO> applicable = coupon.getShopId() == null
                ? groups
                : groups.stream().filter(g -> coupon.getShopId().equals(g.getShopId())).toList();
        if (applicable.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该优惠券仅限指定档口使用");
        }
        BigDecimal subtotal = applicable.stream()
                .map(CheckoutGroupDTO::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo(coupon.getThreshold()) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "订单满 " + coupon.getThreshold() + " 元才能使用该优惠券");
        }

        BigDecimal discount;
        if (coupon.getType() != null && coupon.getType() == Coupon.TYPE_DISCOUNT) {
            // 折扣券：amount 为折扣率（如 0.85 表示 85 折）
            discount = subtotal.multiply(BigDecimal.ONE.subtract(coupon.getAmount()))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            discount = coupon.getAmount();
        }
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "优惠券不可用");
        }

        // 按商品金额比例分摊，尾差归入最后一组
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < applicable.size(); i++) {
            CheckoutGroupDTO group = applicable.get(i);
            BigDecimal share = i == applicable.size() - 1
                    ? discount.subtract(allocated)
                    : discount.multiply(group.getTotalAmount()).divide(subtotal, 2, RoundingMode.DOWN);
            if (share.compareTo(group.getTotalAmount()) > 0) {
                share = group.getTotalAmount();
            }
            group.setDiscountAmount(share);
            allocated = allocated.add(share);
        }
        for (CheckoutGroupDTO group : groups) {
            group.setPayAmount(group.getTotalAmount().add(group.getDeliveryFee())
                    .subtract(group.getDiscountAmount()));
        }
        return new AppliedCoupon(userCouponId, coupon.getName(), discount);
    }

    /**
     * 已应用优惠券的上下文（提交时用于核销与记录分摊）
     */
    private record AppliedCoupon(Long userCouponId, String couponName, BigDecimal discount) {
    }

    private CheckoutItemVO toItemVO(Dish dish, DishSpec spec, BigDecimal specPrice,
                                    int quantity, BigDecimal unitPrice, BigDecimal amount) {
        CheckoutItemVO vo = new CheckoutItemVO();
        vo.setDishId(dish.getId());
        vo.setSpecId(spec == null ? null : spec.getId());
        vo.setSpecName(spec == null ? null : spec.getName());
        vo.setSpecPrice(spec == null ? null : specPrice);
        vo.setDishName(dish.getName());
        vo.setDishImage(dish.getImage());
        vo.setPrice(unitPrice);
        vo.setQuantity(quantity);
        vo.setAmount(amount);
        return vo;
    }

    /**
     * 购物车合并键：同一菜品下不同规格视为不同条目
     */
    private record ItemKey(Long dishId, Long specId) {
    }

    private CheckoutResultDTO assemble(Long paymentId, BigDecimal totalAmount, UserAddress address,
                                       List<CheckoutGroupDTO> groups, AppliedCoupon applied) {
        CheckoutResultDTO result = new CheckoutResultDTO();
        result.setPaymentId(paymentId);
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
            for (CheckoutGroupDTO group : groups) {
                totalAmount = totalAmount.add(group.getPayAmount());
            }
        }
        result.setTotalAmount(totalAmount);
        result.setDiscountAmount(applied == null ? BigDecimal.ZERO : applied.discount());
        result.setUserCouponId(applied == null ? null : applied.userCouponId());
        result.setCouponName(applied == null ? null : applied.couponName());
        result.setAddressSnapshot(buildAddressSnapshot(address));
        result.setGroups(groups);
        return result;
    }

    /**
     * 加载并校验地址归属（越权/不存在一律 404，不泄露他人地址）
     */
    private UserAddress loadAddress(Long userId, Long addressId) {
        UserAddress address = userAddressMapper.selectById(addressId);
        if (address == null || !userId.equals(address.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "收货地址不存在");
        }
        return address;
    }

    /**
     * 收货信息快照：联系人 电话 公司 楼栋 楼层 工位 地图详细地址
     */
    private String buildAddressSnapshot(UserAddress a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getContact()).append(' ').append(a.getPhone());
        String[] parts = {a.getCompany(), a.getBuilding(), a.getFloor(), a.getOffice(), a.getAddress()};
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                sb.append(' ').append(part.trim());
            }
        }
        return sb.toString();
    }

    /** 单号：前缀 + 时间戳 + 6 位随机（唯一键兜底） */
    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FMT) + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
