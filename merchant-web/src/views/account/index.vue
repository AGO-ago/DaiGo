<template>
  <div class="account-page" v-loading="loading">
    <div class="page-card profile-card">
      <el-avatar :size="64" :src="imgUrl(profile?.avatar)" class="avatar">
        {{ avatarText }}
      </el-avatar>
      <div class="profile-info">
        <div class="profile-name">{{ profile?.nickname || '商家账号' }}</div>
        <div class="profile-meta">
          <span>手机号：{{ profile?.phone || '未绑定' }}</span>
          <span>账号 ID：{{ profile?.userId || '-' }}</span>
          <span>档口：{{ shopName }}</span>
        </div>
      </div>
      <el-tag :type="hasPassword ? 'success' : 'warning'" effect="plain">
        {{ hasPassword ? '已设置网页密码' : '未设置网页密码' }}
      </el-tag>
    </div>

    <div class="page-card">
      <div class="page-header">
        <h3 class="page-title">{{ hasPassword ? '修改登录密码' : '设置登录密码' }}</h3>
        <span class="sub-text">密码用于电脑浏览器登录商家中心，长度 6-20 位</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="pwd-form">
        <el-form-item v-if="hasPassword" label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password maxlength="20"
                    placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password maxlength="20"
                    placeholder="请输入 6-20 位新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="20"
                    placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">
            {{ hasPassword ? '修改密码' : '设置密码' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert type="info" :closable="false" show-icon
                title="忘记密码时可在商家小程序-我的中重新设置，或联系平台重置。" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPasswordStatus, setPassword } from '../../api/auth'
import { getMyShop } from '../../api/merchant'
import { useUserStore } from '../../store/user'
import { imgUrl } from '../../utils/format'

const userStore = useUserStore()

const loading = ref(false)
const saving = ref(false)
const hasPassword = ref(false)
const shop = ref(null)
const formRef = ref()

const profile = computed(() => userStore.profile)
const shopName = computed(() => shop.value?.name || profile.value?.shopName || '未绑定档口')
const avatarText = computed(() => (profile.value?.nickname || '商').slice(0, 1))

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateOld = (rule, value, callback) => {
  if (hasPassword.value && !value) {
    callback(new Error('请输入原密码'))
    return
  }
  callback()
}

const validateConfirm = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const rules = {
  oldPassword: [{ validator: validateOld, trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { pattern: /^.{6,20}$/, message: '新密码需 6-20 位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const [status, shopData] = await Promise.all([
      getPasswordStatus().catch(() => false),
      getMyShop().catch(() => null)
    ])
    hasPassword.value = !!status
    shop.value = shopData || null
    if (shop.value && profile.value) {
      userStore.setProfile({
        ...profile.value,
        shopId: shop.value.id,
        shopName: shop.value.name
      })
    }
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirmPassword = ''
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

const submit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  saving.value = true
  try {
    await setPassword({
      oldPassword: hasPassword.value ? form.oldPassword : '',
      newPassword: form.newPassword
    })
    hasPassword.value = true
    resetForm()
    ElMessage.success('密码已保存')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 900px;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 18px;
  flex-wrap: wrap;
}

.avatar {
  background: #ff5722;
  color: #fff;
  font-size: 24px;
}

.profile-info {
  flex: 1;
  min-width: 200px;
}

.profile-name {
  font-size: 20px;
  font-weight: 600;
}

.profile-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}

.pwd-form {
  max-width: 460px;
  margin-bottom: 8px;
}
</style>
