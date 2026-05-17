import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

/**
 * Pinia store that holds the current user's authentication state.
 * Mirrored to localStorage so the token survives page reloads.
 * <p>
 * Usage in a component:
 * <pre>
 * const auth = useAuthStore()
 * auth.setAuth({ token, userId, name, role })   // after login
 * auth.logout()                                   // clear all state
 * auth.isLoggedIn / auth.isAdmin                  // computed flags
 * </pre>
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userId = ref(localStorage.getItem('userId') || '')
  const name = ref(localStorage.getItem('name') || '')
  const role = ref(localStorage.getItem('role') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  /** Persist the login response into the store and localStorage. */
  function setAuth(data) {
    token.value = data.token
    userId.value = String(data.userId)
    name.value = data.name
    role.value = data.role
    avatar.value = data.avatar || ''
    localStorage.setItem('token', data.token)
    localStorage.setItem('userId', String(data.userId))
    localStorage.setItem('name', data.name)
    localStorage.setItem('role', data.role)
    if (data.avatar) localStorage.setItem('avatar', data.avatar)
  }

  /** Clear all auth state from memory and localStorage. */
  function logout() {
    token.value = ''
    userId.value = ''
    name.value = ''
    role.value = ''
    avatar.value = ''
    localStorage.clear()
  }

  return { token, userId, name, role, avatar, isLoggedIn, isAdmin, setAuth, logout }
})
