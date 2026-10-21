const { get } = require('../../utils/request')
const icons = require('../../utils/icons')

const HISTORY_KEY = 'searchHistory'

Page({
  data: {
    icons,
    keyword: '',
    autoFocus: true,
    history: [],
    hotWords: ['盖饭', '牛肉面', '麻辣烫', '炸鸡', '奶茶', '卤味', '水饺', '米线'],
    shops: [],
    dishes: [],
    searched: false,
    loading: false,
    cartCount: 0,
    cartVisible: false
  },

  onLoad(options) {
    this.setData({ history: wx.getStorageSync(HISTORY_KEY) || [] })
    if (options.keyword) {
      this.setData({ keyword: decodeURIComponent(options.keyword), autoFocus: false })
      this.doSearch()
    }
  },

  onShow() {
    this.refreshCart()
  },

  onInput(e) {
    this.setData({ keyword: e.detail.value })
  },

  clearKeyword() {
    this.setData({ keyword: '', searched: false, shops: [], dishes: [] })
  },

  tapKeyword(e) {
    this.setData({ keyword: e.currentTarget.dataset.keyword })
    this.doSearch()
  },

  doSearch() {
    const keyword = (this.data.keyword || '').trim()
    if (!keyword) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' })
      return
    }
    this.saveHistory(keyword)
    this.setData({ loading: true, keyword })
    get('/search', { keyword }, false)
      .then((res) => {
        this.setData({
          shops: (res && res.shops) || [],
          dishes: (res && res.dishes) || [],
          searched: true,
          loading: false
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  saveHistory(keyword) {
    const history = [keyword].concat(
      (this.data.history || []).filter((k) => k !== keyword)
    ).slice(0, 10)
    wx.setStorageSync(HISTORY_KEY, history)
    this.setData({ history })
  },

  clearHistory() {
    wx.removeStorageSync(HISTORY_KEY)
    this.setData({ history: [] })
  },

  goShop(e) {
    const { mallId, shopId, shopName } = e.currentTarget.dataset
    if (!mallId || !shopId) return
    wx.navigateTo({
      url: `/pages/shop/shop?mallId=${mallId}&shopId=${shopId}&shopName=${encodeURIComponent(shopName || '')}`
    })
  },

  /** 搜索结果直接加购（跨档口共用购物车，结算按档口拆单） */
  addDish(e) {
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
    this.refreshCart()
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  },

  refreshCart() {
    const cart = wx.getStorageSync('cart') || []
    this.setData({ cartCount: cart.reduce((s, i) => s + i.count, 0) })
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
