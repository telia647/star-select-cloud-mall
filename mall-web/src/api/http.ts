import axios, { AxiosError } from 'axios'
import type { ApiResult } from '@/types/api'
import { useAuthStore } from '@/stores/auth'

export const http = axios.create({
  baseURL: '/api',
  timeout: 12000
})

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.accessToken) {
    config.headers.Authorization = `Bearer ${authStore.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult<unknown>
    if (result && typeof result === 'object' && 'success' in result) {
      if (!result.success) {
        return Promise.reject(new Error(result.message || '请求失败'))
      }
      return result.data
    }
    return response.data
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      return Promise.reject(new Error('登录状态已过期，请重新登录'))
    }
    return Promise.reject(new Error(error.response?.data?.message || error.message || '网络请求失败'))
  }
)
