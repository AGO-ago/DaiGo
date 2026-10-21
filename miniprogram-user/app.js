App({
  globalData: {
    // 开发环境指向本机后端；真机预览需改为局域网 IP
    baseUrl: 'http://localhost:8080/api',
    token: ''
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  },

  /**
   * 静默登录（浏览接口需 token，统一在此换取）
   * 测试号 + 后端 mock 模式：用稳定设备码换稳定 openid（同一设备始终同一用户）
   * 上线切换真实登录时：改用 wx.login 返回的 res.code 作为登录凭证
   */
  ensureLogin() {
    if (this.globalData.token || wx.getStorageSync('token')) {
      return Promise.resolve(true)
    }
    if (this._loginPromise) {
      return this._loginPromise
    }
    this._loginPromise = new Promise((resolve) => {
      let deviceCode = wx.getStorageSync('device_code')
      if (!deviceCode) {
        deviceCode = 'dev-' + Date.now() + '-' + Math.random().toString(36).slice(2)
        wx.setStorageSync('device_code', deviceCode)
      }
      wx.request({
        url: this.globalData.baseUrl + '/auth/login',
        method: 'POST',
        data: { code: deviceCode, role: 1 },
        success: (r) => {
          const body = r.data || {}
          if (body.code === 0 && body.data && body.data.token) {
            this.globalData.token = body.data.token
            wx.setStorageSync('token', body.data.token)
            wx.setStorageSync('user', body.data)
            resolve(true)
          } else {
            this._loginPromise = null
            resolve(false)
          }
        },
        fail: () => {
          this._loginPromise = null
          resolve(false)
        }
      })
    })
    return this._loginPromise
  }
})
