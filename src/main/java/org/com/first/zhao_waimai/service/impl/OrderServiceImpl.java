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
import org.com.first.zhao_waimai.entity.Dish;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.OrderItem;
import org.com.first.zhao_waimai.entity.Payment;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.UserAddress;
import org.com.first.zhao_waimai.mapper.DishMapper;
import org.com.first.zhao_waimai.mapper.OrderItemMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PaymentMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserAddressMapper;
import org.com.first.zhao_waimai.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final DishMapper dishMapper;
    private final ShopMapper shopMapper;
    private final PaymentMapper paymentMapper;
    private final OrdersMapper ordersMapper;
    private final OrderItemMapper orderItemMapper;

    @Override
    public CheckoutResultDTO checkout(Long userId, CheckoutRequestDTO request) {
        UserAddress address = loadAddress(userId, request.getAddressId());
        List<CheckoutGroupDTO> groups = buildGroups(request.getItems());
        return assemble(null, null, address, groups);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckoutResultDTO submit(Long userId, CheckoutRequestDTO request) {
        UserAddress address = loadAddress(userId, request.getAddressId());
        List<CheckoutGroupDTO> groups = buildGroups(request.getItems());
        String snapshot = buildAddressSnapshot(address);

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
            order.setDiscountAmount(BigDecimal.ZERO);
            order.setPayAmount(group.getPayAmount());
            order.setAddressSnapshot(snapshot);
            order.setRemark(request.getRemark());
            ordersMapper.insert(order);

            for (CheckoutItemVO item : group.getItems()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrderId(order.getId());
                orderItem.setDishId(item.getDishId());
                orderItem.setDishName(item.getDishName());
                orderItem.setDishImage(item.getDishImage());
                orderItem.setPrice(item.getPrice());
                orderItem.setCount(item.getQuantity());
                orderItem.setAmount(item.getAmount());
                orderItemMapper.insert(orderItem);
            }
            group.setOrderId(order.getId());
            totalAmount = totalAmount.add(group.getPayAmount());
        }

        // 3. 回填支付单总额
        payment.setTotalAmount(totalAmount);
        paymentMapper.updateById(payment);

        return assemble(payment.getId(), totalAmount, address, groups);
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
                .set(Orders::getStatus, 5));
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
        vo.setStatus(o.getStatus());
        vo.setPayAmount(o.getPayAmount());
        vo.setCreateTime(o.getCreateTime());
    }

    /**
     * 购物车校验 + 按档口分组：
     * 菜品存在性/上架状态 → 档口营业状态 → 起送价；相同菜品自动合并数量
     */
    private List<CheckoutGroupDTO> buildGroups(List<CheckoutItemDTO> items) {
        // 合并相同菜品数量，保持提交顺序
        Map<Long, Integer> quantityMap = new LinkedHashMap<>();
        for (CheckoutItemDTO item : items) {
            int merged = quantityMap.merge(item.getDishId(), item.getQuantity(), Integer::sum);
            if (merged > 99) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "单品数量不能超过 99");
            }
        }

        Map<Long, CheckoutGroupDTO> groupMap = new LinkedHashMap<>();
        Map<Long, Shop> shopCache = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : quantityMap.entrySet()) {
            Dish dish = dishMapper.selectById(entry.getKey());
            if (dish == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "菜品不存在（可能已删除，请刷新购物车）");
            }
            if (dish.getStatus() == null || dish.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "菜品「" + dish.getName() + "」已下架，请移除后再结算");
            }
            Shop shop = shopCache.computeIfAbsent(dish.getShopId(), shopMapper::selectById);
            if (shop == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "菜品所属档口不存在");
            }
            if (shop.getStatus() == null || shop.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "档口「" + shop.getName() + "」休息中，暂不接单");
            }

            BigDecimal amount = dish.getPrice().multiply(BigDecimal.valueOf(entry.getValue()));
            CheckoutGroupDTO group = groupMap.computeIfAbsent(shop.getId(), k -> {
                CheckoutGroupDTO g = new CheckoutGroupDTO();
                g.setShopId(shop.getId());
                g.setShopName(shop.getName());
                g.setDeliveryFee(shop.getDeliveryFee());
                g.setAverageDeliveryTime(shop.getAverageDeliveryTime());
                g.setTotalAmount(BigDecimal.ZERO);
                g.setItems(new ArrayList<>());
                return g;
            });
            group.setTotalAmount(group.getTotalAmount().add(amount));
            group.getItems().add(toItemVO(dish, entry.getValue(), amount));
        }

        // 起送价校验 + 实付计算（放在分组完成后统一处理）
        for (CheckoutGroupDTO group : groupMap.values()) {
            Shop shop = shopCache.get(group.getShopId());
            if (group.getTotalAmount().compareTo(shop.getMinOrderAmount()) < 0) {
                BigDecimal gap = shop.getMinOrderAmount().subtract(group.getTotalAmount());
                throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                        "档口「" + shop.getName() + "」满 " + shop.getMinOrderAmount() + " 元起送，还差 " + gap + " 元");
            }
            group.setPayAmount(group.getTotalAmount().add(group.getDeliveryFee()));
        }
        return new ArrayList<>(groupMap.values());
    }

    private CheckoutItemVO toItemVO(Dish dish, int quantity, BigDecimal amount) {
        CheckoutItemVO vo = new CheckoutItemVO();
        vo.setDishId(dish.getId());
        vo.setDishName(dish.getName());
        vo.setDishImage(dish.getImage());
        vo.setPrice(dish.getPrice());
        vo.setQuantity(quantity);
        vo.setAmount(amount);
        return vo;
    }

    private CheckoutResultDTO assemble(Long paymentId, BigDecimal totalAmount, UserAddress address, List<CheckoutGroupDTO> groups) {
        CheckoutResultDTO result = new CheckoutResultDTO();
        result.setPaymentId(paymentId);
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
            for (CheckoutGroupDTO group : groups) {
                totalAmount = totalAmount.add(group.getPayAmount());
            }
        }
        result.setTotalAmount(totalAmount);
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
     * 收货信息快照：联系人 电话 公司楼栋楼层工位
     */
    private String buildAddressSnapshot(UserAddress a) {
        StringBuilder sb = new StringBuilder();
        sb.append(a.getContact()).append(' ').append(a.getPhone());
        if (a.getCompany() != null) {
            sb.append(' ').append(a.getCompany());
        }
        sb.append(a.getBuilding()).append("栋");
        if (a.getFloor() != null) {
            sb.append(a.getFloor()).append("层");
        }
        if (a.getOffice() != null) {
            sb.append(a.getOffice()).append("工位");
        }
        return sb.toString();
    }

    /** 单号：前缀 + 时间戳 + 6 位随机（唯一键兜底） */
    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().format(NO_FMT) + ThreadLocalRandom.current().nextInt(100000, 999999);
    }
}
