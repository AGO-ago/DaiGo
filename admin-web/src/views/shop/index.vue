<template>
  <div class="page-card">
    <div class="page-header">
      <h3 class="page-title">档口管理</h3>
    </div>

    <div class="table-toolbar">
      <el-input v-model="filters.keyword" placeholder="搜索档口名称" clearable style="width: 200px"
                @keyup.enter="search" />
      <el-select v-model="filters.mallId" placeholder="全部美食城" clearable style="width: 180px">
        <el-option v-for="mall in malls" :key="mall.id" :label="mall.name" :value="mall.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="全部状态" clearable style="width: 130px">
        <el-option label="营业中" :value="1" />
        <el-option label="已下线" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column label="档口" min-width="200">
        <template #default="{ row }">
          <div class="shop-cell">
            <el-image v-if="row.logo" :src="imgUrl(row.logo)" fit="cover" class="logo" />
            <div>
              <div class="shop-name">{{ row.name }}</div>
              <div class="sub-text">{{ row.mallName || '未分组' }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="商家账号" width="190">
        <template #default="{ row }">
          <template v-if="row.merchantUserId">
            <div>{{ row.merchantName || '用户' + row.merchantUserId }}</div>
            <div class="sub-text">{{ row.merchantPhone || '未绑定手机号' }}</div>
            <el-tag :type="row.merchantHasPassword ? 'success' : 'info'" size="small">
              {{ row.merchantHasPassword ? '网页密码已设置' : '未设置网页密码' }}
            </el-tag>
          </template>
          <el-tag v-else type="info" size="small">未绑定</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column label="配送/起送" width="130">
        <template #default="{ row }">
          ¥{{ money(row.deliveryFee) }} / ¥{{ money(row.minOrderAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="抽佣比例" width="120">
        <template #default="{ row }">
          <span v-if="row.commissionRate !== null && row.commissionRate !== undefined">
            {{ (Number(row.commissionRate) * 100).toFixed(2) }}%
          </span>
          <span v-else class="sub-text">默认</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" active-text="营业"
                     inline-prompt @change="(value) => changeStatus(row, value)" />
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCommission(row)">设置抽佣</el-button>
          <el-button link type="warning" :disabled="!row.merchantUserId"
                     @click="openPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                   layout="total, prev, pager, next" @current-change="load" />

    <el-dialog v-model="commissionVisible" title="设置抽佣比例" width="420px">
      <el-form label-width="100px">
        <el-form-item label="档口">
          <span>{{ commissionRow?.name }}</span>
        </el-form-item>
        <el-form-item label="抽佣比例">
          <el-input-number v-model="commissionPercent" :min="0" :max="30" :precision="2" :step="0.5" />
          <span class="unit">%</span>
        </el-form-item>
      </el-form>
      <p class="tip">允许范围 0-30%，留空或未设置时使用平台默认比例。</p>
      <template #footer>
        <el-button @click="commissionVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveCommission">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" title="重置商家网页登录密码" width="420px">
      <el-form label-width="100px">
        <el-form-item label="档口">
          <span>{{ passwordRow?.name }}</span>
        </el-form-item>
        <el-form-item label="商家手机号">
          <span>{{ passwordRow?.merchantPhone || '未绑定' }}</span>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" type="password" show-password maxlength="20"
                    placeholder="6-20 位" />
        </el-form-item>
      </el-form>
      <p class="tip">重置后请告知商家，商家可用手机号 + 新密码登录商家网页端。</p>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePassword">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listShops, updateShopStatus, updateShopCommission, resetMerchantPassword } from '../../api/shop'
import { listMalls } from '../../api/mall'
import { imgUrl, money } from '../../utils/format'

const loading = ref(false)
const saving = ref(false)
const rows = ref([])
const malls = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const filters = reactive({ keyword: '', mallId: null, status: null })

const commissionVisible = ref(false)
const commissionRow = ref(null)
const commissionPercent = ref(5)

const passwordVisible = ref(false)
const passwordRow = ref(null)
const newPassword = ref('')

const formatTime = (value) => (value ? String(value).replace('T', ' ').slice(0, 19) : '')

const load = async () => {
  loading.value = true
  try {
    const data = await listShops({
      keyword: filters.keyword || undefined,
      mallId: filters.mallId || undefined,
      status: filters.status === null || filters.status === '' ? undefined : filters.status,
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
  filters.mallId = null
  filters.status = null
  search()
}

const changeStatus = async (row, value) => {
  try {
    await updateShopStatus(row.id, value)
    ElMessage.success(value === 1 ? '档口已上线' : '档口已下线')
  } catch (e) {
    load()
  }
}

const openCommission = (row) => {
  commissionRow.value = row
  commissionPercent.value = row.commissionRate === null || row.commissionRate === undefined
    ? 5
    : Number(row.commissionRate) * 100
  commissionVisible.value = true
}

const saveCommission = async () => {
  saving.value = true
  try {
    await updateShopCommission(commissionRow.value.id, Number((commissionPercent.value / 100).toFixed(4)))
    ElMessage.success('抽佣比例已更新')
    commissionVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const openPassword = (row) => {
  passwordRow.value = row
  newPassword.value = ''
  passwordVisible.value = true
}

const savePassword = async () => {
  if (!newPassword.value || newPassword.value.length < 6 || newPassword.value.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }
  saving.value = true
  try {
    await resetMerchantPassword(passwordRow.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    passwordVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  load()
  malls.value = await listMalls()
})
</script>

<style scoped>
.shop-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}

.shop-name {
  font-weight: 500;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.unit {
  margin-left: 8px;
  color: #909399;
}

.tip {
  margin: 0;
  color: #909399;
  font-size: 12px;
}
</style>
