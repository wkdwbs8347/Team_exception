<script setup>
import { ref, computed, onMounted, nextTick, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth'; // Pinia 스토어
import api from '@/api/axios';
import EditProfileModal from '@/modal/EditProfileModal.vue';
import GlobalModal from '@/modal/GlobalModal.vue';
import ConfirmModal from '@/modal/ConfirmModal.vue';

const router = useRouter();
const authStore = useAuthStore();

// 1. 데이터 상태 관리
const isLoading = ref(true);
const myProjects = ref([]);
const sharedProjects = ref([]);
const currentTab = ref('ALL');
const myProjectCount = ref(0);
const sharedProjectCount = ref(0);
const isEditModalOpen = ref(false);

// ✅ [핵심 수정 1] 로컬 변수(ref) 삭제하고 Store를 바라보게 변경(computed)
// 이제 NavBar가 Store를 업데이트하면 여기도 자동으로 바뀝니다.
const notifications = computed(() => authStore.notifications || []);
const unreadNotiCount = computed(() =>
  authStore.notifications ? authStore.notifications.length : 0
);

/* ======================
   ✅ 공용 알림 모달 (alert 대체)
====================== */
const modal = ref({
  open: false,
  message: '',
  type: 'info', // warning | info | success | error
  onConfirm: null,
});

const openModal = (message, type = 'info', onConfirm = null) => {
  modal.value.open = true;
  modal.value.message = message;
  modal.value.type = type;
  modal.value.onConfirm = onConfirm;
};

const closeModal = async () => {
  modal.value.open = false;
  await nextTick();

  if (modal.value.onConfirm) {
    const fn = modal.value.onConfirm;
    modal.value.onConfirm = null;
    fn?.();
  }
};

/* ======================
   ✅ 컨펌 모달 (confirm 대체)
====================== */
const confirmModal = ref({
  open: false,
  message: '',
  type: 'warning',
  confirmText: '삭제',
  cancelText: '취소',
  onConfirm: null,
  onCancel: null,
});

const openConfirm = ({
  message,
  type = 'warning',
  confirmText = '삭제',
  cancelText = '취소',
  onConfirm = null,
  onCancel = null,
}) => {
  confirmModal.value.open = true;
  confirmModal.value.message = message;
  confirmModal.value.type = type;
  confirmModal.value.confirmText = confirmText;
  confirmModal.value.cancelText = cancelText;
  confirmModal.value.onConfirm = onConfirm;
  confirmModal.value.onCancel = onCancel;
};

const closeConfirm = async () => {
  confirmModal.value.open = false;
  await nextTick();
  confirmModal.value.onCancel?.();
  confirmModal.value.onCancel = null;
  confirmModal.value.onConfirm = null;
};

const handleConfirm = async () => {
  confirmModal.value.open = false;
  await nextTick();
  const fn = confirmModal.value.onConfirm;
  confirmModal.value.onConfirm = null;
  confirmModal.value.onCancel = null;
  fn?.();
};

// Enter/Escape 키 처리 (알림/컨펌 모달)
const handleKeydown = (e) => {
  if (modal.value.open) {
    if (e.key === 'Enter' || e.key === 'Escape') {
      e.preventDefault();
      closeModal();
    }
    return;
  }
  if (confirmModal.value.open) {
    if (e.key === 'Escape') {
      e.preventDefault();
      closeConfirm();
    }
    if (e.key === 'Enter') {
      e.preventDefault();
      handleConfirm();
    }
  }
};

onMounted(() => window.addEventListener('keydown', handleKeydown));
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown));

const changeTab = async (tabName) => {
  currentTab.value = tabName;

  // 알림 탭 누를 때 확실하게 서버랑 동기화 (Store 업데이트)
  if (tabName === 'NOTI') {
    try {
      const res = await api.get('/friends/notifications');
      authStore.setNotifications(res.data); // ✅ Store 갱신
    } catch (e) {
      console.error('알림 로드 실패:', e);
      openModal('알림을 불러오지 못했습니다.', 'error');
    }
  }
};

// [수정] 친구 요청 수락/거절 처리
const handleFriendAction = async (action, noti) => {
  console.log(`[프론트] 친구 ${action} 요청 시작:`, noti);

  if (!noti.senderId) {
    openModal('오류: 보낸 사람 정보가 없습니다.', 'error');
    return;
  }

  try {
    await api.post(`/friends/${action}`, {
      senderId: noti.senderId,
      notiId: noti.id,
    });

    // ✅ [핵심 수정 2] 처리된 알림은 Store에서 제거 -> 화면 자동 반영
    const newList = authStore.notifications.filter((n) => n.id !== noti.id);
    authStore.setNotifications(newList);

    openModal(
      action === 'accept'
        ? '친구 요청을 수락했습니다! 🎉'
        : '요청을 거절했습니다.',
      action === 'accept' ? 'success' : 'info'
    );
  } catch (e) {
    console.error(e);
    openModal('처리 중 오류가 발생했습니다.', 'error');
  }
};

// 프로젝트 초대 수락/거절 처리
const handleProjectAction = async (action, noti) => {
  openConfirm({
    message:
      action === 'accept'
        ? '프로젝트 초대를 수락하시겠습니까?'
        : '정말 거절하시겠습니까?',
    type: 'warning',
    confirmText: action === 'accept' ? '수락' : '거절',
    cancelText: '취소',
    onConfirm: async () => {
      try {
        await api.post(`/projects/${action}`, {
          notiId: noti.id,
          webId: noti.relId,
        });

        // ✅ [핵심 수정 3] Store에서 제거
        const newList = authStore.notifications.filter((n) => n.id !== noti.id);
        authStore.setNotifications(newList);

        // 수락 시 프로젝트 목록 갱신
        if (action === 'accept') {
          const res = await api.get('/member/me');
          sharedProjects.value = res.data.sharedProjects || [];
          sharedProjectCount.value = res.data.member.sharedProjectCount || 0;
          openModal(
            '프로젝트에 참여했습니다! 워크스페이스를 확인해보세요. 🎉',
            'success'
          );
        } else {
          openModal('초대를 거절했습니다.', 'info');
        }
      } catch (e) {
        console.error('에러 상세:', e);
        const msg =
          e.response?.data?.message ||
          (typeof e.response?.data === 'string' ? e.response.data : null) ||
          '처리 중 오류가 발생했습니다.';
        openModal(msg, 'error');
      }
    },
  });
};

// 2. 초기 데이터 로드
onMounted(async () => {
  try {
    const response = await api.get('/member/me');
    const data = response.data;

    authStore.user = data.member;

    myProjectCount.value = data.member.myProjectCount || 0;
    sharedProjectCount.value = data.member.sharedProjectCount || 0;

    myProjects.value = data.myProjects || [];
    sharedProjects.value = data.sharedProjects || [];

    isLoading.value = false;
    console.log('통계 및 리스트 로드 완료');
  } catch (error) {
    if (error.response?.status === 401) {
      openModal('로그인이 필요합니다.', 'warning', () => {
        router.push('/login');
      });
    } else {
      openModal('데이터 로드 중 오류가 발생했습니다.', 'error');
    }
  }
});

const enterIDE = (webId) => {
  const nickname = authStore.user?.nickname || 'guest';
  router.push(`/ide/${nickname}/${webId}`);
};

const createNewProject = async () => {
  try {
    const response = await api.post('/projects/create');
    const newWebId = response.data;
    const nickname = authStore.user?.nickname || 'guest';
    router.push(`/ide/${nickname}/${newWebId}`);
  } catch (error) {
    console.error('새 프로젝트 생성 실패:', error);
    if (error.response?.status === 401 || error.response?.status === 403) {
      openModal('로그인이 필요한 서비스입니다.', 'warning', () => {
        router.push('/login');
      });
    } else {
      openModal('프로젝트 생성 중 오류가 발생했습니다.', 'error');
    }
  }
};

const handleLogout = () => {
  openConfirm({
    message: '로그아웃 하시겠습니까?',
    type: 'warning',
    confirmText: '로그아웃',
    cancelText: '취소',
    onConfirm: () => {
      authStore.logout();
      router.push('/login');
    },
  });
};

const filteredProjects = computed(() => {
  if (currentTab.value === 'MY') return myProjects.value;
  if (currentTab.value === 'SHARED') return sharedProjects.value;
  return [...myProjects.value, ...sharedProjects.value];
});

const startRename = (web) => {
  if (web.role !== 'OWNER') return;
  web.isEditing = true;
  web.tempTitle = web.title;
};

const saveNewName = async (web) => {
  if (!web.isEditing) return;

  if (!web.tempTitle.trim()) {
    web.isEditing = false;
    return;
  }

  try {
    await api.put(`/projects/${web.id}/name`, { name: web.tempTitle });
    web.title = web.tempTitle;
    web.isEditing = false;

    const target = myProjects.value.find((p) => p.id === web.id);
    if (target) target.title = web.tempTitle;
  } catch (e) {
    console.error('수정 실패 상세:', e.response?.data || e.message);
    openModal('이름 수정에 실패했습니다.', 'error');
    web.isEditing = false;
  }
};

const formatDate = (date) => {
  if (!date) return 'Just now';
  return new Date(date).toLocaleDateString();
};

const activeMenuId = ref(null);

const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id;
};

onMounted(() => {
  window.addEventListener('click', (e) => {
    if (!e.target.closest('.menu-container')) {
      activeMenuId.value = null;
    }
  });
});

const confirmDelete = async (webId) => {
  activeMenuId.value = null;

  openConfirm({
    message: '정말 삭제하시겠습니까?',
    type: 'warning',
    confirmText: '삭제',
    cancelText: '취소',
    onConfirm: async () => {
      try {
        await api.delete(`/projects/${webId}`);
        myProjects.value = myProjects.value.filter((p) => p.id !== webId);
        myProjectCount.value = Math.max(0, myProjectCount.value - 1);
        openModal('삭제가 완료되었습니다.', 'success');
      } catch (error) {
        console.error('삭제 실패:', error);
        const msg = error.response?.data?.message || '오류가 발생했습니다.';
        openModal(msg, 'error');
      }
    },
  });
};
</script>

<script>
// ⚠️ 다른 기능 건드리지 않기 위해 여기(alert)는 유지
// (setup의 openModal을 Options API에서 직접 못 씀)
import { useAuthStore } from '@/stores/auth';
export default {
  async beforeRouteEnter(to, from, next) {
    const authStore = useAuthStore();
    if (authStore.isAuthed) {
      next();
      return;
    }
    try {
      const isSuccess = await authStore.bootstrap();
      if (isSuccess) {
        next();
      } else {
        throw new Error('인증 실패');
      }
    } catch (e) {
      alert('로그인이 필요한 서비스입니다.');
      next('/login');
    }
  },
};
</script>

<template>
  <div class="mypage-wrapper">
    <header>
      <div class="header-container">
        <div class="logo">
          <div class="logo-icon">&lt;/&gt;</div>
          <span>Web Crafter</span>
        </div>
        <nav>
          <router-link to="/mypage" class="active">Home</router-link>
          <router-link to="/ide/new">IDE</router-link>
          <a href="#" @click.prevent="handleLogout">Logout</a>
        </nav>
      </div>
    </header>

    <main>
      <section class="profile-section">
        <div class="avatar">👨‍💻</div>
        <h1 class="username">{{ authStore.user?.nickname || 'Guest' }}</h1>
        <p class="bio">
          {{ authStore.user?.bio || 'Welcome to your workspace!' }}
        </p>

        <div v-if="authStore.user?.techStacks" class="user-tags">
          <span
            v-for="tag in authStore.user.techStacks.split(',')"
            :key="tag"
            class="mini-tag"
          >
            #{{ tag }}
          </span>
        </div>

        <div class="action-buttons">
          <button class="btn primary" @click="createNewProject">
            + New Project
          </button>
          <button class="btn" @click="isEditModalOpen = true">
            Edit Profile
          </button>
        </div>
      </section>

      <section class="stats-section">
        <div
          class="stat-card"
          :class="{ active: currentTab === 'MY' }"
          @click="currentTab = 'MY'"
          style="cursor: pointer"
        >
          <div class="stat-number">{{ myProjectCount }}</div>
          <div class="stat-label">My Projects</div>
        </div>

        <div
          class="stat-card"
          :class="{ active: currentTab === 'SHARED' }"
          @click="currentTab = 'SHARED'"
          style="cursor: pointer"
        >
          <div class="stat-number">{{ sharedProjectCount }}</div>
          <div class="stat-label">Collaborating</div>
        </div>

        <div
          class="stat-card"
          :class="{ active: currentTab === 'NOTI' }"
          @click="changeTab('NOTI')"
          style="cursor: pointer"
        >
          <div class="stat-number">{{ unreadNotiCount }}</div>
          <div class="stat-label">New Alerts</div>
        </div>
      </section>

      <section class="activity-section">
        <h2
          class="activity-title"
          @click="currentTab = 'ALL'"
          style="cursor: pointer"
        >
          {{ currentTab === 'NOTI' ? 'Notifications' : 'Your Workspaces' }}
          <small v-if="currentTab !== 'ALL' && currentTab !== 'NOTI'"
            >(Filtering: {{ currentTab }})</small
          >
        </h2>

        <div v-if="currentTab === 'NOTI'" class="notification-list">
          <div v-if="notifications.length > 0" class="noti-wrapper">
            <div v-for="noti in notifications" :key="noti.id" class="noti-item">
              <div v-if="noti.type === 'FRIEND_REQ'" class="noti-content">
                <span class="icon">💌</span>
                <div class="text">
                  <span class="sender">{{ noti.senderName }}</span
                  >님이 친구 요청을 보냈습니다.
                </div>

                <div class="noti-actions">
                  <button
                    class="btn-xs accept"
                    @click="handleFriendAction('accept', noti)"
                  >
                    수락
                  </button>
                  <button
                    class="btn-xs reject"
                    @click="handleFriendAction('reject', noti)"
                  >
                    거절
                  </button>
                </div>

                <span class="date">{{ formatDate(noti.regDate) }}</span>
              </div>

              <div
                v-else-if="noti.type === 'PROJECT_INVITE'"
                class="noti-content"
              >
                <span class="icon">📁</span>
                <div class="text">
                  <span class="sender">{{ noti.senderName }}</span
                  >님이 프로젝트에 초대했습니다.
                </div>

                <div class="noti-actions">
                  <button
                    class="btn-xs accept"
                    @click="handleProjectAction('accept', noti)"
                  >
                    수락
                  </button>
                  <button
                    class="btn-xs reject"
                    @click="handleProjectAction('reject', noti)"
                  >
                    거절
                  </button>
                </div>

                <span class="date">{{ formatDate(noti.regDate) }}</span>
              </div>
            </div>
          </div>

          <div v-else class="empty-msg">
            🔔 현재 확인된 새로운 알림이 없습니다.
          </div>
        </div>

        <div v-else class="project-grid">
          <div
            v-for="web in filteredProjects"
            :key="web.id"
            class="activity-item project-card"
            :class="{ shared: web.role !== 'OWNER' }"
          >
            <div class="project-info">
              <div class="activity-text">
                {{ web.role === 'OWNER' ? '📁' : '🤝' }}
                <span
                  v-if="!web.isEditing"
                  @dblclick="startRename(web)"
                  class="editable-title"
                >
                  {{ web.title }}
                </span>
                <input
                  v-else
                  v-model="web.tempTitle"
                  @blur="saveNewName(web)"
                  @keyup.enter="saveNewName(web)"
                  class="inline-edit-input"
                  autofocus
                />
              </div>
              <div class="activity-time">
                {{ web.role }} | Last updated: {{ formatDate(web.updateDate) }}
                <span v-if="web.ownerNickname"
                  >| From @{{ web.ownerNickname }}</span
                >
              </div>
            </div>

            <div class="menu-container">
              <button class="btn-more" @click.stop="toggleMenu(web.id)">
                ⋮
              </button>
              <div v-if="activeMenuId === web.id" class="dropdown-menu">
                <button
                  v-if="web.role === 'OWNER'"
                  class="delete-opt"
                  @click="confirmDelete(web.id)"
                >
                  Delete
                </button>
              </div>
            </div>

            <button class="btn-sm" @click="enterIDE(web.id)">
              {{ web.role === 'OWNER' ? 'Open' : 'Join' }}
            </button>
          </div>

          <div v-if="filteredProjects.length === 0" class="empty-msg">
            표시할 프로젝트가 없습니다.
          </div>
        </div>
      </section>
    </main>

    <EditProfileModal
      v-if="isEditModalOpen"
      :user="authStore.user"
      @close="isEditModalOpen = false"
    />

    <!-- ✅ 알림 모달 -->
    <GlobalModal
      :open="modal.open"
      :message="modal.message"
      :type="modal.type"
      @confirm="closeModal"
    />

    <!-- ✅ 컨펌 모달 -->
    <ConfirmModal
      :open="confirmModal.open"
      :message="confirmModal.message"
      :type="confirmModal.type"
      :confirmText="confirmModal.confirmText"
      :cancelText="confirmModal.cancelText"
      @confirm="handleConfirm"
      @cancel="closeConfirm"
    />
  </div>
</template>

<style scoped>
/* 1. 디자인 가이드 (변수 정의) */
:host {
  --primary-color: #00d9ff;
  --primary-hover: #00b8d4;
  --bg-dark: #0a1628;
  --bg-gradient: linear-gradient(135deg, #0a1628 0%, #0d1f3c 100%);
  --border-color: rgba(0, 217, 255, 0.1);
  --text-main: #ffffff;
  --text-muted: #b0b8c1;
  --card-bg: rgba(0, 217, 255, 0.05);
  --transition: all 0.3s ease;
}

/* 2. 전체 레이아웃 */
.mypage-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1628 0%, #0d1f3c 100%);
  color: #ffffff;
  font-family: inherit;
}

/* 3. Header 스타일 */
header {
  background: rgba(10, 22, 40, 0.95);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 217, 255, 0.1);
  padding: 1rem 2rem;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-container {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #00d9ff 0%, #0099cc 100%);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0a1628;
  font-weight: 900;
}

/* 7. 워크스페이스(카드) 섹션 수정 */
.project-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;

  /* 높이 및 스크롤 설정 */
  height: 150px; /* 요청하신 고정 높이 */
  overflow-y: auto; /* 세로 내용이 넘치면 스크롤바 생성 */
  padding-right: 10px; /* 스크롤바와 카드 사이의 여유 공간 */
}

/* (선택) 스크롤바 디자인을 더 깔끔하게 만들고 싶다면 추가하세요 */
.project-grid::-webkit-scrollbar {
  width: 6px;
}

.project-grid::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
}

.project-grid::-webkit-scrollbar-thumb {
  background: rgba(0, 217, 255, 0.3);
  border-radius: 10px;
}

.project-grid::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 217, 255, 0.5);
}

nav a {
  color: #b0b8c1;
  text-decoration: none;
  margin-left: 2rem;
  font-size: 0.95rem;
  transition: all 0.3s ease;
}

nav a:hover,
nav a.router-link-active {
  color: #00d9ff;
}

/* 4. 메인 컨텐츠 영역 */
main {
  max-width: 900px;
  margin: 0 auto;
  padding: 3rem 2rem;
}

/* 프로필 섹션 */
.profile-section {
  text-align: center;
  margin-bottom: 3.5rem;
}

.avatar {
  width: 130px;
  height: 130px;
  border-radius: 50%;
  margin: 0 auto 1.5rem;
  border: 3px solid #00d9ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 55px;
  box-shadow: 0 0 25px rgba(0, 217, 255, 0.2);
  background: rgba(0, 217, 255, 0.05);
}

.username {
  font-size: 2.2rem;
  font-weight: 700;
  letter-spacing: -0.5px;
  margin-bottom: 0.5rem;
}

.bio {
  color: #b0b8c1;
  margin-bottom: 2rem;
}

/* 5. 버튼 공통 스타일 */
.btn {
  padding: 0.7rem 1.8rem;
  border: 2px solid #00d9ff;
  background: transparent;
  color: #00d9ff;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  margin: 0 0.5rem;
  transition: all 0.3s ease;
}

.btn:hover {
  background: rgba(0, 217, 255, 0.1);
  box-shadow: 0 0 15px rgba(0, 217, 255, 0.2);
  transform: translateY(-2px);
}

.btn.primary {
  background: #00d9ff;
  color: #0a1628;
}

.btn.primary:hover {
  background: #00b8d4;
  border-color: #00b8d4;
}

/* 6. 통계(Stats) 섹션 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem;
  margin-bottom: 3.5rem;
  padding-top: 2rem;
  border-top: 1px solid rgba(0, 217, 255, 0.1);
}

.stat-card {
  text-align: center;
  padding: 1.2rem;
  background: rgba(0, 217, 255, 0.03);
  border-radius: 12px;
  border: 1px solid rgba(0, 217, 255, 0.08);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  border-color: #00d9ff;
  background: rgba(0, 217, 255, 0.06);
}

.stat-number {
  font-size: 1.8rem;
  font-weight: 700;
  color: #00d9ff;
}

.stat-label {
  font-size: 0.8rem;
  color: #b0b8c1;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 0.3rem;
}

/* 7. 워크스페이스(카드) 섹션 */
.activity-title {
  font-size: 1.2rem;
  color: #00d9ff;
  text-align: center;
  margin-bottom: 1.5rem;
  font-weight: 600;
}

.project-card {
  display: flex;
  justify-content: space-between; /* 정보는 왼쪽, 버튼들은 오른쪽 끝 */
  align-items: center;
  padding: 1.2rem 1.5rem;
  background: rgba(255, 255, 255, 0.02);
  border-left: 4px solid #00d9ff;
  border-radius: 0 10px 10px 0;
  transition: all 0.3s ease;
}

.project-card:hover {
  background: rgba(0, 217, 255, 0.05);
  transform: translateX(5px);
}

.project-card.shared {
  border-left-color: #00ff95;
}

.activity-text {
  font-weight: 600;
  font-size: 1.05rem;
  margin-bottom: 0.2rem;
}

.activity-time {
  font-size: 0.85rem;
  color: #7a8a99;
}

.btn-sm {
  background: #00d9ff;
  color: #0a1628;
  border: none;
  padding: 0.5rem 1.2rem;
  border-radius: 6px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-sm:hover {
  background: #00b8d4;
  transform: scale(1.05);
}

.empty-msg {
  text-align: center;
  color: #7a8a99;
  padding: 3rem;
  font-style: italic;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 10px;
}

.user-tags {
  display: flex;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 2rem;
}

.mini-tag {
  color: #00d9ff;
  font-size: 0.85rem;
  background: rgba(0, 217, 255, 0.1);
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
}

.editable-title {
  cursor: pointer;
  padding: 2px 5px;
  border-radius: 4px;
}

.editable-title:hover {
  background: rgba(0, 217, 255, 0.1);
}

.inline-edit-input {
  background: #1e293b;
  border: 1px solid #00d9ff;
  color: white;
  padding: 2px 5px;
  border-radius: 4px;
  outline: none;
  width: auto;
}

/* 2. [핵심] 정보와 버튼 사이를 벌려주는 장치 */
.project-info {
  flex-grow: 1; /* 이 영역이 남는 공간을 다 차지해서 버튼들을 오른쪽으로 밀어냅니다 */
}

/* 3. 점 3개 컨테이너: 간격만 설정 */
.menu-container {
  position: relative;
  display: flex;
  align-items: center;
  margin-left: auto;
  margin-right: 12px; /* ⋮ 버튼과 Open 버튼 사이의 간격 */
}

/* 4. 드롭다운(Delete) 위치 */
.dropdown-menu {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  right: 15px;
  background: transparent;
  border: none;
  z-index: 9999;
}

/* 5. Delete 버튼 */
.delete-opt {
  min-width: 70px;
  height: 38px;
  padding: 0 1.2rem;
  border-radius: 6px;
  background: #2d1b1b;
  border: 1px solid #ff4d4d;
  color: #ff4d4d !important;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* [추가] 알림 리스트 스타일 */
.noti-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.noti-item {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(0, 217, 255, 0.1);
  padding: 1rem;
  border-radius: 8px;
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.noti-item:hover {
  background: rgba(0, 217, 255, 0.05);
  border-color: rgba(0, 217, 255, 0.3);
}

.noti-content {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.noti-content .icon {
  font-size: 1.2rem;
}

.noti-content .text {
  flex-grow: 1;
  font-size: 0.95rem;
  color: #e2e8f0;
}

.noti-content .sender {
  color: #00d9ff;
  font-weight: 700;
}

.noti-content .date {
  font-size: 0.8rem;
  color: #64748b;
  margin-left: auto;
}

/* 버튼 스타일 */
.noti-actions {
  display: flex;
  gap: 8px;
  margin-right: 15px;
}

.btn-xs {
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 700;
  cursor: pointer;
  border: 1px solid transparent;
  transition: 0.2s;
}

.btn-xs.accept {
  background: rgba(0, 217, 255, 0.15);
  color: #00d9ff;
  border-color: rgba(0, 217, 255, 0.3);
}

.btn-xs.accept:hover {
  background: #00d9ff;
  color: #0a1628;
}

.btn-xs.reject {
  background: rgba(255, 77, 77, 0.15);
  color: #ff4d4d;
  border-color: rgba(255, 77, 77, 0.3);
}

.btn-xs.reject:hover {
  background: #ff4d4d;
  color: white;
}
</style>
