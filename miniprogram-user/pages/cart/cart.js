Page({
  data: {
    items: [],
    total: 0
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    const items = wx.getStorageSync('cart') || []
    const total = items.reduce((s, i) => s + i.price * i.count, 0)
    this.setData({ items, total: total.toFixed(2) })
  },

  changeCount(e) {
    const { index, delta } = e.currentTarget.dataset
    const items = this.data.items
    items[index].count += Number(delta)
    if (items[index].count <= 0) {
      items.splice(index, 1)
    }
    wx.setStorageSync('cart', items)
    this.refresh()
  },

  goCheckout() {
    if (!this.data.items.length) return
    wx.showToast({ title: '结算接口待接入', icon: 'none' })
  }
})
