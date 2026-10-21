const app = getApp()

Page({
  data: {
    user: null
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
  },

  onLogin() {
    app.ensureLogin().then((ok) => {
      if (ok) {
        this.setData({ user: wx.getStorageSync('user') })
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
