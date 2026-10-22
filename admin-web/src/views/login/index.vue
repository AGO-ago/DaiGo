<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-badge">橙盟</div>
      <h1>橙盟美食城管理后台</h1>
      <p>档口入驻审核 · 订单监控 · 营销活动 · 结算管理</p>
    </div>

    <div class="login-card">
      <h2>管理员登录</h2>
      <p class="login-tip">请使用平台管理员账号登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" size="large" placeholder="请输入登录账号" :prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password
                    placeholder="请输入登录密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p class="login-hint">本地联调账号：admin / admin123</p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const submit = async () => {
  if (!formRef.value) {
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    await userStore.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    router.replace(route.query.redirect || '/dashboard')
  } catch (e) {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 80px;
  background: radial-gradient(circle at 15% 20%, #ff8a4c 0%, transparent 45%),
              radial-gradient(circle at 85% 80%, #ff5a1f 0%, transparent 40%),
              linear-gradient(135deg, #2b1a12 0%, #1f2d3d 100%);
  color: #fff;
}

.login-brand h1 {
  font-size: 34px;
  margin: 18px 0 12px;
  letter-spacing: 1px;
}

.login-brand p {
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
}

.brand-badge {
  display: inline-flex;
  width: 64px;
  height: 64px;
  border-radius: 18px;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #ffb37a, #ff6b35);
  box-shadow: 0 10px 30px rgba(255, 107, 53, 0.35);
}

.login-card {
  width: 380px;
  padding: 34px 34px 26px;
  border-radius: 16px;
  background: #fff;
  color: #303133;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.28);
}

.login-card h2 {
  margin: 0;
  font-size: 22px;
}

.login-tip {
  color: #909399;
  font-size: 13px;
  margin: 8px 0 22px;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.login-hint {
  margin: 18px 0 0;
  text-align: center;
  color: #b0b3b8;
  font-size: 12px;
}
</style>
