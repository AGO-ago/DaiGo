App({
  globalData: {
    // 开发环境指向本机后端；真机预览需改为局域网 IP
    baseUrl: 'http://localhost:8080/api',
    token: '',
    // 平台公开配置（公告/客服电话/营业时间/配送费/起送价），由 /config/public 拉取
    config: {}
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
    // 先用户本地缓存兜底，避免断网时首页无公告
    this.globalData.config = wx.getStorageSync('platform_config') || {}
    this.setupUpdateManager()
    this.setupPrivacyHandler()
    this.loadPublicConfig()
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

  /** 隐私授权：系统弹隐私协议前，先用确认框说明采集用途 */
  setupPrivacyHandler() {
    if (!wx.onNeedPrivacyAuthorization) return
    wx.onNeedPrivacyAuthorization((resolve) => {
      wx.showModal({
        title: '用户隐私保护提示',
        content: '为了推荐附近美食并计算配送距离，需要获取您的位置信息，请阅读并同意隐私保护指引。',
        confirmText: '同意',
        cancelText: '暂不',
        success: (res) => resolve({ event: res.confirm ? 'agree' : 'disagree' }),
        fail: () => resolve({ event: 'disagree' })
      })
    })
  },

  /** 拉取平台公开配置（失败不阻塞业务，沿用本地缓存） */
  loadPublicConfig() {
    wx.request({
      url: this.globalData.baseUrl + '/config/public',
      method: 'GET',
      timeout: 15000,
      success: (r) => {
        const body = r.data || {}
        if (body.code === 0 && body.data) {
          this.globalData.config = body.data
          wx.setStorageSync('platform_config', body.data)
        }
      }
    })
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
      timeout: 15000,
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
        timeout: 15000,
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
