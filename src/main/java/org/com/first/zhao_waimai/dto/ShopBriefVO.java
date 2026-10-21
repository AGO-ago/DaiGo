package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 档口简要信息（首页美食城卡片内的缩略项）
 */
@Data
@Builder
public class ShopBriefVO {

    /** 档口 id */
    private Long id;

    /** 档口名称 */
    private String name;

    /** 档口 logo */
    private String logo;
}
