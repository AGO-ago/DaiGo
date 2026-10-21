App({
  globalData: {
    // 开发环境指向本机后端；真机预览需改为局域网 IP
    baseUrl: 'http://localhost:8080/api',
    token: ''
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
  }
})
