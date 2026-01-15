import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import IDEView from '../views/IDEView.vue' // <--- 추가
import LDEView from '../views/LDEView.vue' // <--- 추가


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',  // 홈
      name: 'home',
      component: HomeView
    },
    {
      path: '/login', // 로그인
      name: 'login',
      component: LoginView
    },
    {
      path: '/register',  // 회원가입
      name: 'register',
      component: RegisterView
    },
    {
      path: '/ide', // <--- 추가된 경로
      name: 'ide',
      component: IDEView
    },
    {
      path : '/lde', // <--- 추가된 경로
      name: 'lde',
      component: LDEView
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

export default router
