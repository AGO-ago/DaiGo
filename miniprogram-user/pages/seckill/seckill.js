const { get } = require('../../utils/request')
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    dishes: [],
    loading: true,
    countdown: '00:00:00',
    cartCount: 0,
    cartVisible: false
  },

  onLoad() {
    get('/seckill/list', {}, false)
      .then((list) => this.setData({ dishes: list || [], loading: false }))
      .catch(() => this.setData({ loading: false }))
    this.startCountdown()
  },

  onShow() {
    this.refreshCart()
  },

  onUnload() {
    if (this.timer) clearInterval(this.timer)
  },

  startCountdown() {
    const tick = () => {
      const now = new Date()
      const end = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1)
      const left = Math.max(0, Math.floor((end - now) / 1000))
      const h = String(Math.floor(left / 3600)).padStart(2, '0')
      const m = String(Math.floor((left % 3600) / 60)).padStart(2, '0')
      const s = String(left % 60).padStart(2, '0')
      this.setData({ countdown: `${h}:${m}:${s}` })
    }
    tick()
    this.timer = setInterval(tick, 1000)
  },

  /** 抢购：加入购物车 */
  grab(e) {
    const dish = this.data.dishes[e.currentTarget.dataset.index]
    if (!dish) return
    const cart = wx.getStorageSync('cart') || []
    const idx = cart.findIndex((i) => i.dishId === dish.dishId && !i.specId)
    if (idx > -1) {
      cart[idx].count += 1
    } else {
      cart.push({
        dishId: dish.dishId,
        name: dish.name,
        image: dish.image,
        price: dish.price,
        shopId: dish.shopId,
        shopName: dish.shopName,
        count: 1
      })
    }
    wx.setStorageSync('cart', cart)
    this.refreshCart()
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  goShop(e) {
    const { shopId, mallId, shopName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&shopId=${shopId}&shopName=${encodeURIComponent(shopName || '')}`
    })
  },

  refreshCart() {
    const cart = wx.getStorageSync('cart') || []
    this.setData({ cartCount: cart.reduce((s, i) => s + i.count, 0) })
  },

  /** 点击悬浮购物车，从底部上划查看已选餐品 */
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
