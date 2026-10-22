package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台管理员信息
 */
@Data
@Builder
public class AdminProfileVO {

    private Long id;

    private String username;

    private String realName;

    private Integer role;

    private Integer status;

    private LocalDateTime lastLoginTime;
}
