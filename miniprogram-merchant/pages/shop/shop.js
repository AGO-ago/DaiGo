const app = getApp()
const { get, put } = require('../../utils/request')

Page({
  data: {
    loading: true,
    bound: false,
    shop: null,
    mall: null,
    form: {
      name: '',
      logo: '',
      phone: '',
      deliveryFee: '',
      minOrderAmount: '',
      averageDeliveryTime: '',
      businessHours: '',
      status: 1,
      autoAccept: 0
    },
    uploading: false,
    saving: false
  },

  onShow() {
    const user = wx.getStorageSync('user')
    if (!user) {
      this.setData({ loading: false, bound: false, shop: null })
      return
    }
    this.load()
  },

  load() {
    get('/merchant/shop')
      .then((shop) => {
        if (!shop) {
          this.setData({ shop: null, bound: false, loading: false })
          return
        }
        this.setData({
          shop,
          bound: true,
          loading: false,
          form: {
            name: shop.name,
            logo: shop.logo || '',
            phone: shop.phone || '',
            deliveryFee: shop.deliveryFee,
            minOrderAmount: shop.minOrderAmount,
            averageDeliveryTime: shop.averageDeliveryTime,
            businessHours: shop.businessHours || '',
            status: shop.status,
            autoAccept: shop.autoAccept || 0
          }
        })
        if (shop.mallId) {
          this.loadMall(shop.mallId)
        }
      })
      .catch(() => this.setData({ loading: false }))
  },

  loadMall(mallId) {
    get(`/mall/${mallId}`)
      .then((mall) => this.setData({ mall: mall || null }))
      .catch(() => {})
  },

  navToMall() {
    const mall = this.data.mall
    if (!mall || mall.latitude == null || mall.longitude == null) {
      wx.showToast({ title: '门店暂未维护坐标', icon: 'none' })
      return
    }
    wx.openLocation({
      latitude: Number(mall.latitude),
      longitude: Number(mall.longitude),
      name: mall.name,
      address: mall.address || '',
      scale: 18
    })
  },

  goApply() {
    wx.navigateTo({ url: '/pages/apply/apply' })
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value })
  },

  onStatusChange(e) {
    this.setData({ 'form.status': e.detail.value ? 1 : 0 })
  },

  onAutoAcceptChange(e) {
    this.setData({ 'form.autoAccept': e.detail.value ? 1 : 0 })
  },

  chooseLogo() {
    if (this.data.uploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const token = app.globalData.token || wx.getStorageSync('token')
        this.setData({ uploading: true })
        wx.uploadFile({
          url: app.globalData.baseUrl + '/file/upload',
          filePath: res.tempFiles[0].tempFilePath,
          name: 'file',
          header: { Authorization: 'Bearer ' + token },
          success: (upload) => {
            try {
              const body = JSON.parse(upload.data)
              if (body.code === 0) {
                this.setData({ 'form.logo': app.globalData.baseUrl + body.data })
              } else {
                wx.showToast({ title: body.msg || '上传失败', icon: 'none' })
              }
            } catch (err) {
              wx.showToast({ title: '上传失败', icon: 'none' })
            }
          },
          fail: () => wx.showToast({ title: '上传失败', icon: 'none' }),
          complete: () => this.setData({ uploading: false })
        })
      }
    })
  },

  save() {
    if (this.data.saving) return
    const form = this.data.form
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入档口名称', icon: 'none' })
      return
    }
    const payload = {
      name: form.name.trim(),
      logo: form.logo,
      phone: form.phone,
      deliveryFee: Number(form.deliveryFee) || 0,
      minOrderAmount: Number(form.minOrderAmount) || 0,
      averageDeliveryTime: Number(form.averageDeliveryTime) || 30,
      businessHours: form.businessHours,
      status: form.status,
      autoAccept: form.autoAccept
    }
    this.setData({ saving: true })
    put('/merchant/shop', payload)
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'success' })
        this.setData({ saving: false })
        this.load()
      })
      .catch(() => this.setData({ saving: false }))
  }
})
