package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SettlementVO;
import org.com.first.zhao_waimai.entity.OrderRefund;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.entity.Settlement;
import org.com.first.zhao_waimai.entity.Shop;
import org.com.first.zhao_waimai.mapper.OrderRefundMapper;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.mapper.SettlementMapper;
import org.com.first.zhao_waimai.mapper.ShopMapper;
import org.com.first.zhao_waimai.service.AdminSettlementService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台端结算管理实现
 */
@Service
@RequiredArgsConstructor
public class AdminSettlementServiceImpl implements AdminSettlementService {

    /** 已完成 */
    private static final int STATUS_FINISHED = 4;
    /** 默认抽佣比例 */
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.05");

    private final SettlementMapper settlementMapper;
    private final OrdersMapper ordersMapper;
    private final OrderRefundMapper orderRefundMapper;
    private final ShopMapper shopMapper;
    private final PlatformConfigMapper platformConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generate(LocalDate date) {
        AdminGuard.requireAdmin();
        return doGenerate(date);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateByTask(LocalDate date) {
        return doGenerate(date);
    }

    private int doGenerate(LocalDate date) {
        if (date == null) {
            date = LocalDate.now().minusDays(1);
        }
        if (date.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不能生成未来日期的结算单");
        }
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Shop> shops = shopMapper.selectList(Wrappers.<Shop>lambdaQuery().orderByAsc(Shop::getId));
        BigDecimal defaultRate = resolveDefaultRate();
        int created = 0;
        for (Shop shop : shops) {
            Long exists = settlementMapper.selectCount(Wrappers.<Settlement>lambdaQuery()
                    .eq(Settlement::getShopId, shop.getId())
                    .eq(Settlement::getPeriodStart, date)
                    .eq(Settlement::getPeriodEnd, date));
            if (exists != null && exists > 0) {
                continue;
            }
            List<Orders> orders = ordersMapper.selectList(Wrappers.<Orders>lambdaQuery()
                    .eq(Orders::getShopId, shop.getId())
                    .eq(Orders::getStatus, STATUS_FINISHED)
                    .ge(Orders::getFinishTime, start)
                    .lt(Orders::getFinishTime, end));
            BigDecimal goods = sum(orders, Orders::getTotalAmount);
            BigDecimal delivery = sum(orders, Orders::getDeliveryFee);
            BigDecimal discount = sum(orders, Orders::getDiscountAmount);
            BigDecimal refund = orderRefundMapper.selectList(Wrappers.<OrderRefund>lambdaQuery()
                            .eq(OrderRefund::getShopId, shop.getId())
                            .eq(OrderRefund::getStatus, OrderRefund.STATUS_REFUNDED)
                            .ge(OrderRefund::getHandleTime, start)
                            .lt(OrderRefund::getHandleTime, end))
                    .stream().map(OrderRefund::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (orders.isEmpty() && refund.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }
            BigDecimal rate = shop.getCommissionRate() == null ? defaultRate : shop.getCommissionRate();
            BigDecimal commission = goods.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = goods.add(delivery).subtract(discount).subtract(commission).subtract(refund)
                    .setScale(2, RoundingMode.HALF_UP);

            Settlement settlement = new Settlement();
            settlement.setSettlementNo("S" + date.format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + shop.getId());
            settlement.setShopId(shop.getId());
            settlement.setPeriodStart(date);
            settlement.setPeriodEnd(date);
            settlement.setOrderCount(orders.size());
            settlement.setGoodsAmount(goods);
            settlement.setDeliveryAmount(delivery);
            settlement.setDiscountAmount(discount);
            settlement.setCommissionRate(rate);
            settlement.setCommissionAmount(commission);
            settlement.setRefundAmount(refund);
            settlement.setNetAmount(net);
            settlement.setStatus(Settlement.STATUS_PENDING);
            settlementMapper.insert(settlement);
            created++;
        }
        return created;
    }

    @Override
    public PageDTO<SettlementVO> list(Long shopId, Integer status, long page, long size) {
        AdminGuard.requireAdmin();
        Page<Settlement> result = settlementMapper.selectPage(new Page<>(page, size),
                Wrappers.<Settlement>lambdaQuery()
                        .eq(shopId != null, Settlement::getShopId, shopId)
                        .eq(status != null, Settlement::getStatus, status)
                        .orderByDesc(Settlement::getPeriodStart)
                        .orderByAsc(Settlement::getShopId));
        List<Settlement> settlements = result.getRecords();
        Map<Long, String> shopNames = loadShopNames(settlements);
        PageDTO<SettlementVO> dto = new PageDTO<>();
        dto.setList(settlements.stream().map(s -> toVO(s, shopNames.get(s.getShopId()))).toList());
        dto.setTotal(result.getTotal());
        dto.setPage(page);
        dto.setSize(size);
        return dto;
    }

    @Override
    public void settle(Long settlementId) {
        AdminGuard.requireAdmin();
        Settlement settlement = settlementMapper.selectById(settlementId);
        if (settlement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "结算单不存在");
        }
        if (settlement.getStatus() != null && settlement.getStatus() == Settlement.STATUS_SETTLED) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该结算单已结算，不可重复操作");
        }
        Settlement update = new Settlement();
        update.setId(settlementId);
        update.setStatus(Settlement.STATUS_SETTLED);
        update.setSettleTime(LocalDateTime.now());
        settlementMapper.updateById(update);
    }

    private BigDecimal resolveDefaultRate() {
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

    private BigDecimal sum(List<Orders> orders,
                           java.util.function.Function<Orders, BigDecimal> getter) {
        return orders.stream().map(getter).filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, String> loadShopNames(List<Settlement> settlements) {
        List<Long> ids = settlements.stream().map(Settlement::getShopId).distinct().toList();
        Map<Long, String> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Shop shop : shopMapper.selectList(Wrappers.<Shop>lambdaQuery().in(Shop::getId, ids))) {
            map.put(shop.getId(), shop.getName());
        }
        return map;
    }

    private SettlementVO toVO(Settlement settlement, String shopName) {
        SettlementVO vo = new SettlementVO();
        vo.setId(settlement.getId());
        vo.setSettlementNo(settlement.getSettlementNo());
        vo.setShopId(settlement.getShopId());
        vo.setShopName(shopName);
        vo.setPeriodStart(settlement.getPeriodStart());
        vo.setPeriodEnd(settlement.getPeriodEnd());
        vo.setOrderCount(settlement.getOrderCount());
        vo.setGoodsAmount(settlement.getGoodsAmount());
        vo.setDeliveryAmount(settlement.getDeliveryAmount());
        vo.setDiscountAmount(settlement.getDiscountAmount());
        vo.setCommissionRate(settlement.getCommissionRate());
        vo.setCommissionAmount(settlement.getCommissionAmount());
        vo.setRefundAmount(settlement.getRefundAmount());
        vo.setNetAmount(settlement.getNetAmount());
        vo.setStatus(settlement.getStatus());
        vo.setSettleTime(settlement.getSettleTime());
        vo.setCreateTime(settlement.getCreateTime());
        return vo;
    }
}
