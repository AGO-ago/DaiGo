import request from './request'

/**
 * 看板汇总指标
 */
export function getSummary() {
  return request.get('/admin/dashboard/summary')
}

/**
 * 近 N 天趋势
 */
export function getTrend(days = 7) {
  return request.get('/admin/dashboard/trend', { params: { days } })
}

/**
 * 近 N 天档口交易额 TOP10
 */
export function getShopTop(days = 7) {
  return request.get('/admin/dashboard/shop-top', { params: { days } })
}
