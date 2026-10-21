const { get, put } = require('../../utils/request')

const STATUS_ACTION = {
  1: { next: 2, action: 'accept', text: '接单' },
  2: { next: 3, action: 'prepare', text: '开始备餐' },
  3: { next: 4, action: 'deliver', text: '开始配送' },
  4: { next: null, action: '', text: '已完成' }
}

Page({
  data: {
    orders: [],
    STATUS_ACTION,
    statusTabs: [
      { key: '', label: '全部' },
      { key: '1', label: '待接单' },
      { key: '2', label: '备餐中' },
      { key: '3', label: '配送中' },
      { key: '4', label: '已完成' }
    ],
    currentStatus: ''
  },

  onShow() {
    this.loadOrders()
  },

  switchStatus(e) {
    this.setData({ currentStatus: e.currentTarget.dataset.key })
    this.loadOrders()
  },

  loadOrders() {
    get(`/merchant/orders?status=${this.data.currentStatus}`)
      .then((res) => this.setData({ orders: res || [] }))
      .catch(() => this.setData({ orders: [] }))
  },

  onAction(e) {
    const { id, action } = e.currentTarget.dataset
    put(`/merchant/order/${id}/${action}`)
      .then(() => {
        wx.showToast({ title: '操作成功', icon: 'success' })
        this.loadOrders()
      })
      .catch(() => {})
  }
})
