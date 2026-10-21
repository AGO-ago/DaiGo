const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    user: null,
    shop: null,
    loadingShop: false
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
    if (user) {
      this.loadShop()
    }
  },

  loadShop() {
    this.setData({ loadingShop: true })
    get('/merchant/shop')
      .then((shop) => this.setData({ shop: shop || null, loadingShop: false }))
      .catch(() => this.setData({ shop: null, loadingShop: false }))
  },

  onLogin() {
    const that = this
    wx.login({
      success(res) {
        post('/auth/login', { code: res.code, role: 2 }, false)
          .then((data) => {
            app.globalData.token = data.token
            wx.setStorageSync('token', data.token)
            wx.setStorageSync('user', data)
            that.setData({ user: data })
            that.loadShop()
          })
          .catch(() => {})
      }
    })
  },

  goShop() {
    if (!this.data.user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/shop/shop' })
  },

  goDish() {
    wx.switchTab({ url: '/pages/dish/list/list' })
  },

  goComment() {
    wx.navigateTo({ url: '/pages/comment/list/list' })
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前商家账号吗？',
      success: (res) => {
        if (!res.confirm) return
        app.globalData.token = ''
        wx.removeStorageSync('token')
        wx.removeStorageSync('user')
        this.setData({ user: null, shop: null })
      }
    })
  }
})
