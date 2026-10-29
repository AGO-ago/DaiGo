<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span class="logo-icon">代</span>
        <div class="logo-text">
          <strong>代go</strong>
          <small>商家中心</small>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="layout-menu" background-color="#1f2d3d"
               text-color="#bfcbd9" active-text-color="#ff8a65">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="layout-body">
      <el-header class="layout-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.path !== '/dashboard'">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="handleCommand">
          <span class="merchant-info">
            <el-avatar :size="30" :src="avatar" class="merchant-avatar">{{ avatarText }}</el-avatar>
            <span class="merchant-meta">
              <span class="merchant-name">{{ shopName }}</span>
              <span class="merchant-phone">{{ phoneText }}</span>
            </span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">账号设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'
import { imgUrl } from '../utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/orders', title: '订单管理', icon: 'List' },
  { path: '/refunds', title: '退款申请', icon: 'RefreshLeft' },
  { path: '/dishes', title: '菜品管理', icon: 'Food' },
  { path: '/comments', title: '顾客评价', icon: 'ChatDotRound' },
  { path: '/shop', title: '店铺设置', icon: 'Shop' },
  { path: '/bill', title: '账单结算', icon: 'Money' },
  { path: '/coupons', title: '店铺券', icon: 'Ticket' },
  { path: '/apply', title: '入驻信息', icon: 'DocumentChecked' },
  { path: '/account', title: '账号设置', icon: 'User' }
]

const activeMenu = computed(() => route.meta.activeMenu || route.path)
const shopName = computed(() => userStore.profile?.shopName || userStore.profile?.nickname || '商家账号')
const phoneText = computed(() => userStore.profile?.phone || '未绑定手机号')
const avatar = computed(() => imgUrl(userStore.profile?.avatar))
const avatarText = computed(() => shopName.value.slice(0, 1))

const handleCommand = async (command) => {
  if (command === 'account') {
    router.push('/account')
    return
  }
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('确认退出当前商家账号？', '提示', { type: 'warning' })
  } catch (e) {
    return
  }
  userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
  min-width: 0;
}

.layout-aside {
  background: #1f2d3d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow-x: hidden;
}

.layout-body {
  min-width: 0;
  overflow: hidden;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 800;
  color: #ff5722;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.18);
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.logo-text strong {
  font-size: 18px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.5px;
  background: linear-gradient(120deg, #ffffff 30%, #ffb199 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.logo-text small {
  color: #8b9bb0;
  font-size: 11px;
}

.layout-menu {
  border-right: none;
  flex: 1;
}

.layout-header {
  height: 64px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.merchant-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  outline: none;
}

.merchant-avatar {
  background: #ff5722;
  font-size: 13px;
}

.merchant-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.merchant-name {
  font-size: 14px;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.merchant-phone {
  font-size: 12px;
  color: #909399;
}

.layout-main {
  padding: 20px;
  min-width: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

@media (max-width: 1200px) {
  .layout-aside {
    width: 180px !important;
  }

  .logo-text small {
    display: none;
  }

  .merchant-phone {
    display: none;
  }

  .layout-main {
    padding: 16px;
  }
}

@media (max-width: 960px) {
  .layout-aside {
    width: 64px !important;
  }

  .logo {
    justify-content: center;
    padding: 0;
  }

  .logo-text,
  .layout-menu span {
    display: none;
  }

  .layout-menu :deep(.el-menu-item) {
    justify-content: center;
    padding: 0 !important;
  }

  .layout-header {
    padding: 0 16px;
  }

  .merchant-meta {
    display: none;
  }
}
</style>
