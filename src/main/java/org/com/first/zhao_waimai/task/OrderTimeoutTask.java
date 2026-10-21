package org.com.first.zhao_waimai.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.entity.Orders;
import org.com.first.zhao_waimai.entity.Payment;
import org.com.first.zhao_waimai.mapper.OrdersMapper;
import org.com.first.zhao_waimai.mapper.PaymentMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 超时未支付订单自动取消（每分钟扫描一次）
 * 规则：创建超过 15 分钟仍为待支付的支付单及子单，统一置为已取消
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    /** 待支付超时分钟数 */
    private static final int TIMEOUT_MINUTES = 15;

    private final PaymentMapper paymentMapper;
    private final OrdersMapper ordersMapper;

    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutPayments() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Payment> expired = paymentMapper.selectList(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getStatus, 0)
                .lt(Payment::getCreateTime, deadline)
                .last("LIMIT 200"));
        if (expired.isEmpty()) {
            return;
        }
        for (Payment payment : expired) {
            int updated = paymentMapper.update(null, Wrappers.<Payment>lambdaUpdate()
                    .eq(Payment::getId, payment.getId())
                    .eq(Payment::getStatus, 0)
                    .set(Payment::getStatus, 2));
            if (updated > 0) {
                ordersMapper.update(null, Wrappers.<Orders>lambdaUpdate()
                        .eq(Orders::getPaymentId, payment.getId())
                        .eq(Orders::getStatus, 0)
                        .set(Orders::getStatus, 5));
            }
        }
        log.info("超时未支付自动取消 {} 个支付单", expired.size());
    }
}
