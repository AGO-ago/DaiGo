// 与 app.json networkTimeout.request 保持一致
const TIMEOUT = 15000

/**
 * App 实例在 App() 执行后才可用。
 * 本模块在 app.js 第一行就被 require，模块顶层 getApp() 拿到的是 undefined，
 * 因此每次请求时再取一次。
 */
function appInstance() {
  return getApp() || {}
}

function request(options) {
  const { url, method = 'GET', data = {}, auth = true, retried = false } = options
  const app = appInstance()
  return new Promise((resolve, reject) => {
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
          app.globalData.token = ''
          wx.removeStorageSync('token')
          // 登录态失效：自动静默重登并重放一次，避免页面一直空数据
          if (auth && !retried && typeof app.login === 'function') {
            app.login()
              .then(() => request(Object.assign({}, options, { retried: true })))
              .then(resolve)
              .catch(() => {
                wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
                reject(body)
              })
            return
          }
          wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })
          reject(body)
          return
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
  })
}

module.exports = {
  request,
  get: (url, data, auth) => request({ url, method: 'GET', data, auth }),
  post: (url, data, auth) => request({ url, method: 'POST', data, auth }),
  put: (url, data, auth) => request({ url, method: 'PUT', data, auth }),
  del: (url, data, auth) => request({ url, method: 'DELETE', data, auth })
}
