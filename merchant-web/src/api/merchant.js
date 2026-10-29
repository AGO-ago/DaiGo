import request from './request'

/* ---------------- 档口 ---------------- */

/** 当前商家绑定档口（未绑定返回 null） */
export function getMyShop() {
  return request.get('/merchant/shop')
}

/** 修改档口信息（ShopUpdateDTO） */
export function updateShop(data) {
  return request.put('/merchant/shop', data)
}

/** 切换营业状态 0 休息 1 营业 */
export function updateShopStatus(status) {
  return request.put('/merchant/shop/status', null, { params: { status } })
}

/* ---------------- 工作台与订单 ---------------- */

/** 今日经营统计 */
export function getTodayStat() {
  return request.get('/merchant/stat/today')
}

/** 档口订单列表（接口不分页） */
export function listOrders(status) {
  return request.get('/merchant/orders', {
    params: status === '' || status === null || status === undefined ? {} : { status }
  })
}

/** 订单详情 */
export function getOrderDetail(id) {
  return request.get(`/merchant/order/${id}`)
}

/** 推进订单状态：accept / prepare / deliver / complete */
export function updateOrderStatus(id, action) {
  return request.put(`/merchant/order/${id}/${action}`)
}

/** 拒单 */
export function rejectOrder(id, reason) {
  return request.put(`/merchant/order/${id}/reject`, { reason })
}

/* ---------------- 退款 ---------------- */

/** 退款申请列表 */
export function listRefunds(status) {
  return request.get('/merchant/refunds', {
    params: status === '' || status === null || status === undefined ? {} : { status }
  })
}

/** 同意退款 */
export function agreeRefund(id) {
  return request.put(`/merchant/refund/${id}/agree`)
}

/** 拒绝退款 */
export function rejectRefund(id, reason) {
  return request.put(`/merchant/refund/${id}/reject`, { reason })
}

/* ---------------- 菜品与分类 ---------------- */

/** 分类 + 菜品列表 */
export function listDishes() {
  return request.get('/merchant/dishes')
}

/** 新增菜品 */
export function createDish(data) {
  return request.post('/merchant/dish', data)
}

/** 编辑菜品 */
export function updateDish(id, data) {
  return request.put(`/merchant/dish/${id}`, data)
}

/** 删除菜品 */
export function deleteDish(id) {
  return request.delete(`/merchant/dish/${id}`)
}

/** 菜品上下架 */
export function updateDishStatus(id, status) {
  return request.put(`/merchant/dish/${id}/status`, null, { params: { status } })
}

/** 新增菜品分类 */
export function createDishCategory(data) {
  return request.post('/merchant/dish-category', data)
}

/** 编辑菜品分类 */
export function updateDishCategory(id, data) {
  return request.put(`/merchant/dish-category/${id}`, data)
}

/** 删除菜品分类 */
export function deleteDishCategory(id) {
  return request.delete(`/merchant/dish-category/${id}`)
}

/* ---------------- 菜品规格 ---------------- */

/** 规格列表（含停用） */
export function listDishSpecs(dishId) {
  return request.get('/merchant/specs', { params: { dishId } })
}

/** 新增规格 */
export function createDishSpec(dishId, data) {
  return request.post('/merchant/specs', data, { params: { dishId } })
}

/** 编辑规格 */
export function updateDishSpec(id, dishId, data) {
  return request.put(`/merchant/specs/${id}`, data, { params: { dishId } })
}

/** 删除规格 */
export function deleteDishSpec(id) {
  return request.delete(`/merchant/specs/${id}`)
}

/* ---------------- 评价 ---------------- */

/** 档口评价列表 */
export function listComments() {
  return request.get('/merchant/comments')
}

/** 回复评价 */
export function replyComment(id, content) {
  return request.put(`/merchant/comment/${id}/reply`, { content })
}

/* ---------------- 店铺券 ---------------- */

/** 店铺券列表 */
export function listCoupons() {
  return request.get('/merchant/coupons')
}

/** 新增店铺券 */
export function createCoupon(data) {
  return request.post('/merchant/coupons', data)
}

/** 编辑店铺券 */
export function updateCoupon(id, data) {
  return request.put(`/merchant/coupons/${id}`, data)
}

/** 删除店铺券 */
export function deleteCoupon(id) {
  return request.delete(`/merchant/coupons/${id}`)
}

/** 启用/停用店铺券 */
export function updateCouponStatus(id, status) {
  return request.put(`/merchant/coupons/${id}/status`, null, { params: { status } })
}

/* ---------------- 账单与结算 ---------------- */

/** 账单汇总 + 明细：period = today / 7d / month */
export function getBill(period) {
  return request.get('/merchant/bill', { params: { period } })
}

/** 平台结算单列表 */
export function listSettlements() {
  return request.get('/merchant/settlements')
}

/* ---------------- 入驻申请 ---------------- */

/** 最近一次入驻申请 */
export function getMyApply() {
  return request.get('/merchant/apply')
}

/** 提交入驻申请 */
export function submitApply(data) {
  return request.post('/merchant/apply', data)
}

/* ---------------- 公共数据 ---------------- */

/** 美食城列表（公开接口） */
export function listMalls() {
  return request.get('/mall/list')
}

/** 美食城详情（公开接口，含地址与坐标） */
export function getMall(id) {
  return request.get(`/mall/${id}`)
}
