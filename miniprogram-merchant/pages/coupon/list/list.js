const { get, post, put, del } = require('../../../utils/request')

const pad = (n) => String(n).padStart(2, '0')
const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
const dateOf = (s) => (s || '').slice(0, 10)

Page({
  data: {
    list: [],
    loading: true,
    formVisible: false,
    editingId: null,
    form: {
      name: '',
      typeIndex: 0,
      threshold: '',
      amount: '',
      totalCount: '100',
      startDate: '',
      endDate: '',
      status: 1
    }
  },

  onShow() {
    this.load()
  },

  load() {
    get('/merchant/coupons')
      .then((list) => {
        const rows = (list || []).map((c) => ({
          ...c,
          amountText: c.type === 2 ? `${(Number(c.amount) * 10).toFixed(1).replace(/\.0$/, '')}折` : `减 ¥${Number(c.amount)}`,
          thresholdText: Number(c.threshold) > 0 ? `满 ${Number(c.threshold)} 元可用` : '无门槛',
          dateText: `${dateOf(c.startTime)} 至 ${dateOf(c.endTime)}`,
          statusText: c.status === 1 ? '进行中' : '已停用'
        }))
        this.setData({ list: rows, loading: false })
      })
      .catch(() => this.setData({ loading: false }))
  },

  openCreate() {
    const today = todayStr()
    this.setData({
      formVisible: true,
      editingId: null,
      form: {
        name: '',
        typeIndex: 0,
        threshold: '',
        amount: '',
        totalCount: '100',
        startDate: today,
        endDate: today,
        status: 1
      }
    })
  },

  openEdit(e) {
    const id = e.currentTarget.dataset.id
    const c = this.data.list.find((i) => i.id === id)
    if (!c) return
    this.setData({
      formVisible: true,
      editingId: id,
      form: {
        name: c.name,
        typeIndex: c.type === 2 ? 1 : 0,
        threshold: c.threshold,
        amount: c.amount,
        totalCount: String(c.totalCount),
        startDate: dateOf(c.startTime),
        endDate: dateOf(c.endTime),
        status: c.status
      }
    })
  },

  closeForm() {
    this.setData({ formVisible: false })
  },

  noop() {},

  onFormInput(e) {
    this.setData({ [`form.${e.currentTarget.dataset.field}`]: e.detail.value })
  },

  onTypeChange(e) {
    this.setData({ 'form.typeIndex': Number(e.detail.value) })
  },

  onDateChange(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value })
  },

  save() {
    const f = this.data.form
    if (!f.name.trim()) {
      wx.showToast({ title: '请输入券名称', icon: 'none' })
      return
    }
    const amount = Number(f.amount)
    if (!amount || amount <= 0) {
      wx.showToast({ title: f.typeIndex === 1 ? '请输入折扣率，如 0.85' : '请输入减免金额', icon: 'none' })
      return
    }
    if (f.typeIndex === 1 && amount >= 1) {
      wx.showToast({ title: '折扣率需小于 1，如 0.85', icon: 'none' })
      return
    }
    if (!f.startDate || !f.endDate || f.endDate < f.startDate) {
      wx.showToast({ title: '请选择正确的有效期', icon: 'none' })
      return
    }
    const payload = {
      name: f.name.trim(),
      type: f.typeIndex + 1,
      threshold: Number(f.threshold) || 0,
      amount,
      totalCount: Number(f.totalCount) || 1,
      startTime: `${f.startDate}T00:00:00`,
      endTime: `${f.endDate}T23:59:59`,
      status: f.status
    }
    const request = this.data.editingId
      ? put(`/merchant/coupons/${this.data.editingId}`, payload)
      : post('/merchant/coupons', payload)
    request
      .then(() => {
        wx.showToast({ title: '已保存', icon: 'success' })
        this.setData({ formVisible: false })
        this.load()
      })
      .catch(() => {})
  },

  toggle(e) {
    const { id, status } = e.currentTarget.dataset
    put(`/merchant/coupons/${id}/status?status=${status === 1 ? 0 : 1}`)
      .then(() => this.load())
      .catch(() => {})
  },

  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除店铺券',
      content: '删除后用户将无法领取，已领取的券不受影响，确定删除吗？',
      success: (res) => {
        if (!res.confirm) return
        del(`/merchant/coupons/${id}`)
          .then(() => this.load())
          .catch(() => {})
      }
    })
  }
})
