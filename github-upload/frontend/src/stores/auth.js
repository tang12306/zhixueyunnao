import { defineStore } from 'pinia';
import { authAPI } from '@/api';

// 登录状态以后端会话为准：页面加载后先调用 /api/auth/me 确认，不再在 localStorage 里存标记
export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null, // { id, username, name, email, role }
    loaded: false // 是否已向后端确认过登录状态
  }),
  getters: {
    isLoggedIn: (state) => !!state.user,
    isAdmin: (state) => !!state.user && state.user.role === 'ADMIN'
  },
  actions: {
    async fetchMe() {
      try {
        const res = await authAPI.me();
        this.user = res && res.success ? res.user : null;
      } catch (error) {
        this.user = null; // 401（未登录）或后端不可用
      } finally {
        this.loaded = true;
      }
      return this.user;
    },
    async login(username, password) {
      // 失败时抛出 axios 错误，error.response.data.message 是后端给出的提示
      const res = await authAPI.login({ username, password });
      this.user = res.user;
      this.loaded = true;
      return this.user;
    },
    async logout() {
      try {
        await authAPI.logout();
      } catch (error) {
        // 会话可能已经过期，忽略
      }
      this.clear();
    },
    clear() {
      this.user = null;
      this.loaded = true;
    }
  }
});

// 只接受站内路径作为登录后的跳转目标，避免被构造成跳到外站的链接
export function safeRedirect(target) {
  return typeof target === 'string' && target.startsWith('/') && !target.startsWith('//') ? target : '/';
}
