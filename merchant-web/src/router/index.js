import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../store/user'

/**
 * 路由：登录页独立，其余页面统一挂载在商家端布局下
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
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/orders/index.vue'),
        meta: { title: '订单管理', icon: 'List' }
      },
      {
        path: 'orders/:id',
        name: 'OrderDetail',
        component: () => import('../views/orders/detail.vue'),
        meta: { title: '订单详情', activeMenu: '/orders' }
      },
      {
        path: 'refunds',
        name: 'Refunds',
        component: () => import('../views/refunds/index.vue'),
        meta: { title: '退款申请', icon: 'RefreshLeft' }
      },
      {
        path: 'dishes',
        name: 'Dishes',
        component: () => import('../views/dishes/index.vue'),
        meta: { title: '菜品管理', icon: 'Food' }
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('../views/comments/index.vue'),
        meta: { title: '顾客评价', icon: 'ChatDotRound' }
      },
      {
        path: 'shop',
        name: 'Shop',
        component: () => import('../views/shop/index.vue'),
        meta: { title: '店铺设置', icon: 'Shop' }
      },
      {
        path: 'bill',
        name: 'Bill',
        component: () => import('../views/bill/index.vue'),
        meta: { title: '账单结算', icon: 'Money' }
      },
      {
        path: 'coupons',
        name: 'Coupons',
        component: () => import('../views/coupons/index.vue'),
        meta: { title: '店铺券', icon: 'Ticket' }
      },
      {
        path: 'apply',
        name: 'Apply',
        component: () => import('../views/apply/index.vue'),
        meta: { title: '入驻信息', icon: 'DocumentChecked' }
      },
      {
        path: 'account',
        name: 'Account',
        component: () => import('../views/account/index.vue'),
        meta: { title: '账号设置', icon: 'User' }
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
  document.title = `${to.meta.title || '商家中心'} · 代go`
  if (to.meta.public) {
    return userStore.token ? '/dashboard' : true
  }
  if (!userStore.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
