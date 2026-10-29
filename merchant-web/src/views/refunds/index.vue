<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">退款申请</h3>
      <span class="sub-text">同意后金额原路退回（模拟），拒绝需填写理由</span>
    </div>

    <el-tabs v-model="currentStatus" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div class="table-wrap">
      <el-table :data="pagedRows" v-loading="loading" stripe row-key="id" empty-text="暂无退款申请">
        <el-table-column prop="refundNo" label="退款单号" width="190" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="goOrder(row.orderId)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="退款金额" width="110">
          <template #default="{ row }"><span class="amount">¥{{ money(row.amount) }}</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="退款原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="rejectReason" label="拒绝原因" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.rejectReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="处理时间" width="170">
          <template #default="{ row }">{{ formatTime(row.handleTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button link type="success" :loading="agreeingId === row.id" @click="agree(row)">同意退款</el-button>
              <el-button link type="danger" @click="openReject(row)">拒绝</el-button>
            </template>
            <span v-else class="sub-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="rows.length"
                   layout="total, prev, pager, next" />

    <el-dialog v-model="rejectVisible" title="拒绝退款" width="440px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="200" show-word-limit
                placeholder="请填写拒绝理由，将同步给顾客" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { agreeRefund, listRefunds, rejectRefund } from '../../api/merchant'
import { formatTime, money } from '../../utils/format'

const router = useRouter()

const STATUS_MAP = {
  0: { text: '申请中', type: 'warning' },
  1: { text: '已同意', type: 'primary' },
  2: { text: '已拒绝', type: 'danger' },
  3: { text: '已退款', type: 'success' }
}

const tabs = [
  { label: '全部', value: 'all' },
  { label: '申请中', value: '0' },
  { label: '已同意', value: '1' },
  { label: '已拒绝', value: '2' },
  { label: '已退款', value: '3' }
]

const currentStatus = ref('all')
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = 10
const agreeingId = ref(null)

const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return rows.value.slice(start, start + pageSize)
})

const statusText = (status) => STATUS_MAP[status]?.text || '未知'
const statusType = (status) => STATUS_MAP[status]?.type || 'info'

const load = async () => {
  loading.value = true
  try {
    const status = currentStatus.value === 'all' ? '' : Number(currentStatus.value)
    const list = await listRefunds(status)
    rows.value = list || []
    if ((page.value - 1) * pageSize >= rows.value.length) {
      page.value = 1
    }
  } catch (e) {
    rows.value = []
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  page.value = 1
  load()
}

const agree = async (row) => {
  try {
    await ElMessageBox.confirm('同意后订单将标记为已退款，金额原路退回（模拟），确认同意？', '同意退款', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  agreeingId.value = row.id
  try {
    await agreeRefund(row.id)
    ElMessage.success('已同意退款')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    agreeingId.value = null
  }
}

const openReject = (row) => {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('拒绝理由不能为空')
    return
  }
  rejecting.value = true
  try {
    await rejectRefund(rejectTarget.value.id, reason)
    ElMessage.success('已拒绝退款')
    rejectVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    rejecting.value = false
  }
}

const goOrder = (id) => {
  router.push(`/orders/${id}`)
}

onMounted(load)
</script>

<style scoped>
.amount {
  color: #ff5722;
  font-weight: 600;
}
</style>
