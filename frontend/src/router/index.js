import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/admin/users',
    name: 'AdminUserList',
    component: () => import('@/views/admin/UserList.vue'),
    meta: { admin: true }
  },
  {
    path: '/demands',
    name: 'DemandList',
    component: () => import('@/views/DemandList.vue')
  },
  {
    path: '/demands/publish',
    name: 'DemandPublish',
    component: () => import('@/views/DemandPublish.vue')
  },
  {
    path: '/demands/:demandId',
    name: 'DemandDetail',
    component: () => import('@/views/DemandDetail.vue')
  },
  {
    path: '/orders',
    name: 'MyOrders',
    component: () => import('@/views/MyOrders.vue')
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue')
  },
  {
    path: '/chat/:conversationId',
    name: 'ChatDetail',
    component: () => import('@/views/ChatDetail.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  if (!auth.isLoggedIn && !to.meta.guest) {
    next('/login')
  } else if (auth.isLoggedIn && to.meta.guest) {
    next('/')
  } else if (to.meta.admin && !auth.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
