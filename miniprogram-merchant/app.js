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
    }
  },

  /** 商家登录（role=2），返回登录结果 */
  login() {
    if (this.loginPromise) return this.loginPromise
    this.loginPromise = new Promise((resolve, reject) => {
      wx.login({
        success: (res) => {
          post('/auth/login', { code: res.code, role: 2 }, false)
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
  }
})
