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
 * 平台管理员
 */
@Data
@TableName("admin")
public class Admin {

    /** 角色：超级管理员 */
    public static final int ROLE_SUPER = 1;
    /** 角色：运营 */
    public static final int ROLE_OPERATOR = 2;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 密码哈希（PBKDF2WithHmacSHA256，Base64） */
    private String password;

    /** 密码盐（Base64） */
    private String salt;

    /** 姓名 */
    private String realName;

    /** 角色：1 超管 2 运营 */
    private Integer role;

    /** 状态：0 禁用 1 正常 */
    private Integer status;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

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
