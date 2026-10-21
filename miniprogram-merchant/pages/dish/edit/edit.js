const app = getApp()
const { get, post, put } = require('../../../utils/request')

Page({
  data: {
    id: null,
    categories: [],
    categoryNames: [],
    categoryIndex: 0,
    form: {
      categoryId: null,
      name: '',
      image: '',
      price: '',
      originalPrice: '',
      description: '',
      status: 1,
      sort: 0
    },
    uploading: false,
    saving: false
  },

  onLoad(options) {
    this.setData({ id: options.id ? Number(options.id) : null })
    wx.setNavigationBarTitle({ title: options.id ? '编辑菜品' : '新增菜品' })
    this.loadCategories(options.id ? Number(options.id) : null)
  },

  loadCategories(dishId) {
    get('/merchant/dishes')
      .then((res) => {
        const categories = (res && res.categories) || []
        const dishes = (res && res.dishes) || []
        const data = {
          categories,
          categoryNames: categories.map((c) => c.name)
        }
        if (dishId) {
          const dish = dishes.find((d) => d.id === dishId)
          if (dish) {
            const index = categories.findIndex((c) => c.id === dish.categoryId)
            data.categoryIndex = index < 0 ? 0 : index
            data.form = {
              categoryId: dish.categoryId,
              name: dish.name,
              image: dish.image || '',
              price: dish.price,
              originalPrice: dish.originalPrice || '',
              description: dish.description || '',
              status: dish.status,
              sort: dish.sort || 0
            }
          }
        } else if (categories.length) {
          data['form.categoryId'] = categories[0].id
        }
        this.setData(data)
      })
      .catch(() => {})
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value)
    this.setData({
      categoryIndex: index,
      'form.categoryId': this.data.categories[index].id
    })
  },

  onStatusChange(e) {
    this.setData({ 'form.status': e.detail.value ? 1 : 0 })
  },

  chooseImage() {
    if (this.data.uploading) return
    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      success: (res) => {
        const filePath = res.tempFiles[0].tempFilePath
        const token = app.globalData.token || wx.getStorageSync('token')
        this.setData({ uploading: true })
        wx.uploadFile({
          url: app.globalData.baseUrl + '/file/upload',
          filePath,
          name: 'file',
          header: { Authorization: 'Bearer ' + token },
          success: (upload) => {
            try {
              const body = JSON.parse(upload.data)
              if (body.code === 0) {
                // 上传文件挂在 context-path 下（/api/uploads/**），直接拼 baseUrl
                this.setData({ 'form.image': app.globalData.baseUrl + body.data })
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
    if (!form.categoryId) {
      wx.showToast({ title: '请选择菜品分类', icon: 'none' })
      return
    }
    if (!form.name.trim()) {
      wx.showToast({ title: '请输入菜品名称', icon: 'none' })
      return
    }
    const price = Number(form.price)
    if (!price || price <= 0) {
      wx.showToast({ title: '请输入正确的售价', icon: 'none' })
      return
    }
    const payload = {
      categoryId: form.categoryId,
      name: form.name.trim(),
      image: form.image,
      price,
      originalPrice: form.originalPrice ? Number(form.originalPrice) : null,
      description: form.description,
      status: form.status,
      sort: Number(form.sort) || 0
    }
    this.setData({ saving: true })
    const request = this.data.id
      ? put(`/merchant/dish/${this.data.id}`, payload)
      : post('/merchant/dish', payload)
    request
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 600)
      })
      .catch(() => this.setData({ saving: false }))
  }
})
