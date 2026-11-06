<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">系统配置</h3>
      <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <el-form label-width="140px" class="config-form" v-loading="loading">
      <el-divider content-position="left">交易规则</el-divider>
      <el-form-item label="默认抽佣比例">
        <el-input-number v-model="form.commissionPercent" :min="0" :max="30" :precision="2" :step="0.5" />
        <span class="unit">%（档口单独设置的比例优先）</span>
      </el-form-item>
      <el-form-item label="默认配送费">
        <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" :step="1" />
        <span class="unit">元（新档口默认值）</span>
      </el-form-item>
      <el-form-item label="默认起送价">
        <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" :step="1" />
        <span class="unit">元（新档口默认值）</span>
      </el-form-item>
      <el-form-item label="全局自动接单">
        <el-switch v-model="form.autoAccept" />
        <span class="unit">开启后商家端新订单自动接单</span>
      </el-form-item>
      <el-form-item label="待支付超时">
        <el-input-number v-model="form.payTimeoutMinutes" :min="1" :max="120" :step="5" />
        <span class="unit">分钟（超时自动取消订单并退回优惠券）</span>
      </el-form-item>
      <el-form-item label="评价时限">
        <el-input-number v-model="form.commentDaysLimit" :min="1" :max="30" :step="1" />
        <span class="unit">天（订单完成后超过该时限不可评价）</span>
      </el-form-item>

      <el-divider content-position="left">运营展示</el-divider>
      <el-form-item label="首页公告">
        <el-input v-model="form.homeNotice" type="textarea" :rows="3" maxlength="500" show-word-limit
                  placeholder="展示在用户端首页顶部，留空则不展示" style="width: 100%; max-width: 560px" />
      </el-form-item>
      <el-form-item label="客服电话">
        <el-input v-model="form.supportPhone" maxlength="20" placeholder="4000000000" style="width: 240px" />
        <span class="unit">用户端“联系客服”直接拨打</span>
      </el-form-item>
      <el-form-item label="营业时间">
        <el-input v-model="form.businessHours" maxlength="11" placeholder="09:00-22:00" style="width: 240px" />
        <span class="unit">格式 HH:mm-HH:mm</span>
      </el-form-item>

      <el-form-item label="更新时间">
        <span class="sub-text">{{ formatTime(updateTime) || '-' }}</span>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listConfigs, updateConfigs } from '../../api/config'

const loading = ref(false)
const saving = ref(false)
const updateTime = ref('')

const form = reactive({
  commissionPercent: 5,
  deliveryFee: 2,
  minOrderAmount: 15,
  autoAccept: false,
  payTimeoutMinutes: 15,
  commentDaysLimit: 7,
  homeNotice: '',
  supportPhone: '4000000000',
  businessHours: '09:00-22:00'
})

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const data = await listConfigs()
    const map = {}
    let latest = ''
    for (const item of data || []) {
      map[item.configKey] = item.configValue
      if (item.updateTime && item.updateTime > latest) {
        latest = item.updateTime
      }
    }
    form.commissionPercent = map.commission_rate ? Number(map.commission_rate) * 100 : 5
    form.deliveryFee = map.default_delivery_fee ? Number(map.default_delivery_fee) : 2
    form.minOrderAmount = map.default_min_order_amount ? Number(map.default_min_order_amount) : 15
    form.autoAccept = map.auto_accept === '1'
    form.payTimeoutMinutes = map.order_pay_timeout_minutes ? Number(map.order_pay_timeout_minutes) : 15
    form.commentDaysLimit = map.comment_days_limit ? Number(map.comment_days_limit) : 7
    form.homeNotice = map.home_notice || ''
    form.supportPhone = map.support_phone || ''
    form.businessHours = map.business_hours || ''
    updateTime.value = latest
  } finally {
    loading.value = false
  }
}

const save = async () => {
  if (form.commissionPercent < 0 || form.commissionPercent > 30) {
    ElMessage.warning('抽佣比例需在 0% - 30% 之间')
    return
  }
  if (form.businessHours && !/^([01]\d|2[0-3]):[0-5]\d-([01]\d|2[0-3]):[0-5]\d$/.test(form.businessHours)) {
    ElMessage.warning('营业时间格式需为 HH:mm-HH:mm，如 09:00-22:00')
    return
  }
  saving.value = true
  try {
    await updateConfigs({
      commission_rate: (form.commissionPercent / 100).toFixed(4),
      default_delivery_fee: Number(form.deliveryFee).toFixed(2),
      default_min_order_amount: Number(form.minOrderAmount).toFixed(2),
      auto_accept: form.autoAccept ? '1' : '0',
      order_pay_timeout_minutes: String(form.payTimeoutMinutes),
      comment_days_limit: String(form.commentDaysLimit),
      home_notice: form.homeNotice,
      support_phone: form.supportPhone || '',
      business_hours: form.businessHours || ''
    })
    ElMessage.success('配置已保存')
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.config-form {
  max-width: 760px;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.unit {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
