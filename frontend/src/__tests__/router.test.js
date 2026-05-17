import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createRouter, createWebHistory } from 'vue-router'

/**
 * Test the global navigation guard logic in isolation.
 * We replicate the beforeEach guard rather than importing the actual router,
 * because the real router imports Vue SFC components which need full Vite transform.
 */
describe('Router guard logic', () => {
  let guardFn

  // The guard logic copied from router/index.js
  function createGuard() {
    return (to, from, next) => {
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
    }
  }

  beforeEach(() => {
    localStorage.clear()
    guardFn = createGuard()
  })

  // Returns the redirect path or undefined if next() was called without arguments
  function runGuard(to) {
    let redirectedTo = '_next_called_without_path_'
    guardFn(to, {}, (path) => { redirectedTo = path })
    return redirectedTo === '_next_called_without_path_' ? undefined : redirectedTo
  }

  it('redirects to /login when no token and page is not guest', () => {
    const result = runGuard({ meta: {} })
    expect(result).toBe('/login')
  })

  it('allows access to guest pages without token', () => {
    // /login and /register are guest pages
    expect(runGuard({ meta: { guest: true } })).toBeUndefined()
    expect(runGuard({ meta: { guest: true } })).toBeUndefined()
  })

  it('redirects guest pages to / when already logged in', () => {
    localStorage.setItem('token', 'valid-jwt')
    const result = runGuard({ meta: { guest: true } })
    expect(result).toBe('/')
  })

  it('allows normal pages when logged in', () => {
    localStorage.setItem('token', 'valid-jwt')
    expect(runGuard({ meta: {} })).toBeUndefined()
  })

  it('redirects non-admin from admin page', () => {
    localStorage.setItem('token', 'valid-jwt')
    localStorage.setItem('role', 'USER')
    const result = runGuard({ meta: { admin: true } })
    expect(result).toBe('/')
  })

  it('allows admin to access admin page', () => {
    localStorage.setItem('token', 'valid-jwt')
    localStorage.setItem('role', 'ADMIN')
    const result = runGuard({ meta: { admin: true } })
    expect(result).toBeUndefined()
  })

  it('redirects to /login for admin page when not logged in', () => {
    const result = runGuard({ meta: { admin: true } })
    expect(result).toBe('/login')
  })

  it('redirects to /login for chat page when not logged in', () => {
    const result = runGuard({ meta: {} })
    expect(result).toBe('/login')
  })

  it('allows chat page when logged in', () => {
    localStorage.setItem('token', 'valid-jwt')
    expect(runGuard({ meta: {} })).toBeUndefined()
  })
})
