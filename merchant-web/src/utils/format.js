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
 * 扣减金额格式化：为 0 时不显示负号，避免出现 -¥0.00
 */
export function deduction(value) {
  const num = Math.abs(Number(value || 0))
  return num === 0 ? '¥0.00' : `-¥${num.toFixed(2)}`
}

/**
 * 百分比格式化（后端存 0.05 表示 5%）
 */
export function percent(value) {
  return `${(Number(value || 0) * 100).toFixed(2)}%`
}

/**
 * 时间格式化：兼容 ISO 字符串
 */
export function formatTime(value) {
  if (!value) {
    return '-'
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  const text = String(value)
  if (text.includes('T')) {
    return text.replace('T', ' ').slice(0, 19)
  }
  return text
}

function pad(value) {
  return String(value).padStart(2, '0')
}

/**
 * 上传请求头
 */
export function uploadHeaders() {
  const token = localStorage.getItem('merchant_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}
