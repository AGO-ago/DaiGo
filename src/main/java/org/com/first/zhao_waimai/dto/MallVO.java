package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 美食城列表项响应
 */
@Data
@Builder
public class MallVO {

    /** 美食城 id */
    private Long id;

    /** 名称 */
    private String name;

    /** 地址 */
    private String address;

    /** 封面图 URL */
    private String coverImg;

    /** 营业时间 */
    private String businessHours;
}
