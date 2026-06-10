import axios from 'axios'
import { showToast } from 'vant'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 10000
})

client.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  response => {
    const body = response.data
    if (body.code !== 200) {
      // Don't toast here — let each view decide how to handle the error.
      return Promise.reject(new Error(body.msg || '请求失败'))
    }
    return body.data
  },
  error => {
    if (error.response) {
      const body = error.response.data
      if (error.response.status === 401) {
        const AUTH_KEYS = ['token', 'userId', 'name', 'role', 'avatar']
        AUTH_KEYS.forEach(k => localStorage.removeItem(k))
        // Hard redirect to reinitialize the Vue app with empty localStorage,
        // ensuring Pinia auth store and router guard agree on auth state.
        window.location.href = '/login'
        return Promise.reject(error)
      }
      // Business error — let each view decide whether to toast.
      return Promise.reject(new Error(body?.msg || '网络错误'))
    } else {
      showToast('无法连接到服务器')
    }
    return Promise.reject(error)
  }
)

export default client
