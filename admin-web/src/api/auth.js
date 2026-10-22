import request from './request'

/**
 * 管理员登录
 */
export function login(data) {
  return request.post('/admin/auth/login', data)
}

/**
 * 当前管理员信息
 */
export function getProfile() {
  return request.get('/admin/profile')
}
