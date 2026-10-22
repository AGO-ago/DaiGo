<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">订单监控</h3>
      <span class="sub-text">全平台订单查询、明细查看与强制取消（已支付自动退款）</span>
    </div>

    <div class="table-toolbar">
      <el-input v-model="filters.keyword" placeholder="订单号 / 用户" clearable style="width: 200px"
                @keyup.enter="search" />
      <el-select v-model="filters.shopId" placeholder="全部档口" clearable filterable style="width: 190px">
        <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 140px">
        <el-option v-for="(text, key) in statusMap" :key="key" :label="text" :value="Number(key)" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                      start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="orderNo" label="订单号" width="190" show-overflow-tooltip />
      <el-table-column prop="shopName" label="档口" width="130" show-overflow-tooltip />
      <el-table-column prop="userName" label="用户" width="120" show-overflow-tooltip />
      <el-table-column prop="itemSummary" label="商品" min-width="180" show-overflow-tooltip />
      <el-table-column label="实付" width="100">
        <template #default="{ row }">￥{{ money(row.payAmount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusMap[row.status] || '未知' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canCancel(row.status)" link type="danger" @click="cancel(row)">强制取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-drawer v-model="detailVisible" title="订单详情" size="520px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="订单号">{{ current.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="档口">{{ current.shopName }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ current.userName }}（ID {{ current.userId }}）</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(current.status)">{{ statusMap[current.status] || '未知' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品金额">￥{{ money(current.totalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="配送费">￥{{ money(current.deliveryFee) }}</el-descriptions-item>
        <el-descriptions-item label="优惠金额">￥{{ money(current.discountAmount) }}</el-descriptions-item>
        <el-descriptions-item label="实付金额">￥{{ money(current.payAmount) }}</el-descriptions-item>
        <el-descriptions-item label="收货信息">{{ current.addressSnapshot || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ current.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="current.acceptTime" label="接单时间">
          {{ formatTime(current.acceptTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.finishTime" label="完成时间">
          {{ formatTime(current.finishTime) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="current.cancelTime" label="取消时间">
          {{ formatTime(current.cancelTime) }}
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="current && current.items && current.items.length" class="items">
        <div class="items-title">商品明细</div>
        <div v-for="item in current.items" :key="item.id" class="item-row">
          <span class="item-name">
            {{ item.dishName }}
            <span v-if="item.specName" class="spec">{{ item.specName }}</span>
          </span>
          <span class="item-count">×{{ item.count }}</span>
          <span class="item-amount">￥{{ money(item.amount) }}</span>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listOrders, getOrder, cancelOrder } from '../../api/order'
import { listShops } from '../../api/shop'
import { money } from '../../utils/format'

const statusMap = {
  0: '待支付',
  1: '待接单',
  2: '备餐中',
  3: '配送中',
  4: '已完成',
  5: '已取消',
  6: '退款中',
  7: '已退款'
}

const loading = ref(false)
const rows = ref([])
const shops = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const dateRange = ref([])
const filters = reactive({
  keyword: '',
  shopId: null,
  status: null
})

const detailVisible = ref(false)
const current = ref(null)

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')
const statusType = (status) => ({
  0: 'info',
  1: 'warning',
  2: 'warning',
  3: 'primary',
  4: 'success',
  5: 'info',
  6: 'danger',
  7: 'danger'
}[status] || 'info')
const canCancel = (status) => [0, 1, 2, 3, 6].includes(status)

const load = async () => {
  loading.value = true
  try {
    const data = await listOrders({
      keyword: filters.keyword || undefined,
      shopId: filters.shopId || undefined,
      status: filters.status === null ? undefined : filters.status,
      startDate: dateRange.value && dateRange.value[0],
      endDate: dateRange.value && dateRange.value[1],
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
  filters.keyword = ''
  filters.shopId = null
  filters.status = null
  dateRange.value = []
  search()
}

const openDetail = async (row) => {
  const data = await getOrder(row.id)
  current.value = data
  detailVisible.value = true
}

const cancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认强制取消订单 ${row.orderNo}？已支付订单将自动退款并退券。`,
      '强制取消',
      { type: 'warning', confirmButtonText: '确认取消', cancelButtonText: '再想想' }
    )
  } catch (e) {
    return
  }
  await cancelOrder(row.id)
  ElMessage.success('订单已取消')
  load()
}

onMounted(async () => {
  load()
  const data = await listShops({ page: 1, size: 200 })
  shops.value = data.list || []
})
</script>

<style scoped>
.sub-text {
  color: #909399;
  font-size: 12px;
}

.items {
  margin-top: 16px;
}

.items-title {
  font-weight: 600;
  margin-bottom: 10px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
  font-size: 13px;
}

.item-name {
  flex: 1;
}

.spec {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
}

.item-count {
  color: #909399;
}

.item-amount {
  width: 80px;
  text-align: right;
}
</style>
