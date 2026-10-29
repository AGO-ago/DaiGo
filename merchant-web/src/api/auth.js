import request from './request'

/**
 * 商家网页端登录
 */
export function merchantLogin(data) {
  return request.post('/auth/merchant/login', data)
}

/**
 * 是否已设置网页登录密码
 */
export function getPasswordStatus() {
  return request.get('/auth/merchant/password/status')
}

/**
 * 设置/修改网页登录密码
 */
export function setPassword(data) {
  return request.post('/auth/merchant/password', data)
}
