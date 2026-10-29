const app = getApp()
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    user: null,
    phoneInput: '',
    showPhoneInput: false,
    orderEntries: [
      { key: '1', label: '待接单', icon: '📋' },
      { key: '2', label: '备餐中', icon: '🍳' },
      { key: '3', label: '配送中', icon: '🛵' },
      { key: '4', label: '已完成', icon: '✅' }
    ]
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
  },

  onLogin() {
    app.ensureLogin().then((ok) => {
      if (ok) {
        this.setData({ user: wx.getStorageSync('user') })
        wx.showToast({ title: '登录成功', icon: 'success' })
      }
    })
  },

  onGetPhone(e) {
    const code = e.detail && e.detail.code
    if (!code) {
      wx.showToast({ title: '未授权手机号', icon: 'none' })
      return
    }
    app.bindPhone({ phoneCode: code })
      .then((user) => {
        this.setData({ user })
        wx.showToast({ title: '绑定成功', icon: 'success' })
      })
      .catch(() => this.setData({ showPhoneInput: true }))
  },

  togglePhoneInput() {
    this.setData({ showPhoneInput: !this.data.showPhoneInput })
  },

  onPhoneInput(e) {
    this.setData({ phoneInput: e.detail.value })
  },

  onSavePhone() {
    const phone = (this.data.phoneInput || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    app.bindPhone({ phone })
      .then((user) => {
        this.setData({ user, showPhoneInput: false, phoneInput: '' })
        wx.showToast({ title: '绑定成功', icon: 'success' })
      })
      .catch(() => {})
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: (res) => {
        if (!res.confirm) return
        app.globalData.token = ''
        wx.removeStorageSync('token')
        wx.removeStorageSync('user')
        this.setData({ user: null })
      }
    })
  },

  goOrders(e) {
    const status = e.currentTarget.dataset.status || ''
    // tabBar 页面不支持带参跳转，用全局变量传递筛选状态
    getApp().globalData.orderStatusFilter = status
    wx.switchTab({ url: '/pages/order/list/list' })
  },

  goAddress() {
    wx.navigateTo({ url: '/pages/address/list' })
  },

  goFavorite() {
    wx.navigateTo({ url: '/pages/favorite/list/list' })
  },

  goCoupon() {
    wx.navigateTo({ url: '/pages/coupon/mine/mine' })
  },

  goCouponCenter() {
    wx.navigateTo({ url: '/pages/coupon/list/list' })
  },

  contactService() {
    wx.makePhoneCall({ phoneNumber: '4000000000' }).catch(() => {})
  }
})
