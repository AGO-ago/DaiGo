import request from './request'

/** 秒杀活动分页 */
export function listSeckills(params) {
  return request.get('/admin/seckills', { params })
}

/** 新建秒杀活动 */
export function createSeckill(data) {
  return request.post('/admin/seckills', data)
}

/** 编辑秒杀活动 */
export function updateSeckill(id, data) {
  return request.put(`/admin/seckills/${id}`, data)
}

/** 上线/下线 */
export function updateSeckillStatus(id, status) {
  return request.put(`/admin/seckills/${id}/status`, null, { params: { status } })
}

/** 删除秒杀活动 */
export function deleteSeckill(id) {
  return request.delete(`/admin/seckills/${id}`)
}

/** 按名称检索菜品（用于选择秒杀商品） */
export function searchDishes(keyword) {
  return request.get('/search', { params: { keyword } })
}
