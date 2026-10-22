const { get, put } = require('../../../utils/request')
const { fmtTime, money } = require('../../../utils/format')

const STATUS_MAP = {
  0: { text: '申请中', color: 'wait' },
  1: { text: '已同意', color: 'wait' },
  2: { text: '已拒绝', color: 'reject' },
  3: { text: '已退款', color: 'done' }
}

Page({
  data: {
    tabs: [
      { key: '', label: '全部' },
      { key: '0', label: '申请中' },
      { key: '3', label: '已退款' },
      { key: '2', label: '已拒绝' }
    ],
    current: '',
    list: [],
    loading: false
  },

  onShow() {
    this.loadList()
  },

  onPullDownRefresh() {
    this.loadList()
  },

  switchTab(e) {
    this.setData({ current: e.currentTarget.dataset.key, list: [] })
    this.loadList()
  },

  loadList() {
    this.setData({ loading: true })
    const status = this.data.current
    const query = status === '' ? '' : `?status=${status}`
    get(`/merchant/refunds${query}`)
      .then((list) => {
        const mapped = (list || []).map((item) => {
          const meta = STATUS_MAP[item.status] || { text: '未知', color: '' }
          return {
            ...item,
            statusText: meta.text,
            color: meta.color,
            amountText: money(item.amount),
            createTimeText: fmtTime(item.createTime),
            handleTimeText: fmtTime(item.handleTime)
          }
        })
        this.setData({ list: mapped, loading: false })
        wx.stopPullDownRefresh()
      })
      .catch(() => {
        this.setData({ list: [], loading: false })
        wx.stopPullDownRefresh()
      })
  },

  onAgree(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '同意退款',
      content: '同意后订单将标记为已退款，金额原路退回（模拟）',
      success: (res) => {
        if (!res.confirm) return
        put(`/merchant/refund/${id}/agree`)
          .then(() => {
            wx.showToast({ title: '已同意退款', icon: 'success' })
            this.loadList()
          })
          .catch(() => {})
      }
    })
  },

  onReject(e) {
    const { id } = e.currentTarget.dataset
    wx.showModal({
      title: '拒绝退款',
      editable: true,
      placeholderText: '请填写拒绝理由，将同步给顾客',
      success: (res) => {
        if (!res.confirm) return
        const reason = (res.content || '').trim()
        if (!reason) {
          wx.showToast({ title: '拒绝理由不能为空', icon: 'none' })
          return
        }
        put(`/merchant/refund/${id}/reject`, { reason })
          .then(() => {
            wx.showToast({ title: '已拒绝', icon: 'none' })
            this.loadList()
          })
          .catch(() => {})
      }
    })
  },

  goOrder(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${id}` })
  }
})
