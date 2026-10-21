const { get } = require('../../utils/request')

Page({
  data: {
    mallId: null,
    mallName: '',
    mall: null,
    shops: [],
    loading: true
  },

  onLoad(options) {
    this.setData({
      mallId: options.mallId,
      mallName: decodeURIComponent(options.mallName || '')
    })
    this.loadData()
  },

  loadData() {
    Promise.all([
      get(`/mall/${this.data.mallId}`, {}, false),
      get(`/mall/${this.data.mallId}/shops`, {}, false)
    ]).then(([mall, shops]) => {
      this.setData({
        mall: mall,
        shops: shops || [],
        loading: false
      })
      if (mall && mall.name) {
        wx.setNavigationBarTitle({ title: mall.name })
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  goMenu(e) {
    const { shopId, shopName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/menu/menu?shopId=${shopId}&shopName=${encodeURIComponent(shopName)}`
    })
  }
})
