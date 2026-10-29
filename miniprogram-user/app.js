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
   * 静默登录：wx.login 换 openid，无需用户授权即可浏览
   * 本地 mock 模式额外携带稳定 deviceId，保证同一设备账号不漂移
   */
  ensureLogin() {
    if (this.globalData.token || wx.getStorageSync('token')) {
      return Promise.resolve(true)
    }
    if (this._loginPromise) {
      return this._loginPromise
    }
    let deviceId = wx.getStorageSync('device_code')
    if (!deviceId) {
      deviceId = 'dev-' + Date.now() + '-' + Math.random().toString(36).slice(2)
      wx.setStorageSync('device_code', deviceId)
    }
    this._loginPromise = new Promise((resolve) => {
      wx.login({
        success: (res) => this.requestLogin(res.code, deviceId, resolve),
        fail: () => this.requestLogin('', deviceId, resolve)
      })
    })
    return this._loginPromise
  },

  requestLogin(code, deviceId, resolve) {
    wx.request({
      url: this.globalData.baseUrl + '/auth/login',
      method: 'POST',
      data: { code: code || deviceId, deviceId, role: 1 },
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
  },

  /**
   * 绑定手机号：微信一键授权 phoneCode；本地联调可直接传 phone
   */
  bindPhone({ phone, phoneCode }) {
    return new Promise((resolve, reject) => {
      wx.request({
        url: this.globalData.baseUrl + '/auth/phone',
        method: 'POST',
        header: {
          'Content-Type': 'application/json',
          Authorization: 'Bearer ' + (this.globalData.token || wx.getStorageSync('token'))
        },
        data: { phone: phone || undefined, phoneCode: phoneCode || undefined },
        success: (r) => {
          const body = r.data || {}
          if (body.code === 0) {
            const user = wx.getStorageSync('user') || {}
            const next = Object.assign({}, user, { phone: body.data })
            wx.setStorageSync('user', next)
            resolve(next)
          } else {
            reject(new Error(body.msg || '绑定失败'))
          }
        },
        fail: reject
      })
    })
  }
})
