<template>
  <div class="apply-page" v-loading="loading">
    <!-- 已入驻 -->
    <div v-if="shop" class="page-card">
      <div class="status-card approved">
        <div class="status-title">已入驻</div>
        <div class="status-desc">当前账号已绑定档口「{{ shop.name }}」，可直接经营。</div>
        <el-button type="primary" @click="router.push('/shop')">进入店铺设置</el-button>
      </div>
    </div>

    <template v-else>
      <!-- 申请进度 -->
      <div v-if="apply" class="page-card">
        <div class="status-card" :class="applyStatusClass">
          <div class="status-head">
            <span class="status-title">{{ applyStatusText }}</span>
            <el-tag :type="applyStatusType">{{ applyStatusText }}</el-tag>
          </div>
          <div class="status-desc">
            <template v-if="apply.status === 0">申请已提交，平台审核通常在 1 个工作日内完成。</template>
            <template v-else-if="apply.status === 2">驳回原因：{{ apply.rejectReason || '未填写' }}，修改后可重新提交。</template>
            <template v-else>审核已通过，请刷新页面查看档口信息。</template>
          </div>
          <el-descriptions :column="2" border class="apply-desc">
            <el-descriptions-item label="申请档口">{{ apply.shopName }}</el-descriptions-item>
            <el-descriptions-item label="所属美食城">{{ apply.mallName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系人">{{ apply.contact }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ apply.phone }}</el-descriptions-item>
            <el-descriptions-item label="营业时间">{{ apply.businessHours || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ formatTime(apply.createTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="apply.auditTime" label="审核时间">{{ formatTime(apply.auditTime) }}</el-descriptions-item>
            <el-descriptions-item v-if="apply.rejectReason" label="驳回原因" :span="2">
              {{ apply.rejectReason }}
            </el-descriptions-item>
          </el-descriptions>
          <div v-if="apply.status === 1" class="status-actions">
            <el-button @click="load">刷新状态</el-button>
          </div>
        </div>
      </div>

      <!-- 申请表单 -->
      <div v-if="!apply || apply.status === 2" class="page-card">
        <div class="page-header">
          <h3 class="page-title">提交入驻申请</h3>
          <span class="sub-text">提交后由平台审核，通过后自动绑定档口并开通经营权限</span>
        </div>
        <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="apply-form">
          <el-form-item label="档口名称" prop="shopName">
            <el-input v-model="form.shopName" maxlength="64" placeholder="请输入档口名称" />
          </el-form-item>
          <el-form-item label="所属美食城" prop="mallId">
            <el-select v-model="form.mallId" filterable placeholder="请选择美食城" style="width: 100%">
              <el-option v-for="mall in malls" :key="mall.id" :label="mall.name" :value="mall.id" />
            </el-select>
            <div v-if="!malls.length" class="form-tip block">暂无可选美食城，请联系平台确认营业中的美食城</div>
          </el-form-item>
          <el-form-item label="联系人" prop="contact">
            <el-input v-model="form.contact" maxlength="32" placeholder="请输入联系人姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" maxlength="11" placeholder="请输入 11 位手机号" />
          </el-form-item>
          <el-form-item label="营业时间">
            <el-input v-model="form.businessHours" maxlength="64" placeholder="如 09:00-21:00" />
          </el-form-item>
          <el-form-item label="营业执照">
            <ImageUpload v-model="form.licenseImg" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUpload from '../../components/ImageUpload.vue'
import { getMyApply, getMyShop, listMalls, submitApply } from '../../api/merchant'
import { formatTime } from '../../utils/format'

const router = useRouter()

const loading = ref(false)
const submitting = ref(false)
const shop = ref(null)
const apply = ref(null)
const malls = ref([])
const formRef = ref()

const form = reactive({
  shopName: '',
  mallId: null,
  contact: '',
  phone: '',
  businessHours: '',
  licenseImg: ''
})

const rules = {
  shopName: [{ required: true, message: '请输入档口名称', trigger: 'blur' }],
  mallId: [{ required: true, message: '请选择所属美食城', trigger: 'change' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ]
}

const applyStatusText = computed(() => {
  const status = apply.value?.status
  if (status === 0) {
    return '审核中'
  }
  if (status === 2) {
    return '已驳回'
  }
  return '已通过'
})

const applyStatusType = computed(() => {
  const status = apply.value?.status
  if (status === 0) {
    return 'warning'
  }
  if (status === 2) {
    return 'danger'
  }
  return 'success'
})

const applyStatusClass = computed(() => {
  const status = apply.value?.status
  if (status === 0) {
    return 'pending'
  }
  if (status === 2) {
    return 'rejected'
  }
  return 'approved'
})

const load = async () => {
  loading.value = true
  try {
    const [shopData, applyData, mallData] = await Promise.all([
      getMyShop().catch(() => null),
      getMyApply().catch(() => null),
      listMalls().catch(() => [])
    ])
    shop.value = shopData || null
    apply.value = applyData || null
    malls.value = mallData || []
    if (!shop.value && (!apply.value || apply.value.status === 2)) {
      const source = apply.value
      Object.assign(form, {
        shopName: source?.shopName || '',
        mallId: source?.mallId || null,
        contact: source?.contact || '',
        phone: source?.phone || '',
        businessHours: source?.businessHours || '',
        licenseImg: source?.licenseImg || ''
      })
    }
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  try {
    await submitApply({
      shopName: form.shopName.trim(),
      mallId: form.mallId,
      contact: form.contact.trim(),
      phone: form.phone,
      businessHours: form.businessHours,
      licenseImg: form.licenseImg
    })
    ElMessage.success('提交成功')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.apply-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}

.status-card {
  padding: 22px 24px;
  border-radius: 10px;
  background: #f7f9fc;
}

.status-card.approved {
  background: linear-gradient(120deg, #f0f9eb, #f8fff4);
}

.status-card.pending {
  background: linear-gradient(120deg, #fdf6ec, #fffaf0);
}

.status-card.rejected {
  background: linear-gradient(120deg, #fef0f0, #fff8f8);
}

.status-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-title {
  font-size: 20px;
  font-weight: 700;
}

.status-desc {
  margin: 10px 0 16px;
  color: #606266;
  font-size: 14px;
}

.apply-desc {
  background: #fff;
  border-radius: 8px;
}

.status-actions {
  margin-top: 16px;
}

.apply-form {
  max-width: 660px;
}

.form-tip.block {
  display: block;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
  margin-top: 4px;
}
</style>
