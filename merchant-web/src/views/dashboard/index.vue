<template>
  <div class="dashboard" v-loading="loading">
    <template v-if="shop">
      <!-- 档口概览 -->
      <div class="page-card shop-overview">
        <div class="shop-main">
          <el-avatar :size="56" shape="square" :src="imgUrl(shop.logo)" class="shop-logo">
            {{ shop.name?.slice(0, 1) }}
          </el-avatar>
          <div class="shop-info">
            <div class="shop-name">{{ shop.name }}</div>
            <div class="shop-meta">
              <span>{{ shop.businessHours || '营业时间未设置' }}</span>
              <span>起送 ¥{{ money(shop.minOrderAmount) }}</span>
              <span>配送 ¥{{ money(shop.deliveryFee) }}</span>
              <span>约 {{ shop.averageDeliveryTime || 0 }} 分钟</span>
            </div>
          </div>
        </div>
        <div class="shop-switches">
          <div class="switch-item">
            <span class="switch-label">营业状态</span>
            <el-switch :model-value="shop.status === 1" active-color="#ff5722" :loading="statusSaving"
                       active-text="营业中" inactive-text="已休息" @change="toggleBusiness" />
          </div>
          <div class="switch-item">
            <span class="switch-label">自动接单</span>
            <el-switch :model-value="shop.autoAccept === 1" active-color="#ff5722" :loading="autoSaving"
                       @change="toggleAutoAccept" />
          </div>
        </div>
      </div>

      <!-- 今日数据 -->
      <div class="stat-grid">
        <div class="stat-card">
          <div class="stat-label">今日营收</div>
          <div class="stat-value primary">¥{{ money(stat.todayAmount) }}</div>
        </div>
        <div class="stat-card clickable" @click="goOrders('')">
          <div class="stat-label">今日订单数</div>
          <div class="stat-value">{{ stat.todayOrderCount || 0 }}</div>
        </div>
        <div class="stat-card clickable" @click="goOrders(1)">
          <div class="stat-label">待接单</div>
          <div class="stat-value warning">{{ stat.pendingCount || 0 }}</div>
        </div>
        <div class="stat-card clickable" @click="goOrders(4)">
          <div class="stat-label">已完成</div>
          <div class="stat-value success">{{ stat.finishedCount || 0 }}</div>
        </div>
      </div>

      <!-- 待接单 -->
      <div class="page-card">
        <div class="page-header">
          <h3 class="page-title">待接单订单</h3>
          <el-button link type="primary" @click="goOrders(1)">查看全部订单 ›</el-button>
        </div>
        <div class="table-wrap">
          <el-table :data="pendingOrders" stripe empty-text="暂无待接单订单">
            <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <el-button link type="primary" @click="goDetail(row.orderId)">{{ row.orderNo }}</el-button>
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="220">
              <template #default="{ row }">
                <div class="dish-lines">
                  <span v-for="(item, index) in row.items || []" :key="index" class="dish-line">
                    {{ item.dishName }}<span v-if="item.specName">（{{ item.specName }}）</span> ×{{ item.count }}
                  </span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="addressSnapshot" label="收货地址" min-width="200" show-overflow-tooltip />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="实付" width="100">
              <template #default="{ row }"><span class="amount">¥{{ money(row.payAmount) }}</span></template>
            </el-table-column>
            <el-table-column label="下单时间" width="170">
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" :loading="acceptingId === row.orderId"
                           @click="quickAccept(row)">接单</el-button>
                <el-button link type="danger" @click="openReject(row)">拒单</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="当前账号尚未绑定档口">
        <el-button type="primary" @click="router.push('/apply')">去提交入驻申请</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMyShop,
  getTodayStat,
  listOrders,
  updateOrderStatus,
  updateShop,
  updateShopStatus,
  rejectOrder
} from '../../api/merchant'
import { formatTime, imgUrl, money } from '../../utils/format'

const router = useRouter()

const loading = ref(false)
const shop = ref(null)
const stat = reactive({
  todayAmount: 0,
  todayOrderCount: 0,
  pendingCount: 0,
  finishedCount: 0
})
const pendingOrders = ref([])
const statusSaving = ref(false)
const autoSaving = ref(false)
const acceptingId = ref(null)

const rejectVisible = ref(false)
const rejecting = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)

const load = async () => {
  loading.value = true
  try {
    const data = await getMyShop()
    shop.value = data || null
    if (shop.value) {
      await Promise.all([loadStat(), loadPending()])
    }
  } catch (e) {
    shop.value = null
  } finally {
    loading.value = false
  }
}

const loadStat = async () => {
  try {
    const data = await getTodayStat()
    Object.assign(stat, {
      todayAmount: data?.todayAmount || 0,
      todayOrderCount: data?.todayOrderCount || 0,
      pendingCount: data?.pendingCount || 0,
      finishedCount: data?.finishedCount || 0
    })
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  }
}

const loadPending = async () => {
  try {
    const list = await listOrders(1)
    pendingOrders.value = (list || []).slice(0, 5)
  } catch (e) {
    pendingOrders.value = []
  }
}

/** 构造完整 ShopUpdateDTO，避免开关操作覆盖其他字段 */
const buildShopPayload = (overrides = {}) => ({
  name: shop.value.name,
  logo: shop.value.logo || '',
  phone: shop.value.phone || '',
  deliveryFee: shop.value.deliveryFee,
  minOrderAmount: shop.value.minOrderAmount,
  averageDeliveryTime: shop.value.averageDeliveryTime,
  businessHours: shop.value.businessHours || '',
  status: shop.value.status,
  autoAccept: shop.value.autoAccept || 0,
  ...overrides
})

const toggleBusiness = async (value) => {
  const next = value ? 1 : 0
  statusSaving.value = true
  try {
    await updateShopStatus(next)
    shop.value.status = next
    ElMessage.success(next === 1 ? '已开始营业' : '已暂停营业')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    statusSaving.value = false
  }
}

const toggleAutoAccept = async (value) => {
  const next = value ? 1 : 0
  autoSaving.value = true
  try {
    await updateShop(buildShopPayload({ autoAccept: next }))
    shop.value.autoAccept = next
    ElMessage.success(next === 1 ? '已开启自动接单' : '已关闭自动接单')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    autoSaving.value = false
  }
}

const quickAccept = async (row) => {
  try {
    await ElMessageBox.confirm(`确认接单 #${row.orderNo}？`, '接单确认', { type: 'warning' })
  } catch (e) {
    return
  }
  acceptingId.value = row.orderId
  try {
    await updateOrderStatus(row.orderId, 'accept')
    ElMessage.success('已接单')
    await Promise.all([loadStat(), loadPending()])
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    acceptingId.value = null
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
    ElMessage.warning('拒单原因不能为空')
    return
  }
  rejecting.value = true
  try {
    await rejectOrder(rejectTarget.value.orderId, reason)
    ElMessage.success('已拒单并退款')
    rejectVisible.value = false
    await Promise.all([loadStat(), loadPending()])
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    rejecting.value = false
  }
}

const goOrders = (status) => {
  router.push({ path: '/orders', query: { status: String(status) } })
}

const goDetail = (id) => {
  router.push(`/orders/${id}`)
}

onMounted(load)
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.shop-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.shop-main {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.shop-logo {
  background: #ff5722;
  color: #fff;
  font-size: 20px;
}

.shop-name {
  font-size: 20px;
  font-weight: 600;
}

.shop-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.shop-switches {
  display: flex;
  align-items: center;
  gap: 28px;
  flex-wrap: wrap;
}

.switch-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.switch-label {
  font-size: 14px;
  color: #606266;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.stat-card.clickable {
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.stat-card.clickable:hover {
  box-shadow: 0 4px 14px rgba(255, 87, 34, 0.16);
}

.stat-label {
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-value.primary {
  color: #ff5722;
}

.stat-value.warning {
  color: #e6a23c;
}

.stat-value.success {
  color: #67c23a;
}

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

@media (max-width: 1100px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
