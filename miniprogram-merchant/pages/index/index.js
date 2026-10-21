const app = getApp()
const { get, put } = require('../../utils/request')

Page({
  data: {
    user: null,
    shop: null,
    stat: null,
    pendingOrders: [],
    loading: true
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
    if (user) {
      this.refresh()
    } else {
      this.setData({ loading: false })
    }
  },

  onLogin() {
    app.login()
      .then((data) => {
        this.setData({ user: data })
        this.refresh()
      })
      .catch(() => {})
  },

  refresh() {
    get('/merchant/shop')
      .then((shop) => {
        this.setData({ shop: shop || null, loading: false })
        if (!shop) return
        get('/merchant/stat/today')
          .then((stat) => this.setData({ stat: stat || null }))
          .catch(() => {})
        get('/merchant/orders', { status: 1 })
          .then((orders) => this.setData({ pendingOrders: (orders || []).slice(0, 5) }))
          .catch(() => {})
      })
      .catch(() => this.setData({ shop: null, loading: false }))
  },

  onPullDownRefresh() {
    this.refresh()
    wx.stopPullDownRefresh()
  },

  toggleBusiness() {
    const shop = this.data.shop
    if (!shop) return
    const next = shop.status === 1 ? 0 : 1
    put(`/merchant/shop/status?status=${next}`)
      .then(() => {
        this.setData({ 'shop.status': next })
        wx.showToast({ title: next === 1 ? '已开始营业' : '已暂停营业', icon: 'none' })
      })
      .catch(() => {})
  },

  accept(e) {
    const { id } = e.currentTarget.dataset
    put(`/merchant/order/${id}/accept`)
      .then(() => {
        wx.showToast({ title: '已接单', icon: 'success' })
        this.refresh()
      })
      .catch(() => {})
  },

  goOrders(e) {
    const status = e.currentTarget.dataset.status
    app.globalData.orderStatusFilter = status === undefined ? '' : String(status)
    wx.switchTab({ url: '/pages/orders/orders' })
  },
  goShop() {
    wx.navigateTo({ url: '/pages/shop/shop' })
  }
})
