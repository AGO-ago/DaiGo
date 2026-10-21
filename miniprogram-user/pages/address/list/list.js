const { get, put, del } = require('../../utils/request')

Page({
  data: {
    list: [],
    selectMode: false,
    loading: true
  },

  onLoad(options) {
    this.setData({ selectMode: options.select === '1' })
  },

  onShow() {
    this.loadList()
  },

  loadList() {
    get('/user/address')
      .then((list) => {
        const data = list || []
        this.setData({ list: data, loading: false })
        // 同步默认地址到本地，首页定位栏直接展示
        const def = data.find((a) => a.isDefault === 1)
        if (def) {
          wx.setStorageSync('defaultAddress', def)
        } else {
          wx.removeStorageSync('defaultAddress')
        }
      })
      .catch(() => this.setData({ list: [], loading: false }))
  },

  pick(e) {
    const address = this.data.list[e.currentTarget.dataset.index]
    if (!address) return
    if (this.data.selectMode) {
      wx.setStorageSync('selectedAddress', address)
      wx.navigateBack()
    } else {
      wx.navigateTo({ url: `/pages/address/edit?id=${address.id}` })
    }
  },

  setDefault(e) {
    const id = e.currentTarget.dataset.id
    put(`/user/address/${id}/default`)
      .then(() => {
        const address = this.data.list.find((a) => a.id === id)
        if (address) {
          wx.setStorageSync('defaultAddress', { ...address, isDefault: 1 })
        }
        this.loadList()
      })
      .catch(() => {})
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除地址',
      content: '确定删除该收货地址吗？',
      success: (res) => {
        if (!res.confirm) return
        del(`/user/address/${id}`)
          .then(() => this.loadList())
          .catch(() => {})
      }
    })
  },

  add() {
    wx.navigateTo({ url: '/pages/address/edit' })
  }
})
