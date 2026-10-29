const { post } = require('./utils/request')

App({
  globalData: {
    // 开发环境指向本机后端；真机预览需改为局域网 IP
    baseUrl: 'http://localhost:8080/api',
    token: '',
    shop: null,
    orderStatusFilter: null
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      return
    }
    // 首次进入自动静默登录（微信 openid）
    this.login().catch(() => {})
  },

  /** 商家登录（role=2），返回登录结果 */
  login(phone) {
    if (this.loginPromise) return this.loginPromise
    this.loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          post('/auth/login', { code: res.code, role: 2, phone: phone || undefined }, false)
            .then((data) => {
              this.globalData.token = data.token
              wx.setStorageSync('token', data.token)
              wx.setStorageSync('user', data)
              this.loginPromise = null
              resolve(data)
            })
            .catch((err) => {
              this.loginPromise = null
              reject(err)
            })
        },
        fail: (err) => {
          this.loginPromise = null
          reject(err)
        }
      })
    })
    return this.loginPromise
  },

  /**
   * 绑定手机号：优先微信一键授权 phoneCode；本地联调可直接传 phone
   */
  bindPhone({ phone, phoneCode }) {
    return post('/auth/phone', { phone: phone || undefined, phoneCode: phoneCode || undefined })
      .then((boundPhone) => {
        const user = wx.getStorageSync('user') || {}
        const next = Object.assign({}, user, { phone: boundPhone })
        wx.setStorageSync('user', next)
        return next
      })
  }
})
