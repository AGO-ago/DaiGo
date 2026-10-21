const { get } = require('../../utils/request')
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    dishes: [],
    loading: true,
    countdown: '00:00:00'
  },

  onLoad() {
    get('/dish/hot', { limit: 20 }, false)
      .then((list) => this.setData({ dishes: list || [], loading: false }))
      .catch(() => this.setData({ loading: false }))
    this.startCountdown()
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
    const idx = cart.findIndex((i) => i.dishId === dish.id)
    if (idx > -1) {
      cart[idx].count += 1
    } else {
      cart.push({
        dishId: dish.id,
        name: dish.name,
        image: dish.image,
        price: dish.price,
        shopId: dish.shopId,
        shopName: dish.shopName,
        count: 1
      })
    }
    wx.setStorageSync('cart', cart)
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  goShop(e) {
    const { shopId, mallId, shopName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&shopId=${shopId}&shopName=${encodeURIComponent(shopName || '')}`
    })
  },

  goCart() {
    wx.navigateTo({ url: '/pages/cart/cart' })
  }
})
