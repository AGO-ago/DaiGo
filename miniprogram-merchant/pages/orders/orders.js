const app = getApp()
const { get, put } = require('../../utils/request')

/** 状态文案与下一步操作 */
const STATUS_MAP = {
  0: { text: '待支付', action: '', btn: '' },
  1: { text: '待接单', action: 'accept', btn: '接单' },
  2: { text: '备餐中', action: 'prepare', btn: '开始配送' },
  3: { text: '配送中', action: 'deliver', btn: '确认完成' },
  4: { text: '已完成', action: '', btn: '' },
  5: { text: '已取消', action: '', btn: '' }
}

Page({
  data: {
    statusTabs: [
      { key: '1', label: '待接单' },
      { key: '2', label: '备餐中' },
      { key: '3', label: '配送中' },
      { key: '4', label: '已完成' },
      { key: '', label: '全部' }
    ],
    currentStatus: '1',
    orders: [],
    loading: false
  },

  onShow() {
    // 工作台点击统计卡片时通过全局变量传入筛选状态（'' 表示全部）
    const filter = app.globalData.orderStatusFilter
    if (filter !== null && filter !== undefined) {
      this.setData({ currentStatus: String(filter) })
      app.globalData.orderStatusFilter = null
    }
    this.loadOrders()
  },

  onPullDownRefresh() {
    this.loadOrders()
  },

  switchStatus(e) {
    this.setData({ currentStatus: e.currentTarget.dataset.key, orders: [], loading: true })
    this.loadOrders()
  },

  loadOrders() {
    this.setData({ loading: true })
    const status = this.data.currentStatus
    const query = status === '' ? '' : `?status=${status}`
    get(`/merchant/orders${query}`)
      .then((list) => {
        const orders = (list || []).map((o) => {
          const meta = STATUS_MAP[o.status] || { text: '未知', action: '', btn: '' }
          return { ...o, statusText: meta.text, action: meta.action, btnText: meta.btn }
        })
        this.setData({ orders, loading: false })
        wx.stopPullDownRefresh()
      })
      .catch(() => {
        this.setData({ orders: [], loading: false })
        wx.stopPullDownRefresh()
      })
  },

  onAction(e) {
    const { id, action, text } = e.currentTarget.dataset
    wx.showModal({
      title: text,
      content: `确认对订单 #${id} 执行「${text}」？`,
      success: (res) => {
        if (!res.confirm) return
        put(`/merchant/order/${id}/${action}`)
          .then(() => {
            wx.showToast({ title: '操作成功', icon: 'success' })
            this.loadOrders()
          })
          .catch(() => {})
      }
    })
  },

  callCustomer() {
    wx.showToast({ title: '请在地址中查看顾客电话', icon: 'none' })
  }
})
