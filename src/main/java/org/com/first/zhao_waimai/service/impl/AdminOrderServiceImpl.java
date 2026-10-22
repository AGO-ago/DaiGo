package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminOrderVO;
import org.com.first.zhao_waimai.dto.OrderItemVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.entity.OrderCoupon;
import org.com.first.zhao_waimai.entity.OrderItem;
import org.com.first.zhao_waimai.entity.OrderRefund;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.Payment;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.entity.UserCoupon;
import org.com.first.zhao_waimai.mapper.OrderCouponMapper;
import org.com.first.zhao_waimai.mapper.OrderItemMapper;
import org.com.first.zhao_waimai.mapper.OrderRefundMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PaymentMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserCouponMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AdminOrderService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 平台端订单监控实现
 */
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    /** 未支付 */
    private static final int STATUS_UNPAID = 0;
    /** 已完成 */
    private static final int STATUS_FINISHED = 4;
    /** 已取消 */
    private static final int STATUS_CANCELED = 5;
    /** 已退款 */
    private static final int STATUS_REFUNDED = 7;

    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderCouponMapper orderCouponMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final PaymentMapper paymentMapper;
    private final UserCouponMapper userCouponMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    @Override
    public PageDTO<AdminOrderVO> list(String keyword, Long shopId, Integer status,
                                      LocalDate startDate, LocalDate endDate, long page, long size) {
        AdminGuard.requireAdmin();
        Page<Orders> result = ordersMapper.selectPage(new Page<>(page, size), Wrappers.<Orders>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), Orders::getOrderNo, keyword)
                .eq(shopId != null, Orders::getShopId, shopId)
                .eq(status != null, Orders::getStatus, status)
                .ge(startDate != null, Orders::getCreateTime, startDate == null ? null : startDate.atStartOfDay())
                .lt(endDate != null, Orders::getCreateTime, endDate == null ? null : endDate.plusDays(1).atStartOfDay())
                .orderByDesc(Orders::getId));
        List<Orders> orders = result.getRecords();
        Map<Long, String> shopNames = loadShopNames(orders);
        Map<Long, String> userNames = loadUserNames(orders);
        Map<Long, String> summaries = loadSummaries(orders);
        PageDTO<AdminOrderVO> dto = new PageDTO<>();
        dto.setList(orders.stream()
                .map(o -> toVO(o, shopNames.get(o.getShopId()), userNames.get(o.getUserId()),
                        summaries.get(o.getId()), null))
                .toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public AdminOrderVO detail(Long orderId) {
        AdminGuard.requireAdmin();
        Orders order = requireOrder(orderId);
        Shop shop = shopMapper.selectById(order.getShopId());
        User user = userMapper.selectById(order.getUserId());
        List<OrderItemVO> items = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId))
                .stream().map(this::toItemVO).toList();
        return toVO(order, shop == null ? null : shop.getName(), user == null ? null : user.getNickname(),
                summarise(items), items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long adminId, Long orderId) {
        AdminGuard.requireAdmin();
        Orders order = requireOrder(orderId);
        Integer status = order.getStatus();
        if (status == null || status == STATUS_FINISHED || status == STATUS_CANCELED
                || status == STATUS_REFUNDED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该订单当前状态不可取消");
        }
        LocalDateTime now = LocalDateTime.now();
        Orders update = new Orders();
        update.setId(orderId);
        update.setCancelTime(now);
        if (status == STATUS_UNPAID) {
            update.setStatus(STATUS_CANCELED);
            ordersMapper.updateById(update);
            return;
        }

        // 已支付：置为已退款，登记退款单并退回未使用优惠券
        update.setStatus(STATUS_REFUNDED);
        ordersMapper.updateById(update);
        saveRefundRecord(adminId, order, now);
        restoreCoupons(orderId);
        closePaymentIfAllClosed(order.getPaymentId());
    }

    private void saveRefundRecord(Long adminId, Orders order, LocalDateTime now) {
        OrderRefund applying = orderRefundMapper.selectOne(Wrappers.<OrderRefund>lambdaQuery()
                .eq(OrderRefund::getOrderId, order.getId())
                .eq(OrderRefund::getStatus, OrderRefund.STATUS_APPLYING)
                .orderByDesc(OrderRefund::getId)
                .last("LIMIT 1"));
        if (applying != null) {
            orderRefundMapper.update(null, Wrappers.<OrderRefund>lambdaUpdate()
                    .eq(OrderRefund::getId, applying.getId())
                    .set(OrderRefund::getStatus, OrderRefund.STATUS_REFUNDED)
                    .set(OrderRefund::getHandleAdminId, adminId)
                    .set(OrderRefund::getHandleTime, now));
            return;
        }
        OrderRefund refund = new OrderRefund();
        refund.setRefundNo("R" + System.currentTimeMillis() + order.getId());
        refund.setOrderId(order.getId());
        refund.setPaymentId(order.getPaymentId());
        refund.setUserId(order.getUserId());
        refund.setShopId(order.getShopId());
        refund.setAmount(order.getPayAmount());
        refund.setReason("平台强制取消");
        refund.setStatus(OrderRefund.STATUS_REFUNDED);
        refund.setPrevStatus(order.getStatus());
        refund.setHandleAdminId(adminId);
        refund.setHandleTime(now);
        orderRefundMapper.insert(refund);
    }

    /**
     * 退回该订单占用的优惠券
     */
    private void restoreCoupons(Long orderId) {
        List<OrderCoupon> records = orderCouponMapper.selectList(Wrappers.<OrderCoupon>lambdaQuery()
                .eq(OrderCoupon::getOrderId, orderId));
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
        if (paymentId == null) {
            return;
        }
        Long active = ordersMapper.selectCount(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getPaymentId, paymentId)
                .notIn(Orders::getStatus, STATUS_CANCELED, STATUS_REFUNDED));
        if (active == 0) {
            paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .eq(Payment::getId, paymentId)
                    .eq(Payment::getStatus, 1)
                    .set(Payment::getStatus, 3));
        }
    }

    private Orders requireOrder(Long orderId) {
        Orders order = ordersMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
        return order;
    }

    private Map<Long, String> loadShopNames(List<Orders> orders) {
        List<Long> ids = orders.stream().map(Orders::getShopId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, ids))) {
            map.put(shop.getId(), shop.getName());
        }
        return map;
    }

    private Map<Long, String> loadUserNames(List<Orders> orders) {
        List<Long> ids = orders.stream().map(Orders::getUserId).filter(java.util.Objects::nonNull)
                .distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectList(Wrappers.<User>lambdaQuery().in(User::getId, ids))) {
            map.put(user.getId(), user.getNickname());
        }
        return map;
    }

    /**
     * 批量加载商品摘要：首个菜品名 + 等 N 件
     */
    private Map<Long, String> loadSummaries(List<Orders> orders) {
        Map<Long, String> map = new HashMap<>();
        List<Long> ids = orders.stream().map(Orders::getId).toList();
        if (ids.isEmpty()) {
            return map;
        }
        Map<Long, List<OrderItem>> grouped = orderItemMapper.selectList(Wrappers.<OrderItem>lambdaQuery()
                        .in(OrderItem::getOrderId, ids))
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));
        for (Map.Entry<Long, List<OrderItem>> entry : grouped.entrySet()) {
            map.put(entry.getKey(), summarise(entry.getValue().stream().map(this::toItemVO).toList()));
        }
        return map;
    }

    private String summarise(List<OrderItemVO> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        int total = items.stream().mapToInt(i -> i.getCount() == null ? 0 : i.getCount()).sum();
        String first = items.get(0).getDishName();
        return total > 1 ? first + " 等 " + total + " 件" : first;
    }

    private OrderItemVO toItemVO(OrderItem item) {
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
        return vo;
    }

    private AdminOrderVO toVO(Orders order, String shopName, String userName,
                              String summary, List<OrderItemVO> items) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setPaymentId(order.getPaymentId());
        vo.setUserId(order.getUserId());
        vo.setUserName(userName);
        vo.setShopId(order.getShopId());
        vo.setShopName(shopName);
        vo.setStatus(order.getStatus());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDeliveryFee(order.getDeliveryFee());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setAddressSnapshot(order.getAddressSnapshot());
        vo.setRemark(order.getRemark());
        vo.setItemSummary(summary);
        vo.setAcceptTime(order.getAcceptTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items == null ? null : new ArrayList<>(items));
        return vo;
    }
}
