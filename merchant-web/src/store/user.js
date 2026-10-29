import { defineStore } from 'pinia'
import { merchantLogin } from '../api/auth'

const TOKEN_KEY = 'merchant_token'
const USER_KEY = 'merchant_user'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

/**
 * 商家登录态：token 与用户信息持久化在 localStorage
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: readUser()
  }),
  actions: {
    async login(payload) {
      const data = await merchantLogin(payload)
      this.token = data.token
      this.profile = data
      localStorage.setItem(TOKEN_KEY, data.token)
      localStorage.setItem(USER_KEY, JSON.stringify(data))
      return data
    },
    setProfile(profile) {
      this.profile = profile
      localStorage.setItem(USER_KEY, JSON.stringify(profile))
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
