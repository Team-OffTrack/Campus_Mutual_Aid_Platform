import { createRouter, createWebHistory } from 'vue-router'

/**
 * Route definitions with lazy-loaded page components.
 *
 * Route meta:
 * - guest: true  → only accessible when NOT logged in
 * - admin: true  → requires localStorage role === 'ADMIN'
 *
 * The beforeEach guard enforces these rules and redirects to /login or / as needed.
 */
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * Global navigation guard:
 * - Missing token + not a guest page → redirect /login
 * - Has token + guest page → redirect /
 * - Admin route + role !== 'ADMIN' → redirect /
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (!token && !to.meta.guest) {
    next('/login')
  } else if (token && to.meta.guest) {
    next('/')
  } else if (to.meta.admin) {
    const role = localStorage.getItem('role')
    if (role !== 'ADMIN') {
      next('/')
      return
    }
    next()
  } else {
    next()
  }
})

export default router
