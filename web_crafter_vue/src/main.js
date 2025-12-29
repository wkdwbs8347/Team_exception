import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import './assets/tailwind.css' // ✅ tailwind 추가

createApp(App).use(createPinia()).use(router).mount('#app')