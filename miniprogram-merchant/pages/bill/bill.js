const { get } = require('../../utils/request')
const { fmtTime, money, percent } = require('../../utils/format')

Page({
  data: {
    periods: [
      { key: 'today', label: '今日' },
      { key: '7d', label: '近 7 天' },
      { key: 'month', label: '本月' }
    ],
    period: 'today',
    summary: null,
    items: [],
    settlements: [],
    loading: false
  },

  onShow() {
    this.loadAll()
  },

  onPullDownRefresh() {
    this.loadAll()
  },

  switchPeriod(e) {
    this.setData({ period: e.currentTarget.dataset.key })
    this.loadBill()
  },

  loadAll() {
    this.loadBill()
    this.loadSettlements()
  },

  loadBill() {
    this.setData({ loading: true })
    get('/merchant/bill', { period: this.data.period })
      .then((data) => {
        const summary = data.summary || {}
        const refundAmount = Number(summary.refundAmount || 0)
        this.setData({
          summary: {
            orderCount: summary.orderCount || 0,
            goodsAmount: money(summary.goodsAmount),
            deliveryAmount: money(summary.deliveryAmount),
            commissionAmount: money(summary.commissionAmount),
            refundAmount: money(refundAmount),
            hasRefund: refundAmount > 0,
            netAmount: money(summary.netAmount)
          },
          items: (data.items || []).map((item) => ({
            ...item,
            goodsAmountText: money(item.goodsAmount),
            deliveryAmountText: money(item.deliveryAmount),
            platformDiscountText: money(item.platformDiscount),
            shopDiscountText: money(item.shopDiscount),
            commissionRateText: percent(item.commissionRate),
            commissionAmountText: money(item.commissionAmount),
            netAmountText: money(item.netAmount),
            finishTimeText: fmtTime(item.finishTime)
          })),
          loading: false
        })
        wx.stopPullDownRefresh()
      })
      .catch(() => {
        this.setData({ loading: false })
        wx.stopPullDownRefresh()
      })
  },

  loadSettlements() {
    get('/merchant/settlements')
      .then((list) => {
        this.setData({
          settlements: (list || []).map((item) => ({
            ...item,
            periodText: `${item.periodStart} ~ ${item.periodEnd}`,
            goodsAmountText: money(item.goodsAmount),
            deliveryAmountText: money(item.deliveryAmount),
            commissionAmountText: money(item.commissionAmount),
            refundAmountText: money(item.refundAmount),
            netAmountText: money(item.netAmount),
            commissionRateText: percent(item.commissionRate),
            statusText: item.status === 1 ? '已结算' : '待结算',
            settleTimeText: fmtTime(item.settleTime)
          }))
        })
      })
      .catch(() => {})
  },

  goOrder(e) {
    const { id } = e.currentTarget.dataset
    wx.navigateTo({ url: `/pages/order/detail/detail?id=${id}` })
  }
})
