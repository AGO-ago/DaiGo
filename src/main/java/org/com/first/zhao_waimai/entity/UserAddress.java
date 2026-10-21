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
 * 用户地址（写字楼工位）
 */
@Data
@TableName("user_address")
public class UserAddress {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    private Long userId;

    /** 联系人姓名 */
    private String contact;

    /** 公司名称 */
    private String company;

    /** 楼栋 */
    private String building;

    /** 楼层 */
    private String floor;

    /** 工位/房号 */
    private String office;

    /** 联系电话 */
    private String phone;

    /** 是否默认：0 否 1 是 */
    private Integer isDefault;

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
