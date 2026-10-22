import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 统一请求封装：
 * - 自动携带管理员令牌
 * - 业务码非 0 时提示并抛错
 * - 401 清理登录态并跳转登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      if (body.code === 401) {
        localStorage.removeItem('admin_token')
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body ? body.data : null
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      localStorage.removeItem('admin_token')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
