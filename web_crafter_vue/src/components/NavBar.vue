<script setup>
import api from '@/api/axios';
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import { useRouter } from 'vue-router';
import GlobalModal from '@/modal/GlobalModal.vue';
import { useAuthStore } from '@/stores/auth';
import { Blocks } from 'lucide-vue-next';


defineProps({ scrollY: Number });

const router = useRouter();
const auth = useAuthStore();

/* ✅ 프로필 카드 클릭 → 마이페이지 이동 */
const handleProfileCardClick = () => {
  closeMenu();

  // 비로그인 상태


  // 로그인 상태
  router.push('/mypage');
};

const isMenuOpen = ref(false);
const toggleMenu = () => (isMenuOpen.value = !isMenuOpen.value);

// 전역모달
const modal = ref({ open: false, message: '', type: 'info', onConfirm: null });
const openModal = (message, type = 'info', onConfirm = null) => {
  modal.value.open = true;
  modal.value.message = message;
  modal.value.type = type;
  modal.value.onConfirm = onConfirm;
};
const closeModal = () => {
  modal.value.open = false;
  const fn = modal.value.onConfirm;
  modal.value.onConfirm = null;
  fn?.();
};

/* 엔터키로 모달 끄기 */
const handleKeydown = (e) => {
  if (!modal.value.open) return;
  if (e.key === 'esc') {
    e.preventDefault();
    closeModal();
  }
};

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown);

  // 이미 정보가 있다면 API 호출 스킵 (선택 사항)
  // if (auth.me) return; 

  try {
    // 📡 서버에 "나 누구야?" 라고 물어봅니다.
    const response = await api.get('/member/me');
    const data = response.data; // { member: {...}, ... }

    // 💾 받아온 정보를 Pinia 스토어의 'me'에 저장합니다.
    // (NavBar 템플릿에서 auth.me를 쓰고 있으므로 여기에 넣어야 합니다)
    auth.me = data.member;
    
    // 로그인 상태 true로 변경 (스토어 로직에 따라 다를 수 있음)
    auth.isAuthed = true; 

    console.log("NavBar: 사용자 정보 로드 완료", auth.me);
  } catch (error) {
    // 401 에러(비로그인)는 자연스러운 현상이므로 조용히 넘어갑니다.
    // 로그인 안 한 사람은 guest 상태로 남습니다.
    if (error.response?.status !== 401) {
        console.error("사용자 정보 로드 실패:", error);
    }
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});

const handleLogout = async () => {
  try {
    await auth.logout();
    isMenuOpen.value = false;
    openModal('로그아웃 되었습니다.', 'success', () => router.push('/'));
  } catch (e) {
    openModal(e?.response?.data?.message || '로그아웃 실패', 'error');
  }
};

const createNewProject = async () => {
  try {
    const response = await api.post('/projects/create'); 
    const newWebId = response.data;

    // 2. ✅ 닉네임 가져오기 수정 (auth.user -> auth.me)
    // 위 onMounted에서 auth.me에 데이터를 넣었으므로 여기서도 me를 써야 합니다.
    const nickname = auth.me?.nickname || 'guest';

    router.push(`/ide/${nickname}/${newWebId}`);

  } catch (error) {
    console.error("새 프로젝트 생성 실패:", error);
    if (error.response?.status === 401 || error.response?.status === 403) {
      openModal('로그인이 필요한 서비스입니다.', 'warning', () => {
        router.push('/login');
      });
    } else {
      openModal("프로젝트 생성 중 오류가 발생했습니다.", "error");
    }
  }
};


/* ✅ 프로필 영역 표시용 (auth.me 기반) */
const userName = computed(
  () => auth.me?.nickname || auth.me?.name || auth.me?.email || '사용자'
);
const userSub = computed(() => auth.me?.email || '로그인 상태');
const userInitial = computed(() => {
  const t = (userName.value || 'U').trim();
  return t[0]?.toUpperCase() || 'U';
});

/* ✅ (선택) 메뉴 닫기 유틸 */
const closeMenu = () => (isMenuOpen.value = false);
</script>

<template>
  <nav class="navbar" :class="{ scrolled: scrollY > 50 }">
    <div class="navbar-container">
      <div class="navbar-brand">
        <span class="logo-icon"><Blocks :size="28" /></span>
        <span class="logo-text">
          <RouterLink to="/" @click="closeMenu">Web Crafter</RouterLink>
        </span>
      </div>

      <button
        class="menu-toggle"
        @click="toggleMenu"
        :class="{ active: isMenuOpen }"
      >
        <span></span><span></span><span></span>
      </button>

      <!-- ✅ Service-grade Drawer -->
      <ul class="nav-menu" :class="{ active: isMenuOpen }">
        <!-- 헤더: 왼쪽 브랜드 + 오른쪽 회원가입(비로그인만) -->
        <li class="drawer-head">
          <div class="drawer-brand">
            <span class="drawer-title">Web Crafter</span>
            <span class="drawer-sub">Workspace</span>
          </div>

          <RouterLink
            v-if="!auth.isAuthed"
            class="head-action"
            to="/register"
            @click="closeMenu"
          >
            회원가입
          </RouterLink>
        </li>

        <!-- 메뉴 섹션 -->
        <li class="drawer-section">
          <button
            class="drawer-item"
            @click.prevent="createNewProject"
          >
            <span class="drawer-dot"></span>
            <span class="drawer-text">웹 만들기</span>
            <span class="drawer-chevron">›</span>
          </button>
        </li>

        <li class="drawer-divider"></li>

        <!-- 하단 프로필 + 오른쪽 로그인/로그아웃 버튼만 -->
        <li class="drawer-footer">
          <div class="profile-card" @click="handleProfileCardClick">
            <div class="profile-left">
              <!-- ✅ 프로필 이미지 영역 -->
              <div class="avatar">
                <!-- 로그인 상태 + 이미지 있을 때 -->
                <img
                  v-if="auth.isAuthed && auth.me?.profileImage"
                  :src="auth.me.profileImage"
                  alt="profile"
                />

                <!-- 로그인 상태 + 이미지 없을 때 -->
                <span v-else-if="auth.isAuthed">
                  {{ userInitial }}
                </span>

                <!-- 비로그인 상태 -->
                <svg v-else class="avatar-icon" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M12 12c2.761 0 5-2.239 5-5s-2.239-5-5-5-5 2.239-5 5 2.239 5 5 5zM4 20c0-3.314 3.582-6 8-6s8 2.686 8 6"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </div>

              <!-- ✅ 텍스트 영역 -->
              <div class="profile-meta" v-if="auth.isAuthed">
                <div class="profile-name" :title="userName">{{ userName }}</div>
                <div class="profile-sub" :title="userSub">{{ userSub }}</div>
              </div>

              <!-- ❌ 비로그인 상태는 ellipsis 없는 안내 문구 -->
              <div class="profile-meta guest" v-else>
                <div class="profile-guest-title">로그아웃 상태</div>
                <div class="profile-guest-desc">
                  로그인을 진행해주세요
                </div>
              </div>
            </div>

            <div class="profile-actions">
              <button
                v-if="!auth.isAuthed"
                class="profile-btn"
                type="button"
                @click.stop="
                  closeMenu();
                  router.push('/login');
                "
              >
                로그인
              </button>

              <button
                v-else
                class="profile-btn danger"
                type.stop="button"
                @click="handleLogout"
              >
                로그아웃
              </button>
            </div>
          </div>
        </li>
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
/* =========================
   Top Navbar (Stable)
========================= */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1200;
  background: rgba(26, 26, 46, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.12);
  transition:
    background 0.25s ease,
    box-shadow 0.25s ease;
}

.navbar.scrolled {
  background: rgba(26, 26, 46, 0.96);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.28);
}

.navbar-container {
  position: relative;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  font-size: 1.5rem;
  font-weight: 900;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-icon {
  font-size: 2rem;
}

.logo-text {
  letter-spacing: 0.06em;
}

/* =========================
   Hamburger
========================= */
.menu-toggle {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  gap: 0.4rem;
  height: 44px;
  width: 44px;
  padding: 0;
  border-radius: 12px;
  transition: background 0.18s ease;
}

.menu-toggle:hover {
  background: rgba(0, 212, 255, 0.08);
}

.menu-toggle span {
  width: 25px;
  height: 3px;
  background: #00d4ff;
  border-radius: 2px;
  transition:
    transform 0.28s ease,
    opacity 0.2s ease;
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

/* =========================
   Drawer (Service-grade)
========================= */
.nav-menu {
  position: absolute;
  right: 14px;
  top: calc(100% + 10px);

  width: 332px;
  height: calc(100vh - 92px);
  padding: 14px;

  display: flex;
  flex-direction: column;

  border-radius: 20px;

  background: linear-gradient(
    180deg,
    rgba(10, 20, 44, 0.96),
    rgba(8, 16, 36, 0.96)
  );
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.09);

  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);

  transform: translateX(110%);
  opacity: 0.92;
  transition:
    transform 0.28s ease,
    opacity 0.28s ease;
  z-index: 1100;

  overflow: hidden;
}

.nav-menu.active {
  transform: translateX(0);
  opacity: 1;
}

/* top glow line */
.nav-menu::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10px;
  right: 10px;
  height: 1px;
  background: linear-gradient(
    90deg,
    rgba(0, 212, 255, 0),
    rgba(0, 212, 255, 0.35),
    rgba(0, 212, 255, 0)
  );
}

/* Header */
.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  padding: 12px 12px 14px;
  margin: 0 0 10px;

  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.drawer-brand {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.drawer-title {
  font-size: 1.08rem;
  font-weight: 900;
  letter-spacing: 0.09em;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
  white-space: nowrap;
}

.drawer-sub {
  font-size: 0.75rem;
  letter-spacing: 0.12em;
  color: rgba(224, 224, 224, 0.62);
  text-transform: uppercase;
}

/* Header right action (Sign up) */
.head-action {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;

  padding: 9px 12px;
  border-radius: 12px;

  text-decoration: none;
  font-weight: 900;
  font-size: 0.82rem;
  letter-spacing: 0.02em;

  color: rgba(224, 224, 224, 0.96);
  border: 1px solid rgba(0, 212, 255, 0.24);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.16),
    rgba(255, 255, 255, 0.03)
  );

  box-shadow:
    0 14px 30px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);

  transition:
    transform 0.16s ease,
    border-color 0.16s ease,
    background 0.16s ease;
  -webkit-tap-highlight-color: transparent;
}

.head-action:hover {
  transform: translateY(-1px);
  border-color: rgba(0, 212, 255, 0.38);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.22),
    rgba(255, 255, 255, 0.03)
  );
}

.head-action:active {
  transform: translateY(0);
}

/* Section */
.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 6px 6px;
}

.drawer-divider {
  height: 1px;
  margin: 6px 10px 8px;
  background: linear-gradient(
    90deg,
    rgba(255, 255, 255, 0.04),
    rgba(255, 255, 255, 0.1),
    rgba(255, 255, 255, 0.04)
  );
}

/* Item card */
.drawer-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;

  width: 100%;
  padding: 13px 12px;
  border-radius: 16px;

  text-decoration: none;
  color: rgba(224, 224, 224, 0.94);
  font-weight: 850;

  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.06),
    rgba(255, 255, 255, 0.03)
  );
  border: 1px solid rgba(255, 255, 255, 0.09);

  box-shadow:
    0 12px 26px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);

  transform: translateY(0);
  transition:
    transform 0.16s ease,
    background 0.16s ease,
    border-color 0.16s ease,
    box-shadow 0.16s ease;
}

/* left accent bar */
.drawer-item::before {
  content: '';
  position: absolute;
  left: 10px;
  top: 10px;
  bottom: 10px;
  width: 3px;
  border-radius: 999px;
  background: rgba(0, 212, 255, 0);
  opacity: 0;
  transition:
    opacity 0.16s ease,
    background 0.16s ease;
}

.drawer-item:hover {
  transform: translateY(-2px);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.14),
    rgba(255, 255, 255, 0.03)
  );
  border-color: rgba(0, 212, 255, 0.22);
  box-shadow:
    0 18px 36px rgba(0, 0, 0, 0.36),
    0 0 0 1px rgba(0, 212, 255, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.07);
}

.drawer-item:hover::before {
  opacity: 1;
  background: rgba(0, 212, 255, 0.75);
}

.drawer-item.router-link-active,
.drawer-item.router-link-exact-active {
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.18),
    rgba(255, 255, 255, 0.03)
  );
  border-color: rgba(0, 212, 255, 0.34);
}

.drawer-item.router-link-active::before,
.drawer-item.router-link-exact-active::before {
  opacity: 1;
  background: rgba(0, 212, 255, 0.9);
}

.drawer-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(0, 212, 255, 0.55);
  box-shadow: 0 0 0 4px rgba(0, 212, 255, 0.12);
}

.drawer-text {
  flex: 1;
}

.drawer-chevron {
  opacity: 0.65;
  transform: translateX(0);
  transition:
    transform 0.16s ease,
    opacity 0.16s ease;
}

.drawer-item:hover .drawer-chevron {
  opacity: 1;
  transform: translateX(4px);
}

/* =========================
   Profile Footer
========================= */
.drawer-footer {
  margin-top: auto;
  padding: 10px 8px 10px;
}

.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  padding: 12px 12px;
  border-radius: 18px;

  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.06),
    rgba(255, 255, 255, 0.03)
  );
  border: 1px solid rgba(255, 255, 255, 0.09);

  box-shadow:
    0 16px 34px rgba(0, 0, 0, 0.4),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
}

.profile-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1; /* ✅ ellipsis 안정 */
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: grid;
  place-items: center;

  background: rgba(0, 212, 255, 0.14);
  border: 1px solid rgba(0, 212, 255, 0.22);
  color: rgba(224, 224, 224, 0.95);
  font-weight: 950;
  flex-shrink: 0;
}

.profile-meta {
  min-width: 0; /* ✅ ellipsis 필수 */
  flex: 1;
}

.profile-name {
  font-size: 0.94rem;
  font-weight: 900;
  color: rgba(224, 224, 224, 0.96);

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-sub {
  margin-top: 2px;
  font-size: 0.74rem;
  color: rgba(224, 224, 224, 0.6);

  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* Buttons */
.profile-btn {
  padding: 9px 12px;
  border-radius: 12px;

  border: 1px solid rgba(0, 212, 255, 0.22);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.16),
    rgba(255, 255, 255, 0.03)
  );
  color: rgba(224, 224, 224, 0.96);

  font-weight: 900;
  cursor: pointer;

  transition:
    transform 0.16s ease,
    border-color 0.16s ease,
    background 0.16s ease;
  -webkit-tap-highlight-color: transparent;
}

.profile-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(0, 212, 255, 0.36);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.22),
    rgba(255, 255, 255, 0.03)
  );
}

.profile-btn:active {
  transform: translateY(0);
}

.profile-btn.danger {
  border-color: rgba(255, 90, 90, 0.35);
  background: linear-gradient(
    180deg,
    rgba(255, 90, 90, 0.16),
    rgba(255, 255, 255, 0.03)
  );
}

.profile-btn.danger:hover {
  border-color: rgba(255, 90, 90, 0.48);
  background: linear-gradient(
    180deg,
    rgba(255, 90, 90, 0.22),
    rgba(255, 255, 255, 0.03)
  );
}

/* ✅ 링크 밑줄/텍스트 hover 충돌 제거 */
.nav-menu li a::after {
  display: none !important;
}
.nav-menu li a:hover {
  color: rgba(224, 224, 224, 0.94) !important;
}

/* =========================
   Mobile
========================= */
@media (max-width: 768px) {
  .navbar-container {
    padding: 1rem 1.2rem;
  }

  .nav-menu {
    right: 10px;
    left: 10px;
    width: auto;
    height: calc(100vh - 92px);
  }
}

/* =========================
   Avatar (Image Slot)
========================= */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  flex-shrink: 0;

  display: grid;
  place-items: center;

  background: rgba(0, 212, 255, 0.14);
  border: 1px solid rgba(0, 212, 255, 0.22);
  overflow: hidden;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar span {
  color: rgba(224, 224, 224, 0.95);
  font-weight: 900;
}

.avatar-icon {
  width: 22px;
  height: 22px;
  color: rgba(224, 224, 224, 0.85);
}

/* =========================
   Profile Text
========================= */

/* 로그인 상태 → ellipsis 허용 */
.profile-meta {
  min-width: 0;
  flex: 1;
}

.profile-name,
.profile-sub {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ❌ 비로그인 상태 → ellipsis 제거 */
.profile-meta.guest {
  flex: 1;
}

.profile-meta.guest .profile-guest-title,
.profile-meta.guest .profile-guest-desc {
  white-space: normal;
  overflow: visible;
  text-overflow: unset;
  line-height: 1.3;
}

.profile-guest-title {
  font-size: 0.9rem;
  font-weight: 800;
  color: rgba(224, 224, 224, 0.95);
}

.profile-guest-desc {
  margin-top: 2px;
  font-size: 0.74rem;
  color: rgba(224, 224, 224, 0.65);
}

.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 12px;
  border-radius: 18px;
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.06),
    rgba(255, 255, 255, 0.03)
  );
  border: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 0 16px 34px rgba(0, 0, 0, 0.4), inset 0 1px 0 rgba(255, 255, 255, 0.06);

  cursor: pointer; /* 클릭 가능한 느낌 */
  transition: transform 0.12s ease, box-shadow 0.12s ease, background 0.12s ease;
}

/* 마우스 올렸을 때 */
.profile-card:hover {
  transform: translateY(-2px); /* 살짝 떠오르듯 */
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.5);
  background: linear-gradient(
    180deg,
    rgba(0, 212, 255, 0.08),
    rgba(255, 255, 255, 0.04)
  );
}

/* 클릭했을 때 */
.profile-card:active {
  transform: scale(0.97); /* 눌린 느낌 */
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

</style>
