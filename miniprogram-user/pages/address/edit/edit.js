const { get, post, put } = require('../../../utils/request')
const locationUtil = require('../../../utils/location')

Page({
  data: {
    id: null,
    form: {
      contact: '',
      company: '',
      building: '',
      floor: '',
      office: '',
      address: '',
      latitude: null,
      longitude: null,
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
              address: address.address || '',
              latitude: address.latitude || null,
              longitude: address.longitude || null,
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

  /** 地图选点：优先以已选坐标为中心，未选时以当前定位为中心 */
  chooseMap() {
    const { latitude, longitude } = this.data.form
    const center = (latitude && longitude)
      ? Promise.resolve({ latitude, longitude })
      : locationUtil.getLocation().catch(() => null)
    center
      .then((loc) => locationUtil.chooseOnMap(loc || {}))
      .then((res) => {
        if (!res || (!res.name && !res.address)) return
        const patch = {
          'form.address': res.address || res.name,
          'form.latitude': res.latitude,
          'form.longitude': res.longitude
        }
        // 公司为空时用选点名称自动补全，减少手动输入
        if (!this.data.form.company) {
          patch['form.company'] = res.name || ''
        }
        this.setData(patch)
      })
  },

  /** 查看已选位置 */
  previewMap() {
    const { latitude, longitude, address, company } = this.data.form
    if (!latitude || !longitude) {
      wx.showToast({ title: '请先地图选点', icon: 'none' })
      return
    }
    locationUtil.openMap(latitude, longitude, company || '收货地址', address)
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
