const { get } = require('../../utils/request')

Page({
  data: {
    stat: null,
    pendingCount: 0
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    get('/merchant/stat/today')
      .then((res) => this.setData({ stat: res || {} }))
      .catch(() => this.setData({ stat: null }))
    get('/merchant/orders?status=1')
      .then((res) => this.setData({ pendingCount: (res && res.length) || 0 }))
      .catch(() => this.setData({ pendingCount: 0 }))
  },

  goOrders() {
    wx.switchTab({ url: '/pages/orders/orders' })
  }
})
