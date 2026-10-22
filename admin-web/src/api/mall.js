import request from './request'

/** 美食城列表 */
export function listMalls() {
  return request.get('/admin/mall')
}

/** 新增美食城 */
export function createMall(data) {
  return request.post('/admin/mall', data)
}

/** 编辑美食城 */
export function updateMall(id, data) {
  return request.put(`/admin/mall/${id}`, data)
}

/** 删除美食城 */
export function deleteMall(id) {
  return request.delete(`/admin/mall/${id}`)
}
