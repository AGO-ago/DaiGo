<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">订单管理</h3>
      <span class="sub-text">接单、备餐、配送与拒单操作</span>
    </div>

    <el-tabs v-model="currentStatus" @tab-change="handleTabChange">
      <el-tab-pane v-for="tab in tabs" :key="tab.value" :label="tab.label" :name="tab.value" />
    </el-tabs>

    <div class="table-wrap">
      <el-table :data="pagedRows" v-loading="loading" stripe row-key="orderId" empty-text="暂无订单">
        <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.orderId)">{{ row.orderNo }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="商品明细" min-width="230">
          <template #default="{ row }">
            <div class="dish-lines">
              <span v-for="(item, index) in row.items || []" :key="index" class="dish-line">
                {{ item.dishName }}<span v-if="item.specName">（{{ item.specName }}）</span> ×{{ item.count }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="addressSnapshot" label="收货地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="remark" label="备注" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="实付" width="100">
          <template #default="{ row }"><span class="amount">¥{{ money(row.payAmount) }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row.orderId)">详情</el-button>
            <el-button v-for="op in rowActions(row)" :key="op.action" link :type="op.type"
                       :loading="actionLoading === `${row.orderId}-${op.action}`" @click="handleAction(row, op)">
              {{ op.label }}
            </el-button>
            <el-button v-if="row.status === 6" link type="danger" @click="router.push('/refunds')">处理退款</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-pagination v-model:current-page="page" :page-size="pageSize" :total="rows.length"
                   layout="total, prev, pager, next" />

    <el-dialog v-model="rejectVisible" title="拒单原因" width="440px">
      <el-input v-model="rejectReason" type="textarea" :rows="4" maxlength="200" show-word-limit
                placeholder="请填写拒单原因，将同步给顾客" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejecting" @click="confirmReject">确认拒单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, rejectOrder, updateOrderStatus } from '../../api/merchant'
import { formatTime, money } from '../../utils/format'

const route = useRoute()
const router = useRouter()

const STATUS_MAP = {
  0: { text: '待支付', type: 'info' },
  1: { text: '待接单', type: 'warning' },
  2: { text: '备餐中', type: 'primary' },
  3: { text: '配送中', type: '' },
  4: { text: '已完成', type: 'success' },
  5: { text: '已取消', type: 'info' },
  6: { text: '退款中', type: 'danger' },
  7: { text: '已退款', type: 'info' }
}

const ACTION_MAP = {
  1: { action: 'accept', label: '接单', type: 'primary' },
  2: { action: 'prepare', label: '开始配送', type: 'primary' },
  3: { action: 'deliver', label: '确认完成', type: 'success' }
}

const tabs = [
  { label: '待接单', value: '1' },
  { label: '备餐中', value: '2' },
  { label: '配送中', value: '3' },
  { label: '已完成', value: '4' },
  { label: '已取消', value: '5' },
  { label: '退款中', value: '6' },
  { label: '已退款', value: '7' },
  { label: '全部', value: 'all' }
]

const currentStatus = ref('1')
const loading = ref(false)
const rows = ref([])
const page = ref(1)
const pageSize = 10
const actionLoading = ref('')

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

const rowActions = (row) => {
  if (row.status === 1) {
    return [
      ACTION_MAP[1],
      { action: 'reject', label: '拒单', type: 'danger' }
    ]
  }
  const next = ACTION_MAP[row.status]
  return next ? [next] : []
}

const load = async () => {
  loading.value = true
  try {
    const status = currentStatus.value === 'all' ? '' : Number(currentStatus.value)
    const list = await listOrders(status)
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

const initStatusFromQuery = () => {
  const query = route.query.status
  if (query === undefined) {
    currentStatus.value = '1'
  } else {
    currentStatus.value = query === '' ? 'all' : String(query)
  }
  page.value = 1
}

const handleTabChange = () => {
  page.value = 1
  load()
}

const handleAction = async (row, op) => {
  if (op.action === 'reject') {
    rejectTarget.value = row
    rejectReason.value = ''
    rejectVisible.value = true
    return
  }
  try {
    await ElMessageBox.confirm(`确认对订单 #${row.orderNo} 执行「${op.label}」？`, '操作确认', { type: 'warning' })
  } catch (e) {
    return
  }
  actionLoading.value = `${row.orderId}-${op.action}`
  try {
    await updateOrderStatus(row.orderId, op.action)
    ElMessage.success('操作成功')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    actionLoading.value = ''
  }
}

const confirmReject = async () => {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('拒单原因不能为空')
    return
  }
  rejecting.value = true
  try {
    await rejectOrder(rejectTarget.value.orderId, reason)
    ElMessage.success('已拒单并退款')
    rejectVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    rejecting.value = false
  }
}

const goDetail = (id) => {
  router.push(`/orders/${id}`)
}

watch(() => route.query.status, () => {
  initStatusFromQuery()
  load()
})

onMounted(() => {
  initStatusFromQuery()
  load()
})
</script>

<style scoped>
.dish-lines {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: 13px;
}

.amount {
  color: #ff5722;
  font-weight: 600;
}
</style>
