import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    // 文件下载（blob 类型）直接返回 Blob 数据
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        sessionStorage.removeItem('token')
        sessionStorage.removeItem('refreshToken')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res.data
  },
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('token')
      sessionStorage.removeItem('refreshToken')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
