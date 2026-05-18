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
      showToast(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return body.data
  },
  error => {
    if (error.response) {
      const body = error.response.data
      showToast(body?.msg || '网络错误')
      if (error.response.status === 401) {
        const AUTH_KEYS = ['token', 'userId', 'name', 'role', 'avatar']
        AUTH_KEYS.forEach(k => localStorage.removeItem(k))
        import('@/router').then(m => m.default.push('/login'))
      }
    } else {
      showToast('无法连接到服务器')
    }
    return Promise.reject(error)
  }
)

export default client
