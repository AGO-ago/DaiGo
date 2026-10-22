const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    user: null,
    shop: null,
    apply: null,
    remindOn: true,
    loadingShop: false
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({
      user: user || null,
      remindOn: wx.getStorageSync('order_remind') !== false
    })
    if (user) {
      this.loadShop()
    }
  },

  loadShop() {
    this.setData({ loadingShop: true })
    get('/merchant/shop')
      .then((shop) => {
        this.setData({ shop: shop || null, loadingShop: false })
        if (!shop) {
          this.loadApply()
        }
      })
      .catch(() => this.setData({ shop: null, loadingShop: false }))
  },

  loadApply() {
    get('/merchant/apply')
      .then((apply) => this.setData({ apply: apply || null }))
      .catch(() => this.setData({ apply: null }))
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
    wx.navigateTo({ url: this.data.shop ? '/pages/shop/shop' : '/pages/apply/apply' })
  },

  goDish() {
    wx.switchTab({ url: '/pages/dish/list/list' })
  },

  goComment() {
    wx.navigateTo({ url: '/pages/comment/list/list' })
  },

  goRefund() {
    wx.navigateTo({ url: '/pages/refund/list/list' })
  },

  goBill() {
    wx.navigateTo({ url: '/pages/bill/bill' })
  },

  goCoupon() {
    wx.navigateTo({ url: '/pages/coupon/list/list' })
  },

  onRemindChange(e) {
    const on = e.detail.value
    this.setData({ remindOn: on })
    wx.setStorageSync('order_remind', on)
    wx.showToast({ title: on ? '已开启新订单提醒' : '已关闭新订单提醒', icon: 'none' })
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
        this.setData({ user: null, shop: null, apply: null })
      }
    })
  }
})
