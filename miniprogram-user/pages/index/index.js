const { get } = require('../../utils/request')

Page({
  data: {
    malls: [],
    loading: true
  },

  onLoad() {
    this.loadMalls()
  },

  onShow() {
    if (this.data.malls.length) {
      this.loadMalls()
    }
  },

  loadMalls() {
    get('/mall/list', {}, false)
      .then((res) => {
        this.setData({ malls: res || [], loading: false })
      })
      .catch(() => {
        this.setData({ loading: false })
      })
  },

  goShop(e) {
    const { mallId, mallName } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/shop/shop?mallId=${mallId}&mallName=${encodeURIComponent(mallName)}` })
  }
})
