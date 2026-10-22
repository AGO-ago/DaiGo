import request from './request'

/** 评价分页 */
export function listComments(params) {
  return request.get('/admin/comments', { params })
}

/** 平台回复 */
export function replyComment(id, content) {
  return request.put(`/admin/comment/${id}/reply`, { content })
}

/** 隐藏/恢复评价 */
export function hideComment(id, hidden, reason) {
  return request.put(`/admin/comment/${id}/hide`, { hidden, reason })
}
