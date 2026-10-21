const { get, post } = require('../../utils/request')
const icons = require('../../utils/icons')

const amountText = (item) =>
  item.type === 2 ? `${(Number(item.amount) * 10).toFixed(1).replace(/\.0$/, '')}折` : `¥${Number(item.amount).toFixed(0)}`

Page({
  data: {
    icons,
    address: null,
    preview: null,
    remark: '',
    submitting: false,
    loading: true,
    // 优惠券
    coupons: [],
    couponVisible: false,
    selectedCouponId: null,
    selectedCouponName: '',
    couponUsableCount: 0
  },

  onShow() {
    // 全局选中的收货地址（首页定位栏/地址列表选择）优先，其次取默认地址
    const picked = wx.getStorageSync('selectedAddress')
    if (picked) {
      this.setData({ address: picked })
      this.refresh()
      return
    }
    this.loadDefaultAddress()
  },

  loadDefaultAddress() {
    get('/user/address')
      .then((list) => {
        const address = (list || []).find((a) => a.isDefault === 1) || (list || [])[0] || null
        this.setData({ address })
        this.refresh()
      })
      .catch(() => this.setData({ loading: false }))
  },

  refresh() {
    this.loadPreview()
    this.loadCoupons()
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
    const payload = {
      addressId: this.data.address.id,
      remark: this.data.remark,
      items: cart.map((i) => ({ dishId: i.dishId, quantity: i.count }))
    }
    if (this.data.selectedCouponId) {
      payload.userCouponId = this.data.selectedCouponId
    }
    post('/order/checkout', payload)
      .then((preview) => {
        this.setData({ preview, loading: false })
        this.markCoupons()
      })
      .catch(() => {
        // 选中的券因购物车变动不可用时，自动取消选择后重试
        if (this.data.selectedCouponId) {
          this.setData({ selectedCouponId: null, selectedCouponName: '' })
          this.loadPreview()
          return
        }
        this.setData({ preview: null, loading: false })
      })
  },

  /** 我的可用券，并按当前购物车计算可用状态 */
  loadCoupons() {
    get('/user/coupon', { status: 0 })
      .then((list) => {
        this.setData({
          coupons: (list || []).map((c) => ({ ...c, amountText: amountText(c) }))
        })
        this.markCoupons()
      })
      .catch(() => {})
  },

  markCoupons() {
    const preview = this.data.preview
    if (!preview) return
    const groups = preview.groups || []
    let usableCount = 0
    const coupons = this.data.coupons.map((c) => {
      const target = c.shopId ? groups.find((g) => g.shopId === c.shopId) : null
      const subtotal = c.shopId
        ? (target ? Number(target.totalAmount) : 0)
        : groups.reduce((sum, g) => sum + Number(g.totalAmount), 0)
      const threshold = Number(c.threshold) || 0
      const usable = subtotal > 0 && subtotal >= threshold
      if (usable) usableCount++
      const reason = !usable
        ? (c.shopId && !target ? '本单不含该档口' : `还差¥${(threshold - subtotal).toFixed(2)}可用`)
        : ''
      return { ...c, usable, reason, selected: c.id === this.data.selectedCouponId }
    })
    this.setData({ coupons, couponUsableCount: usableCount })
  },

  openCoupon() {
    if (!this.data.coupons.length) {
      wx.showToast({ title: '暂无可用优惠券', icon: 'none' })
      return
    }
    this.markCoupons()
    this.setData({ couponVisible: true })
  },

  closeCoupon() {
    this.setData({ couponVisible: false })
  },

  noop() {},

  pickCoupon(e) {
    const coupon = this.data.coupons.find((c) => c.id === e.currentTarget.dataset.id)
    if (!coupon) return
    if (!coupon.usable) {
      wx.showToast({ title: coupon.reason || '该券不可用', icon: 'none' })
      return
    }
    this.setData({
      selectedCouponId: coupon.id,
      selectedCouponName: coupon.name,
      couponVisible: false
    })
    this.loadPreview()
  },

  clearCoupon() {
    this.setData({
      selectedCouponId: null,
      selectedCouponName: '',
      couponVisible: false
    })
    this.loadPreview()
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
    const payload = {
      addressId: this.data.address.id,
      remark: this.data.remark,
      items: cart.map((i) => ({ dishId: i.dishId, quantity: i.count }))
    }
    if (this.data.selectedCouponId) {
      payload.userCouponId = this.data.selectedCouponId
    }
    post('/order/submit', payload).then((res) => {
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
