<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import GlobalModal from '@/modal/GlobalModal.vue'
import { useAuthStore } from '@/stores/auth'

defineProps({ scrollY: Number })

const router = useRouter()
const auth = useAuthStore()

const isMenuOpen = ref(false)
const toggleMenu = () => (isMenuOpen.value = !isMenuOpen.value)

// 전역모달
const modal = ref({ open: false, message: '', type: 'info', onConfirm: null })
const openModal = (message, type = 'info', onConfirm = null) => {
  modal.value.open = true
  modal.value.message = message
  modal.value.type = type
  modal.value.onConfirm = onConfirm
}
const closeModal = () => {
  modal.value.open = false
  const fn = modal.value.onConfirm
  modal.value.onConfirm = null
  fn?.()
}

/* 엔터키로 모달 끄기 */
const handleKeydown = (e) => {
  if (!modal.value.open) return
  if (e.key === 'Enter') {
    e.preventDefault()
    closeModal()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const handleLogout = async () => {
  try {
    await auth.logout()
    isMenuOpen.value = false
    openModal('로그아웃 되었습니다.', 'success', () => router.push('/'))
  } catch (e) {
    openModal(e?.response?.data?.message || '로그아웃 실패', 'error')
  }
}

const handleIdeClick = () => {
  isMenuOpen.value = false

  // 로그인 안 된 상태
  if (!auth.isAuthed) {
    openModal('로그인이 필요한 기능입니다.', 'warning', () => {
      router.push('/login')
    })
    return
  }

  // 로그인 되어 있으면 정상 이동
  router.push('/ide')
}
</script>

<template>
  <nav class="navbar" :class="{ scrolled: scrollY > 50 }">
    <div class="navbar-container">
      <div class="navbar-brand">
        <span class="logo-icon">✨</span>
        <span class="logo-text">
          <RouterLink to="/" @click="isMenuOpen = false"
            >Web Crafter</RouterLink
          >
        </span>
      </div>

      <button
        class="menu-toggle"
        @click="toggleMenu"
        :class="{ active: isMenuOpen }"
      >
        <span></span><span></span><span></span>
      </button>

      <ul class="nav-menu" :class="{ active: isMenuOpen }">
        <li>
          <RouterLink to="/ide" @click.prevent="handleIdeClick"
            >웹 만들기</RouterLink
          >
        </li>

        <!-- 로그인 상태에 따라 메뉴 토글 -->
        <template v-if="!auth.isAuthed">
          <li>
            <RouterLink to="/login" @click="isMenuOpen = false"
              >로그인</RouterLink
            >
          </li>
          <li>
            <RouterLink to="/register" @click="isMenuOpen = false"
              >회원가입</RouterLink
            >
          </li>
        </template>

        <template v-else>
          <li>
            <button class="nav-btn" type="button" @click="handleLogout">
              로그아웃
            </button>
          </li>
        </template>
      </ul>
    </div>
  </nav>

  <GlobalModal
    :open="modal.open"
    :message="modal.message"
    :type="modal.type"
    @confirm="closeModal"
  />
</template>

<style scoped>
.nav-btn {
  background: none;
  border: none;
  color: #e0e0e0;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  padding-bottom: 0.5rem;
}

.nav-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.nav-btn:hover:not(:disabled) {
  color: #00d4ff;
}

.nav-btn::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
  transition: width 0.3s ease;
}

.nav-btn:hover:not(:disabled)::after {
  width: 100%;
}

.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(15, 15, 30, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
  transition: all 0.3s ease;
}

.navbar.scrolled {
  background: rgba(15, 15, 30, 0.95);
  box-shadow: 0 4px 20px rgba(0, 212, 255, 0.1);
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.5rem;
  font-weight: bold;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-icon {
  font-size: 2rem;
}

.logo-text {
  letter-spacing: 1px;
}

.nav-menu {
  display: flex;
  list-style: none;
  gap: 2rem;
}

.nav-menu li a {
  color: #e0e0e0;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  padding-bottom: 0.5rem;
}

.nav-menu li a::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
  transition: width 0.3s ease;
}

.nav-menu li a:hover {
  color: #00d4ff;
}

.nav-menu li a:hover::after {
  width: 100%;
}

.menu-toggle {
  display: none;
  flex-direction: column;
  background: none;
  border: none;
  cursor: pointer;
  gap: 0.4rem;
}

.menu-toggle span {
  width: 25px;
  height: 3px;
  background: #00d4ff;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.menu-toggle.active span:nth-child(1) {
  transform: rotate(45deg) translate(8px, 8px);
}

.menu-toggle.active span:nth-child(2) {
  opacity: 0;
}

.menu-toggle.active span:nth-child(3) {
  transform: rotate(-45deg) translate(7px, -7px);
}

@media (max-width: 768px) {
  .menu-toggle {
    display: flex;
  }

  .nav-menu {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    background: rgba(15, 15, 30, 0.95);
    backdrop-filter: blur(10px);
    gap: 0;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.3s ease;
    border-bottom: 1px solid rgba(0, 212, 255, 0.1);
  }

  .nav-menu.active {
    max-height: 300px;
  }

  .nav-menu li {
    padding: 1rem 2rem;
    border-bottom: 1px solid rgba(0, 212, 255, 0.05);
  }

  .nav-menu li a::after {
    display: none;
  }
}
</style>
