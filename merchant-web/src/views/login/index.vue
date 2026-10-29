<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-row">
        <div class="brand-mark">代</div>
        <div class="brand-name">代<span class="brand-en">go</span></div>
      </div>
      <h1>商家中心</h1>
      <p>接单备餐 · 菜品管理 · 店铺券 · 账单结算</p>
    </div>

    <div class="login-card">
      <h2>商家登录</h2>
      <p class="login-tip">请使用商家手机号与网页登录密码登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" size="large" maxlength="11" placeholder="请输入商家手机号"
                    :prefix-icon="Iphone" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password
                    placeholder="请输入登录密码" :prefix-icon="Lock" />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>
      <p class="login-hint">
        账号为商家手机号，密码在商家小程序-我的中设置，或联系平台重置
      </p>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Iphone, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../../store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ phone: '', password: '' })
const rules = {
  phone: [
    { required: true, message: '请输入商家手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入登录密码', trigger: 'blur' }]
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
    await userStore.login({ phone: form.phone.trim(), password: form.password })
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
              radial-gradient(circle at 85% 80%, #ff5722 0%, transparent 40%),
              linear-gradient(135deg, #2b1a12 0%, #1f2d3d 100%);
  color: #fff;
}

.brand-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: 800;
  color: #ff5722;
  background: #ffffff;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.24);
}

.brand-name {
  font-size: 44px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 2px;
  color: #ffffff;
  text-shadow: 0 4px 14px rgba(0, 0, 0, 0.28);
}

.brand-en {
  font-size: 48px;
  color: #ffe27a;
}

.login-brand h1 {
  font-size: 28px;
  margin: 20px 0 10px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 1px;
  background: linear-gradient(120deg, #ffffff 20%, #ffd36e 60%, #ff8a4c 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.login-brand p {
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
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
  line-height: 1.6;
}

@media (max-width: 900px) {
  .login-page {
    gap: 32px;
    padding: 20px;
  }

  .login-brand {
    display: none;
  }
}
</style>
