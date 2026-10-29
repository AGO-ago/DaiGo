/**
 * 定位工具：缓存当前位置坐标，供首页排序、地址列表算距离使用
 */
const CACHE_KEY = 'userLocation'

/**
 * 获取当前位置（gcj02），已有缓存时直接返回
 * @param {Object} options { force: 是否强制重新定位 }
 * @returns {Promise<{latitude: Number, longitude: Number}>}
 */
function getLocation(options = {}) {
  const cached = !options.force && wx.getStorageSync(CACHE_KEY)
  if (cached && cached.latitude) {
    return Promise.resolve(cached)
  }
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: 'gcj02',
      success: (res) => {
        const location = { latitude: res.latitude, longitude: res.longitude }
        wx.setStorageSync(CACHE_KEY, location)
        resolve(location)
      },
      fail: reject
    })
  })
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
  return Number((6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))).toFixed(2))
}

module.exports = {
  getLocation,
  distanceKm
}
