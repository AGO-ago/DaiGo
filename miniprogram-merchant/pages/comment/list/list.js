const { get } = require('../../../utils/request')

Page({
  data: {
    list: [],
    loading: true,
    avgScore: '0.0'
  },

  onShow() {
    this.load()
  },

  load() {
    get('/merchant/comments')
      .then((list) => {
        const rows = (list || []).map((c) => ({
          ...c,
          stars: '★★★★★'.slice(0, c.score) + '☆☆☆☆☆'.slice(0, 5 - c.score)
        }))
        const avg = rows.length
          ? (rows.reduce((sum, c) => sum + c.score, 0) / rows.length).toFixed(1)
          : '0.0'
        this.setData({ list: rows, avgScore: avg, loading: false })
      })
      .catch(() => this.setData({ loading: false }))
  }
})
