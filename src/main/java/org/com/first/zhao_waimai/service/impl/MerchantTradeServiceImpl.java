package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.MerchantBillVO;
import org.com.first.zhao_waimai.dto.MerchantOrderDetailVO;
import org.com.first.zhao_waimai.dto.MerchantRefundVO;
import org.com.first.zhao_waimai.dto.OrderItemVO;
import org.com.first.zhao_waimai.entity.Coupon;
import org.com.first.zhao_waimai.entity.OrderCoupon;
import org.com.first.zhao_waimai.entity.OrderItem;
import org.com.first.zhao_waimai.entity.OrderRefund;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.Payment;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.entity.Settlement;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.CouponMapper;
import org.com.first.zhao_waimai.mapper.OrderCouponMapper;
import org.com.first.zhao_waimai.mapper.OrderItemMapper;
import org.com.first.zhao_waimai.mapper.OrderRefundMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PaymentMapper;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.mapper.SettlementMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.service.MerchantTradeService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 商家端交易闭环实现
 * 金额口径：抽佣基数 = 商品金额 - 平台券分摊；配送费 100% 归商家；店铺券由商家承担
 */
@Service
@RequiredArgsConstructor
public class MerchantTradeServiceImpl implements MerchantTradeService {

    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 默认抽佣比例（平台配置缺失时兜底） */
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.05");
    /** 账单明细/结算单最多返回条数 */
    private static final int LIST_LIMIT = 100;

    private final ShopMapper shopMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final PaymentMapper paymentMapper;
    private final SettlementMapper settlementMapper;
    private final PlatformConfigMapper platformConfigMapper;

    @Override
    public MerchantOrderDetailVO orderDetail(Long userId, Long orderId) {
        Shop shop = requireShop(userId);
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !shop.getId().equals(order.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        MerchantOrderDetailVO vo = new MerchantOrderDetailVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setPrepareTime(order.getPrepareTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setItems(loadItems(orderId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOrder(Long userId, Long orderId, String reason) {
        Shop shop = requireShop(userId);
        Orders order = ordersMapper.selectById(orderId);
        if (order == null || !shop.getId().equals(order.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        int updated = ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getId, orderId)
                .eq(Orders::getStatus, 1)
                .set(Orders::getStatus, 5)
                .set(Orders::getCancelTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "订单状态已变更，仅待接单订单可拒单");
        }
        // 拒单立即模拟退款：直接生成已退款退款单，便于双方对账
        OrderRefund refund = new OrderRefund();
        refund.setRefundNo(genNo("R"));
        refund.setOrderId(order.getId());
        refund.setPaymentId(order.getPaymentId());
        refund.setUserId(order.getUserId());
        refund.setShopId(order.getShopId());
        refund.setAmount(order.getPayAmount());
        refund.setReason(reason);
        refund.setStatus(OrderRefund.STATUS_REFUNDED);
        refund.setPrevStatus(1);
        refund.setHandleTime(LocalDateTime.now());
        orderRefundMapper.insert(refund);

        restoreCouponIfNoActiveSiblings(order);
        closePaymentIfAllClosed(order.getPaymentId());
    }

    @Override
    public List<MerchantRefundVO> listRefunds(Long userId, Integer status) {
        Shop shop = requireShop(userId);
        List<OrderRefund> refunds = orderRefundMapper.selectList(Wrappers.<OrderRefund>lambdaQuery()
                .eq(OrderRefund::getShopId, shop.getId())
                .eq(status != null, OrderRefund::getStatus, status)
                .orderByDesc(OrderRefund::getId)
                .last("LIMIT " + LIST_LIMIT));
        if (refunds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> orderNos = new HashMap<>();
        for (Orders order : ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .in(Orders::getId, refunds.stream().map(OrderRefund::getOrderId).distinct().toList()))) {
            orderNos.put(order.getId(), order.getOrderNo());
        }
        List<MerchantRefundVO> list = new ArrayList<>();
        for (OrderRefund refund : refunds) {
            MerchantRefundVO vo = new MerchantRefundVO();
            vo.setId(refund.getId());
            vo.setRefundNo(refund.getRefundNo());
            vo.setOrderId(refund.getOrderId());
            vo.setOrderNo(orderNos.get(refund.getOrderId()));
            vo.setAmount(refund.getAmount());
            vo.setReason(refund.getReason());
            vo.setStatus(refund.getStatus());
            vo.setRejectReason(refund.getRejectReason());
            vo.setCreateTime(refund.getCreateTime());
            vo.setHandleTime(refund.getHandleTime());
            list.add(vo);
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agreeRefund(Long userId, Long refundId) {
        Shop shop = requireShop(userId);
        OrderRefund refund = requireRefund(shop, refundId);
        int updated = refundMapperUpdate(refund, OrderRefund.STATUS_REFUNDED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "退款单已处理，请刷新后重试");
        }
        Orders order = ordersMapper.selectById(refund.getOrderId());
        if (order != null) {
            ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                    .eq(Orders::getId, order.getId())
                    .eq(Orders::getStatus, 6)
                    .set(Orders::getStatus, 7)
                    .set(Orders::getCancelTime, LocalDateTime.now()));
            restoreCouponIfNoActiveSiblings(order);
            closePaymentIfAllClosed(order.getPaymentId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long userId, Long refundId, String reason) {
        Shop shop = requireShop(userId);
        OrderRefund refund = requireRefund(shop, refundId);
        int updated = orderRefundMapper.update(null, Wrappers.<OrderRefund>lambdaUpdate()
                .eq(OrderRefund::getId, refundId)
                .eq(OrderRefund::getStatus, OrderRefund.STATUS_APPLYING)
                .set(OrderRefund::getStatus, OrderRefund.STATUS_REJECTED)
                .set(OrderRefund::getRejectReason, reason)
                .set(OrderRefund::getHandleTime, LocalDateTime.now()));
        if (updated == 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "退款单已处理，请刷新后重试");
        }
        int prev = refund.getPrevStatus() == null ? 1 : refund.getPrevStatus();
        ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                .eq(Orders::getId, refund.getOrderId())
                .eq(Orders::getStatus, 6)
                .set(Orders::getStatus, prev));
    }

    @Override
    public MerchantBillVO bill(Long userId, String period) {
        Shop shop = requireShop(userId);
        LocalDateTime start = resolvePeriodStart(period);
        LocalDateTime end = LocalDateTime.now();
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getShopId, shop.getId())
                .eq(Orders::getStatus, 4)
                .ge(Orders::getFinishTime, start)
                .le(Orders::getFinishTime, end)
                .orderByDesc(Orders::getFinishTime)
                .last("LIMIT " + LIST_LIMIT));

        BigDecimal rate = resolveCommissionRate(shop);
        Map<Long, BigDecimal> platformDiscounts = loadPlatformDiscounts(orders);
        // 周期内已退款金额（口径与平台结算单一致）
        BigDecimal refundSum = orderRefundMapper.selectList(Wrappers.<OrderRefund>lambdaQuery()
                        .eq(OrderRefund::getShopId, shop.getId())
                        .eq(OrderRefund::getStatus, OrderRefund.STATUS_REFUNDED)
                        .ge(OrderRefund::getHandleTime, start)
                        .le(OrderRefund::getHandleTime, end))
                .stream().map(r -> nvl(r.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        MerchantBillVO result = new MerchantBillVO();
        BigDecimal goodsSum = BigDecimal.ZERO;
        BigDecimal deliverySum = BigDecimal.ZERO;
        BigDecimal commissionSum = BigDecimal.ZERO;
        BigDecimal netSum = BigDecimal.ZERO;

        for (Orders order : orders) {
            BigDecimal goods = nvl(order.getTotalAmount());
            BigDecimal delivery = nvl(order.getDeliveryFee());
            BigDecimal discount = nvl(order.getDiscountAmount());
            BigDecimal platformDiscount = platformDiscounts.getOrDefault(order.getId(), BigDecimal.ZERO);
            if (platformDiscount.compareTo(discount) > 0) {
                platformDiscount = discount;
            }
            BigDecimal shopDiscount = discount.subtract(platformDiscount);
            BigDecimal commissionBase = goods.subtract(platformDiscount).max(BigDecimal.ZERO);
            BigDecimal commission = commissionBase.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = goods.add(delivery).subtract(shopDiscount).subtract(commission)
                    .setScale(2, RoundingMode.HALF_UP);

            MerchantBillVO.BillItem item = new MerchantBillVO.BillItem();
            item.setOrderId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setFinishTime(order.getFinishTime());
            item.setGoodsAmount(goods);
            item.setDeliveryAmount(delivery);
            item.setDiscountAmount(discount);
            item.setPlatformDiscount(platformDiscount);
            item.setShopDiscount(shopDiscount);
            item.setPayAmount(nvl(order.getPayAmount()));
            item.setCommissionRate(rate);
            item.setCommissionAmount(commission);
            item.setNetAmount(net);
            result.getItems().add(item);

            goodsSum = goodsSum.add(goods);
            deliverySum = deliverySum.add(delivery);
            commissionSum = commissionSum.add(commission);
            netSum = netSum.add(net);
        }

        result.setSummary(MerchantBillVO.Summary.builder()
                .orderCount((long) orders.size())
                .goodsAmount(goodsSum)
                .deliveryAmount(deliverySum)
                .commissionAmount(commissionSum)
                .refundAmount(refundSum)
                .netAmount(netSum.subtract(refundSum).setScale(2, RoundingMode.HALF_UP))
                .build());
        return result;
    }

    @Override
    public List<Settlement> settlements(Long userId) {
        Shop shop = requireShop(userId);
        return settlementMapper.selectList(Wrappers.<Settlement>lambdaQuery()
                .eq(Settlement::getShopId, shop.getId())
                .orderByDesc(Settlement::getPeriodStart)
                .last("LIMIT " + LIST_LIMIT));
    }

    /**
     * 平台券分摊：order_coupon → user_coupon → coupon，仅统计 shop_id 为空的平台券
     */
    private Map<Long, BigDecimal> loadPlatformDiscounts(List<Orders> orders) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (orders.isEmpty()) {
            return map;
        }
        List<OrderCoupon> records = orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .in(OrderCoupon::getOrderId, orders.stream().map(Orders::getId).toList()));
        if (records.isEmpty()) {
            return map;
        }
        Map<Long, UserCoupon> userCoupons = new HashMap<>();
        for (UserCoupon uc : userCouponMapper.selectList(Wrappers.<UserCoupon>lambdaQuery()
                .in(UserCoupon::getId, records.stream().map(OrderCoupon::getUserCouponId).distinct().toList()))) {
            userCoupons.put(uc.getId(), uc);
        }
        Map<Long, Coupon> coupons = new HashMap<>();
        List<Long> couponIds = userCoupons.values().stream().map(UserCoupon::getCouponId).distinct().toList();
        if (!couponIds.isEmpty()) {
            for (Coupon coupon : couponMapper.selectList(Wrappers.<Coupon>lambdaQuery()
                    .in(Coupon::getId, couponIds))) {
                coupons.put(coupon.getId(), coupon);
            }
        }
        for (OrderCoupon record : records) {
            UserCoupon uc = userCoupons.get(record.getUserCouponId());
            Coupon coupon = uc == null ? null : coupons.get(uc.getCouponId());
            if (coupon != null && coupon.getShopId() == null) {
                map.merge(record.getOrderId(), nvl(record.getAmount()), BigDecimal::add);
            }
        }
        return map;
    }

    /**
     * 退券：仅当同一支付单下没有其它在用子单时整券退回，避免拆单场景重复退券
     */
    private void restoreCouponIfNoActiveSiblings(Orders order) {
        Long active = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getPaymentId, order.getPaymentId())
                .ne(Orders::getId, order.getId())
                .notIn(Orders::getStatus, 5, 7));
        if (active > 0) {
            return;
        }
        List<Long> orderIds = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                        .eq(Orders::getPaymentId, order.getPaymentId()))
                .stream().map(Orders::getId).toList();
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

    /**
     * 支付单下全部子单已取消/已退款时，支付单置已退款
     */
    private void closePaymentIfAllClosed(Long paymentId) {
        Long active = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getPaymentId, paymentId)
                .notIn(Orders::getStatus, 5, 7));
        if (active == 0) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .eq(Payment::getStatus, 1)
                    .set(Payment::getStatus, 3));
        }
    }

    private OrderRefund requireRefund(Shop shop, Long refundId) {
        OrderRefund refund = orderRefundMapper.selectById(refundId);
        if (refund == null || !shop.getId().equals(refund.getShopId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "退款单不存在");
        }
        return refund;
    }

    private int refundMapperUpdate(OrderRefund refund, int status) {
        return orderRefundMapper.update(null, Wrappers.<OrderRefund>lambdaUpdate()
                .eq(OrderRefund::getId, refund.getId())
                .eq(OrderRefund::getStatus, OrderRefund.STATUS_APPLYING)
                .set(OrderRefund::getStatus, status)
                .set(OrderRefund::getHandleTime, LocalDateTime.now()));
    }

    private BigDecimal resolveCommissionRate(Shop shop) {
        if (shop.getCommissionRate() != null) {
            return shop.getCommissionRate();
        }
        PlatformConfig config = platformConfigMapper.selectOne(Wrappers.<PlatformConfig>lambdaQuery()
                .eq(PlatformConfig::getConfigKey, PlatformConfig.KEY_COMMISSION_RATE)
                .last("LIMIT 1"));
        if (config == null || config.getConfigValue() == null || config.getConfigValue().isBlank()) {
            return DEFAULT_COMMISSION_RATE;
        }
        try {
            return new BigDecimal(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            return DEFAULT_COMMISSION_RATE;
        }
    }

    private LocalDateTime resolvePeriodStart(String period) {
        LocalDate today = LocalDate.now();
        String value = period == null ? "today" : period;
        return switch (value) {
            case "today" -> today.atStartOfDay();
            case "7d" -> today.minusDays(6).atStartOfDay();
            case "month" -> today.withDayOfMonth(1).atStartOfDay();
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR, "统计周期仅支持 today/7d/month");
        };
    }

    private List<OrderItemVO> loadItems(Long orderId) {
        List<OrderItemVO> list = new ArrayList<>();
        for (OrderItem item : orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                .eq(OrderItem::getOrderId, orderId)
                .orderByAsc(OrderItem::getId))) {
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
            list.add(vo);
        }
        return list;
    }

    private Shop requireShop(Long userId) {
        Integer role = UserContext.getRole();
        if (role == null || role != CommonConstants.ROLE_MERCHANT) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "请使用商家账号登录");
        }
        Shop shop = shopMapper.selectOne(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getMerchantUserId, userId)
                .last("LIMIT 1"));
        if (shop == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号未绑定档口，请先提交入驻申请");
        }
        return shop;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 单号：前缀 + 时间戳 + 6 位随机（唯一键兜底） */
    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FMT) + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
