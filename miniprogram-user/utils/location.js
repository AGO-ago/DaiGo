/**
 * 定位工具：缓存当前位置坐标（带有效期），供首页距离排序、地址列表算距离使用
 */
const CACHE_KEY = 'userLocation'
/** 定位缓存有效期：10 分钟，过期后自动重新定位 */
const CACHE_TTL = 10 * 60 * 1000
/** 地球半径（公里） */
const EARTH_RADIUS_KM = 6371

/** 读取未过期的定位缓存，无缓存或已过期返回 null */
function readCache() {
  const cached = wx.getStorageSync(CACHE_KEY)
  if (!cached || !cached.latitude || !cached.longitude) return null
  if (!cached.time || Date.now() - cached.time > CACHE_TTL) return null
  return cached
}

/** 清除定位缓存（重新定位、切换收货地址时使用） */
function clearCache() {
  wx.removeStorageSync(CACHE_KEY)
}

function showModal(title, content) {
  wx.showModal({ title, content, showCancel: false, confirmText: '知道了' })
}

/** 未授权时引导用户去设置页开启位置权限，返回是否已开启 */
function guideToSetting() {
  return new Promise((resolve) => {
    wx.showModal({
      title: '需要位置权限',
      content: '开启「位置信息」权限后，才能按距离为你推荐附近的美食城',
      confirmText: '去开启',
      cancelText: '暂不',
      success: (modal) => {
        if (!modal.confirm) return resolve(false)
        wx.openSetting({
          success: (setting) => {
            resolve(!!(setting.authSetting && setting.authSetting['scope.userLocation']))
          },
          fail: () => resolve(false)
        })
      },
      fail: () => resolve(false)
    })
  })
}

/**
 * 获取当前位置（gcj02），有效期内直接复用缓存
 * @param {Object} options { force: 是否强制重新定位 }
 * @returns {Promise<{latitude: Number, longitude: Number, time: Number}>}
 */
function getLocation(options = {}) {
  const cached = options.force ? null : readCache()
  if (cached) return Promise.resolve(cached)
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const location = { latitude: res.latitude, longitude: res.longitude, time: Date.now() }
        wx.setStorageSync(CACHE_KEY, location)
        resolve(location)
      },
      fail: (err) => {
        const message = (err && err.errMsg) || '定位失败'
        reject(Object.assign(new Error(message), {
          denied: /deny|denied|unauthorized/i.test(message),
          privacy: /privacy/i.test(message),
          raw: err
        }))
      }
    })
  })
}

/**
 * 获取定位并处理异常：拒绝授权时引导开启，隐私未配置/系统定位关闭时给出提示
 * @param {Object} options { force, silent: true 时不弹任何提示，失败直接返回 null }
 * @returns {Promise<{latitude, longitude, time}|null>} 失败返回 null，不抛错
 */
function ensureLocation(options = {}) {
  return getLocation(options).catch((err) => {
    if (options.silent) return null
    if (err.privacy) {
      showModal('无法获取定位', '小程序尚未完成隐私授权，请在微信后台配置「用户隐私保护指引」后重试')
      return null
    }
    if (!err.denied) {
      showModal('定位失败', '请确认手机已开启定位服务后重试')
      return null
    }
    return guideToSetting().then((granted) => {
      if (!granted) return null
      return getLocation({ force: true }).catch(() => null)
    })
  })
}

/**
 * 地图选点：以 center 为中心打开地图，自动处理取消与未授权
 * @param {Object} center { latitude, longitude }，可省略
 * @returns {Promise<Object|null>} 取消或失败返回 null
 */
function chooseOnMap(center = {}) {
  return new Promise((resolve) => {
    wx.chooseLocation({
      latitude: center.latitude,
      longitude: center.longitude,
      success: (res) => resolve(res),
      fail: (err) => {
        const message = (err && err.errMsg) || ''
        if (/cancel/i.test(message)) return resolve(null)
        if (/deny|denied|unauthorized|auth/i.test(message)) {
          guideToSetting().then(() => resolve(null))
          return
        }
        showModal('选点失败', '请稍后重试')
        resolve(null)
      }
    })
  })
}

/** 打开内置地图查看/导航到指定坐标 */
function openMap(latitude, longitude, name, address) {
  if (!latitude || !longitude) return false
  wx.openLocation({
    latitude: Number(latitude),
    longitude: Number(longitude),
    name: name || '目的地',
    address: address || '',
    scale: 16
  })
  return true
}

/**
 * 两点间球面距离（公里，保留两位小数）
 */
function distanceKm(lat1, lng1, lat2, lng2) {
  if ([lat1, lng1, lat2, lng2].some((v) => v === null || v === undefined || v === '')) {
    return null
  }
  const toRad = (deg) => (Number(deg) * Math.PI) / 180
  const dLat = toRad(lat2) - toRad(lat1)
  const dLng = toRad(lng2) - toRad(lng1)
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLng / 2) ** 2
  return Number((EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFixed(2))
}

/**
 * 距离格式化：不足 1 公里显示米，其余显示公里
 * @param {Number} km 公里数
 * @returns {String} 如 "320m" / "1.2km"，无有效值返回空串
 */
function formatDistance(km) {
  if (km === null || km === undefined || km === '') return ''
  const value = Number(km)
  if (isNaN(value)) return ''
  return value < 1 ? `${Math.round(value * 1000)}m` : `${value.toFixed(1)}km`
}

module.exports = {
  getLocation,
  ensureLocation,
  clearCache,
  chooseOnMap,
  openMap,
  distanceKm,
  formatDistance
}
