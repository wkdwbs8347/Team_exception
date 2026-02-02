<script setup>
import api from '@/api/axios';
import { ref, onMounted, onBeforeUnmount, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import GlobalModal from '@/modal/GlobalModal.vue';
import { useAuthStore } from '@/stores/auth';
import { Blocks, Users, Bell, Compass } from 'lucide-vue-next';
import FriendListModal from '@/modal/FriendListModal.vue';

defineProps({ scrollY: Number });

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const isFriendListOpen = ref(false);
const isMenuOpen = ref(false);
const friendModalMode = ref('manage');

let stompClient = null;
let connected = false;
let connecting = false;
let scriptsAppending = false;
let subscribed = false;

// 페이지 이동 시 메뉴 닫기
watch(
  () => route.path,
  () => {
    isFriendListOpen.value = false;
    isMenuOpen.value = false;
  }
);

/**
 * 🚀 [실시간 핵심] CDN 로드 + 웹소켓 연결 (중복 방지 완성본)
 */
const loadLibrariesAndConnect = () => {
  if (!auth.me?.id) return;
  if (connected || connecting) return;

  // ✅ 이미 라이브러리 로드되어 있으면 바로 연결만
  if (window.SockJS && window.Stomp) {
    connectSocketOnce();
    return;
  }

  // ✅ script 태그 중복 append 방지
  if (scriptsAppending) return;
  scriptsAppending = true;

  const ensureScript = (id, src) => {
    return new Promise((resolve, reject) => {
      // 이미 있으면 resolve
      const existing = document.getElementById(id);
      if (existing) {
        // 로드 완료 보장 (이미 로드됐을 수도 / 로딩중일 수도)
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', reject);
        // 이미 로드된 경우 대비: 다음 tick에 resolve 시도
        setTimeout(resolve, 0);
        return;
      }

      const s = document.createElement('script');
      s.id = id;
      s.src = src;
      s.onload = () => resolve();
      s.onerror = (e) => reject(e);
      document.head.appendChild(s);
    });
  };

  Promise.all([
    ensureScript(
      'sockjs-cdn',
      'https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js'
    ),
    ensureScript(
      'stomp-cdn',
      'https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js'
    ),
  ])
    .then(() => {
      scriptsAppending = false;
      connectSocketOnce();
    })
    .catch((e) => {
      scriptsAppending = false;
      console.error('CDN 로드 실패:', e);
      setTimeout(loadLibrariesAndConnect, 3000);
    });
};

const connectSocketOnce = () => {
  if (!auth.me?.id) return;
  if (connected || connecting) return;
  if (!window.SockJS || !window.Stomp) return;

  connecting = true;

  const socket = new window.SockJS('http://localhost:8080/wsproject');
  stompClient = window.Stomp.over(socket);

  // ✅ 디버그 켜기 (지금은 null이라 로그가 안 보임)
  stompClient.debug = (msg) => console.log('[STOMP]', msg);

  stompClient.connect(
    { 'x-user-id': String(auth.me.id) },
    () => {
      connecting = false;
      connected = true;
      console.log('🚀 실시간 알림 웹소켓 연결 성공', auth.me.id);

      if (subscribed) return;
      subscribed = true;

      // 🔔 notifications
      stompClient.subscribe(`/topic/user/${auth.me.id}/notifications`, (res) => {
        if (!res.body) return;
        const payload = JSON.parse(res.body);

        if (Array.isArray(payload)) {
          auth.setNotifications(payload);
          return;
        }

        const prev = Array.isArray(auth.notifications) ? auth.notifications : [];
        const next = prev.some((n) => n.id === payload.id) ? prev : [payload, ...prev];
        auth.setNotifications(next);
      });

      // ✅ 2) [추가됨] 채팅 메시지 알림 (빨간 점용)
      stompClient.subscribe(`/topic/notifications/${auth.me.id}`, (res) => {
        const msg = JSON.parse(res.body);
        console.log('🔔 채팅 알림 도착:', msg);
        auth.pushIncomingChat(msg);

        // 스토어의 '안 읽은 사람 목록'에 추가 -> 종 아이콘 & 친구 목록에 빨간 점 뜸
        auth.unreadSenders.add(msg.senderId);
      });

      // ✅ 1) 개인 토픽 presence
      stompClient.subscribe(`/topic/user/${auth.me.id}/presence`, (res) => {
        console.log('📥 presence(user) raw:', res.body);
        if (!res.body) return;

        try {
          const { userId, status } = JSON.parse(res.body);
          console.log('✅ presence(user) parsed:', userId, status);
          auth.updateFriendPresence(userId, status);
        } catch (e) {
          console.error('presence(user) JSON 파싱 실패:', e);
        }
      });



      // ✅ 2) 공용 토픽 presence (서버가 여기에 publish 하는 경우가 많음)
      stompClient.subscribe(`/topic/presence`, (res) => {
        console.log('📥 presence(global) raw:', res.body);
        if (!res.body) return;

        try {
          const { userId, status } = JSON.parse(res.body);
          console.log('✅ presence(global) parsed:', userId, status);
          auth.updateFriendPresence(userId, status);
        } catch (e) {
          console.error('presence(global) JSON 파싱 실패:', e);
        }
      });

      // ✅ 3) [여기에 추가] 친구 목록 갱신 신호
      stompClient.subscribe(`/topic/user/${auth.me.id}/friends`, async (res) => {
        console.log('👥 friends refresh:', res.body);

        try {
          const fres = await api.get('/friends/list');

          // store에 setFriends 있으면 그걸 쓰고
          if (typeof auth.setFriends === 'function') {
            auth.setFriends(Array.isArray(fres.data) ? fres.data : []);
          } 
          // 없으면 me.friends에 넣는 임시 방식
          else {
            auth.me = {
              ...(auth.me || {}),
              friends: Array.isArray(fres.data) ? fres.data : [],
            };
          }
        } catch (e) {
          console.error('친구 목록 reload 실패', e);
        }
      });
      
    },
    (error) => {
      connecting = false;
      connected = false;
      subscribed = false;
      console.error('소켓 연결 실패:', error);
      setTimeout(loadLibrariesAndConnect, 3000);
    }
  );
};

const handleProfileCardClick = () => {
  closeMenu();
  router.push('/mypage');
};

const toggleMenu = () => (isMenuOpen.value = !isMenuOpen.value);
const closeMenu = () => (isMenuOpen.value = false);

// 모달 관련 로직
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

const handleKeydown = (e) => {
  if (modal.value.open && e.key === 'Escape') {
    e.preventDefault();
    closeModal();
  }
};

onMounted(async () => {
  window.addEventListener('keydown', handleKeydown);

  // ✅ 1) 무조건 store로 로그인 복원 (형태 통일)
  await auth.bootstrap();

  // 로그인 아니면 종료
  if (!auth.me?.id) return;

  // ✅ 2) 소켓 연결
  loadLibrariesAndConnect();

  // ✅ 3) 초기 알림 리스트 (실패해도 앱은 계속)
  try {
    const notiRes = await api.get('/friends/notifications');
    auth.setNotifications(Array.isArray(notiRes.data) ? notiRes.data : []);
  } catch (error) {
    console.error('알림 초기 로드 실패:', error);
    auth.setNotifications([]);
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);

  try {
    if (stompClient?.connected) stompClient.disconnect();
  } finally {
    stompClient = null;
    connected = false;
    connecting = false;
    subscribed = false;
    scriptsAppending = false;
  }
});

const handleLogout = async () => {
  try {
    await auth.logout();
    if (stompClient) stompClient.disconnect();
    isMenuOpen.value = false;
    isFriendListOpen.value = false;
    openModal('로그아웃 되었습니다.', 'success', () => router.push('/'));
  } catch (e) {
    openModal('로그아웃 실패', 'error');
  }
};

const unreadCount = computed(() => {
  const list = auth.notifications || auth.me?.notifications || [];
  return list.filter((n) => !n.isRead).length;
});

const goToNotificationTab = () => {
  if (!auth.isAuthed) return openModal('로그인이 필요합니다.', 'warning');
  router.push({ path: '/mypage', query: { tab: 'NOTI' } });
  closeMenu();
};

const openFriendManage = () => {
  friendModalMode.value = 'manage';
  isFriendListOpen.value = true;
  closeMenu();
};

const createNewProject = async () => {
  try {
    const response = await api.post('/projects/create');
    const newWebId = response.data;

    const nick = auth.me?.nickname || 'guest';
    router.push(`/ide/${nick}/${newWebId}`);
  } catch (error) {
    if (error.response?.status === 401) {
      openModal('로그인이 필요합니다.', 'warning', () => router.push('/login'));
    } else {
      openModal('프로젝트 생성 중 오류 발생', 'error');
    }
  }
};

const userName = computed(() => auth.me?.nickname || '사용자');
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
        <button class="noti-btn" @click="goToNotificationTab" title="알림">
          <Bell :size="24" />
          <span v-if="unreadCount > 0" class="noti-badge">
            {{ unreadCount > 99 ? '99+' : unreadCount }}
          </span>
        </button>
      </div>

      <button class="menu-toggle" @click="toggleMenu" :class="{ active: isMenuOpen }">
        <span></span><span></span><span></span>
      </button>

      <ul class="nav-menu" :class="{ active: isMenuOpen }">
        <li class="drawer-head">
          <div class="drawer-brand">
            <span class="drawer-title">Web Crafter</span>
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

        <li class="drawer-section">
          <button class="drawer-item" @click.prevent="createNewProject">
            <span class="icon-wrapper">
              <Blocks :size="18" color="#00d4ff" />
            </span>
            <span class="drawer-text">웹 만들기</span>
            <span class="drawer-chevron">›</span>
          </button>
          <button class="drawer-item" @click="openFriendManage">
            <span class="icon-wrapper"><Users :size="18" color="#00d4ff" /></span>
            <span class="drawer-text">친구 목록</span>
            <span class="drawer-chevron">›</span>
          </button>
          <router-link to="/explore" class="drawer-item" @click="closeMenu">
            <span class="icon-wrapper"><Compass :size="18" color="#00d4ff" /></span>
            <span class="drawer-text">프로젝트 탐색</span>
            <span class="drawer-chevron">›</span>
          </router-link>
        </li>

        <li class="drawer-divider"></li>

        <li class="drawer-footer">
          <div class="profile-card" @click="handleProfileCardClick">
            <!-- ✅ [변경] 프로필 사진 영역 제거 + 텍스트만 -->
            <div class="profile-meta">
              <div v-if="!auth.isAuthed" class="login-please">
                로그인을 진행해주세요
              </div>
              <div v-else class="welcome">
                <span class="nickname">{{ userName }}</span>
                <span> 님 환영합니다.</span>
              </div>
            </div>

            <div class="profile-actions">
              <button
                v-if="!auth.isAuthed"
                class="profile-btn"
                @click.stop="
                  closeMenu();
                  router.push('/login');
                "
              >
                로그인
              </button>
              <button v-else class="profile-btn danger" @click.stop="handleLogout">
                로그아웃
              </button>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </nav>

  <GlobalModal :open="modal.open" :message="modal.message" :type="modal.type" @confirm="closeModal" />
  <FriendListModal
    :isOpen="isFriendListOpen"
    :currentUser="auth.me"
    :mode="friendModalMode"
    :webId="route.params.webId"
    @close="isFriendListOpen = false"
  />
</template>

<style scoped>
/* 정렬된 CSS */
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1200;
  background: rgba(26, 26, 46, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.12);
  transition: background 0.25s, box-shadow 0.25s;
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
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
}
.logo-icon {
  font-size: 2rem;
}
.logo-text {
  letter-spacing: 0.06em;
}

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
  border-radius: 12px;
  transition: background 0.18s;
}
.menu-toggle:hover {
  background: rgba(0, 212, 255, 0.08);
}
.menu-toggle span {
  width: 25px;
  height: 3px;
  background: #00d4ff;
  border-radius: 2px;
  transition: transform 0.28s, opacity 0.2s;
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
  background: linear-gradient(180deg, rgba(10, 20, 44, 0.96), rgba(8, 16, 36, 0.96));
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.09);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.55);
  transform: translateX(110%);
  opacity: 0;
  transition: transform 0.28s, opacity 0.28s;
  z-index: 1100;
  overflow: hidden;
}
.nav-menu.active {
  transform: translateX(0);
  opacity: 1;
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 12px 14px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.drawer-title {
  font-size: 1.08rem;
  font-weight: 900;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
  -webkit-text-fill-color: transparent;
}
.drawer-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 6px;
}
.drawer-divider {
  height: 1px;
  margin: 6px 10px;
  background: rgba(255, 255, 255, 0.1);
}
.drawer-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 13px 12px;
  border-radius: 16px;
  color: rgba(224, 224, 224, 0.94);
  font-weight: 850;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.09);
  transition: all 0.16s;
  cursor: pointer;
}
.drawer-item:hover {
  transform: translateY(-2px);
  background: rgba(0, 212, 255, 0.1);
  border-color: rgba(0, 212, 255, 0.22);
}
.icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 6px;
}

.drawer-footer {
  margin-top: auto;
  padding: 10px 8px;
}
.profile-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.09);
  cursor: pointer;
  transition: all 0.12s;
}
.profile-card:hover {
  transform: translateY(-2px);
  background: rgba(0, 212, 255, 0.08);
}

/* ✅ [추가] 프로필 사진 제거 후 텍스트 레이아웃 */
.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

/* ✅ 로그인 안됨: 빨간 글씨 */
.login-please {
  color: #ff4d4d;
  font-weight: 900;
  font-size: 0.95rem;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ✅ 로그인 됨: 환영 문구 */
.welcome {
  color: rgba(224, 224, 224, 0.94);
  font-weight: 850;
  font-size: 0.95rem;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ✅ 닉네임만 초록 */
.nickname {
  color: #22c55e;
  font-weight: 950;
}

.navbar-actions {
  margin-left: auto;
  margin-right: 12px;
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
  transition: all 0.2s;
}
.noti-btn:hover {
  color: #00d4ff;
  background: rgba(0, 212, 255, 0.1);
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.3);
}
.noti-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  background: #ef4444;
  color: white;
  font-size: 0.65rem;
  font-weight: 800;
  min-width: 16px;
  height: 16px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #1a1a2e;
}

.profile-actions {
  display: flex;
  align-items: center;
}

.profile-btn {
  padding: 9px 12px;
  border-radius: 12px;
  border: 1px solid rgba(0, 212, 255, 0.22);
  background: rgba(0, 212, 255, 0.1);
  color: #fff;
  font-weight: 900;
  cursor: pointer;
}
.profile-btn.danger {
  border-color: rgba(255, 90, 90, 0.35);
  background: rgba(255, 90, 90, 0.1);
}

@keyframes pulse {
  0% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.7);
  }
  70% {
    transform: scale(1);
    box-shadow: 0 0 0 6px rgba(239, 68, 68, 0);
  }
  100% {
    transform: scale(0.95);
    box-shadow: 0 0 0 0 rgba(239, 68, 68, 0);
  }
}
</style>