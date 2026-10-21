const { get } = require('../../../utils/request')

Page({
  data: {
    orders: [],
    statusTabs: [
      { key: '', label: '全部' },
      { key: '1', label: '待接单' },
      { key: '2', label: '备餐中' },
      { key: '3', label: '配送中' },
      { key: '4', label: '已完成' }
    ],
    currentStatus: ''
  },

  onLoad() {
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  switchStatus(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ currentStatus: key })
    this.loadOrders()
  },

  loadOrders() {
    get(`/order/list?status=${this.data.currentStatus}`)
      .then((res) => this.setData({ orders: res || [] }))
      .catch(() => this.setData({ orders: [] }))
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${e.currentTarget.dataset.id}` })
  }
})
