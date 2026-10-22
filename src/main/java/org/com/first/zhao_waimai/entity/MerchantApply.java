package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商家入驻申请
 */
@Data
@TableName("merchant_apply")
public class MerchantApply {

    /** 状态：待审核 */
    public static final int STATUS_PENDING = 0;
    /** 状态：已通过 */
    public static final int STATUS_APPROVED = 1;
    /** 状态：已驳回 */
    public static final int STATUS_REJECTED = 2;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 申请账号 id */
    private Long userId;

    /** 档口名称 */
    private String shopName;

    /** 所属美食城 id */
    private Long mallId;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 营业执照图 URL */
    private String licenseImg;

    /** 营业时间 */
    private String businessHours;

    /** 状态：0 待审 1 通过 2 驳回 */
    private Integer status;

    /** 驳回原因 */
    private String rejectReason;

    /** 审核管理员 id */
    private Long auditAdminId;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核通过后绑定的档口 id */
    private Long shopId;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
