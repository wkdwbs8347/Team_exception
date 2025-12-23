import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import './assets/tailwind.css' // ✅ tailwind 추가

createApp(App).use(router).mount('#app')