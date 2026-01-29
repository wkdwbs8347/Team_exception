import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import IDEView from '../views/IDEView.vue' // <--- 추가
import LDEView from '../views/LDEView.vue' // <--- 추가
import MyPageView from '../views/MyPageView.vue'
import ExploreView from '../views/ExploreView.vue' // <--- 추가


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
      path: '/mypage',
      name: 'mypage',
      component: MyPageView,
      // (이 페이지는 인증이 필요하다는 표시)
      meta: { requiresAuth: true }
    },
    {
      // :nickname과 :webId 뒤에 ?를 붙여서 값이 없어도 /ide 주소로 접속 가능하게 합니다.
      path: '/ide/:nickname?/:webId?', 
      name: 'ide',
      component: () => import('../views/IDEView.vue'),
      props: true
    },
    {
      path: '/lde/:nickname?/:webId?', // 👈 LDE도 파라미터를 받을 수 있게 수정 [cite: 2026-01-21]
      name: 'lde',
      component: () => import('../views/LDEView.vue'),
      props: true
    },
    {
      path: '/explore', // 👈 Explore 페이지 라우트 추가 [cite: 2026-01-21
      name: 'explore',
      component: ExploreView
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
