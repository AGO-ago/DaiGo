package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 美食城列表项响应（首页卡片）
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

    /** 档口数量 */
    private Integer shopCount;

    /** 距离（公里，保留两位；未传定位时为 null） */
    private Double distance;

    /** 档口缩略列表（最多 6 个） */
    private List<ShopBriefVO> shops;
}
