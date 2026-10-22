const app = getApp()
const { get, put } = require('../../utils/request')

/** 状态文案与下一步操作 */
const STATUS_MAP = {
  0: { text: '待支付', action: '', btn: '' },
  1: { text: '待接单', action: 'accept', btn: '接单' },
  2: { text: '备餐中', action: 'prepare', btn: '开始配送' },
  3: { text: '配送中', action: 'deliver', btn: '确认完成' },
  4: { text: '已完成', action: '', btn: '' },
  5: { text: '已取消', action: '', btn: '' },
  6: { text: '退款中', action: '', btn: '' },
  7: { text: '已退款', action: '', btn: '' }
}

Page({
  data: {
    statusTabs: [
      { key: '1', label: '待接单' },
      { key: '2', label: '备餐中' },
      { key: '3', label: '配送中' },
      { key: '4', label: '已完成' },
      { key: '6', label: '退款' },
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
          return { ...o, statusText: meta.text, action: meta.action, btnText: meta.btn, canReject: o.status === 1 }
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

  onReject(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '拒单原因',
      editable: true,
      placeholderText: '请填写拒单原因，将同步给顾客',
      success: (res) => {
        if (!res.confirm) return
        const reason = (res.content || '').trim()
        if (!reason) {
          wx.showToast({ title: '拒单原因不能为空', icon: 'none' })
          return
        }
        put(`/merchant/order/${id}/reject`, { reason })
          .then(() => {
            wx.showToast({ title: '已拒单并退款', icon: 'none' })
            this.loadOrders()
          })
          .catch(() => {})
      }
    })
  },

  goDetail(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${id}` })
  },

  callCustomer() {
    wx.showToast({ title: '请在地址中查看顾客电话', icon: 'none' })
  }
})
