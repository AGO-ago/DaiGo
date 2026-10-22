import request from './request'

/** 按日生成结算单，date 缺省为昨日，返回生成条数 */
export function generateSettlement(date) {
  return request.post('/admin/settlement/generate', null, { params: { date } })
}

/** 结算单分页 */
export function listSettlements(params) {
  return request.get('/admin/settlements', { params })
}

/** 标记已结算 */
export function settleSettlement(id) {
  return request.put(`/admin/settlement/${id}/settle`)
}
