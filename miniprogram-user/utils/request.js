const app = getApp()

/**
 * 统一请求封装
 * @param {Object} options { url, method, data, auth }
 * @returns {Promise} 业务 data
 */
function request(options) {
  const { url, method = 'GET', data = {}, auth = true } = options
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
      success(res) {
        if (res.statusCode === 401) {
          app.globalData.token = ''
          wx.removeStorageSync('token')
        }
        const body = res.data || {}
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
