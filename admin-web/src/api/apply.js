import request from './request'

/** 入驻申请分页 */
export function listApplies(params) {
  return request.get('/admin/applies', { params })
}

/** 审核通过 */
export function approveApply(id, data) {
  return request.put(`/admin/apply/${id}/approve`, data || {})
}

/** 审核驳回 */
export function rejectApply(id, reason) {
  return request.put(`/admin/apply/${id}/reject`, { reason })
}
