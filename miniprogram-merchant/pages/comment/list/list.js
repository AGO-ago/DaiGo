const { get, put } = require('../../../utils/request')

Page({
  data: {
    list: [],
    loading: true,
    avgScore: '0.0',
    replyVisible: false,
    replyTarget: null,
    replyContent: ''
  },

  onShow() {
    this.load()
  },

  load() {
    get('/merchant/comments')
      .then((list) => {
        const rows = (list || []).map((c) => ({
          ...c,
          stars: '★★★★★'.slice(0, c.score) + '☆☆☆☆☆'.slice(0, 5 - c.score),
          showTime: c.createTime ? c.createTime.replace('T', ' ').slice(0, 16) : '',
          replyTimeText: c.replyTime ? c.replyTime.replace('T', ' ').slice(0, 16) : ''
        }))
        const avg = rows.length
          ? (rows.reduce((sum, c) => sum + c.score, 0) / rows.length).toFixed(1)
          : '0.0'
        this.setData({ list: rows, avgScore: avg, loading: false })
      })
      .catch(() => this.setData({ loading: false }))
  },

  openReply(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      replyVisible: true,
      replyTarget: item,
      replyContent: item.replyContent || ''
    })
  },

  closeReply() {
    this.setData({ replyVisible: false, replyTarget: null, replyContent: '' })
  },

  onReplyInput(e) {
    this.setData({ replyContent: e.detail.value })
  },

  submitReply() {
    const content = (this.data.replyContent || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入回复内容', icon: 'none' })
      return
    }
    put(`/merchant/comment/${this.data.replyTarget.id}/reply`, { content })
      .then(() => {
        wx.showToast({ title: '回复成功', icon: 'success' })
        this.closeReply()
        this.load()
      })
      .catch(() => {})
  }
})
