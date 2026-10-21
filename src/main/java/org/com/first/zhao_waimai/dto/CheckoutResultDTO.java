package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算响应（预览与提交共用）
 */
@Data
public class CheckoutResultDTO {

    /** 支付单 id（预览时为 null） */
    private Long paymentId;

    /** 应付总额（所有子单实付之和） */
    private BigDecimal totalAmount;

    /** 收货信息快照 */
    private String addressSnapshot;

    /** 按档口拆分的子单分组 */
    private List<CheckoutGroupDTO> groups;
}
