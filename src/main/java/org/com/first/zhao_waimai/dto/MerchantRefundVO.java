package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家端退款单
 */
@Data
public class MerchantRefundVO {

    private Long id;

    private String refundNo;

    private Long orderId;

    private String orderNo;

    private BigDecimal amount;

    private String reason;

    private Integer status;

    private String rejectReason;

    private LocalDateTime createTime;

    private LocalDateTime handleTime;
}
