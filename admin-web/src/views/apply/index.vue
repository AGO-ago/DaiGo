<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">入驻审核</h3>
    </div>

    <el-tabs v-model="activeStatus" @tab-change="handleTabChange">
      <el-tab-pane label="待审核" :name="0" />
      <el-tab-pane label="已通过" :name="1" />
      <el-tab-pane label="已驳回" :name="2" />
      <el-tab-pane label="全部" name="all" />
    </el-tabs>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="shopName" label="申请档口" min-width="140" show-overflow-tooltip />
      <el-table-column prop="userName" label="申请人" width="120" show-overflow-tooltip />
      <el-table-column prop="mallName" label="美食城" width="140" show-overflow-tooltip />
      <el-table-column label="联系人" width="160">
        <template #default="{ row }">
          <div>{{ row.contact }}</div>
          <div class="sub-text">{{ row.phone }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="businessHours" label="营业时间" width="130" />
      <el-table-column prop="createTime" label="提交时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <template v-if="row.status === 0">
            <el-button link type="success" @click="openApprove(row)">通过</el-button>
            <el-button link type="danger" @click="reject(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-drawer v-model="detailVisible" title="入驻申请详情" size="420px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="申请档口">{{ current.shopName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ current.userName }}（ID {{ current.userId }}）</el-descriptions-item>
        <el-descriptions-item label="所属美食城">{{ current.mallName }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ current.contact }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ current.phone }}</el-descriptions-item>
        <el-descriptions-item label="营业时间">{{ current.businessHours || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(current.status)">{{ statusText(current.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="current.rejectReason" label="驳回原因">
          {{ current.rejectReason }}
        </el-descriptions-item>
        <el-descriptions-item label="营业执照">
          <el-image v-if="current.licenseImg" :src="imgUrl(current.licenseImg)" fit="contain"
                    :preview-src-list="[imgUrl(current.licenseImg)]" class="license" />
          <span v-else class="sub-text">未上传</span>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.auditTime" label="审核时间">
          {{ formatTime(current.auditTime) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-dialog v-model="approveVisible" title="审核通过" width="560px">
      <el-form label-width="110px">
        <el-form-item label="绑定方式">
          <el-radio-group v-model="approveForm.mode">
            <el-radio value="new">按申请信息新建档口</el-radio>
            <el-radio value="existing">绑定已有空档口</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="approveForm.mode === 'existing'" label="选择档口">
          <el-select v-model="approveForm.shopId" filterable placeholder="请选择未绑定的档口" style="width: 100%">
            <el-option v-for="shop in bindableShops" :key="shop.id"
                       :label="`${shop.name}（${shop.mallName || '未分组'}）`" :value="shop.id" />
          </el-select>
          <div v-if="!bindableShops.length" class="sub-text">暂无未绑定的档口，请选择新建</div>
        </el-form-item>
        <template v-else>
          <el-form-item label="配送费">
            <el-input-number v-model="approveForm.deliveryFee" :min="0" :precision="2" :step="1" />
            <span class="unit">元</span>
          </el-form-item>
          <el-form-item label="起送价">
            <el-input-number v-model="approveForm.minOrderAmount" :min="0" :precision="2" :step="1" />
            <span class="unit">元</span>
          </el-form-item>
          <el-form-item label="预计送达">
            <el-input-number v-model="approveForm.averageDeliveryTime" :min="5" :max="180" :step="5" />
            <span class="unit">分钟</span>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="approveVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="approve">确认通过</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApplies, approveApply, rejectApply } from '../../api/apply'
import { listShops } from '../../api/shop'
import { imgUrl } from '../../utils/format'

const activeStatus = ref(0)
const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const current = ref(null)

const approveVisible = ref(false)
const approveRow = ref(null)
const bindableShops = ref([])
const approveForm = reactive({
  mode: 'new',
  shopId: null,
  deliveryFee: 2,
  minOrderAmount: 15,
  averageDeliveryTime: 30
})

const statusText = (status) => ({ 0: '待审核', 1: '已通过', 2: '已驳回' }[status] || '未知')
const statusType = (status) => ({ 0: 'warning', 1: 'success', 2: 'danger' }[status] || 'info')

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const data = await listApplies({
      status: activeStatus.value === 'all' ? undefined : activeStatus.value,
      page: page.value,
      size: size.value
    })
    rows.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  load()
}

const openDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const openApprove = async (row) => {
  approveRow.value = row
  Object.assign(approveForm, {
    mode: 'new',
    shopId: null,
    deliveryFee: 2,
    minOrderAmount: 15,
    averageDeliveryTime: 30
  })
  approveVisible.value = true
  const data = await listShops({ page: 1, size: 100, status: 1 })
  bindableShops.value = (data.list || []).filter((shop) => !shop.merchantUserId)
}

const approve = async () => {
  if (approveForm.mode === 'existing' && !approveForm.shopId) {
    ElMessage.warning('请选择要绑定的档口')
    return
  }
  saving.value = true
  try {
    const payload = approveForm.mode === 'existing'
      ? { shopId: approveForm.shopId }
      : {
          deliveryFee: approveForm.deliveryFee,
          minOrderAmount: approveForm.minOrderAmount,
          averageDeliveryTime: approveForm.averageDeliveryTime
        }
    await approveApply(approveRow.value.id, payload)
    ElMessage.success('已通过审核并绑定档口')
    approveVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const reject = async (row) => {
  const result = await ElMessageBox.prompt('请输入驳回原因（商家端可见）', '驳回申请', {
    inputPlaceholder: '如：营业执照信息不清晰，请重新上传',
    inputValidator: (value) => (value && value.trim() ? true : '驳回原因不能为空'),
    type: 'warning'
  }).catch(() => null)
  if (!result) {
    return
  }
  await rejectApply(row.id, result.value.trim())
  ElMessage.success('已驳回')
  load()
}

onMounted(load)
</script>

<style scoped>
.sub-text {
  color: #909399;
  font-size: 12px;
}

.license {
  width: 180px;
  height: 120px;
  border-radius: 6px;
}

.unit {
  margin-left: 8px;
  color: #909399;
}
</style>
