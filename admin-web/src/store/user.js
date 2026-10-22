import { defineStore } from 'pinia'
import { login as loginApi, getProfile } from '../api/auth'

/**
 * 管理员登录态：token 持久化在 localStorage
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    profile: null
  }),
  actions: {
    async login(payload) {
      const data = await loginApi(payload)
      this.token = data.token
      localStorage.setItem('admin_token', data.token)
      this.profile = data
      return data
    },
    async loadProfile() {
      this.profile = await getProfile()
      return this.profile
    },
    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('admin_token')
    }
  }
})
