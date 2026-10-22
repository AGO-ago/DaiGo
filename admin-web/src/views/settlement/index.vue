<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">结算管理</h3>
      <div class="generate">
        <el-date-picker v-model="generateDate" type="date" value-format="YYYY-MM-DD"
                        placeholder="选择结算日期（默认昨日）" style="width: 210px" />
        <el-button type="primary" :loading="generating" @click="generate">生成结算单</el-button>
      </div>
    </div>

    <div class="table-toolbar">
      <el-select v-model="shopId" placeholder="全部档口" clearable filterable style="width: 190px">
        <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
      </el-select>
      <el-select v-model="status" placeholder="全部状态" clearable style="width: 140px">
        <el-option label="待结算" :value="0" />
        <el-option label="已结算" :value="1" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <span class="sub-text">抽佣规则：档口单独设置优先，否则取系统默认抽佣比例</span>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="settlementNo" label="结算单号" width="150" show-overflow-tooltip />
      <el-table-column prop="shopName" label="档口" width="130" show-overflow-tooltip />
      <el-table-column label="结算周期" width="180">
        <template #default="{ row }">
          {{ row.periodStart }} 至 {{ row.periodEnd }}
        </template>
      </el-table-column>
      <el-table-column prop="orderCount" label="订单数" width="80" />
      <el-table-column label="商品金额" width="100">
        <template #default="{ row }">￥{{ money(row.goodsAmount) }}</template>
      </el-table-column>
      <el-table-column label="配送费" width="90">
        <template #default="{ row }">￥{{ money(row.deliveryAmount) }}</template>
      </el-table-column>
      <el-table-column label="优惠" width="90">
        <template #default="{ row }">-￥{{ money(row.discountAmount) }}</template>
      </el-table-column>
      <el-table-column label="抽佣" width="130">
        <template #default="{ row }">
          <div>-￥{{ money(row.commissionAmount) }}</div>
          <div class="sub-text">比例 {{ rateText(row.commissionRate) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="退款" width="90">
        <template #default="{ row }">-￥{{ money(row.refundAmount) }}</template>
      </el-table-column>
      <el-table-column label="应结金额" width="110">
        <template #default="{ row }">
          <span class="net">￥{{ money(row.netAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'">
            {{ row.status === 1 ? '已结算' : '待结算' }}
          </el-tag>
          <div v-if="row.status === 1 && row.settleTime" class="sub-text">
            {{ formatTime(row.settleTime) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" link type="primary" @click="settle(row)">标记已结算</el-button>
          <span v-else class="sub-text">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateSettlement, listSettlements, settleSettlement } from '../../api/settlement'
import { listShops } from '../../api/shop'
import { money } from '../../utils/format'

const loading = ref(false)
const generating = ref(false)
const rows = ref([])
const shops = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const shopId = ref(null)
const status = ref(null)
const generateDate = ref('')

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')
const rateText = (value) => `${(Number(value || 0) * 100).toFixed(2).replace(/\.?0+$/, '')}%`

const load = async () => {
  loading.value = true
  try {
    const data = await listSettlements({
      shopId: shopId.value || undefined,
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
  shopId.value = null
  status.value = null
  search()
}

const generate = async () => {
  const label = generateDate.value || '昨日'
  try {
    await ElMessageBox.confirm(`确认生成 ${label} 的结算单？已生成的档口将自动跳过。`, '生成结算单', {
      type: 'warning'
    })
  } catch (e) {
    return
  }
  generating.value = true
  try {
    const count = await generateSettlement(generateDate.value || undefined)
    ElMessage.success(count > 0 ? `已生成 ${count} 张结算单` : '没有需要生成的新结算单')
    load()
  } finally {
    generating.value = false
  }
}

const settle = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认将结算单 ${row.settlementNo} 标记为已结算？应结金额 ￥${money(row.netAmount)}。`,
      '标记已结算',
      { type: 'warning' }
    )
  } catch (e) {
    return
  }
  await settleSettlement(row.id)
  ElMessage.success('已标记结算')
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

.generate {
  display: flex;
  gap: 10px;
}

.net {
  color: #ff6b35;
  font-weight: 600;
}
</style>
