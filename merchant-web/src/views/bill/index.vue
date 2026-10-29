<template>
  <div class="bill-page">
    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">账单结算</h3>
        <el-radio-group v-model="period" @change="loadBill">
          <el-radio-button value="today">今日</el-radio-button>
          <el-radio-button value="7d">近 7 天</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
      </div>

      <div class="summary-card" v-loading="loading">
        <div class="summary-main">
          <div class="summary-label">应结金额</div>
          <div class="summary-value">¥{{ money(summary.netAmount) }}</div>
          <div class="summary-sub">完成订单 {{ summary.orderCount || 0 }} 单</div>
        </div>
        <div class="summary-grid">
          <div class="summary-item">
            <div class="item-label">商品金额</div>
            <div class="item-value">¥{{ money(summary.goodsAmount) }}</div>
          </div>
          <div class="summary-item">
            <div class="item-label">配送费</div>
            <div class="item-value">¥{{ money(summary.deliveryAmount) }}</div>
          </div>
          <div class="summary-item">
            <div class="item-label">平台抽佣</div>
            <div class="item-value minus">{{ deduction(summary.commissionAmount) }}</div>
          </div>
          <div class="summary-item">
            <div class="item-label">周期退款</div>
            <div class="item-value minus">{{ deduction(summary.refundAmount) }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">订单明细</h3>
      </div>
      <div class="table-wrap">
        <el-table :data="pagedItems" v-loading="loading" stripe row-key="orderId"
                  empty-text="该周期暂无已完成订单">
          <el-table-column prop="orderNo" label="订单号" width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button link type="primary" @click="router.push(`/orders/${row.orderId}`)">{{ row.orderNo }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="完成时间" width="170">
            <template #default="{ row }">{{ formatTime(row.finishTime) }}</template>
          </el-table-column>
          <el-table-column label="商品 / 配送" width="150">
            <template #default="{ row }">
              ¥{{ money(row.goodsAmount) }} / ¥{{ money(row.deliveryAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="平台券补贴" width="120">
            <template #default="{ row }">
              <span class="plus">+¥{{ money(row.platformDiscount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="店铺券成本" width="120">
            <template #default="{ row }">
              <span class="minus">{{ deduction(row.shopDiscount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="抽佣比例" width="110">
            <template #default="{ row }">{{ percent(row.commissionRate) }}</template>
          </el-table-column>
          <el-table-column label="抽佣金额" width="110">
            <template #default="{ row }">
              <span class="minus">{{ deduction(row.commissionAmount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="应结" width="110">
            <template #default="{ row }"><span class="net">¥{{ money(row.netAmount) }}</span></template>
          </el-table-column>
        </el-table>
      </div>
      <el-pagination v-model:current-page="itemPage" :page-size="pageSize" :total="items.length"
                     layout="total, prev, pager, next" />
    </div>

    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">平台结算单</h3>
        <span class="sub-text">结算单由平台生成，商家端只读</span>
      </div>
      <div class="table-wrap">
        <el-table :data="pagedSettlements" v-loading="settlementLoading" stripe row-key="id"
                  empty-text="暂无结算单">
          <el-table-column prop="settlementNo" label="结算单号" width="190" show-overflow-tooltip />
          <el-table-column label="结算周期" width="200">
            <template #default="{ row }">{{ row.periodStart }} ~ {{ row.periodEnd }}</template>
          </el-table-column>
          <el-table-column prop="orderCount" label="订单数" width="90" />
          <el-table-column label="商品 / 配送" width="150">
            <template #default="{ row }">
              ¥{{ money(row.goodsAmount) }} / ¥{{ money(row.deliveryAmount) }}
            </template>
          </el-table-column>
          <el-table-column label="抽佣比例" width="110">
            <template #default="{ row }">{{ percent(row.commissionRate) }}</template>
          </el-table-column>
          <el-table-column label="抽佣金额" width="110">
            <template #default="{ row }"><span class="minus">{{ deduction(row.commissionAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="退款金额" width="110">
            <template #default="{ row }"><span class="minus">{{ deduction(row.refundAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="应结金额" width="110">
            <template #default="{ row }"><span class="net">¥{{ money(row.netAmount) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'">
                {{ row.status === 1 ? '已结算' : '待结算' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结算时间" width="170">
            <template #default="{ row }">{{ formatTime(row.settleTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <el-pagination v-model:current-page="settlementPage" :page-size="pageSize" :total="settlements.length"
                     layout="total, prev, pager, next" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getBill, listSettlements } from '../../api/merchant'
import { formatTime, deduction, money, percent } from '../../utils/format'

const router = useRouter()

const period = ref('today')
const loading = ref(false)
const settlementLoading = ref(false)
const summary = ref({})
const items = ref([])
const settlements = ref([])
const itemPage = ref(1)
const settlementPage = ref(1)
const pageSize = 10

const pagedItems = computed(() => {
  const start = (itemPage.value - 1) * pageSize
  return items.value.slice(start, start + pageSize)
})

const pagedSettlements = computed(() => {
  const start = (settlementPage.value - 1) * pageSize
  return settlements.value.slice(start, start + pageSize)
})

const loadBill = async () => {
  loading.value = true
  itemPage.value = 1
  try {
    const data = await getBill(period.value)
    summary.value = data?.summary || {}
    items.value = data?.items || []
  } catch (e) {
    summary.value = {}
    items.value = []
  } finally {
    loading.value = false
  }
}

const loadSettlements = async () => {
  settlementLoading.value = true
  try {
    settlements.value = (await listSettlements()) || []
  } catch (e) {
    settlements.value = []
  } finally {
    settlementLoading.value = false
  }
}

onMounted(() => {
  loadBill()
  loadSettlements()
})
</script>

<style scoped>
.bill-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 40px;
  flex-wrap: wrap;
  padding: 20px 24px;
  border-radius: 10px;
  background: linear-gradient(120deg, #fff3ef, #fffaf7);
}

.summary-main {
  min-width: 180px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin: 6px 0 4px;
  font-size: 34px;
  font-weight: 700;
  color: #ff5722;
}

.summary-sub {
  color: #909399;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(110px, 1fr));
  gap: 18px 30px;
  flex: 1;
}

.item-label {
  color: #909399;
  font-size: 12px;
}

.item-value {
  margin-top: 6px;
  font-size: 17px;
  font-weight: 600;
}

.minus {
  color: #f56c6c;
}

.plus {
  color: #67c23a;
}

.net {
  color: #ff5722;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(110px, 1fr));
  }
}
</style>
