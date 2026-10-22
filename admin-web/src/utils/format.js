/**
 * 图片地址：后端返回 /uploads/xxx 相对路径，开发环境经 /api 代理访问
 */
export function imgUrl(path) {
  if (!path) {
    return ''
  }
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) {
    return path
  }
  return path.startsWith('/api') ? path : `/api${path.startsWith('/') ? '' : '/'}${path}`
}

/**
 * 金额格式化
 */
export function money(value) {
  return Number(value || 0).toFixed(2)
}

/**
 * 上传请求头
 */
export function uploadHeaders() {
  const token = localStorage.getItem('admin_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}
