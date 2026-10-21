const { get, post } = require('../../utils/request')
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    address: null,
    preview: null,
    remark: '',
    submitting: false,
    loading: true
  },

  onShow() {
    // 从地址列表选择返回时优先使用已选地址
    const picked = wx.getStorageSync('selectedAddress')
    if (picked) {
      wx.removeStorageSync('selectedAddress')
      this.setData({ address: picked })
      this.loadPreview()
      return
    }
    this.loadDefaultAddress()
  },

  loadDefaultAddress() {
    get('/user/address')
      .then((list) => {
        const address = (list || []).find((a) => a.isDefault === 1) || (list || [])[0] || null
        this.setData({ address })
        this.loadPreview()
      })
      .catch(() => this.setData({ loading: false }))
  },

  /** 结算预览：后端校验起送价/上架状态并按档口拆单 */
  loadPreview() {
    const cart = wx.getStorageSync('cart') || []
    if (!cart.length) {
      this.setData({ loading: false })
      return
    }
    if (!this.data.address) {
      this.setData({ preview: null, loading: false })
      return
    }
    post('/order/checkout', {
      addressId: this.data.address.id,
      remark: this.data.remark,
      items: cart.map((i) => ({ dishId: i.dishId, quantity: i.count }))
    }).then((preview) => {
      this.setData({ preview, loading: false })
    }).catch(() => this.setData({ preview: null, loading: false }))
  },

  chooseAddress() {
    wx.navigateTo({ url: '/pages/address/list?select=1' })
  },

  onRemarkInput(e) {
    this.setData({ remark: e.detail.value })
  },

  submit() {
    if (this.data.submitting) return
    if (!this.data.address) {
      wx.showToast({ title: '请先选择收货地址', icon: 'none' })
      return
    }
    const cart = wx.getStorageSync('cart') || []
    if (!cart.length) return

    this.setData({ submitting: true })
    post('/order/submit', {
      addressId: this.data.address.id,
      remark: this.data.remark,
      items: cart.map((i) => ({ dishId: i.dishId, quantity: i.count }))
    }).then((res) => {
      // 首版模拟支付：提交后直接发起支付
      wx.showModal({
        title: '确认支付',
        content: `应付金额 ¥${res.totalAmount}`,
        confirmText: '立即支付',
        success: (modal) => {
          if (modal.confirm) {
            this.doPay(res.paymentId)
          } else {
            this.setData({ submitting: false })
            wx.removeStorageSync('cart')
            wx.redirectTo({ url: '/pages/order/list/list' })
          }
        }
      })
    }).catch(() => this.setData({ submitting: false }))
  },

  doPay(paymentId) {
    post(`/order/pay/${paymentId}`)
      .then(() => {
        wx.removeStorageSync('cart')
        wx.showToast({ title: '支付成功', icon: 'success' })
        setTimeout(() => {
          wx.redirectTo({ url: '/pages/order/list/list' })
        }, 800)
      })
      .catch(() => this.setData({ submitting: false }))
  }
})
