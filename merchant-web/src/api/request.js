import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * 商家端统一请求封装：
 * - 自动携带商家令牌
 * - 业务码非 0 时提示并抛错
 * - 401 清理登录态并跳转登录页
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('merchant_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      const message = body.msg || body.message || '请求失败'
      if (body.code === 401) {
        clearLoginState()
        if (!window.location.pathname.startsWith('/login')) {
          window.location.href = '/login'
        }
      }
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return body ? body.data : null
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      clearLoginState()
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    ElMessage.error(error.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  }
)

function clearLoginState() {
  localStorage.removeItem('merchant_token')
  localStorage.removeItem('merchant_user')
}

export default request
