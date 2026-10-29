const { get } = require('../../utils/request')
const icons = require('../../utils/icons')
const locationUtil = require('../../utils/location')

Page({
  data: {
    icons,
    // 收货位置（可在地址列表中选择）
    location: '定位中…',
    latitude: null,
    longitude: null,
    malls: [],
    homeNotice: '',
    loading: true,
    cartCount: 0,
    cartVisible: false,
    statusBarHeight: 20
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

  /** 自动定位：成功后按距离排序展示美食城，失败则回退为手选地址 */
  initLocation() {
    return locationUtil.getLocation()
      .then((loc) => {
        this.setData({ latitude: loc.latitude, longitude: loc.longitude })
        this.loadLocation()
      })
      .catch(() => {
        this.loadLocation()
      })
  },

  loadLocation() {
    const address = wx.getStorageSync('selectedAddress') || wx.getStorageSync('defaultAddress')
    if (address) {
      this.setData({ location: address.company || address.address || '选择收货地址' })
    } else if (this.data.latitude) {
      this.setData({ location: '当前定位' })
    } else {
      this.setData({ location: '选择收货地址' })
    }
  },

  loadAll() {
    const params = {}
    if (this.data.latitude && this.data.longitude) {
      params.lat = this.data.latitude
      params.lng = this.data.longitude
    }
    Promise.all([
      get('/mall/list', params, false),
      get('/config/public', {}, false)
    ]).then(([malls, config]) => {
      this.setData({
        malls: malls || [],
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

  /** 选择收货地址（写字楼/公司） */
  chooseLocation() {
    wx.navigateTo({ url: '/pages/address/list?select=1' })
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
  }
})
