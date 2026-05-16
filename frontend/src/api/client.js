import axios from 'axios'
import { showToast } from 'vant'

/**
 * Pre-configured Axios instance.
 *
 * Request interceptor: attaches the JWT Bearer token read from localStorage.
 * Response interceptor: unwraps the ApiResult envelope and shows Vant toasts on errors.
 * On 401 the local authentication state is cleared and the user is redirected to /login.
 */
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE,
  timeout: 10000
})

// Attach JWT token stored in localStorage to every outgoing request
client.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Unwrap ApiResult.data, show Vant toast on business errors, redirect on 401
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
        localStorage.clear()
        window.location.href = '/login'
      }
    } else {
      showToast('无法连接到服务器')
    }
    return Promise.reject(error)
  }
)

export default client
