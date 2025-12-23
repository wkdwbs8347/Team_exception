// router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/components/main/HomeView.vue'
import Test from '@/views/Test.vue'  // import 추가

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView,
  },
  {
    path: '/test',       // /test 주소로 접근 가능
    name: 'test',
    component: Test
  },
]

export default createRouter({
  history: createWebHistory(),
  routes,
})