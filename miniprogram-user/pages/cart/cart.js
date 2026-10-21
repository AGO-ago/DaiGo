Page({
  data: {
    groups: [],
    count: 0,
    total: '0.00'
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    const cart = wx.getStorageSync('cart') || []
    const map = {}
    cart.forEach((item, index) => {
      const key = item.shopId
      if (!map[key]) {
        map[key] = { shopId: key, shopName: item.shopName, items: [] }
      }
      map[key].items.push({ ...item, index })
    })
    const count = cart.reduce((s, i) => s + i.count, 0)
    const total = cart.reduce((s, i) => s + i.price * i.count, 0)
    this.setData({
      groups: Object.values(map),
      count,
      total: total.toFixed(2)
    })
  },

  changeCount(e) {
    const { index, delta } = e.currentTarget.dataset
    const cart = wx.getStorageSync('cart') || []
    const target = cart[Number(index)]
    if (!target) return
    target.count += Number(delta)
    if (target.count <= 0) {
      cart.splice(Number(index), 1)
    }
    wx.setStorageSync('cart', cart)
    this.refresh()
  },

  clear() {
    wx.showModal({
      title: '清空购物车',
      content: '确定清空所有已选商品吗？',
      success: (res) => {
        if (!res.confirm) return
        wx.removeStorageSync('cart')
        this.refresh()
      }
    })
  },

  goShop() {
    wx.switchTab({ url: '/pages/index/index' })
  },

  goCheckout() {
    if (!this.data.count) return
    wx.navigateTo({ url: '/pages/checkout/checkout' })
  }
})
