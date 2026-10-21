const { get } = require('../../utils/request')

Page({
  data: {
    shopId: null,
    shopName: '',
    categories: [],   // 分类名
    dishes: [],       // 当前分类下的菜品
    activeIndex: 0,
    loading: true
  },

  onLoad(options) {
    this.setData({ shopId: options.shopId, shopName: decodeURIComponent(options.shopName || '') })
    wx.setNavigationBarTitle({ title: this.data.shopName })
    this.loadDishes()
  },

  loadDishes() {
    get(`/shop/${this.data.shopId}/dishes`, {}, false)
      .then((res) => {
        const tree = res || []
        const categories = tree.map((c) => c.categoryName)
        this.allDishes = tree.flatMap((c) => (c.dishes || []).map((d) => ({ ...d, category: c.categoryName })))
        this.setData({ categories, loading: false })
        this.applyCategory(0)
      })
      .catch(() => {
        this.setData({ loading: false })
      })
  },

  switchCategory(e) {
    this.applyCategory(Number(e.currentTarget.dataset.index))
  },

  applyCategory(index) {
    const category = this.data.categories[index]
    const dishes = (this.allDishes || []).filter((d) => d.category === category)
    this.setData({ activeIndex: index, dishes })
  },

  addToCart(e) {
    const { dishId, name, price } = e.currentTarget.dataset
    const cart = wx.getStorageSync('cart') || []
    const idx = cart.findIndex((i) => i.dishId === dishId)
    if (idx > -1) {
      cart[idx].count += 1
    } else {
      cart.push({ dishId, name, price, shopId: this.data.shopId, shopName: this.data.shopName, count: 1 })
    }
    wx.setStorageSync('cart', cart)
    wx.showToast({ title: '已加入购物车', icon: 'success' })
  }
})
