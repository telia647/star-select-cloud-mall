import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { LoginResponse, UserResponse } from '@/types/api'
import { getMe, login as loginApi } from '@/api/mall'

const ACCESS_TOKEN_KEY = 'mall_access_token'
const REFRESH_TOKEN_KEY = 'mall_refresh_token'

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem(ACCESS_TOKEN_KEY) || '')
  const refreshToken = ref(localStorage.getItem(REFRESH_TOKEN_KEY) || '')
  const user = ref<UserResponse | null>(null)

  const isLoggedIn = computed(() => Boolean(accessToken.value))
  const isAdmin = computed(() => user.value?.roleCode === 'ADMIN')

  async function login(username: string, password: string) {
    const response = await loginApi({ username, password })
    setTokens(response)
    await loadMe()
  }

  async function loadMe() {
    if (!accessToken.value) {
      return
    }
    user.value = await getMe()
  }

  function setTokens(response: LoginResponse) {
    accessToken.value = response.accessToken
    refreshToken.value = response.refreshToken
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, response.refreshToken)
  }

  function logout() {
    accessToken.value = ''
    refreshToken.value = ''
    user.value = null
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  }

  return {
    accessToken,
    refreshToken,
    user,
    isLoggedIn,
    isAdmin,
    login,
    loadMe,
    logout
  }
})
