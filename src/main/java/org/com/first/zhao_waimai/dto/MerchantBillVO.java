package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商家账单：汇总 + 明细
 */
@Data
public class MerchantBillVO {

    /** 汇总 */
    private Summary summary;

    /** 订单明细 */
    private List<BillItem> items = new ArrayList<>();

    @Data
    @Builder
    public static class Summary {
        private Long orderCount;
        private BigDecimal goodsAmount;
        private BigDecimal deliveryAmount;
        private BigDecimal commissionAmount;
        private BigDecimal netAmount;
    }

    @Data
    public static class BillItem {
        private Long orderId;
        private String orderNo;
        private LocalDateTime finishTime;
        private BigDecimal goodsAmount;
        private BigDecimal deliveryAmount;
        private BigDecimal discountAmount;
        private BigDecimal platformDiscount;
        private BigDecimal shopDiscount;
        private BigDecimal payAmount;
        private BigDecimal commissionRate;
        private BigDecimal commissionAmount;
        private BigDecimal netAmount;
    }
}
