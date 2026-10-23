package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.DashboardShopTopVO;
import org.com.first.zhao_waimai.dto.DashboardSummaryVO;
import org.com.first.zhao_waimai.dto.DashboardTrendVO;
import org.com.first.zhao_waimai.entity.MerchantApply;
import org.com.first.zhao_waimai.entity.OrderComment;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.Settlement;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.MerchantApplyMapper;
import org.com.first.zhao_waimai.mapper.OrderCommentMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.SettlementMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AdminDashboardService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台看板实现：金额/订单口径与商家端一致（已支付且未取消），日期按服务器本地时区（Asia/Shanghai）
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    /** 计入交易额的订单状态：待接单/备餐中/配送中/已完成/退款中 */
    private static final List<Integer> GMV_STATUS = List.of(1, 2, 3, 4, 6);
    /** 趋势最大天数 */
    private static final int MAX_TREND_DAYS = 30;

    private final OrdersMapper ordersMapper;
    private final UserMapper userMapper;
    private final ShopMapper shopMapper;
    private final MerchantApplyMapper merchantApplyMapper;
    private final SettlementMapper settlementMapper;
    private final OrderCommentMapper orderCommentMapper;

    @Override
    public DashboardSummaryVO summary() {
        AdminGuard.requireAdmin();
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();

        List<Orders> todayOrders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .ge(Orders::getCreateTime, todayStart)
                .in(Orders::getStatus, GMV_STATUS));
        BigDecimal todayGmv = sumAmount(todayOrders);
        long todayCount = todayOrders.size();

        List<Orders> allOrders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .in(Orders::getStatus, GMV_STATUS)
                .select(Orders::getId, Orders::getPayAmount));
        BigDecimal totalGmv = sumAmount(allOrders);

        long newUsers = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .ge(User::getCreateTime, todayStart));
        long pendingApply = merchantApplyMapper.selectCount(Wrappers.<MerchantApply>lambdaQuery()
                .eq(MerchantApply::getStatus, MerchantApply.STATUS_PENDING));
        long pendingSettlement = settlementMapper.selectCount(Wrappers.<Settlement>lambdaQuery()
                .eq(Settlement::getStatus, Settlement.STATUS_PENDING));
        long badComment = orderCommentMapper.selectCount(Wrappers.<OrderComment>lambdaQuery()
                .le(OrderComment::getScore, 2)
                .eq(OrderComment::getHidden, 0));
        long onlineShop = shopMapper.selectCount(Wrappers.<Shop>lambdaQuery()
                .eq(Shop::getStatus, 1));

        return DashboardSummaryVO.builder()
                .todayGmv(todayGmv)
                .todayOrderCount(todayCount)
                .todayAvgAmount(todayCount == 0 ? BigDecimal.ZERO
                        : todayGmv.divide(BigDecimal.valueOf(todayCount), 2, RoundingMode.HALF_UP))
                .todayNewUsers(newUsers)
                .totalGmv(totalGmv)
                .totalOrders((long) allOrders.size())
                .pendingApplyCount(pendingApply)
                .pendingSettlementCount(pendingSettlement)
                .badCommentCount(badComment)
                .onlineShopCount(onlineShop)
                .build();
    }

    @Override
    public List<DashboardTrendVO> trend(int days) {
        AdminGuard.requireAdmin();
        int span = Math.max(1, Math.min(days, MAX_TREND_DAYS));
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(span - 1L);
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .ge(Orders::getCreateTime, startDate.atStartOfDay())
                .in(Orders::getStatus, GMV_STATUS));

        Map<LocalDate, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<LocalDate, Long> countMap = new LinkedHashMap<>();
        for (int i = 0; i < span; i++) {
            LocalDate date = startDate.plusDays(i);
            amountMap.put(date, BigDecimal.ZERO);
            countMap.put(date, 0L);
        }
        for (Orders order : orders) {
            LocalDate date = order.getCreateTime().toLocalDate();
            if (!amountMap.containsKey(date)) {
                continue;
            }
            BigDecimal amount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
            amountMap.put(date, amountMap.get(date).add(amount));
            countMap.put(date, countMap.get(date) + 1);
        }

        List<DashboardTrendVO> list = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> entry : amountMap.entrySet()) {
            list.add(DashboardTrendVO.builder()
                    .date(entry.getKey().toString())
                    .gmv(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                    .orderCount(countMap.get(entry.getKey()))
                    .build());
        }
        return list;
    }

    @Override
    public List<DashboardShopTopVO> shopTop(int days) {
        AdminGuard.requireAdmin();
        int span = Math.max(1, Math.min(days, MAX_TREND_DAYS));
        LocalDate startDate = LocalDate.now().minusDays(span - 1L);
        List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .ge(Orders::getCreateTime, startDate.atStartOfDay())
                .in(Orders::getStatus, GMV_STATUS)
                .select(Orders::getId, Orders::getShopId, Orders::getPayAmount));
        if (orders.isEmpty()) {
            return List.of();
        }
        Map<Long, BigDecimal> amountMap = new LinkedHashMap<>();
        Map<Long, Long> countMap = new LinkedHashMap<>();
        for (Orders order : orders) {
            Long shopId = order.getShopId();
            BigDecimal amount = order.getPayAmount() == null ? BigDecimal.ZERO : order.getPayAmount();
            amountMap.merge(shopId, amount, BigDecimal::add);
            countMap.merge(shopId, 1L, Long::sum);
        }
        Map<Long, String> shopNames = new LinkedHashMap<>();
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, amountMap.keySet()))) {
            shopNames.put(shop.getId(), shop.getName());
        }
        return amountMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(10)
                .map(entry -> DashboardShopTopVO.builder()
                        .shopId(entry.getKey())
                        .shopName(shopNames.getOrDefault(entry.getKey(), "未知档口"))
                        .gmv(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                        .orderCount(countMap.getOrDefault(entry.getKey(), 0L))
                        .build())
                .toList();
    }

    private BigDecimal sumAmount(List<Orders> orders) {
        return orders.stream()
                .map(o -> o.getPayAmount() == null ? BigDecimal.ZERO : o.getPayAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
