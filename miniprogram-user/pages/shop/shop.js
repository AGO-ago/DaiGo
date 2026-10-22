const { get, post, del } = require('../../utils/request')

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
    cartVisible: false,
    favorited: false,
    // 评价
    rating: '5.0',
    commentCount: 0,
    comments: [],
    commentVisible: false,
    // 规格选择弹层
    specVisible: false,
    specDish: null,
    specIndex: 0,
    specCount: 1,
    specUnitPrice: '0.00',
    specTotalPrice: '0.00'
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
    this.loadFavorite(shop.id)
    this.loadComments(shop.id)
  },

  /** 档口评价：评分 + 最近评价 */
  loadComments(shopId) {
    get(`/comment/shop/${shopId}`, {}, false)
      .then((list) => {
        const rows = list || []
        const rating = rows.length
          ? (rows.reduce((sum, c) => sum + c.score, 0) / rows.length).toFixed(1)
          : '5.0'
        this.setData({
          rating,
          commentCount: rows.length,
          comments: rows.slice(0, 20).map((c) => ({
            ...c,
            createTime: (c.createTime || '').replace('T', ' ').slice(0, 16),
            replyTimeText: (c.replyTime || '').replace('T', ' ').slice(0, 16),
            stars: '★★★★★'.slice(0, c.score) + '☆☆☆☆☆'.slice(0, 5 - c.score)
          }))
        })
      })
      .catch(() => {})
  },

  openComments() {
    this.setData({ commentVisible: true })
  },

  closeComments() {
    this.setData({ commentVisible: false })
  },

  noop() {},

  /** 收藏状态回显（未登录时静默失败，不影响菜单浏览） */
  loadFavorite(shopId) {
    get('/favorite/ids')
      .then((ids) => this.setData({ favorited: (ids || []).includes(shopId) }))
      .catch(() => this.setData({ favorited: false }))
  },

  toggleFavorite() {
    const shop = this.data.shop
    if (!shop) return
    const request = this.data.favorited ? del(`/favorite/${shop.id}`) : post(`/favorite/${shop.id}`)
    request
      .then(() => {
        const favorited = !this.data.favorited
        this.setData({ favorited })
        wx.showToast({ title: favorited ? '已收藏' : '已取消收藏', icon: 'none' })
      })
      .catch(() => {})
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

  /** 加购入口：有规格先弹层选择，无规格直接加 1 份 */
  addToCart(e) {
    const dishId = Number(e.currentTarget.dataset.dishId)
    const dish = this.findDish(dishId)
    if (!dish) return
    if (dish.specs && dish.specs.length) {
      this.openSpec(dish)
      return
    }
    this.addItem(dish, null, 1)
  },

  /** 打开规格选择弹层（默认选中第一个规格） */
  openSpec(dish) {
    this.setData({
      specVisible: true,
      specDish: dish,
      specIndex: 0,
      specCount: 1
    })
    this.refreshSpecPrice(0, 1)
  },

  closeSpec() {
    this.setData({ specVisible: false, specDish: null })
  },

  pickSpec(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({ specIndex: index })
    this.refreshSpecPrice(index, this.data.specCount)
  },

  changeSpecCount(e) {
    const delta = Number(e.currentTarget.dataset.delta)
    const count = Math.min(99, Math.max(1, this.data.specCount + delta))
    this.setData({ specCount: count })
    this.refreshSpecPrice(this.data.specIndex, count)
  },

  /** 单价 = 菜品价 + 规格加价；总价 = 单价 × 份数 */
  refreshSpecPrice(index, count) {
    const dish = this.data.specDish
    if (!dish) return
    const spec = (dish.specs || [])[index]
    const unit = Number(dish.price) + Number((spec && spec.price) || 0)
    this.setData({
      specUnitPrice: unit.toFixed(2),
      specTotalPrice: (unit * count).toFixed(2)
    })
  },

  confirmSpec() {
    const dish = this.data.specDish
    if (!dish) return
    const spec = (dish.specs || [])[this.data.specIndex] || null
    this.addItem(dish, spec, this.data.specCount)
    this.closeSpec()
  },

  /** 写入本地购物车：同一菜品不同规格视为不同条目 */
  addItem(dish, spec, count) {
    const shop = this.data.shop
    const specId = spec ? spec.id : null
    const key = `${dish.id}-${specId || 0}`
    const unitPrice = Number(dish.price) + Number((spec && spec.price) || 0)
    const cart = wx.getStorageSync('cart') || []
    const idx = cart.findIndex((i) => i.key === key || (!i.key && i.dishId === dish.id && !i.specId))
    if (idx > -1) {
      cart[idx].count += count
    } else {
      cart.push({
        key,
        dishId: dish.id,
        specId,
        specName: spec ? spec.name : '',
        specPrice: spec ? Number(spec.price || 0) : 0,
        name: dish.name,
        image: dish.image,
        price: unitPrice,
        shopId: shop.id,
        shopName: shop.name,
        count
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
