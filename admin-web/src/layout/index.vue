<template>
  <el-container class="layout">
    <el-aside width="220px" class="layout-aside">
      <div class="logo">
        <span class="logo-icon">橙</span>
        <div class="logo-text">
          <strong>橙盟美食城</strong>
          <small>平台管理后台</small>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="layout-menu" background-color="#1f2d3d"
               text-color="#bfcbd9" active-text-color="#ff9a6b">
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item v-if="route.path !== '/dashboard'">{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
        <el-dropdown @command="handleCommand">
          <span class="admin-info">
            <el-avatar :size="28" class="admin-avatar">{{ avatarText }}</el-avatar>
            <span class="admin-name">{{ adminName }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/dashboard', title: '数据看板', icon: 'DataLine' },
  { path: '/mall', title: '美食城管理', icon: 'OfficeBuilding' },
  { path: '/apply', title: '入驻审核', icon: 'DocumentChecked' },
  { path: '/shop', title: '档口管理', icon: 'Shop' },
  { path: '/order', title: '订单监控', icon: 'List' },
  { path: '/coupon', title: '平台券管理', icon: 'Ticket' },
  { path: '/seckill', title: '秒杀活动', icon: 'AlarmClock' },
  { path: '/comment', title: '评价处理', icon: 'ChatDotRound' },
  { path: '/settlement', title: '结算管理', icon: 'Money' },
  { path: '/config', title: '系统配置', icon: 'Setting' }
]

const activeMenu = computed(() => route.path)
const adminName = computed(() => userStore.profile?.realName || userStore.profile?.username || '管理员')
const avatarText = computed(() => adminName.value.slice(0, 1))

onMounted(async () => {
  if (!userStore.profile) {
    try {
      await userStore.loadProfile()
    } catch (e) {
      // 令牌失效由请求拦截器统一处理
    }
  }
})

const handleCommand = async (command) => {
  if (command !== 'logout') {
    return
  }
  await ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' })
  userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.layout-aside {
  background: #1f2d3d;
  display: flex;
  flex-direction: column;
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
  border-radius: 9px;
  background: linear-gradient(135deg, #ff8a4c, #ff5a1f);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
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

.admin-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.admin-avatar {
  background: #ff6b35;
  font-size: 13px;
}

.admin-name {
  font-size: 14px;
}

.layout-main {
  padding: 20px;
  overflow-y: auto;
}
</style>
