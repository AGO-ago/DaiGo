const { get } = require('../../../utils/request')

const STATUS_TEXT = {
  0: '待支付',
  1: '待接单',
  2: '备餐中',
  3: '配送中',
  4: '已完成',
  5: '已取消'
}

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
    currentStatus: '',
    loading: true
  },

  onLoad() {
    this.loadOrders()
  },

  onShow() {
    this.loadOrders()
  },

  switchStatus(e) {
    this.setData({ currentStatus: e.currentTarget.dataset.key })
    this.loadOrders()
  },

  loadOrders() {
    // 空状态不传 status，避免后端把空串解析为数字时报错
    const query = this.data.currentStatus ? { status: this.data.currentStatus } : {}
    get('/order/list', query)
      .then((res) => {
        const orders = ((res && res.list) || []).map((o) => ({
          ...o,
          id: o.orderId,
          statusText: STATUS_TEXT[o.status] || '',
          summary: (o.items || []).map((i) => i.dishName).join('、'),
          createTime: (o.createTime || '').replace('T', ' ').slice(0, 16)
        }))
        this.setData({ orders, loading: false })
      })
      .catch(() => this.setData({ orders: [], loading: false }))
  },

  goDetail(e) {
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${e.currentTarget.dataset.id}` })
  }
})
