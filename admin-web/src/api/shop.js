import request from './request'

/** 档口分页 */
export function listShops(params) {
  return request.get('/admin/shops', { params })
}

/** 档口上下线 */
export function updateShopStatus(id, status) {
  return request.put(`/admin/shop/${id}/status`, null, { params: { status } })
}

/** 设置抽佣比例 */
export function updateShopCommission(id, commissionRate) {
  return request.put(`/admin/shop/${id}/commission`, { commissionRate })
}
