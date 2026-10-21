const { get, post, put } = require('../../../utils/request')

Page({
  data: {
    id: null,
    form: {
      contact: '',
      company: '',
      building: '',
      floor: '',
      office: '',
      phone: '',
      isDefault: false
    },
    saving: false
  },

  onLoad(options) {
    if (options.id) {
      this.setData({ id: Number(options.id) })
      wx.setNavigationBarTitle({ title: '编辑地址' })
      this.loadDetail(Number(options.id))
    } else {
      wx.setNavigationBarTitle({ title: '新增地址' })
    }
  },

  loadDetail(id) {
    get('/user/address')
      .then((list) => {
        const address = (list || []).find((a) => a.id === id)
        if (address) {
          this.setData({
            form: {
              contact: address.contact || '',
              company: address.company || '',
              building: address.building || '',
              floor: address.floor || '',
              office: address.office || '',
              phone: address.phone || '',
              isDefault: address.isDefault === 1
            }
          })
        }
      })
      .catch(() => {})
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [`form.${field}`]: e.detail.value })
  },

  onDefaultChange(e) {
    this.setData({ 'form.isDefault': e.detail.value })
  },

  save() {
    if (this.data.saving) return
    const { contact, company, building, phone } = this.data.form
    if (!contact || !company || !building || !phone) {
      wx.showToast({ title: '请填写联系人、公司、楼栋和电话', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    const request = this.data.id
      ? put(`/user/address/${this.data.id}`, this.data.form)
      : post('/user/address', this.data.form)
    request
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'success' })
        setTimeout(() => wx.navigateBack(), 600)
      })
      .catch(() => this.setData({ saving: false }))
  }
})
