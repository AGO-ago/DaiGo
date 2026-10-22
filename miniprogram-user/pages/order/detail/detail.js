const { get, post, put } = require('../../../utils/request')

const STATUS_TEXT = {
  0: '待支付',
  1: '待接单',
  2: '备餐中',
  3: '配送中',
  4: '已完成',
  5: '已取消',
  6: '退款中',
  7: '已退款'
}

Page({
  data: {
    order: null,
    statusText: '',
    loading: true
  },

  onLoad(options) {
    this.orderId = options.id
  },

  onShow() {
    this.loadDetail()
  },

  loadDetail() {
    get(`/order/${this.orderId}`)
      .then((order) => {
        this.setData({
          order: {
            ...order,
            createTime: (order.createTime || '').replace('T', ' ').slice(0, 16)
          },
          statusText: STATUS_TEXT[order.status] || '',
          loading: false
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  pay() {
    post(`/order/pay/${this.data.order.paymentId}`)
      .then(() => {
        wx.showToast({ title: '支付成功', icon: 'success' })
        this.loadDetail()
      })
      .catch(() => {})
  },

  cancel() {
    wx.showModal({
      title: '取消订单',
      content: '确定取消这笔订单吗？',
      success: (res) => {
        if (!res.confirm) return
        put(`/order/${this.orderId}/cancel`)
          .then(() => {
            wx.showToast({ title: '已取消', icon: 'success' })
            this.loadDetail()
          })
          .catch(() => {})
      }
    })
  },

  /** 再来一单：按下单快照重新加购，进入档口菜单（价格/上架状态由结算接口再次校验） */
  repeatOrder() {
    const order = this.data.order
    if (!order || !order.items || !order.items.length) return
    const cart = wx.getStorageSync('cart') || []
    order.items.forEach((item) => {
      const key = `${item.dishId}-${item.specId || 0}`
      const idx = cart.findIndex((i) => i.key === key || (!i.key && i.dishId === item.dishId && !i.specId))
      if (idx > -1) {
        cart[idx].count += item.count
      } else {
        cart.push({
          key,
          dishId: item.dishId,
          specId: item.specId || null,
          specName: item.specName || '',
          specPrice: item.specPrice || 0,
          name: item.dishName,
          image: item.dishImage,
          price: item.price,
          shopId: order.shopId,
          shopName: order.shopName,
          count: item.count
        })
      }
    })
    wx.setStorageSync('cart', cart)
    wx.showToast({ title: '已加入购物车', icon: 'success' })
    setTimeout(() => {
      wx.navigateTo({
        url: `/pages/shop/shop?mallId=${order.mallId}&shopId=${order.shopId}&shopName=${encodeURIComponent(order.shopName || '')}`
      })
    }, 500)
  },

  /** 申请退款（待接单/备餐中可申请，由商家审核） */
  applyRefund() {
    wx.showModal({
      title: '申请退款',
      editable: true,
      placeholderText: '请填写退款原因',
      success: (res) => {
        if (!res.confirm) return
        const reason = (res.content || '').trim()
        if (!reason) {
          wx.showToast({ title: '请填写退款原因', icon: 'none' })
          return
        }
        post(`/order/${this.orderId}/refund`, { reason })
          .then(() => {
            wx.showToast({ title: '已提交退款申请', icon: 'success' })
            this.loadDetail()
          })
          .catch(() => {})
      }
    })
  },

  /** 评价已完成订单 */
  goComment() {
    wx.navigateTo({ url: `/pages/comment/comment?orderId=${this.orderId}&shopName=${encodeURIComponent(this.data.order.shopName || '')}` })
  }
})
