const { get, del } = require('../../../utils/request')

Page({
  data: {
    list: [],
    loading: true
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    get('/favorite')
      .then((list) => this.setData({ list: list || [], loading: false }))
      .catch(() => this.setData({ list: [], loading: false }))
  },

  goShop(e) {
    const { mallId, shopId, shopName } = e.currentTarget.dataset
    if (!mallId) return
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&shopId=${shopId}&shopName=${encodeURIComponent(shopName || '')}`
    })
  },

  remove(e) {
    const shopId = e.currentTarget.dataset.id
    del(`/favorite/${shopId}`)
      .then(() => {
        wx.showToast({ title: '已取消收藏', icon: 'success' })
        this.loadList()
      })
      .catch(() => {})
  }
})
