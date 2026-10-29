const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    user: null,
    shop: null,
    apply: null,
    remindOn: true,
    loadingShop: false,
    phoneInput: '',
    showPhoneInput: false,
    hasPassword: false,
    showPwdForm: false,
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({
      user: user || null,
      remindOn: wx.getStorageSync('order_remind') !== false
    })
    if (user) {
      this.loadShop()
      this.loadPasswordStatus()
    }
  },

  loadPasswordStatus() {
    get('/auth/merchant/password/status')
      .then((hasPassword) => this.setData({ hasPassword: !!hasPassword }))
      .catch(() => {})
  },

  openPassword() {
    if (!this.data.user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      return
    }
    this.setData({ showPwdForm: !this.data.showPwdForm })
  },

  onPwdInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  onSavePassword() {
    const { hasPassword, oldPassword, newPassword, confirmPassword } = this.data
    if (hasPassword && !oldPassword) {
      wx.showToast({ title: '请输入原密码', icon: 'none' })
      return
    }
    if (!/^.{6,20}$/.test(newPassword)) {
      wx.showToast({ title: '新密码需 6-20 位', icon: 'none' })
      return
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
      return
    }
    post('/auth/merchant/password', { oldPassword, newPassword })
      .then(() => {
        this.setData({
          hasPassword: true,
          showPwdForm: false,
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        })
        wx.showToast({ title: '密码已保存', icon: 'success' })
      })
      .catch(() => {})
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
    app.login()
      .then((data) => {
        that.setData({ user: data })
        that.loadShop()
      })
      .catch(() => {})
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
