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
  const wornBadgeKey = ref(localStorage.getItem('wornBadgeKey') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  const AUTH_KEYS = ['token', 'userId', 'name', 'role', 'avatar', 'wornBadgeKey']

  /** Persist the login response into the store and localStorage. */
  function setAuth(data) {
    if (!data) return
    token.value = data.token || ''
    userId.value = data.userId != null ? String(data.userId) : ''
    name.value = data.name || ''
    role.value = data.role || ''
    avatar.value = data.avatar || ''
    wornBadgeKey.value = data.wornBadgeKey || ''
    localStorage.setItem('token', token.value)
    localStorage.setItem('userId', userId.value)
    localStorage.setItem('name', name.value)
    localStorage.setItem('role', role.value)
    localStorage.setItem('avatar', avatar.value)
    localStorage.setItem('wornBadgeKey', wornBadgeKey.value)
  }

  /** Set the worn badge key. Called from Profile.vue when wear/unwear. */
  function setWornBadgeKey(key) {
    wornBadgeKey.value = key || ''
    localStorage.setItem('wornBadgeKey', wornBadgeKey.value)
  }

  /** Clear all auth state from memory and localStorage. */
  function logout() {
    token.value = ''
    userId.value = ''
    name.value = ''
    role.value = ''
    avatar.value = ''
    wornBadgeKey.value = ''
    AUTH_KEYS.forEach(k => localStorage.removeItem(k))
  }

  return { token, userId, name, role, avatar, wornBadgeKey, isLoggedIn, isAdmin, setAuth, setWornBadgeKey, logout }
})
