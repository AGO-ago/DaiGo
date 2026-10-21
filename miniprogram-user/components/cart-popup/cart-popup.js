Component({
  properties: {
    visible: { type: Boolean, value: false },
    /** 是否展示去结算按钮（秒杀页也复用） */
    showCheckout: { type: Boolean, value: true }
  },

  data: {
    groups: [],
    count: 0,
    total: '0.00'
  },

  observers: {
    visible(val) {
      if (val) this.refresh()
    }
  },

  methods: {
    noop() {},

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
      this.setData({ groups: Object.values(map), count, total: total.toFixed(2) })
      this.triggerEvent('change', { count, total: total.toFixed(2) })
    },

    changeCount(e) {
      const { index, delta } = e.currentTarget.dataset
      const cart = wx.getStorageSync('cart') || []
      const target = cart[Number(index)]
      if (!target) return
      target.count += Number(delta)
      if (target.count <= 0) cart.splice(Number(index), 1)
      wx.setStorageSync('cart', cart)
      this.refresh()
    },

    clear() {
      wx.removeStorageSync('cart')
      this.refresh()
    },

    close() {
      this.triggerEvent('close')
    },

    goCheckout() {
      if (!this.data.count) return
      this.triggerEvent('close')
      wx.navigateTo({ url: '/pages/checkout/checkout' })
    }
  }
})
