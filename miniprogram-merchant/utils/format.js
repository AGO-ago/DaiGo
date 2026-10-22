/**
 * 商家端通用格式化
 */

/** 时间：2026-10-22T21:08:44 -> 10-22 21:08 */
function fmtTime(value) {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  const match = text.match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2})/)
  if (!match) return text
  return `${match[2]}-${match[3]} ${match[4]}:${match[5]}`
}

/** 金额：保留两位小数 */
function money(value) {
  const num = Number(value || 0)
  return num.toFixed(2)
}

/** 抽佣比例：0.05 -> 5% */
function percent(value) {
  const num = Number(value || 0)
  return (num * 100).toFixed(1).replace(/\.0$/, '') + '%'
}

module.exports = {
  fmtTime,
  money,
  percent
}
