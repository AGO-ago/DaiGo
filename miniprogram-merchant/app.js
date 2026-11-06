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
    this.setupUpdateManager()
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
      return
    }
    // 首次进入自动静默登录（微信 openid）
    this.login().catch(() => {})
  },

  /** 全局异常：开发期输出控制台，线上可接日志上报 */
  onError(err) {
    console.error('[app.onError]', err)
  },

  onUnhandledRejection(res) {
    console.error('[app.onUnhandledRejection]', res && res.reason)
  },

  /** 版本更新：新包下载完成后引导重启 */
  setupUpdateManager() {
    if (!wx.getUpdateManager) return
    const manager = wx.getUpdateManager()
    manager.onUpdateReady(() => {
      wx.showModal({
        title: '更新提示',
        content: '新版本已准备好，是否重启应用？',
        success: (res) => {
          if (res.confirm) manager.applyUpdate()
        }
      })
    })
    manager.onUpdateFailed(() => {
      wx.showToast({ title: '新版本下载失败，请稍后重试', icon: 'none' })
    })
  },

  /** 商家登录（role=2），返回登录结果 */
  login(phone) {
    if (this.loginPromise) return this.loginPromise
    this.loginPromise = new Promise((resolve, reject) => {
      const doLogin = (code) => {
        // 本地 mock 模式下后端不依赖 code（按 deviceId 生成 openid），
        // 因此测试号/touristappid 下 wx.login 失败时用占位 code 降级登录
        post('/auth/login', {
          code: code || 'devtools',
          role: 2,
          phone: phone || undefined,
          deviceId: 'merchant-2'
        }, false)
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
      }
      wx.login({
        success: (res) => doLogin(res.code),
        fail: () => doLogin('devtools')
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
