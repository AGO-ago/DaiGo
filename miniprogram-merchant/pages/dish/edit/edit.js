const app = getApp()
const { get, post, put, del } = require('../../../utils/request')

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
    saving: false,
    // 规格（仅编辑已有菜品时可维护）
    specs: [],
    newSpecName: '',
    newSpecPrice: ''
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
    if (dishId) {
      this.loadSpecs(dishId)
    }
  },

  /** 规格列表（含停用，便于商家恢复） */
  loadSpecs(dishId) {
    get('/merchant/specs', { dishId })
      .then((list) => this.setData({ specs: list || [] }))
      .catch(() => {})
  },

  onSpecInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  addSpec() {
    const name = (this.data.newSpecName || '').trim()
    if (!name) {
      wx.showToast({ title: '请输入规格名', icon: 'none' })
      return
    }
    const price = Number(this.data.newSpecPrice) || 0
    post(`/merchant/specs?dishId=${this.data.id}`, { name, price, status: 1, sort: this.data.specs.length })
      .then(() => {
        this.setData({ newSpecName: '', newSpecPrice: '' })
        this.loadSpecs(this.data.id)
      })
      .catch(() => {})
  },

  /** 启用/停用规格 */
  toggleSpec(e) {
    const { id, status } = e.currentTarget.dataset
    const spec = this.data.specs.find((s) => s.id === id)
    if (!spec) return
    put(`/merchant/specs/${id}?dishId=${this.data.id}`, {
      name: spec.name,
      price: spec.price || 0,
      sort: spec.sort || 0,
      status: status === 1 ? 0 : 1
    })
      .then(() => this.loadSpecs(this.data.id))
      .catch(() => {})
  },

  removeSpec(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除规格',
      content: '删除后用户端将不再展示该规格，确定删除吗？',
      success: (res) => {
        if (!res.confirm) return
        del(`/merchant/specs/${id}`)
          .then(() => this.loadSpecs(this.data.id))
          .catch(() => {})
      }
    })
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
