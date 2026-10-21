const app = getApp()
const icons = require('../../utils/icons')

Page({
  data: {
    icons,
    user: null,
    orderEntries: [
      { key: '1', label: '待接单', icon: '📋' },
      { key: '2', label: '备餐中', icon: '🍳' },
      { key: '3', label: '配送中', icon: '🛵' },
      { key: '4', label: '已完成', icon: '✅' }
    ]
  },

  onShow() {
    const user = wx.getStorageSync('user')
    this.setData({ user: user || null })
  },

  onLogin() {
    app.ensureLogin().then((ok) => {
      if (ok) {
        this.setData({ user: wx.getStorageSync('user') })
        wx.showToast({ title: '登录成功', icon: 'success' })
      }
    })
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: (res) => {
        if (!res.confirm) return
        app.globalData.token = ''
        wx.removeStorageSync('token')
        wx.removeStorageSync('user')
        this.setData({ user: null })
      }
    })
  },

  goOrders(e) {
    const status = e.currentTarget.dataset.status || ''
    // tabBar 页面不支持带参跳转，用全局变量传递筛选状态
    getApp().globalData.orderStatusFilter = status
    wx.switchTab({ url: '/pages/order/list/list' })
  },

  goAddress() {
    wx.navigateTo({ url: '/pages/address/list' })
  },

  goFavorite() {
    wx.navigateTo({ url: '/pages/favorite/list/list' })
  },

  contactService() {
    wx.makePhoneCall({ phoneNumber: '4000000000' }).catch(() => {})
  }
})
