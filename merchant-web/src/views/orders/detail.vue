<template>
  <div class="order-detail" v-loading="loading">
    <div v-if="order" class="page-card detail-head">
      <div>
        <div class="head-status">
          <el-tag :type="statusType(order.status)" size="large">{{ statusText(order.status) }}</el-tag>
          <span class="head-no">订单号 {{ order.orderNo }}</span>
        </div>
        <div class="head-time">下单时间：{{ formatTime(order.createTime) }}</div>
      </div>
      <div class="head-amount">
        <span class="label">顾客实付</span>
        <span class="value">¥{{ money(order.payAmount) }}</span>
      </div>
    </div>

    <template v-if="order">
      <div class="detail-grid">
        <div class="page-card">
          <h3 class="page-title">订单进度</h3>
          <el-timeline class="timeline">
            <el-timeline-item v-for="step in timeline" :key="step.label" :timestamp="step.time"
                              :color="step.active ? '#ff5722' : '#c0c4cc'">
              {{ step.label }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="!timeline.length" description="暂无进度记录" :image-size="70" />
        </div>

        <div class="page-card">
          <h3 class="page-title">收货与备注</h3>
          <div class="info-row">
            <span class="info-label">收货地址</span>
            <span class="info-value">{{ order.addressSnapshot || '-' }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">订单备注</span>
            <span class="info-value">{{ order.remark || '无' }}</span>
          </div>
        </div>
      </div>

      <div class="page-card">
        <h3 class="page-title">商品明细</h3>
        <div class="table-wrap">
          <el-table :data="order.items || []" stripe empty-text="暂无商品">
            <el-table-column label="菜品" min-width="220">
              <template #default="{ row }">
                <div class="dish-cell">
                  <el-image v-if="row.dishImage" :src="imgUrl(row.dishImage)" fit="cover" class="dish-img" />
                  <div>
                    <div>{{ row.dishName }}</div>
                    <div v-if="row.specName" class="sub-text">规格：{{ row.specName }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="110">
              <template #default="{ row }">¥{{ money(row.price) }}</template>
            </el-table-column>
            <el-table-column prop="count" label="数量" width="90" />
            <el-table-column label="小计" width="110">
              <template #default="{ row }">¥{{ money(row.amount) }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="amount-box">
          <div class="amount-row"><span>商品金额</span><span>¥{{ money(order.totalAmount) }}</span></div>
          <div class="amount-row"><span>配送费</span><span>¥{{ money(order.deliveryFee) }}</span></div>
          <div class="amount-row" v-if="Number(order.discountAmount) > 0">
            <span>优惠金额</span><span class="minus">{{ deduction(order.discountAmount) }}</span>
          </div>
          <div class="amount-row total"><span>顾客实付</span><span>¥{{ money(order.payAmount) }}</span></div>
        </div>
      </div>

      <div class="page-card">
        <h3 class="page-title">订单信息</h3>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ statusText(order.status) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="接单时间">{{ formatTime(order.acceptTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始备餐">{{ formatTime(order.prepareTime) }}</el-descriptions-item>
          <el-descriptions-item label="开始配送">{{ formatTime(order.deliveryTime) }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ formatTime(order.finishTime) }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ formatTime(order.cancelTime) }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-footer">
        <el-button @click="router.back()">返回</el-button>
        <el-button v-if="order.status === 1" type="danger" plain @click="openReject">拒单</el-button>
        <el-button v-if="order.status === 6" type="danger" plain @click="router.push('/refunds')">处理退款</el-button>
        <el-button v-if="nextAction" type="primary" :loading="actionLoading" @click="handleAction">
          {{ nextAction.label }}
        </el-button>
      </div>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="订单不存在或已被删除">
        <el-button type="primary" @click="router.push('/orders')">返回订单列表</el-button>
      </el-empty>
    </div>

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
import { getOrderDetail, rejectOrder, updateOrderStatus } from '../../api/merchant'
import { formatTime, deduction, imgUrl, money } from '../../utils/format'

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
  1: { action: 'accept', label: '接单' },
  2: { action: 'prepare', label: '开始配送' },
  3: { action: 'deliver', label: '确认完成' }
}

const loading = ref(false)
const order = ref(null)
const actionLoading = ref(false)

const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectReason = ref('')

const statusText = (status) => STATUS_MAP[status]?.text || '未知'
const statusType = (status) => STATUS_MAP[status]?.type || 'info'
const nextAction = computed(() => ACTION_MAP[order.value?.status] || null)

const timeline = computed(() => {
  if (!order.value) {
    return []
  }
  const data = order.value
  const steps = [
    { label: '顾客下单', time: data.createTime },
    { label: '商家接单', time: data.acceptTime },
    { label: '开始备餐', time: data.prepareTime },
    { label: '开始配送', time: data.deliveryTime },
    { label: '订单完成', time: data.finishTime }
  ]
  if (data.cancelTime) {
    steps.push({ label: data.status === 7 ? '退款完成' : '订单取消', time: data.cancelTime })
  }
  return steps
    .filter((step) => step.time)
    .map((step, index) => ({
      label: step.label,
      time: formatTime(step.time),
      active: index === 0
    }))
})

const load = async () => {
  loading.value = true
  try {
    order.value = await getOrderDetail(route.params.id)
  } catch (e) {
    order.value = null
  } finally {
    loading.value = false
  }
}

const handleAction = async () => {
  const action = nextAction.value
  if (!action) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认执行「${action.label}」？`, '操作确认', { type: 'warning' })
  } catch (e) {
    return
  }
  actionLoading.value = true
  try {
    await updateOrderStatus(order.value.orderId, action.action)
    ElMessage.success('操作成功')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    actionLoading.value = false
  }
}

const openReject = () => {
  rejectReason.value = ''
  rejectVisible.value = true
}

const confirmReject = async () => {
  const reason = rejectReason.value.trim()
  if (!reason) {
    ElMessage.warning('拒单原因不能为空')
    return
  }
  rejecting.value = true
  try {
    await rejectOrder(order.value.orderId, reason)
    ElMessage.success('已拒单并退款')
    rejectVisible.value = false
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    rejecting.value = false
  }
}

watch(() => route.params.id, load)
onMounted(load)
</script>

<style scoped>
.order-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.head-status {
  display: flex;
  align-items: center;
  gap: 12px;
}

.head-no {
  color: #606266;
  font-size: 15px;
}

.head-time {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.head-amount {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.head-amount .label {
  color: #909399;
  font-size: 13px;
}

.head-amount .value {
  color: #ff5722;
  font-size: 30px;
  font-weight: 700;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.timeline {
  margin-top: 16px;
  padding-left: 4px;
}

.info-row {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  width: 76px;
  flex-shrink: 0;
  color: #909399;
}

.info-value {
  flex: 1;
  word-break: break-all;
}

.dish-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dish-img {
  width: 46px;
  height: 46px;
  border-radius: 6px;
  flex-shrink: 0;
}

.amount-box {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.amount-row {
  display: flex;
  gap: 40px;
  min-width: 240px;
  justify-content: space-between;
  color: #606266;
  font-size: 14px;
}

.amount-row.total {
  font-size: 17px;
  font-weight: 700;
  color: #ff5722;
}

.minus {
  color: #67c23a;
}

.detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 900px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
