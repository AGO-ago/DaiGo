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
