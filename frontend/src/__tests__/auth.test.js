import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'

describe('Auth Store', () => {
  beforeEach(() => {
    // Fresh Pinia instance for each test
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('starts with empty state and is not logged in', () => {
    const auth = useAuthStore()
    expect(auth.token).toBe('')
    expect(auth.userId).toBe('')
    expect(auth.name).toBe('')
    expect(auth.role).toBe('')
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.isAdmin).toBe(false)
  })

  it('setAuth persists to store and localStorage', () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 'jwt-abc', userId: 42, name: 'Alice', role: 'ADMIN' })

    expect(auth.token).toBe('jwt-abc')
    expect(auth.userId).toBe('42')
    expect(auth.name).toBe('Alice')
    expect(auth.role).toBe('ADMIN')
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.isAdmin).toBe(true)

    expect(localStorage.getItem('token')).toBe('jwt-abc')
    expect(localStorage.getItem('userId')).toBe('42')
    expect(localStorage.getItem('name')).toBe('Alice')
    expect(localStorage.getItem('role')).toBe('ADMIN')
  })

  it('logout clears store and localStorage', () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 'jwt-abc', userId: 1, name: 'Bob', role: 'USER' })
    auth.logout()

    expect(auth.token).toBe('')
    expect(auth.userId).toBe('')
    expect(auth.name).toBe('')
    expect(auth.role).toBe('')
    expect(auth.isLoggedIn).toBe(false)
    expect(auth.isAdmin).toBe(false)
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('isAdmin is false for USER role', () => {
    const auth = useAuthStore()
    auth.setAuth({ token: 'jwt', userId: 1, name: 'C', role: 'USER' })
    expect(auth.isLoggedIn).toBe(true)
    expect(auth.isAdmin).toBe(false)
  })

  it('reads token from localStorage on init', () => {
    localStorage.setItem('token', 'existing-token')
    localStorage.setItem('userId', '99')
    localStorage.setItem('name', 'Preloaded')
    localStorage.setItem('role', 'USER')

    const auth = useAuthStore()
    expect(auth.token).toBe('existing-token')
    expect(auth.userId).toBe('99')
    expect(auth.isLoggedIn).toBe(true)
  })
})
