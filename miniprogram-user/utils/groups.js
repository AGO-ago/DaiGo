/**
 * 微信群配置
 * qr：群二维码图片，替换 assets 下的图片即可（长按可识别入群）
 */
const GROUPS = [
  {
    id: 1,
    name: '远雄大厦福利群',
    desc: '每日发券 · 秒杀预告 · 新品试吃',
    qr: '/assets/group-qr-1.png'
  },
  {
    id: 2,
    name: '创联工厂福利群',
    desc: '同商圈邻居互帮互助，随叫随到',
    qr: '/assets/group-qr-2.png'
  },
  {
    id: 3,
    name: '远洋大厦福利群',
    desc: '深夜拼单更划算，红包福利不断',
    qr: '/assets/group-qr-3.png'
  }
]

module.exports = { GROUPS }
