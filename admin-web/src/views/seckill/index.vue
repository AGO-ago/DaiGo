<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">秒杀活动</h3>
      <el-button type="primary" @click="openCreate">新建活动</el-button>
    </div>

    <div class="table-toolbar">
      <el-input v-model="keyword" placeholder="菜品 / 档口名称" clearable style="width: 220px"
                @keyup.enter="search" />
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="已上线" :value="1" />
        <el-option label="已下线" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="秒杀商品" min-width="220">
        <template #default="{ row }">
          <div class="dish-cell">
            <el-image v-if="row.dishImage" :src="imgUrl(row.dishImage)" fit="cover" class="dish-img" />
            <div>
              <div>{{ row.dishName }}</div>
              <div class="sub-text">{{ row.shopName }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="140">
        <template #default="{ row }">
          <div class="price">￥{{ money(row.seckillPrice) }}</div>
          <div class="origin">原价 ￥{{ money(row.dishPrice) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="活动时段" width="210">
        <template #default="{ row }">
          <div class="sub-text">{{ formatTime(row.startTime) }}</div>
          <div class="sub-text">至 {{ formatTime(row.endTime) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="库存（已售/总量）" width="150">
        <template #default="{ row }">{{ row.soldCount }} / {{ row.stock }}</template>
      </el-table-column>
      <el-table-column prop="limitPerUser" label="每人限购" width="100" />
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

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑秒杀活动' : '新建秒杀活动'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="秒杀商品" prop="dishId">
          <el-select v-model="form.dishId" filterable remote clearable reserve-keyword
                     :remote-method="searchDish" :loading="dishLoading"
                     placeholder="输入菜品或档口名称搜索" style="width: 100%">
            <el-option v-for="dish in dishOptions" :key="dish.id"
                       :label="`${dish.name}（${dish.shopName}）￥${money(dish.price)}`" :value="dish.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="秒杀价" prop="seckillPrice">
          <el-input-number v-model="form.seckillPrice" :min="0.01" :precision="2" :step="1" />
          <span class="unit">元（需低于菜品原价）</span>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择开始时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
                          placeholder="选择结束时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="活动库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :step="10" />
          <span class="unit">份</span>
        </el-form-item>
        <el-form-item label="每人限购" prop="limitPerUser">
          <el-input-number v-model="form.limitPerUser" :min="1" :step="1" />
          <span class="unit">份</span>
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
  listSeckills, createSeckill, updateSeckill, updateSeckillStatus, deleteSeckill, searchDishes
} from '../../api/seckill'
import { imgUrl, money } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const dishLoading = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const status = ref(null)

const dialogVisible = ref(false)
const formRef = ref(null)
const dishOptions = ref([])

const defaultForm = () => ({
  id: null,
  dishId: null,
  seckillPrice: 9.9,
  startTime: '',
  endTime: '',
  stock: 50,
  limitPerUser: 1
})

const form = reactive(defaultForm())

const rules = {
  dishId: [{ required: true, message: '请选择秒杀商品', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  stock: [{ required: true, message: '请输入活动库存', trigger: 'blur' }],
  limitPerUser: [{ required: true, message: '请输入每人限购', trigger: 'blur' }]
}

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 16) : '')

const load = async () => {
  loading.value = true
  try {
    const data = await listSeckills({
      keyword: keyword.value || undefined,
      status: status.value === null ? undefined : status.value,
      page: page.value,
      size: size.value
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  page.value = 1
  load()
}

const reset = () => {
  keyword.value = ''
  status.value = null
  search()
}

const searchDish = async (query) => {
  if (!query) {
    dishOptions.value = []
    return
  }
  dishLoading.value = true
  try {
    const data = await searchDishes(query)
    dishOptions.value = data.dishes || []
  } finally {
    dishLoading.value = false
  }
}

const openCreate = () => {
  Object.assign(form, defaultForm())
  dishOptions.value = []
  dialogVisible.value = true
}

const openEdit = (row) => {
  dishOptions.value = [{
    id: row.dishId,
    name: row.dishName,
    shopName: row.shopName,
    price: row.dishPrice
  }]
  Object.assign(form, {
    id: row.id,
    dishId: row.dishId,
    seckillPrice: Number(row.seckillPrice),
    startTime: row.startTime,
    endTime: row.endTime,
    stock: row.stock,
    limitPerUser: row.limitPerUser
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
      dishId: form.dishId,
      seckillPrice: form.seckillPrice,
      startTime: form.startTime,
      endTime: form.endTime,
      stock: form.stock,
      limitPerUser: form.limitPerUser
    }
    if (form.id) {
      await updateSeckill(form.id, payload)
    } else {
      await createSeckill(payload)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (row, checked) => {
  await updateSeckillStatus(row.id, checked ? 1 : 0)
  ElMessage.success(checked ? '已上线' : '已下线')
  load()
}

const remove = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除「${row.dishName}」的秒杀活动？`, '删除确认', { type: 'warning' })
  } catch (e) {
    return
  }
  await deleteSeckill(row.id)
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

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-img {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  flex-shrink: 0;
}

.price {
  color: #ff6b35;
  font-weight: 600;
}

.origin {
  color: #909399;
  font-size: 12px;
  text-decoration: line-through;
}

.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
