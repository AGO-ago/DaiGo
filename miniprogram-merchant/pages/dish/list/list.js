const { get } = require('../../../utils/request')

Page({
  data: {
    dishes: []
  },

  onShow() {
    this.loadDishes()
  },

  loadDishes() {
    get('/merchant/dishes')
      .then((res) => this.setData({ dishes: res || [] }))
      .catch(() => this.setData({ dishes: [] }))
  },

  onAdd() {
    wx.showToast({ title: '新增接口待接入', icon: 'none' })
  },

  onEdit(e) {
    const id = e.currentTarget.dataset.id
    wx.showToast({ title: `编辑 ${id} 待接入`, icon: 'none' })
  }
})
