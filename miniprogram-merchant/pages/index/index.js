const app = getApp()
const { get, put } = require('../../utils/request')

Page({
  data: {
    user: null,
    shop: null,
    stat: null,
    pendingOrders: [],
    remindOn: true,
    loading: true
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({
      user: user || null,
      remindOn: wx.getStorageSync('order_remind') !== false
    })
    this.startPolling()
    if (user) {
      this.refresh()
    } else {
      this.setData({ loading: false })
    }
  },

  onHide() {
    this.stopPolling()
  },

  onUnload() {
    this.stopPolling()
  },

  onLogin() {
    app.login()
      .then((data) => {
        this.setData({ user: data })
        this.startPolling()
        this.refresh()
      })
      .catch(() => {})
  },

  refresh() {
    get('/merchant/shop')
      .then((shop) => {
        this.setData({ shop: shop || null, loading: false })
        if (!shop) return
        get('/merchant/stat/today')
          .then((stat) => this.setData({ stat: stat || null }))
          .catch(() => {})
        get('/merchant/orders', { status: 1 })
          .then((orders) => {
            const list = orders || []
            this.lastPendingCount = list.length
            this.setData({ pendingOrders: list.slice(0, 5) })
          })
          .catch(() => {})
      })
      .catch(() => this.setData({ shop: null, loading: false }))
  },

  /** 每 20s 轮询待接单数量，数量增加时提醒；页面隐藏时停止 */
  startPolling() {
    this.stopPolling()
    if (!this.data.user) return
    this.pollTimer = setInterval(() => this.pollPending(), 20000)
  },

  stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer)
      this.pollTimer = null
    }
  },

  pollPending() {
    get('/merchant/orders', { status: 1 })
      .then((orders) => {
        const list = orders || []
        const last = this.lastPendingCount
        this.lastPendingCount = list.length
        this.setData({ pendingOrders: list.slice(0, 5) })
        if (last !== undefined && list.length > last) {
          this.remindNewOrder()
        }
      })
      .catch(() => {})
  },

  /** 新订单提醒：震动 + 提示音，音频缺失时降级为震动 + 提示 */
  remindNewOrder() {
    if (!this.data.remindOn) return
    wx.vibrateLong()
    if (!this.audio) {
      this.audio = wx.createInnerAudioContext()
      this.audio.src = '/assets/order.mp3'
      this.audio.onError(() => {})
    }
    this.audio.play()
    wx.showToast({ title: '有新订单，请及时接单', icon: 'none', duration: 2500 })
  },

  onPullDownRefresh() {
    this.refresh()
    wx.stopPullDownRefresh()
  },

  toggleBusiness() {
    const shop = this.data.shop
    if (!shop) return
    const next = shop.status === 1 ? 0 : 1
    put(`/merchant/shop/status?status=${next}`)
      .then(() => {
        this.setData({ 'shop.status': next })
        wx.showToast({ title: next === 1 ? '已开始营业' : '已暂停营业', icon: 'none' })
      })
      .catch(() => {})
  },

  accept(e) {
    const { id } = e.currentTarget.dataset
    put(`/merchant/order/${id}/accept`)
      .then(() => {
        wx.showToast({ title: '已接单', icon: 'success' })
        this.refresh()
      })
      .catch(() => {})
  },

  goOrders(e) {
    const status = e.currentTarget.dataset.status
    app.globalData.orderStatusFilter = status === undefined ? '' : String(status)
    wx.switchTab({ url: '/pages/orders/orders' })
  },
  goOrderDetail(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${id}` })
  },
  goShop() {
    wx.navigateTo({ url: '/pages/shop/shop' })
  }
})
