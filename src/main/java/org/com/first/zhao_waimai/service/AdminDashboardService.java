package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.DashboardShopTopVO;
import org.com.first.zhao_waimai.dto.DashboardSummaryVO;
import org.com.first.zhao_waimai.dto.DashboardTrendVO;

import java.util.List;

/**
 * 平台数据看板服务
 */
public interface AdminDashboardService {

    /**
     * 今日/累计指标与待办
     */
    DashboardSummaryVO summary();

    /**
     * 近 N 天交易趋势
     */
    List<DashboardTrendVO> trend(int days);

    /**
     * 近 N 天档口交易额 TOP10
     */
    List<DashboardShopTopVO> shopTop(int days);
}
