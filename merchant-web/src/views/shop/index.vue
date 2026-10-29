<template>
  <div class="shop-page" v-loading="loading">
    <template v-if="shop">
      <div class="page-card">
        <div class="page-header">
          <h3 class="page-title">店铺设置</h3>
          <span class="sub-text">修改后立即同步至用户端</span>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" class="shop-form">
          <el-form-item label="档口 Logo">
            <ImageUpload v-model="form.logo" />
          </el-form-item>
          <el-form-item label="档口名称" prop="name">
            <el-input v-model="form.name" maxlength="64" placeholder="请输入档口名称" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.phone" maxlength="20" placeholder="请输入联系电话" />
          </el-form-item>
          <el-form-item label="营业时间">
            <el-input v-model="form.businessHours" maxlength="64" placeholder="如 10:00-20:00" />
          </el-form-item>
          <el-form-item label="配送费（元）" prop="deliveryFee">
            <el-input-number v-model="form.deliveryFee" :min="0" :precision="2" :step="1" style="width: 180px" />
          </el-form-item>
          <el-form-item label="起送价（元）" prop="minOrderAmount">
            <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" :step="1" style="width: 180px" />
          </el-form-item>
          <el-form-item label="预计送达（分钟）" prop="averageDeliveryTime">
            <el-input-number v-model="form.averageDeliveryTime" :min="1" :max="180" :step="5" style="width: 180px" />
          </el-form-item>
          <el-form-item label="营业状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-color="#ff5722"
                       active-text="营业中" inactive-text="休息中" />
          </el-form-item>
          <el-form-item label="自动接单">
            <el-switch v-model="form.autoAccept" :active-value="1" :inactive-value="0" active-color="#ff5722" />
            <span class="form-tip">开启后新订单自动接单，需平台同时开启全局自动接单</span>
          </el-form-item>
          <el-form-item label="门店位置">
            <div v-if="mall" class="mall-info">
              <div class="mall-name">{{ mall.name }}</div>
              <div class="mall-address">{{ mall.address || '平台暂未维护详细地址' }}</div>
            </div>
            <span v-else class="form-tip">暂未获取到所在美食城信息</span>
            <el-button v-if="mall && mall.latitude != null && mall.longitude != null"
                       link type="primary" class="mall-map-btn" @click="openMallMap">地图查看</el-button>
          </el-form-item>
          <el-form-item label="档口公告">
            <el-input :model-value="shop.announcement || ''" type="textarea" :rows="2" disabled
                      placeholder="暂无公告" />
            <div class="form-tip block">公告目前由平台统一维护，商家端暂不支持修改</div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
            <el-button @click="load">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </template>

    <div v-else-if="!loading" class="page-card">
      <el-empty description="当前账号尚未入驻或未绑定档口">
        <el-button type="primary" @click="router.push('/apply')">去提交入驻申请</el-button>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ImageUpload from '../../components/ImageUpload.vue'
import { getMall, getMyShop, updateShop } from '../../api/merchant'

const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const shop = ref(null)
const mall = ref(null)
const formRef = ref()

const form = reactive({
  name: '',
  logo: '',
  phone: '',
  deliveryFee: 0,
  minOrderAmount: 0,
  averageDeliveryTime: 30,
  businessHours: '',
  status: 1,
  autoAccept: 0
})

const rules = {
  name: [{ required: true, message: '请输入档口名称', trigger: 'blur' }],
  deliveryFee: [{ required: true, message: '请输入配送费', trigger: 'blur' }],
  minOrderAmount: [{ required: true, message: '请输入起送价', trigger: 'blur' }],
  averageDeliveryTime: [{ required: true, message: '请输入预计送达时间', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const data = await getMyShop()
    shop.value = data || null
    mall.value = null
    if (shop.value) {
      Object.assign(form, {
        name: shop.value.name || '',
        logo: shop.value.logo || '',
        phone: shop.value.phone || '',
        deliveryFee: Number(shop.value.deliveryFee) || 0,
        minOrderAmount: Number(shop.value.minOrderAmount) || 0,
        averageDeliveryTime: shop.value.averageDeliveryTime || 30,
        businessHours: shop.value.businessHours || '',
        status: shop.value.status ?? 1,
        autoAccept: shop.value.autoAccept || 0
      })
      if (shop.value.mallId) {
        getMall(shop.value.mallId)
          .then((res) => { mall.value = res || null })
          .catch(() => { mall.value = null })
      }
    }
  } catch (e) {
    shop.value = null
  } finally {
    loading.value = false
  }
}

const save = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await updateShop({
      name: form.name.trim(),
      logo: form.logo,
      phone: form.phone,
      deliveryFee: form.deliveryFee,
      minOrderAmount: form.minOrderAmount,
      averageDeliveryTime: form.averageDeliveryTime,
      businessHours: form.businessHours,
      status: form.status,
      autoAccept: form.autoAccept
    })
    ElMessage.success('已保存')
    await load()
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

const openMallMap = () => {
  if (!mall.value || mall.value.latitude == null || mall.value.longitude == null) {
    return
  }
  const { longitude, latitude, name, address } = mall.value
  const url = `https://uri.amap.com/marker?position=${longitude},${latitude}`
    + `&name=${encodeURIComponent(name || '门店')}`
    + `&src=daigo&coordinate=gaode&callnative=0`
    + (address ? `&address=${encodeURIComponent(address)}` : '')
  window.open(url, '_blank')
}

onMounted(load)
</script>

<style scoped>
.shop-page {
  min-height: 200px;
}

.shop-form {
  max-width: 680px;
}

.form-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.form-tip.block {
  display: block;
  margin: 4px 0 0;
  line-height: 1.6;
}

.mall-info {
  line-height: 1.6;
}

.mall-name {
  color: #303133;
  font-weight: 600;
}

.mall-address {
  color: #909399;
  font-size: 12px;
}

.mall-map-btn {
  margin-left: 12px;
}

@media (max-width: 768px) {
  .shop-form {
    max-width: 100%;
  }

  .shop-form :deep(.el-form-item__label) {
    width: 100px !important;
  }
}
</style>
