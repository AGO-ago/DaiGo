// 与 app.json networkTimeout.request 保持一致
const TIMEOUT = 15000

/**
 * App 实例在 App() 执行后才可用。
 * 本模块可能在 app.js 加载阶段就被 require，模块顶层 getApp() 会拿到 undefined，
 * 因此每次请求时再取一次。
 */
function appInstance() {
  return getApp() || {}
}

/**
 * 统一请求封装
 * @param {Object} options { url, method, data, auth }
 * @param {Boolean} retried 已重试过登录（避免循环重试）
 * @returns {Promise} 业务 data
 */
function request(options, retried) {
  const { url, method = 'GET', data = {}, auth = true } = options
  const app = appInstance()
  // 需要鉴权但还没有 token 时，先静默登录
  const prepare = (auth && !app.globalData.token && !wx.getStorageSync('token'))
    ? app.ensureLogin()
    : Promise.resolve()

  return prepare.then(() => new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' }
    const token = app.globalData.token || wx.getStorageSync('token')
    if (auth && token) {
      header['Authorization'] = 'Bearer ' + token
    }
    wx.request({
      url: app.globalData.baseUrl + url,
      method,
      data,
      header,
      timeout: TIMEOUT,
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401 || body.code === 401) {
          // token 过期：清掉本地凭证，重新静默登录后重放一次
          app.globalData.token = ''
          wx.removeStorageSync('token')
          if (!retried && auth) {
            app.ensureLogin()
              .then((ok) => (ok ? request(options, true) : Promise.reject(body)))
              .then(resolve)
              .catch(reject)
            return
          }
        }
        if (body.code === 0) {
          resolve(body.data)
        } else {
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端是否启动', icon: 'none' })
        reject(err)
      }
    })
  }))
}

module.exports = {
  request,
  get: (url, data, auth) => request({ url, method: 'GET', data, auth }),
  post: (url, data, auth) => request({ url, method: 'POST', data, auth }),
  put: (url, data, auth) => request({ url, method: 'PUT', data, auth }),
  del: (url, data, auth) => request({ url, method: 'DELETE', data, auth })
}
