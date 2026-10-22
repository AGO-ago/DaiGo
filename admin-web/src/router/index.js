import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

/**
 * 路由：登录页独立，其余页面统一挂载在后台布局下
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'DataLine' }
      },
      {
        path: 'mall',
        name: 'Mall',
        component: () => import('../views/mall/index.vue'),
        meta: { title: '美食城管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'apply',
        name: 'Apply',
        component: () => import('../views/apply/index.vue'),
        meta: { title: '入驻审核', icon: 'DocumentChecked' }
      },
      {
        path: 'shop',
        name: 'Shop',
        component: () => import('../views/shop/index.vue'),
        meta: { title: '档口管理', icon: 'Shop' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('../views/order/index.vue'),
        meta: { title: '订单监控', icon: 'List' }
      },
      {
        path: 'coupon',
        name: 'Coupon',
        component: () => import('../views/coupon/index.vue'),
        meta: { title: '平台券管理', icon: 'Ticket' }
      },
      {
        path: 'seckill',
        name: 'Seckill',
        component: () => import('../views/seckill/index.vue'),
        meta: { title: '秒杀活动', icon: 'AlarmClock' }
      },
      {
        path: 'comment',
        name: 'Comment',
        component: () => import('../views/comment/index.vue'),
        meta: { title: '评价处理', icon: 'ChatDotRound' }
      },
      {
        path: 'settlement',
        name: 'Settlement',
        component: () => import('../views/settlement/index.vue'),
        meta: { title: '结算管理', icon: 'Money' }
      },
      {
        path: 'config',
        name: 'Config',
        component: () => import('../views/config/index.vue'),
        meta: { title: '系统配置', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  document.title = `${to.meta.title || '管理后台'} · 橙盟美食城`
  if (to.meta.public) {
    return userStore.token ? '/dashboard' : true
  }
  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
