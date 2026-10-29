<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">店铺券</h3>
      <el-button type="primary" :icon="Plus" @click="openCreate">新建店铺券</el-button>
    </div>
    <div class="tip">店铺券仅限本档口使用，成本由商家承担</div>

    <div class="table-wrap">
      <el-table :data="pagedRows" v-loading="loading" stripe row-key="id" empty-text="还没有店铺券">
        <el-table-column prop="name" label="券名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 2 ? 'warning' : 'primary'" effect="plain">
              {{ row.type === 2 ? '折扣券' : '满减券' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优惠内容" min-width="180">
          <template #default="{ row }">
            <span class="coupon-amount">{{ amountText(row) }}</span>
            <span class="sub-text">{{ thresholdText(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="160">
          <template #default="{ row }">
            剩余 {{ row.remainCount ?? 0 }} / {{ row.totalCount ?? 0 }} · 已领 {{ row.receivedCount ?? 0 }}
          </template>
        </el-table-column>
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }">
            {{ dateText(row.startTime) }} 至 {{ dateText(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" active-color="#ff5722"
                       active-text="启用" inactive-text="停用"
                       @change="(value) => toggleStatus(row, value)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="rows.length"
                   layout="total, prev, pager, next" />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑店铺券' : '新建店铺券'" width="560px"
               :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：满30减5" />
        </el-form-item>
        <el-form-item label="券类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" :step="1" style="width: 180px" />
          <span class="form-tip">0 表示无门槛</span>
        </el-form-item>
        <el-form-item :label="form.type === 2 ? '折扣率' : '减免金额'" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :max="form.type === 2 ? 0.99 : 99999"
                           :precision="2" :step="form.type === 2 ? 0.05 : 1" style="width: 180px" />
          <span class="form-tip">{{ form.type === 2 ? '如 0.85 表示 8.5 折' : '单位：元' }}</span>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :step="10" style="width: 180px" />
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          format="YYYY-MM-DD HH:mm:ss" placeholder="选择生效时间" style="width: 220px" />
        </el-form-item>
        <el-form-item label="失效时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          format="YYYY-MM-DD HH:mm:ss" placeholder="选择失效时间" style="width: 220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-color="#ff5722" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createCoupon, deleteCoupon, listCoupons, updateCoupon, updateCouponStatus } from '../../api/merchant'
import { money } from '../../utils/format'

const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = 10

const formVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = reactive({
  id: null,
  name: '',
  type: 1,
  threshold: 0,
  amount: 1,
  totalCount: 100,
  startTime: '',
  endTime: '',
  status: 1
})

const validateTime = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请选择时间'))
    return
  }
  if (form.startTime && form.endTime && form.endTime <= form.startTime) {
    callback(new Error('失效时间需晚于生效时间'))
    return
  }
  callback()
}

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入优惠额度', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
  startTime: [{ required: true, validator: validateTime, trigger: 'change' }],
  endTime: [{ required: true, validator: validateTime, trigger: 'change' }]
}

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return rows.value.slice(start, start + pageSize)
})

const amountText = (row) => {
  if (row.type === 2) {
    return `${(Number(row.amount || 0) * 10).toFixed(1).replace(/\.0$/, '')}折`
  }
  return `减 ¥${money(row.amount)}`
}

const thresholdText = (row) => {
  return Number(row.threshold || 0) > 0 ? `满 ${money(row.threshold)} 元可用` : '无门槛'
}

const dateText = (value) => (value ? String(value).replace('T', ' ').slice(0, 10) : '-')

const toDateTime = (value) => {
  if (!value) {
    return ''
  }
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = (num) => String(num).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  return String(value).replace(' ', 'T').slice(0, 19)
}

const load = async () => {
  loading.value = true
  try {
    rows.value = (await listCoupons()) || []
    if ((page.value - 1) * pageSize >= rows.value.length) {
      page.value = 1
    }
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

const today = () => {
  const now = new Date()
  const pad = (num) => String(num).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

const openCreate = () => {
  Object.assign(form, {
    id: null,
    name: '',
    type: 1,
    threshold: 0,
    amount: 1,
    totalCount: 100,
    startTime: `${today()}T00:00:00`,
    endTime: `${today()}T23:59:59`,
    status: 1
  })
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    threshold: Number(row.threshold) || 0,
    amount: Number(row.amount) || 0,
    totalCount: row.totalCount || 1,
    startTime: toDateTime(row.startTime),
    endTime: toDateTime(row.endTime),
    status: row.status ?? 1
  })
  formVisible.value = true
}

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (form.type === 2 && Number(form.amount) >= 1) {
    ElMessage.warning('折扣率需小于 1，如 0.85')
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      type: form.type,
      threshold: form.threshold,
      amount: form.amount,
      totalCount: form.totalCount,
      startTime: form.startTime,
      endTime: form.endTime,
      status: form.status
    }
    if (form.id) {
      await updateCoupon(form.id, payload)
    } else {
      await createCoupon(payload)
    }
    ElMessage.success('已保存')
    formVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, value) => {
  const next = value ? 1 : 0
  try {
    await updateCouponStatus(row.id, next)
    row.status = next
    ElMessage.success(next === 1 ? '已启用' : '已停用')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm('删除后用户将无法领取，已领取的券不受影响，确定删除吗？', '删除店铺券', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  try {
    await deleteCoupon(row.id)
    ElMessage.success('已删除')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

onMounted(load)
</script>

<style scoped>
.tip {
  padding: 10px 12px;
  margin-bottom: 16px;
  border-radius: 6px;
  background: #fff8f5;
  color: #b26a4c;
  font-size: 13px;
}

.coupon-amount {
  color: #ff5722;
  font-weight: 600;
  margin-right: 10px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
