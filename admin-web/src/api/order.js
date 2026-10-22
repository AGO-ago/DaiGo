import request from './request'

/** 全平台订单分页查询 */
export function listOrders(params) {
  return request.get('/admin/orders', { params })
}

/** 订单详情（含明细） */
export function getOrder(id) {
  return request.get(`/admin/order/${id}`)
}

/** 强制取消订单（已支付自动退款） */
export function cancelOrder(id) {
  return request.put(`/admin/order/${id}/cancel`)
}
