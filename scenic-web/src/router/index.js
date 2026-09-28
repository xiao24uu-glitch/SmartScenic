import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/',
    component: () => import('../layouts/VisitorLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('../views/home/Index.vue'),
        // 首页无需登录，游客可浏览
      },
      {
        path: 'tickets',
        name: 'Tickets',
        component: () => import('../views/ticket/Index.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'group-purchase',
        name: 'GroupPurchase',
        component: () => import('../views/ticket/GroupPurchase.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('../views/order/Index.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'pay',
        name: 'Pay',
        component: () => import('../views/pay/Index.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'orders/:orderNo',
        name: 'OrderDetail',
        component: () => import('../views/order/Detail.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'face-register',
        name: 'FaceRegister',
        component: () => import('../views/face/Register.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'ai-assistant',
        name: 'AiAssistant',
        component: () => import('../views/ai/Index.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/user/Profile.vue'),
        meta: { requiresAuth: true },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresStaff: true },
    children: [
      // 默认重定向
      {
        path: '',
        redirect: (to) => {
          const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || 'null')
          if (userInfo?.roleCode === 'CHECKER') return '/admin/checker-dashboard'
          return '/admin/dashboard'
        },
      },
      // === 检票员页面 ===
      {
        path: 'checker-dashboard',
        name: 'CheckerDashboard',
        component: () => import('../views/admin/CheckerDashboard.vue'),
      },
      // === 管理员 + 超级管理员页面 ===
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/admin/Dashboard.vue'),
      },
      {
        path: 'tickets',
        name: 'AdminTickets',
        component: () => import('../views/admin/Tickets.vue'),
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/Orders.vue'),
      },
      {
        path: 'groups',
        name: 'AdminGroups',
        component: () => import('../views/admin/Groups.vue'),
      },
      {
        path: 'refunds',
        name: 'AdminRefunds',
        component: () => import('../views/admin/Refunds.vue'),
      },
      {
        path: 'entries',
        name: 'AdminEntries',
        component: () => import('../views/admin/Entries.vue'),
      },
      {
        path: 'faces',
        name: 'AdminFaces',
        component: () => import('../views/admin/Faces.vue'),
      },
      {
        path: 'monitor',
        name: 'Monitor',
        component: () => import('../views/admin/Monitor.vue'),
      },
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('../views/admin/Reports.vue'),
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('../views/admin/Users.vue'),
      },
      {
        path: 'config',
        name: 'AdminConfig',
        component: () => import('../views/admin/Config.vue'),
      },
      {
        path: 'spots',
        name: 'AdminSpots',
        component: () => import('../views/admin/Spots.vue'),
      },
      {
        path: 'facilities',
        name: 'AdminFacilities',
        component: () => import('../views/admin/Facilities.vue'),
      },
      {
        path: 'gates',
        name: 'AdminGates',
        component: () => import('../views/admin/Gates.vue'),
      },
      {
        path: 'coupons',
        name: 'AdminCoupons',
        component: () => import('../views/admin/Coupons.vue'),
      },
      {
        path: 'ai-logs',
        name: 'AdminAiLogs',
        component: () => import('../views/admin/AiLogs.vue'),
      },
      {
        path: 'oper-logs',
        name: 'AdminOperLogs',
        component: () => import('../views/admin/OperLogs.vue'),
      },
      {
        path: 'announcements',
        name: 'AdminAnnouncements',
        component: () => import('../views/admin/Announcements.vue'),
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = sessionStorage.getItem('token')

  // 1. 登录/注册页：已登录则跳首页，未登录则放行
  if (to.path === '/login' || to.path === '/register') {
    if (token) {
      next('/')
    } else {
      next()
    }
    return
  }

  // 2. 需要登录的页面：未登录则跳登录页（记录来源，登录后跳回）
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!token) {
      sessionStorage.setItem('redirect', to.fullPath)
      next('/login')
      return
    }
  }

  // 3. 后台管理权限校验：仅允许 ADMIN、MANAGER、CHECKER
  if (to.meta.requiresStaff) {
    const userInfo = JSON.parse(sessionStorage.getItem('userInfo') || 'null')
    const allowedRoles = ['超级管理员', '景区管理员', '检票员']
    if (!userInfo || !allowedRoles.includes(userInfo.roleName)) {
      next('/')
      return
    }
  }

  next()
})

export default router
