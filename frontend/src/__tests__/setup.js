import { vi } from 'vitest'

// Mock localStorage
const store = {}
global.localStorage = {
  getItem: vi.fn((key) => store[key] ?? null),
  setItem: vi.fn((key, value) => { store[key] = String(value) }),
  removeItem: vi.fn((key) => { delete store[key] }),
  clear: vi.fn(() => { Object.keys(store).forEach(k => delete store[k]) })
}

// Mock Vant showToast (called by client.js interceptor)
vi.mock('vant', () => ({
  showToast: vi.fn()
}))
