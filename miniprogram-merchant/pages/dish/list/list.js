const { get, post, put, del } = require('../../../utils/request')

Page({
  data: {
    bound: true,
    loading: true,
    categories: [],
    dishes: [],
    grouped: [],
    manage: false
  },

  onShow() {
    const user = wx.getStorageSync('user')
    if (!user) {
      this.setData({ bound: false, loading: false })
      return
    }
    get('/merchant/shop')
      .then((shop) => {
        if (!shop) {
          this.setData({ bound: false, loading: false })
          return
        }
        this.setData({ bound: true })
        this.loadDishes()
      })
      .catch(() => this.setData({ bound: false, loading: false }))
  },

  loadDishes() {
    get('/merchant/dishes')
      .then((res) => {
        const categories = (res && res.categories) || []
        const dishes = (res && res.dishes) || []
        const grouped = categories.map((c) => ({
          ...c,
          dishes: dishes.filter((d) => d.categoryId === c.id)
        }))
        this.setData({ categories, dishes, grouped, loading: false })
      })
      .catch(() => this.setData({ loading: false }))
  },

  toggleManage() {
    this.setData({ manage: !this.data.manage })
  },

  toggleStatus(e) {
    const { id, status } = e.currentTarget.dataset
    const next = status === 1 ? 0 : 1
    put(`/merchant/dish/${id}/status?status=${next}`)
      .then(() => {
        wx.showToast({ title: next === 1 ? '已上架' : '已下架', icon: 'none' })
        this.loadDishes()
      })
      .catch(() => {})
  },

  addDish() {
    if (!this.data.categories.length) {
      wx.showToast({ title: '请先添加菜品分类', icon: 'none' })
      return
    }
    wx.navigateTo({ url: '/pages/dish/edit/edit' })
  },

  editDish(e) {
    wx.navigateTo({ url: `/pages/dish/edit/edit?id=${e.currentTarget.dataset.id}` })
  },

  deleteDish(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: '删除菜品',
      content: `确定删除「${name}」吗？`,
      success: (res) => {
        if (!res.confirm) return
        del(`/merchant/dish/${id}`)
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadDishes()
          })
          .catch(() => {})
      }
    })
  },

  addCategory() {
    wx.showModal({
      title: '新增分类',
      editable: true,
      placeholderText: '请输入分类名称',
      success: (res) => {
        if (!res.confirm) return
        const name = (res.content || '').trim()
        if (!name) {
          wx.showToast({ title: '分类名称不能为空', icon: 'none' })
          return
        }
        post('/merchant/dish-category', { name, sort: this.data.categories.length })
          .then(() => {
            wx.showToast({ title: '新增成功', icon: 'success' })
            this.loadDishes()
          })
          .catch(() => {})
      }
    })
  },

  renameCategory(e) {
    const { id, name, sort } = e.currentTarget.dataset
    wx.showModal({
      title: '重命名分类',
      editable: true,
      placeholderText: '请输入分类名称',
      content: name,
      success: (res) => {
        if (!res.confirm) return
        const newName = (res.content || '').trim()
        if (!newName) return
        put(`/merchant/dish-category/${id}`, { name: newName, sort })
          .then(() => {
            wx.showToast({ title: '已保存', icon: 'success' })
            this.loadDishes()
          })
          .catch(() => {})
      }
    })
  },

  deleteCategory(e) {
    const { id, name } = e.currentTarget.dataset
    wx.showModal({
      title: '删除分类',
      content: `确定删除「${name}」？分类下有菜品时无法删除`,
      success: (res) => {
        if (!res.confirm) return
        del(`/merchant/dish-category/${id}`)
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' })
            this.loadDishes()
          })
          .catch(() => {})
      }
    })
  }
})
