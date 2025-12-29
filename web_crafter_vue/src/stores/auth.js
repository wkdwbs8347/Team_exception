import { defineStore } from 'pinia'
import api from '@/api/axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthed: false,
    me: null, // { id, nickname, ... }
    bootstrapped: false, // 최초 복원 시도 완료 여부
  }),

  getters: {
    nickname: (s) => s.me?.nickname || '',
  },

  actions: {
    async fetchMe() {
      const res = await api.get('/member/me')
      this.me = res.data
      this.isAuthed = true
      return res.data
    },

    // 앱 시작/새로고침 시 "세션 or remember로 복원" 단 한 번 수행
    async bootstrap() {
      if (this.bootstrapped) return this.isAuthed

      try {
        await this.fetchMe()
      } catch (e) {
        if (e?.response?.status === 401) {
          // remember 쿠키가 있다면 세션 재발급 시도
          try {
            await api.post('/member/refresh')
            await this.fetchMe()
          } catch {
            this.isAuthed = false
            this.me = null
          }
        } else {
          this.isAuthed = false
          this.me = null
        }
      } finally {
        this.bootstrapped = true
      }

      return this.isAuthed
    },

    // 로그인: 성공하면 즉시 NavBar 반영 (새로고침 필요 X)
    async login({ email, password, rememberMe }) {
      await api.post('/member/login', { email, password, rememberMe })
      // 서버가 login에서 remember(자동로그인) 처리까지
      await this.fetchMe()
    },

    // 로그아웃: 서버에 요청 후 상태 초기화
    async logout() {
      await api.post('/member/logout')
      this.isAuthed = false
      this.me = null
      // bootstrapped는 유지해도 되고 false로 돌려도 됨(선택)
      // this.bootstrapped = false
    },
  },
})
