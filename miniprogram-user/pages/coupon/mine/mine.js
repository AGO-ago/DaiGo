const { get } = require('../../../utils/request')

const amountText = (item) =>
  item.type === 2 ? `${(Number(item.amount) * 10).toFixed(1).replace(/\.0$/, '')}折` : Number(item.amount).toFixed(0)

const thresholdText = (item) =>
  Number(item.threshold) > 0 ? `满${Number(item.threshold).toFixed(0)}元可用` : '无门槛'

Page({
  data: {
    tabs: [
      { key: 0, label: '未使用' },
      { key: 1, label: '已使用' }
    ],
    active: 0,
    list: [],
    loading: true
  },

  onShow() {
    this.load()
  },

  switchTab(e) {
    const active = Number(e.currentTarget.dataset.key)
    if (active === this.data.active) return
    this.setData({ active, list: [], loading: true })
    this.load()
  },

  load() {
    get('/user/coupon', { status: this.data.active })
      .then((list) => {
        this.setData({
          list: (list || []).map((item) => ({
            ...item,
            amountText: amountText(item),
            thresholdText: thresholdText(item),
            endText: item.endTime ? String(item.endTime).slice(0, 10).replace(/-/g, '.') : ''
          })),
          loading: false
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  use() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  goCenter() {
    wx.navigateTo({ url: '/pages/coupon/list/list' })
  }
})
