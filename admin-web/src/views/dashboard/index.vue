<template>
  <div v-loading="loading">
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <p class="stat-label">{{ card.label }}</p>
          <p class="stat-value">
            <span v-if="card.money" class="money-symbol">¥</span>{{ card.value }}
          </p>
        </div>
      </div>
    </div>

    <div class="dashboard-row">
      <div class="page-card chart-card">
        <div class="page-header">
          <h3 class="page-title">交易趋势</h3>
          <el-radio-group v-model="days" size="small" @change="loadTrend">
            <el-radio-button :value="7">近 7 天</el-radio-button>
            <el-radio-button :value="14">近 14 天</el-radio-button>
            <el-radio-button :value="30">近 30 天</el-radio-button>
          </el-radio-group>
        </div>
        <div ref="chartRef" class="chart"></div>
      </div>

      <div class="page-card todo-card">
        <div class="page-header">
          <h3 class="page-title">待办事项</h3>
        </div>
        <div v-for="todo in todos" :key="todo.label" class="todo-item" @click="go(todo.path)">
          <div class="todo-left">
            <span class="todo-dot" :style="{ background: todo.color }"></span>
            <span>{{ todo.label }}</span>
          </div>
          <div class="todo-right">
            <strong :style="{ color: todo.count > 0 ? todo.color : '#909399' }">{{ todo.count }}</strong>
            <el-icon><ArrowRight /></el-icon>
          </div>
        </div>
        <el-empty v-if="!summary" description="暂无数据" :image-size="60" />
      </div>
    </div>

    <div class="page-card overview-card">
      <div class="page-header">
        <h3 class="page-title">今日概览</h3>
        <span class="refresh-tip">数据更新于 {{ updatedAt }}</span>
      </div>
      <div class="overview-grid">
        <div class="overview-item">
          <span>营业中档口</span>
          <strong>{{ summary?.onlineShopCount ?? 0 }}</strong>
        </div>
        <div class="overview-item">
          <span>累计交易额</span>
          <strong>¥{{ money(summary?.totalGmv) }}</strong>
        </div>
        <div class="overview-item">
          <span>累计订单</span>
          <strong>{{ summary?.totalOrders ?? 0 }}</strong>
        </div>
        <div class="overview-item">
          <span>今日新增用户</span>
          <strong>{{ summary?.todayNewUsers ?? 0 }}</strong>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { getSummary, getTrend } from '../../api/dashboard'

const router = useRouter()
const loading = ref(false)
const summary = ref(null)
const days = ref(7)
const chartRef = ref()
const updatedAt = ref('')
let chart = null

const money = (value) => Number(value || 0).toFixed(2)

const statCards = computed(() => [
  {
    label: '今日交易额',
    value: money(summary.value?.todayGmv),
    money: true,
    icon: 'Money',
    bg: '#fff1e8',
    color: '#ff6b35'
  },
  {
    label: '今日订单数',
    value: summary.value?.todayOrderCount ?? 0,
    icon: 'List',
    bg: '#e8f3ff',
    color: '#409eff'
  },
  {
    label: '今日客单价',
    value: money(summary.value?.todayAvgAmount),
    money: true,
    icon: 'TrendCharts',
    bg: '#eafaf1',
    color: '#2fbf71'
  },
  {
    label: '今日新增用户',
    value: summary.value?.todayNewUsers ?? 0,
    icon: 'User',
    bg: '#f3ecff',
    color: '#8b5cf6'
  }
])

const todos = computed(() => [
  {
    label: '待审核入驻申请',
    count: summary.value?.pendingApplyCount ?? 0,
    color: '#ff6b35',
    path: '/apply'
  },
  {
    label: '待结算单',
    count: summary.value?.pendingSettlementCount ?? 0,
    color: '#e6a23c',
    path: '/settlement'
  },
  {
    label: '待处理差评',
    count: summary.value?.badCommentCount ?? 0,
    color: '#f56c6c',
    path: '/comment'
  }
])

const go = (path) => router.push(path)

const loadSummary = async () => {
  summary.value = await getSummary()
}

const loadTrend = async () => {
  const list = await getTrend(days.value)
  await nextTick()
  if (!chartRef.value) {
    return
  }
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['交易额', '订单数'], top: 0, right: 0 },
    grid: { left: 10, right: 20, top: 40, bottom: 10, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: list.map((item) => item.date.slice(5)),
      axisLine: { lineStyle: { color: '#dcdfe6' } },
      axisLabel: { color: '#909399' }
    },
    yAxis: [
      {
        type: 'value',
        name: '交易额',
        splitLine: { lineStyle: { color: '#f0f2f5' } },
        axisLabel: { color: '#909399' }
      },
      {
        type: 'value',
        name: '订单数',
        minInterval: 1,
        splitLine: { show: false },
        axisLabel: { color: '#909399' }
      }
    ],
    series: [
      {
        name: '交易额',
        type: 'line',
        smooth: true,
        areaStyle: { color: 'rgba(255, 107, 53, 0.12)' },
        lineStyle: { color: '#ff6b35', width: 3 },
        itemStyle: { color: '#ff6b35' },
        data: list.map((item) => Number(item.gmv || 0))
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        lineStyle: { color: '#409eff', width: 2 },
        itemStyle: { color: '#409eff' },
        data: list.map((item) => item.orderCount)
      }
    ]
  })
}

const resize = () => chart && chart.resize()

const load = async () => {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadTrend()])
    updatedAt.value = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } finally {
    loading.value = false
  }
}

watch(days, () => {
  loadTrend()
})

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-label {
  margin: 0 0 6px;
  color: #909399;
  font-size: 13px;
}

.stat-value {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
}

.money-symbol {
  font-size: 15px;
  margin-right: 2px;
}

.dashboard-row {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.chart {
  height: 320px;
}

.todo-card {
  display: flex;
  flex-direction: column;
}

.todo-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.todo-item:hover {
  background: #f7f8fa;
}

.todo-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.todo-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #c0c4cc;
}

.overview-card {
  margin-top: 16px;
}

.refresh-tip {
  color: #b0b3b8;
  font-size: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  background: #fafbfc;
  border-radius: 8px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #909399;
  font-size: 13px;
}

.overview-item strong {
  color: #303133;
  font-size: 20px;
}

@media (max-width: 1100px) {
  .stat-grid,
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .dashboard-row {
    grid-template-columns: 1fr;
  }
}
</style>
