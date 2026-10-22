const { get } = require('../../utils/request')
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    // 收货位置（可在地址列表中选择）
    location: '选择收货地址',
    hotDishes: [],
    malls: [],
    loading: true,
    countdown: '00:00:00',
    cartCount: 0,
    cartVisible: false
  },

  onLoad() {
    this.loadLocation()
    this.loadAll()
    this.startCountdown()
  },

  onShow() {
    this.loadLocation()
    this.refreshCart()
    if (this.data.malls.length) {
      this.loadAll()
    }
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },

  loadLocation() {
    const address = wx.getStorageSync('selectedAddress') || wx.getStorageSync('defaultAddress')
    if (address) {
      this.setData({ location: address.company || '选择收货地址' })
    }
  },

  loadAll() {
    Promise.all([
      get('/mall/list', {}, false),
      get('/seckill/list', {}, false)
    ]).then(([malls, seckills]) => {
      this.setData({
        malls: malls || [],
        hotDishes: (seckills || []).slice(0, 6),
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

  /** 今日抢购倒计时（每天 24:00 结束） */
  startCountdown() {
    const tick = () => {
      const now = new Date()
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
      let left = Math.max(0, Math.floor((end - now) / 1000))
      const h = String(Math.floor(left / 3600)).padStart(2, '0')
      const m = String(Math.floor((left % 3600) / 60)).padStart(2, '0')
      const s = String(left % 60).padStart(2, '0')
      this.setData({ countdown: `${h}:${m}:${s}` })
    }
    tick()
    this.timer = setInterval(tick, 1000)
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

  /** 今日抢购 - 更多 */
  goSeckill() {
    wx.navigateTo({ url: '/pages/seckill/seckill' })
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
