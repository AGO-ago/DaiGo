const { get } = require('../../utils/request')

Page({
  data: {
    mallId: null,
    mallName: '',
    mall: null,
    shops: [],
    activeShopIndex: 0,
    shop: null,
    categories: [],
    activeCategoryIndex: 0,
    dishes: [],
    loading: true,
    cartCount: 0,
    cartTotal: '0.00',
    cartVisible: false
  },

  onLoad(options) {
    this.targetShopId = options.shopId ? Number(options.shopId) : null
    this.setData({
      mallId: options.mallId,
      mallName: decodeURIComponent(options.mallName || '')
    })
    this.loadMall()
  },

  onShow() {
    this.refreshCart()
  },

  /** 加载美食城头部 + 全部档口，默认选中第一个并加载其菜单 */
  loadMall() {
    Promise.all([
      get(`/mall/${this.data.mallId}`, {}, false),
      get(`/mall/${this.data.mallId}/shops`, {}, false)
    ]).then(([mall, shops]) => {
      const list = shops || []
      this.setData({ mall, shops: list, loading: false })
      if (mall && mall.name) {
        wx.setNavigationBarTitle({ title: mall.name })
      }
      if (list.length) {
        // 从首页/秒杀页带 shopId 进入时，直接选中该档口
        let index = 0
        if (this.targetShopId) {
          const found = list.findIndex((s) => s.id === this.targetShopId)
          if (found > -1) index = found
        }
        this.selectShop(index)
      }
    }).catch(() => {
      this.setData({ loading: false })
    })
  },

  /** 切换档口（全部店家并排展示，点击即切换菜单） */
  onSelectShop(e) {
    const index = Number(e.currentTarget.dataset.index)
    if (index === this.data.activeShopIndex && this.data.categories.length) {
      return
    }
    this.selectShop(index)
  },

  selectShop(index) {
    const shop = this.data.shops[index]
    if (!shop) return
    this.setData({
      activeShopIndex: index,
      shop: shop,
      categories: [],
      dishes: [],
      activeCategoryIndex: 0
    })
    this.loadDishes(shop.id)
  },

  loadDishes(shopId) {
    get(`/shop/${shopId}/dishes`, {}, false)
      .then((tree) => {
        const categories = (tree || []).map((c) => ({
          categoryId: c.categoryId,
          categoryName: c.categoryName,
          dishes: (c.dishes || []).map((d, i) => ({
            ...d,
            hotLabel: i === 0 ? '门店销量第 1 名' : (d.soldCount >= 150 ? '门店热销' : '')
          }))
        }))
        this.setData({ categories, activeCategoryIndex: 0 })
        this.applyCategory(0)
      })
      .catch(() => {})
  },

  switchCategory(e) {
    this.applyCategory(Number(e.currentTarget.dataset.index))
  },

  applyCategory(index) {
    const category = this.data.categories[index]
    this.setData({
      activeCategoryIndex: index,
      dishes: category ? category.dishes : []
    })
  },

  /** 加入购物车（跨档口共用一份本地购物车，结算时后端按档口拆单） */
  addToCart(e) {
    const dishId = Number(e.currentTarget.dataset.dishId)
    const shop = this.data.shop
    const dish = this.findDish(dishId)
    if (!dish) return

    const cart = wx.getStorageSync('cart') || []
    const idx = cart.findIndex((i) => i.dishId === dishId)
    if (idx > -1) {
      cart[idx].count += 1
    } else {
      cart.push({
        dishId: dish.id,
        name: dish.name,
        image: dish.image,
        price: dish.price,
        shopId: shop.id,
        shopName: shop.name,
        count: 1
      })
    }
    wx.setStorageSync('cart', cart)
    this.refreshCart()
  },

  findDish(dishId) {
    for (const c of this.data.categories) {
      const found = c.dishes.find((d) => d.id === dishId)
      if (found) return found
    }
    return null
  },

  refreshCart() {
    const cart = wx.getStorageSync('cart') || []
    const count = cart.reduce((s, i) => s + i.count, 0)
    const total = cart.reduce((s, i) => s + i.price * i.count, 0)
    this.setData({ cartCount: count, cartTotal: total.toFixed(2) })
  },

  /** 点击购物车条，从底部上划展示已选餐品（美团交互） */
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

  goCheckout() {
    if (!this.data.cartCount) return
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  }
})
