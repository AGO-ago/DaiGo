const { get } = require('../../utils/request')

Page({
  data: {
    mallId: null,
    mallName: '',
    shops: [],
    loading: true
  },

  onLoad(options) {
    this.setData({ mallId: options.mallId, mallName: decodeURIComponent(options.mallName || '') })
    this.loadShops()
  },

  loadShops() {
    get(`/mall/${this.data.mallId}/shops`, {}, false)
      .then((res) => {
        this.setData({ shops: res || [], loading: false })
      })
      .catch(() => {
        this.setData({ loading: false })
      })
  },

  goMenu(e) {
    const { shopId, shopName } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/menu/menu?shopId=${shopId}&shopName=${encodeURIComponent(shopName)}` })
  }
})
