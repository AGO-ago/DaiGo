const { get, put } = require('../../../utils/request')
const { fmtTime, money } = require('../../../utils/format')

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
    id: null,
    order: null,
    statusText: '',
    action: '',
    actionText: '',
    timeline: [],
    loading: true
  },

  onLoad(options) {
    this.setData({ id: options.id })
  },

  onShow() {
    this.loadDetail()
  },

  loadDetail() {
    if (!this.data.id) return
    this.setData({ loading: true })
    get(`/merchant/order/${this.data.id}`)
      .then((order) => {
        const meta = STATUS_MAP[order.status] || { text: '未知', action: '', btn: '' }
        this.setData({
          order: {
            ...order,
            payAmountText: money(order.payAmount),
            totalAmountText: money(order.totalAmount),
            deliveryFeeText: money(order.deliveryFee),
            discountAmountText: money(order.discountAmount),
            items: (order.items || []).map((item) => ({ ...item, amountText: money(item.amount) }))
          },
          statusText: meta.text,
          action: meta.action,
          actionText: meta.btn,
          timeline: this.buildTimeline(order),
          loading: false
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  buildTimeline(order) {
    const steps = [
      { label: '顾客下单', time: order.createTime },
      { label: '商家接单', time: order.acceptTime },
      { label: '开始备餐', time: order.prepareTime },
      { label: '开始配送', time: order.deliveryTime },
      { label: '订单完成', time: order.finishTime }
    ]
    if (order.cancelTime) {
      steps.push({ label: order.status === 7 ? '退款完成' : '订单取消', time: order.cancelTime })
    }
    return steps
      .filter((step) => step.time)
      .map((step, index) => ({
        label: step.label,
        time: fmtTime(step.time),
        active: index === 0
      }))
  },

  onAction() {
    const { id, action } = this.data
    if (!action) return
    const text = this.data.actionText
    wx.showModal({
      title: text,
      content: `确认执行「${text}」？`,
      success: (res) => {
        if (!res.confirm) return
        put(`/merchant/order/${id}/${action}`)
          .then(() => {
            wx.showToast({ title: '操作成功', icon: 'success' })
            this.loadDetail()
          })
          .catch(() => {})
      }
    })
  },

  onReject() {
    const id = this.data.id
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
            this.loadDetail()
          })
          .catch(() => {})
      }
    })
  },

  goRefunds() {
    wx.navigateTo({ url: '/pages/refund/list/list' })
  }
})
