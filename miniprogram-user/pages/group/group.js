const { GROUPS } = require('../../utils/groups')

Page({
  data: {
    groups: GROUPS,
    // 默认展开第一个群
    activeId: GROUPS.length ? GROUPS[0].id : null
  },

  /** 展开/收起群二维码 */
  toggle(e) {
    const id = e.currentTarget.dataset.id
    this.setData({ activeId: this.data.activeId === id ? null : id })
  },

  /** 全屏预览二维码，便于长按识别入群 */
  previewQr(e) {
    const qr = e.currentTarget.dataset.qr
    if (!qr) return
    wx.previewImage({ urls: [qr], current: qr })
  }
})
