const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    loading: true,
    shop: null,
    apply: null,
    malls: [],
    mallIndex: -1,
    mallName: '',
    form: {
      shopName: '',
      mallId: null,
      contact: '',
      phone: '',
      businessHours: '',
      licenseImg: ''
    },
    licensePreview: '',
    uploading: false,
    submitting: false
  },

  onShow() {
    const user = wx.getStorageSync('user')
    if (!user) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      this.setData({ loading: false })
      return
    }
    this.load()
  },

  load() {
    this.setData({ loading: true })
    Promise.all([
      get('/merchant/shop').catch(() => null),
      get('/merchant/apply').catch(() => null),
      get('/mall/list', {}, false).catch(() => [])
    ])
      .then(([shop, apply, malls]) => {
        const data = {
          shop: shop || null,
          apply: apply || null,
          malls: malls || [],
          loading: false
        }
        if (apply && apply.status === 2) {
          data.form = {
            shopName: apply.shopName || '',
            mallId: apply.mallId,
            contact: apply.contact || '',
            phone: apply.phone || '',
            businessHours: apply.businessHours || '',
            licenseImg: ''
          }
          const index = (malls || []).findIndex((m) => m.id === apply.mallId)
          data.mallIndex = index
          data.mallName = index >= 0 ? malls[index].name : (apply.mallName || '')
        }
        this.setData(data)
      })
      .catch(() => this.setData({ loading: false }))
  },

  onInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value })
  },

  chooseMall() {
    const malls = this.data.malls
    if (!malls.length) {
      wx.showToast({ title: '暂无可选美食城', icon: 'none' })
      return
    }
    wx.showActionSheet({
      itemList: malls.map((m) => m.name),
      success: (res) => {
        const mall = malls[res.tapIndex]
        this.setData({
          mallIndex: res.tapIndex,
          mallName: mall.name,
          'form.mallId': mall.id
        })
      }
    })
  },

  chooseLicense() {
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
                this.setData({
                  'form.licenseImg': body.data,
                  licensePreview: app.globalData.baseUrl + body.data
                })
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

  submit() {
    if (this.data.submitting) return
    const form = this.data.form
    if (!form.shopName.trim()) {
      wx.showToast({ title: '请输入档口名称', icon: 'none' })
      return
    }
    if (!form.mallId) {
      wx.showToast({ title: '请选择所属美食城', icon: 'none' })
      return
    }
    if (!form.contact.trim()) {
      wx.showToast({ title: '请输入联系人', icon: 'none' })
      return
    }
    if (!/^1\d{10}$/.test(form.phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    post('/merchant/apply', {
      shopName: form.shopName.trim(),
      mallId: form.mallId,
      contact: form.contact.trim(),
      phone: form.phone,
      businessHours: form.businessHours,
      licenseImg: form.licenseImg
    })
      .then(() => {
        wx.showToast({ title: '提交成功', icon: 'success' })
        this.setData({ submitting: false })
        this.load()
      })
      .catch(() => this.setData({ submitting: false }))
  },

  goShop() {
    wx.navigateTo({ url: '/pages/shop/shop' })
  }
})
