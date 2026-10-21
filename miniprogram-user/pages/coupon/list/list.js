const { get, post } = require('../../../utils/request')

/** 券面金额文案：满减显示 ¥X，折扣显示 X折 */
const amountText = (item) =>
  item.type === 2 ? `${(Number(item.amount) * 10).toFixed(1).replace(/\.0$/, '')}折` : Number(item.amount).toFixed(0)

const thresholdText = (item) =>
  Number(item.threshold) > 0 ? `满${Number(item.threshold).toFixed(0)}元可用` : '无门槛'

const endText = (time) => (time ? String(time).slice(5, 10).replace('-', '.') : '')

Page({
  data: {
    list: [],
    loading: true
  },

  onShow() {
    this.load()
  },

  load() {
    get('/coupon/list')
      .then((list) => {
        this.setData({
          list: (list || []).map((item) => ({
            ...item,
            amountText: amountText(item),
            thresholdText: thresholdText(item),
            endText: endText(item.endTime)
          })),
          loading: false
        })
      })
      .catch(() => this.setData({ loading: false }))
  },

  receive(e) {
    const { id } = e.currentTarget.dataset
    post(`/coupon/${id}/receive`)
      .then(() => {
        wx.showToast({ title: '领取成功', icon: 'success' })
        this.load()
      })
      .catch(() => {})
  },

  goMine() {
    wx.navigateTo({ url: '/pages/coupon/mine/mine' })
  }
})
