const { get } = require('../../utils/request')
const icons = require('../../utils/icons')
const locationUtil = require('../../utils/location')
const { GROUPS } = require('../../utils/groups')

Page({
  data: {
    icons,
    // 收货位置（可在地址列表中选择）
    location: '定位中…',
    latitude: null,
    longitude: null,
    // 定位状态：locating 定位中；locateFail 既无坐标也无收货地址
    locating: true,
    locateFail: false,
    malls: [],
    homeNotice: '',
    loading: true,
    cartCount: 0,
    cartVisible: false,
    statusBarHeight: 20,
    // 首页展示的群卡片（取第一个群）
    featuredGroup: GROUPS[0] || {}
  },

  onLoad() {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    this.setData({ statusBarHeight: info.statusBarHeight || 20 })
    this.initLocation().then(() => this.loadAll())
  },

  onShow() {
    this.loadLocation()
    this.refreshCart()
    if (this.data.malls.length) {
      this.loadAll()
    }
  },

  /** 距离计算基准：优先已选收货地址，其次设备定位 */
  currentCoord() {
    const address = wx.getStorageSync('selectedAddress') || wx.getStorageSync('defaultAddress')
    if (address && address.latitude && address.longitude) {
      return { latitude: Number(address.latitude), longitude: Number(address.longitude) }
    }
    if (this.data.latitude && this.data.longitude) {
      return { latitude: this.data.latitude, longitude: this.data.longitude }
    }
    return null
  },

  /** 自动定位：已有收货地址时直接用其坐标，避免多余授权；失败只在定位栏提示可重试 */
  initLocation() {
    const address = wx.getStorageSync('selectedAddress') || wx.getStorageSync('defaultAddress')
    if (address && address.latitude && address.longitude) {
      this.setData({ locating: false })
      this.loadLocation()
      return Promise.resolve()
    }
    this.setData({ locating: true })
    return locationUtil.ensureLocation({ silent: true }).then((loc) => {
      if (loc) {
        this.setData({ latitude: loc.latitude, longitude: loc.longitude })
      }
      this.setData({ locating: false })
      this.loadLocation()
    })
  },

  /** 定位栏文案：收货地址 > 当前定位 > 定位失败（仅此时可点击重试） */
  loadLocation() {
    const address = wx.getStorageSync('selectedAddress') || wx.getStorageSync('defaultAddress')
    if (address) {
      this.setData({ location: address.company || address.address || '收货地址', locateFail: false })
    } else if (this.data.locating) {
      this.setData({ location: '定位中…', locateFail: false })
    } else if (this.data.latitude) {
      this.setData({ location: '当前定位', locateFail: false })
    } else {
      this.setData({ location: '定位失败，点击重试', locateFail: true })
    }
  },

  /** 拉取美食城列表：有坐标时后端按距离由近到远排序 */
  loadAll() {
    const coord = this.currentCoord()
    const params = coord ? { lat: coord.latitude, lng: coord.longitude } : {}
    Promise.all([
      get('/mall/list', params, false),
      get('/config/public', {}, false)
    ]).then(([malls, config]) => {
      this.setData({
        malls: (malls || []).map((mall) => ({
          ...mall,
          distanceText: locationUtil.formatDistance(mall.distance)
        })),
        homeNotice: (config && config.homeNotice) || '',
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  refreshCart() {
    const cart = wx.getStorageSync('cart') || []
    this.setData({ cartCount: cart.reduce((s, i) => s + i.count, 0) })
  },

  /** 点击定位栏：重新定位或切换到其他收货地址 */
  chooseLocation() {
    wx.showActionSheet({
      itemList: ['重新定位', '选择收货地址'],
      success: (res) => {
        if (res.tapIndex === 1) {
          wx.navigateTo({ url: '/pages/address/list?select=1' })
          return
        }
        this.relocate()
      }
    })
  },

  /** 手动重新定位（未授权时会引导去设置） */
  relocate() {
    this.setData({ locating: true, location: '定位中…' })
    locationUtil.ensureLocation({ force: true }).then((loc) => {
      if (!loc) {
        this.setData({ locating: false })
        this.loadLocation()
        return
      }
      // 重新定位后以当前位置为准，清掉本地暂存的收货地址（下次进地址列表会重建默认地址）
      wx.removeStorageSync('selectedAddress')
      wx.removeStorageSync('defaultAddress')
      this.setData({
        latitude: loc.latitude,
        longitude: loc.longitude,
        locating: false,
        locateFail: false
      })
      this.loadLocation()
      this.loadAll()
    })
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/search' })
  },

  /** 进入美食城：查看该美食城全部档口 */
  goMall(e) {
    const { mallId, mallName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&mallName=${encodeURIComponent(mallName)}`
    })
  },

  /** 点击档口缩略图直达该档口菜单（复用美食城聚合页并选中档口） */
  goShop(e) {
    const { shopId, shopName, mallId } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&shopId=${shopId}&shopName=${encodeURIComponent(shopName)}`
    })
  },

  openCart() {
    this.setData({ cartVisible: true })
  },

  closeCart() {
    this.setData({ cartVisible: false })
    this.refreshCart()
  },

  onCartChange() {
    this.refreshCart()
  },

  /** 打开扫码入群页 */
  goGroup() {
    wx.navigateTo({ url: '/pages/group/group' })
  },

  /** 分享给好友：默认分享首页 */
  onShareAppMessage() {
    return {
      title: '代go · 校园美食城，附近档口一键下单',
      path: '/pages/index/index'
    }
  },

  /** 分享到朋友圈 */
  onShareTimeline() {
    return {
      title: '代go · 校园美食城，附近档口一键下单'
    }
  }
})
