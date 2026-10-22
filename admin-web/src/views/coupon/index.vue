<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">平台券管理</h3>
      <el-button type="primary" @click="openCreate">新建平台券</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="name" label="券名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'warning' : 'success'" effect="plain">
            {{ row.type === 1 ? '满减' : '折扣' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="使用条件" min-width="150">
        <template #default="{ row }">
          <span v-if="row.type === 1">满￥{{ money(row.threshold) }} 减￥{{ money(row.amount) }}</span>
          <span v-else>满￥{{ money(row.threshold) }} 享 {{ (Number(row.amount) * 10).toFixed(1) }} 折</span>
        </template>
      </el-table-column>
      <el-table-column label="发行 / 剩余 / 已领" width="170">
        <template #default="{ row }">
          {{ row.totalCount }} / {{ row.remainCount }} / {{ row.receivedCount }}
        </template>
      </el-table-column>
      <el-table-column label="有效期" width="200">
        <template #default="{ row }">
          <div class="sub-text">{{ formatTime(row.startTime) }}</div>
          <div class="sub-text">至 {{ formatTime(row.endTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(val) => toggleStatus(row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑平台券' : '新建平台券'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="券名称" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如：平台新人立减券" />
        </el-form-item>
        <el-form-item label="券类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">满减券</el-radio>
            <el-radio :value="2">折扣券</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="使用门槛" prop="threshold">
          <el-input-number v-model="form.threshold" :min="0" :precision="2" :step="1" />
          <span class="unit">元</span>
        </el-form-item>
        <el-form-item :label="form.type === 1 ? '减免金额' : '折扣率'" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :max="form.type === 2 ? 0.99 : 9999"
                           :precision="2" :step="form.type === 2 ? 0.05 : 1" />
          <span class="unit">{{ form.type === 1 ? '元' : '（0.85 表示 8.5 折）' }}</span>
        </el-form-item>
        <el-form-item label="发放总量" prop="totalCount">
          <el-input-number v-model="form.totalCount" :min="1" :step="10" />
          <span class="unit">张</span>
        </el-form-item>
        <el-form-item label="生效时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCoupons, createCoupon, updateCoupon, updateCouponStatus, deleteCoupon
} from '../../api/coupon'
import { money } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const dialogVisible = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  id: null,
  name: '',
  type: 1,
  threshold: 0,
  amount: 5,
  totalCount: 100,
  startTime: '',
  endTime: ''
})

const form = reactive(defaultForm())

const rules = {
  name: [{ required: true, message: '请输入券名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择券类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入优惠额度', trigger: 'blur' }],
  totalCount: [{ required: true, message: '请输入发放总量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择生效时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择失效时间', trigger: 'change' }]
}

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const data = await listCoupons()
    rows.value = data || []
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    type: row.type,
    threshold: Number(row.threshold || 0),
    amount: Number(row.amount),
    totalCount: row.totalCount,
    startTime: row.startTime,
    endTime: row.endTime
  })
  dialogVisible.value = true
}

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.name,
      type: form.type,
      threshold: form.threshold,
      amount: form.amount,
      totalCount: form.totalCount,
      startTime: form.startTime,
      endTime: form.endTime
    }
    if (form.id) {
      await updateCoupon(form.id, payload)
    } else {
      await createCoupon(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, checked) => {
  await updateCouponStatus(row.id, checked ? 1 : 0)
  ElMessage.success(checked ? '已启用' : '已停用')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除平台券「${row.name}」？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteCoupon(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: #909399;
  font-size: 12px;
}

.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
