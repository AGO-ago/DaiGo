const app = getApp()
const { post } = require('../../utils/request')

Page({
  data: {
    user: null
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
  },

  onLogin() {
    const that = this
    wx.login({
      success(res) {
        post('/auth/login', { code: res.code }, false)
          .then((data) => {
            app.globalData.token = data.token
            wx.setStorageSync('token', data.token)
            wx.setStorageSync('user', data.user)
            that.setData({ user: data.user })
          })
          .catch(() => {})
      }
    })
  },

  onLogout() {
    app.globalData.token = ''
    wx.removeStorageSync('token')
    wx.removeStorageSync('user')
    this.setData({ user: null })
  }
})
