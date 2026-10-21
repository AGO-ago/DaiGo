const { post } = require('../../utils/request')

const SCORE_TEXT = {
  1: '非常差',
  2: '较差',
  3: '一般',
  4: '满意',
  5: '非常满意'
}

Page({
  data: {
    orderId: null,
    shopName: '',
    score: 5,
    scoreText: SCORE_TEXT[5],
    content: '',
    contentLength: 0,
    submitting: false
  },

  onLoad(options) {
    this.setData({
      orderId: options.orderId ? Number(options.orderId) : null,
      shopName: decodeURIComponent(options.shopName || '')
    })
  },

  setScore(e) {
    const score = Number(e.currentTarget.dataset.score)
    this.setData({ score, scoreText: SCORE_TEXT[score] })
  },

  onInput(e) {
    this.setData({ content: e.detail.value, contentLength: e.detail.value.length })
  },

  submit() {
    if (this.data.submitting) return
    if (!this.data.orderId) return
    this.setData({ submitting: true })
    post('/comment', {
      orderId: this.data.orderId,
      score: this.data.score,
      content: this.data.content
    }).then(() => {
      wx.showToast({ title: '评价成功', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 600)
    }).catch(() => this.setData({ submitting: false }))
  }
})
