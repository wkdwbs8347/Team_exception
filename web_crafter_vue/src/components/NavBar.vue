<script setup>
import api from '@/api/axios';
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GlobalModal from '@/modal/GlobalModal.vue';
import { useAuthStore } from '@/stores/auth';
import { Blocks, Users, Bell, UserPlus, Compass } from 'lucide-vue-next'; // 👈 [수정] Users 아이콘 추가
import FriendListModal from '@/modal/FriendListModal.vue'; // 👈 [추가] 모달 불러오기


defineProps({ scrollY: Number });

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const isFriendListOpen = ref(false);
const isMenuOpen = ref(false);
const notificationCount = ref(0);

watch(
  () => route.path,
  () => {
    isFriendListOpen.value = false; // 친구 목록 닫기
    isMenuOpen.value = false;       // 사이드 메뉴 닫기
  }
);

/* ✅ 프로필 카드 클릭 → 마이페이지 이동 */
const handleProfileCardClick = () => {
  closeMenu();

  // 비로그인 상태


  // 로그인 상태
  router.push('/mypage');
};


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

  // 1. 내 정보 불러오기 (로그인 유지 확인용)
  try {
    const response = await api.get('/member/me');
    // 순수한 유저 정보만 auth 스토어에 저장
    auth.me = response.data.member;
    auth.isAuthed = true;
  } catch (error) {
    // 로그인 안 된 상태면 여기서 중단 (알림 로드 안 함)
    if (error.response?.status !== 401) {
       console.error("사용자 정보 로드 실패:", error);
    }
    return; 
  }

  // 2. [수정] 알림 개수 따로 불러오기 (로그인 성공 시에만 실행됨)
  try {
    const notiRes = await api.get('/friends/notifications');
    
    // ✅ auth.me를 건드리지 않고, 내 전용 변수에 저장!
    notificationCount.value = notiRes.data.length; 
    
    console.log("알림 개수 로드 완료:", notificationCount.value);
  } catch (notiError) {
    console.warn("알림 로드 실패:", notiError);
    notificationCount.value = 0;
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});

const handleLogout = async () => {
  try {
    await auth.logout();
    isMenuOpen.value = false;
    isFriendListOpen.value = false;
    openModal('로그아웃 되었습니다.', 'success', () => router.push('/'));
  } catch (e) {
    openModal(e?.response?.data?.message || '로그아웃 실패', 'error');
  }
};

/* ✅ [추가] 읽지 않은 알림 개수 계산 */
const unreadCount = computed(() => {
  // auth.me가 덮어씌워져도 이 변수는 안전합니다.
  return notificationCount.value;
});

/* ✅ [추가] 알림 버튼 클릭 시 마이페이지 이동 */
const goToNotificationTab = () => {
  if (!auth.isAuthed) {
    openModal('로그인이 필요합니다.', 'warning');
    return;
  }
  // 마이페이지 알림 탭으로 이동
  router.push({ path: '/mypage', query: { tab: 'NOTI' } });
  closeMenu(); // 혹시 메뉴가 열려있으면 닫기
};

/* ✅ [추가] 현재 페이지가 IDE(작업실)인지 확인하는 변수 */
const isIdePage = computed(() => {
  return route.path.startsWith('/ide/');
});

/* ✅ [추가] 초대 모달 열기 (나중에 실제 모달과 연결) */
const friendModalMode = ref('manage');

// 1. 현재 프로젝트의 주인(방장)인지 확인하기 위한 변수
const projectOwnerId = ref(null);

// 2. "내가 방장인가?" 계산 (FriendListModal로 넘겨줄 값)
const isOwner = computed(() => {
  // 로그인 안 했거나, 방장 정보가 아직 없으면 false
  if (!auth.me || !projectOwnerId.value) return false;
  // 내 ID와 방장 ID가 같으면 true
  return auth.me.id === projectOwnerId.value;
});

// 3. 방장 권한 확인 함수
const checkProjectOwner = async () => {
  if (!currentWebId.value) return; // 프로젝트 화면이 아니면 패스

  try {
    // 내 프로젝트 목록을 가져와서 현재 프로젝트에서의 내 역할(Role)을 확인합니다.
    const res = await api.get('/projects/list');
    const thisProject = res.data.find(p => p.id === Number(currentWebId.value));

    if (thisProject && thisProject.role === 'OWNER') {
       projectOwnerId.value = auth.me.id; // 내가 방장임!
    } else {
       projectOwnerId.value = -1; // 방장 아님
    }
  } catch (e) {
    console.error("권한 확인 실패:", e);
    projectOwnerId.value = -1;
  }
};

// [추가] 현재 프로젝트 ID (URL 파라미터에서 추출)
const currentWebId = computed(() => route.params.webId);

// ✅ [추가] 사이드바 '친구 목록' 클릭 시 실행할 함수 (이게 없어서 안 눌렸던 겁니다!)
const openFriendManage = () => {
  friendModalMode.value = 'manage'; // 관리 모드로 설정
  isFriendListOpen.value = true;    // 모달 열기
  closeMenu();                      // 사이드바 닫기
};

// ✅ [수정] 'Invite' 버튼 클릭 시 실행할 함수
const openInviteModal = async () => {
  await checkProjectOwner();
  friendModalMode.value = 'invite'; // 초대 모드로 설정
  isFriendListOpen.value = true;    // 모달 열기
};

// NavBar.vue의 createNewProject 함수 교체
const createNewProject = async () => {
  try {
    // 1. [안전장치] 내 정보(auth.me)가 비어있다면 서버에서 다시 가져옵니다.
    if (!auth.me) {
       try {
         const res = await api.get('/member/me');
         // 서버 응답 구조에 따라 안전하게 할당 (member 안에 있는지, 바로 있는지)
         auth.me = res.data.member || res.data; 
         auth.isAuthed = true;
       } catch (e) {
         console.warn("사용자 정보를 가져올 수 없습니다. 로그인이 필요합니다.");
         throw { response: { status: 401 } }; // 강제로 로그인 유도 로직으로 이동
       }
    }

    // 2. 프로젝트 생성 요청 (DB에 Row 생성)
    const response = await api.post('/projects/create'); 
    const newWebId = response.data; // 생성된 Web ID

    // 3. 🔥 [핵심 수정] 닉네임 추출 로직 강화 (guest 방지)
    // auth.me 데이터 구조가 가끔 { member: { nickname: ... } } 형태로 래핑될 때가 있어 두 가지 다 확인해야 합니다.
    let targetNickname = 'guest';

    if (auth.me) {
        if (auth.me.nickname) {
            targetNickname = auth.me.nickname;
        } else if (auth.me.member && auth.me.member.nickname) {
            targetNickname = auth.me.member.nickname;
        }
    }

    // 만약 로그인 상태인데도 닉네임을 못 찾았다면, 로그를 찍어 확인해봐야 함
    if (auth.isAuthed && targetNickname === 'guest') {
        console.error("🚨 닉네임 추출 실패! auth.me 데이터 확인:", auth.me);
    }

    // 4. 해당 유저의 IDE로 이동
    router.push(`/ide/${targetNickname}/${newWebId}`);

  } catch (error) {
    console.error("새 프로젝트 생성 실패:", error);
    // 401(인증안됨), 403(권한없음) 에러일 경우 로그인 페이지로
    if (error.response?.status === 401 || error.response?.status === 403) {
      openModal('로그인이 필요한 서비스입니다.', 'warning', () => {
        closeMenu(); // 메뉴 닫고
        router.push('/login');
      });
    } else {
      openModal("프로젝트 생성 중 오류가 발생했습니다.", "error");
    }
  }
};


/* ✅ 프로필 영역 표시용 (auth.me 기반) */
const userName = computed(() => {
  // 1. 혹시 데이터가 두 번 감싸져 있니? (auth.me.member.nickname)
  if (auth.me?.member?.nickname) return auth.me.member.nickname;
  
  // 2. 아니면 정상적으로 들어있니? (auth.me.nickname)
  if (auth.me?.nickname) return auth.me.nickname;

  // 3. 다 없으면
  return '사용자';
});
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

      <div class="navbar-actions" v-if="auth.isAuthed">
        
        <button 
          v-if="isIdePage" 
          class="invite-btn" 
          @click="openInviteModal"
        >
          <UserPlus :size="16" />
          <span>Invite</span>
        </button>

        <button class="noti-btn" @click="goToNotificationTab" title="알림">
          <Bell :size="24" />
          <span v-if="unreadCount > 0" class="noti-badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </div>

      <button
        class="menu-toggle"
        @click="toggleMenu"
        :class="{ active: isMenuOpen }"
      >
        <span></span><span></span><span></span>
      </button>

      <ul class="nav-menu" :class="{ active: isMenuOpen }">
         <li class="drawer-head">
            <div class="drawer-brand">
              <span class="drawer-title">Web Crafter</span>
              <span class="drawer-sub">Workspace</span>
            </div>
            <RouterLink v-if="!auth.isAuthed" class="head-action" to="/register" @click="closeMenu">
              회원가입
            </RouterLink>
         </li>
         <li class="drawer-section">
            <button class="drawer-item" @click.prevent="createNewProject">
              <span class="drawer-dot"></span><span class="drawer-text">웹 만들기</span><span class="drawer-chevron">›</span>
            </button>
            <button class="drawer-item" @click="openFriendManage">
               <span style="display:flex; align-items:center; justify-content:center; margin-right:6px;">
                 <Users :size="18" color="#00d4ff"/>
               </span>
               <span class="drawer-text">친구 목록</span><span class="drawer-chevron">›</span>
            </button>
            <router-link to="/explore" class="drawer-item" @click="closeMenu" active-class="" exact-active-class="">
               <span style="display:flex; align-items:center; justify-content:center; margin-right:6px;">
                 <Compass :size="18" color="#00d4ff"/> 
               </span>
               <span class="drawer-text"  style=" text-align: center;">프로젝트 탐색</span>
               <span class="drawer-chevron">›</span>
            </router-link>
         </li>
         <li class="drawer-divider"></li>
         <li class="drawer-footer">
            <div class="profile-card" @click="handleProfileCardClick">
               <div class="profile-left">
                  <div class="avatar">
                    <img v-if="auth.isAuthed && auth.me?.profileImage" :src="auth.me.profileImage" alt="profile" />
                    <span v-else-if="auth.isAuthed">{{ userInitial }}</span>
                    <svg v-else class="avatar-icon" viewBox="0 0 24 24" fill="none"><path d="M12 12c2.761 0 5-2.239 5-5s-2.239-5-5-5-5 2.239-5 5 2.239 5 5 5zM4 20c0-3.314 3.582-6 8-6s8 2.686 8 6" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  </div>
                  <div class="profile-meta" v-if="auth.isAuthed">
                    <div class="profile-name" :title="userName">{{ userName }}</div>
                    <div class="profile-sub" :title="userSub">{{ userSub }}</div>
                  </div>
                  <div class="profile-meta guest" v-else>
                    <div class="profile-guest-title">로그아웃 상태</div>
                    <div class="profile-guest-desc">로그인을 진행해주세요</div>
                  </div>
               </div>
               <div class="profile-actions">
                  <button v-if="!auth.isAuthed" class="profile-btn" type="button" @click.stop="closeMenu(); router.push('/login');">로그인</button>
                  <button v-else class="profile-btn danger" type.stop="button" @click="handleLogout">로그아웃</button>
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

  <FriendListModal
    :isOpen="isFriendListOpen"
    :currentUser="auth.me"
    :mode="friendModalMode"
    :webId="currentWebId"
    :isOwner="isOwner"
    @close="isFriendListOpen = false"
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

/* ... 기존 스타일 끝나는 곳 바로 아래에 추가 👇 ... */

/* =========================
   ✅ [추가] 알림 벨(Bell) 스타일
========================= */

/* 로고와 햄버거 버튼 사이 공간을 밀어내서 오른쪽으로 붙임 */
.navbar-actions {
  margin-left: auto; /* 핵심: 왼쪽 여백을 다 차지해서 오른쪽으로 밀어버림 */
  margin-right: 12px; /* 햄버거 버튼과의 간격 */
  display: flex;
  align-items: center;
}

.noti-btn {
  position: relative;
  background: transparent;
  border: none;
  color: #b0b8c1;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

/* 마우스 올렸을 때 네온 효과 */
.noti-btn:hover {
  color: #00d4ff;
  background: rgba(0, 212, 255, 0.1);
  transform: translateY(-1px);
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.3);
}

.noti-btn:active {
  transform: scale(0.95);
}

/* 빨간색 숫자 뱃지 */
.noti-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #ef4444; /* Alert Red */
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #1a1a2e; /* 배경색과 맞춰서 분리감 줌 */
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
  animation: popIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

@keyframes popIn {
  from { transform: scale(0); }
  to { transform: scale(1); }
}

/* ... 기존 스타일 맨 아래에 추가 ... */

/* 초대 버튼 스타일 */
.invite-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  
  /* 알림 종과 간격 */
  margin-right: 12px; 
  
  padding: 6px 14px;
  height: 36px;
  border-radius: 8px;
  border: none;
  
  /* 눈에 띄는 네온 컬러 배경 */
  background: rgba(0, 212, 255, 0.15); 
  border: 1px solid rgba(0, 212, 255, 0.4);
  color: #00d4ff;
  
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.invite-btn:hover {
  background: #00d4ff;
  color: #0a1628; /* 글자색을 어둡게 반전 */
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.4);
  transform: translateY(-1px);
}

.invite-btn:active {
  transform: scale(0.96);
}
</style>
