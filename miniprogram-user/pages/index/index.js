const { get } = require('../../utils/request')
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    // 收货位置（首版固定为写字楼/校区，后续接入定位选择）
    location: '青岛理工大学(黄岛校区)',
    hotDishes: [],
    malls: [],
    loading: true,
    countdown: '00:00:00'
  },

  onLoad() {
    this.loadAll()
    this.startCountdown()
  },

  onShow() {
    if (this.data.malls.length) {
      this.loadAll()
    }
  },

  onUnload() {
    if (this.timer) {
      clearInterval(this.timer)
    }
  },

  loadAll() {
    Promise.all([
      get('/mall/list', {}, false),
      get('/dish/hot', { limit: 6 }, false)
    ]).then(([malls, hotDishes]) => {
      this.setData({
        malls: malls || [],
        hotDishes: hotDishes || [],
        loading: false
      })
    }).catch(() => {
      this.setData({ loading: false })
    })
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

  goCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  }
})
