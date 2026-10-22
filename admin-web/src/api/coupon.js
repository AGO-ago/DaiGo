import request from './request'

/** 平台券列表 */
export function listCoupons() {
  return request.get('/admin/coupons')
}

/** 新建平台券 */
export function createCoupon(data) {
  return request.post('/admin/coupons', data)
}

/** 编辑平台券 */
export function updateCoupon(id, data) {
  return request.put(`/admin/coupons/${id}`, data)
}

/** 启用/停用 */
export function updateCouponStatus(id, status) {
  return request.put(`/admin/coupons/${id}/status`, null, { params: { status } })
}

/** 删除平台券 */
export function deleteCoupon(id) {
  return request.delete(`/admin/coupons/${id}`)
}
