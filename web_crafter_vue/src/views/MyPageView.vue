<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth'; // Pinia/Vuex 스토어
import api from '@/api/axios'; // Axios 인터셉터 설정 파일
import EditProfileModal from '@/modal/EditProfileModal.vue'; // 프로필 수정 모달
import GlobalModal from '@/modal/GlobalModal.vue';
const router = useRouter();
const authStore = useAuthStore();

// 1. 데이터 상태 관리 (DB 컬럼 구조 반영)
const myProjects = ref([]);    // 내가 방장인 프로젝트
const sharedProjects = ref([]); // 초대받은 협업 프로젝트
const currentTab = ref('ALL'); // ✅ 추가: 현재 선택된 탭 (ALL, MY, SHARED)
const myProjectCount = ref(0);    // 숫자 표시용 변수 추가
const sharedProjectCount = ref(0);
const unreadNotiCount = ref(0); // 읽지 않은 알림 수
const isEditModalOpen = ref(false); // 모달 제어를 위한 상태 변수

// 2. 초기 데이터 로드 (백엔드 API 연동)
onMounted(async () => {
  try {
    const response = await api.get('/member/me');
    const data = response.data; // { member: {...}, myProjects: [...], sharedProjects: [...] }

    // 1. 전체 유저 및 통계 정보를 스토어에 저장 (data.member 사용)
    authStore.user = data.member;
    
    // 2. ✅ 백엔드 Map 구조에 맞춰 데이터 할당
    // 이제 숫자는 data.member 안에 들어있습니다.
    myProjectCount.value = data.member.myProjectCount || 0;
    sharedProjectCount.value = data.member.sharedProjectCount || 0;
    unreadNotiCount.value = data.member.unreadNotiCount || 0;

    // 3. ✅ 프로젝트 리스트 할당 (HTML의 v-for 문과 연결됨)
    myProjects.value = data.myProjects || [];
    sharedProjects.value = data.sharedProjects || [];

    isLoading.value = false;
    console.log("통계 및 리스트 로드 완료:", data);
  } catch (error) {
    if (error.response?.status === 401) {
      alert("로그인이 필요합니다.");
      router.push('/login'); // 로그인이 안 되어 있으면 즉시 이동
    }
  }
})

// MyPageView.vue 내 수정
const enterIDE = (webId) => {
  // authStore에서 현재 로그인한 유저의 닉네임을 가져옵니다.
  const nickname = authStore.user?.nickname || 'guest';
  
  // ✅ 닉네임을 경로에 포함시켜 이동
  router.push(`/ide/${nickname}/${webId}`);
};

// MyPageView.vue <script setup> 내부 수정

const createNewProject = async () => {
  try {
    // 1. 프로젝트 생성 API 호출
    // withCredentials: true 설정 덕분에 세션 쿠키가 함께 전송됩니다.
    const response = await api.post('/projects/create'); 
    const newWebId = response.data; // 서버에서 발급된 webId

    // 2. 현재 사용자 닉네임 가져오기
    const nickname = authStore.user?.nickname || 'guest';

    // 3. 생성된 고유 경로로 이동 (예: /ide/test/25) [cite: 2026-01-19]
    // 이동하면 IDE 컴포넌트에서 해당 webId를 기반으로 데이터를 불러오게 됩니다.
    router.push(`/ide/${nickname}/${newWebId}`);

  } catch (error) {
    console.error("새 프로젝트 생성 실패:", error);
    
    // 세션 만료 시 로그인 페이지로 유도
    if (error.response?.status === 401 || error.response?.status === 403) {
      openModal('로그인이 필요한 서비스입니다.', 'warning', () => {
      router.push('/login');
    });
    } else {
      alert("프로젝트 생성 중 오류가 발생했습니다.");
    }
  }
};

const handleLogout = () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    authStore.logout();
    router.push('/login');
  }
};

const filteredProjects = computed(() => {
  if (currentTab.value === 'MY') return myProjects.value;
  if (currentTab.value === 'SHARED') return sharedProjects.value;
  return [...myProjects.value, ...sharedProjects.value];
});

// ✅ 개별 프로젝트의 편집 상태를 추적하기 위한 함수
const startRename = (web) => {
  if (web.role !== 'OWNER') return;
  web.isEditing = true;
  web.tempTitle = web.title;
};

// MyPageView.vue <script setup> 내 saveNewName 수정
const saveNewName = async (web) => {
  if (!web.isEditing) return;
  
  // 공백 입력 방지
  if (!web.tempTitle.trim()) {
    web.isEditing = false;
    return;
  }

  try {
    // ✅ 경로를 /projects로 맞추고, 데이터 구조를 { name: ... }로 전달
    await api.put(`/projects/${web.id}/name`, { name: web.tempTitle });
    
    // ✅ 성공 시에만 실제 title을 변경하고 편집 모드 종료
    web.title = web.tempTitle;
    web.isEditing = false;
    
    // 로컬 리스트 데이터도 업데이트 (필요 시)
    const target = myProjects.value.find(p => p.id === web.id);
    if (target) target.title = web.tempTitle;
    
  } catch (e) {
    console.error("수정 실패 상세:", e.response?.data || e.message);
    alert("이름 수정에 실패했습니다. 서버 로그를 확인해 주세요.");
    web.isEditing = false;
  }
};

// 날짜 포맷 함수
const formatDate = (date) => {
  if (!date) return 'Just now';
  return new Date(date).toLocaleDateString();
};
  // ✅ 1. 드롭다운 메뉴 상태 관리 변수
const activeMenuId = ref(null);

// ✅ 2. 메뉴 토글 함수: 클릭 시 메뉴를 열거나 닫음
const toggleMenu = (id) => {
  activeMenuId.value = activeMenuId.value === id ? null : id;
};

// ✅ 3. 메뉴 외부 클릭 시 자동으로 닫히는 로직 등록
onMounted(() => {
  window.addEventListener('click', (e) => {
    // 클릭된 요소가 메뉴 영역(.menu-container)이 아니면 메뉴를 닫음
    if (!e.target.closest('.menu-container')) {
      activeMenuId.value = null;
    }
  });
});

// ✅ 4. 프로젝트 삭제 실행 함수
const confirmDelete = async (webId) => {
  activeMenuId.value = null; // 메뉴를 먼저 닫음

  if (!confirm("정말로 이 프로젝트를 삭제하시겠습니까?")) return;

  try {
    // 서버에 삭제 요청 (설계하신 /projects/:id 경로 사용)
    await api.delete(`/projects/${webId}`); 
    
    // UI에서 해당 프로젝트 즉시 제거
    myProjects.value = myProjects.value.filter(p => p.id !== webId);
    
    // 상단 통계 숫자 1 감소
    myProjectCount.value = Math.max(0, myProjectCount.value - 1);
    
    alert("삭제되었습니다.");
  } catch (error) {
    console.error("삭제 실패:", error);
    const msg = error.response?.data?.message || "삭제 권한이 없거나 오류가 발생했습니다.";
    alert(msg);
  }
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
        <p class="bio">{{ authStore.user?.bio || 'Welcome to your workspace!' }}</p>
        
        <div v-if="authStore.user?.techStacks" class="user-tags">
          <span v-for="tag in authStore.user.techStacks.split(',')" :key="tag" class="mini-tag">
            #{{ tag }}
          </span>
        </div>
        
        <div class="action-buttons">
          <button class="btn primary" @click="createNewProject">+ New Project</button>
          <button class="btn" @click="isEditModalOpen = true">Edit Profile</button>
        </div>
      </section>

      <section class="stats-section">
        <div class="stat-card" 
            :class="{ active: currentTab === 'MY' }" 
            @click="currentTab = 'MY'" 
            style="cursor:pointer">
          <div class="stat-number">{{ myProjectCount }}</div>
          <div class="stat-label">My Projects</div>
        </div>

        <div class="stat-card" 
            :class="{ active: currentTab === 'SHARED' }" 
            @click="currentTab = 'SHARED'" 
            style="cursor:pointer">
          <div class="stat-number">{{ sharedProjectCount }}</div>
          <div class="stat-label">Collaborating</div>
        </div>

        <div class="stat-card" 
            :class="{ active: currentTab === 'ALL' }" 
            @click="currentTab = 'ALL'" 
            style="cursor:pointer">
          <div class="stat-number">{{ unreadNotiCount }}</div>
          <div class="stat-label">New Alerts</div>
        </div>
      </section>

      <section class="activity-section">
        <h2 class="activity-title" @click="currentTab = 'ALL'" style="cursor:pointer">
          Your Workspaces <small v-if="currentTab !== 'ALL'">(Filtering: {{ currentTab }})</small>
        </h2>
        
        <div class="project-grid">
          <div v-for="web in filteredProjects" 
              :key="web.id" 
              class="activity-item project-card"
              :class="{ shared: web.role !== 'OWNER' }">
            
            <div class="project-info">
<div class="activity-text">
  {{ web.role === 'OWNER' ? '📁' : '🤝' }}
  
  <span v-if="!web.isEditing" @dblclick="startRename(web)" class="editable-title">
    {{ web.title }}
  </span>
  
  <input v-else 
         v-model="web.tempTitle" 
         @blur="saveNewName(web)" 
         @keyup.enter="saveNewName(web)"
         class="inline-edit-input" 
         autofocus />
</div>
              
              <div class="activity-time">
                {{ web.role }} | Last updated: {{ formatDate(web.updateDate) }}
                <span v-if="web.ownerNickname">| From @{{ web.ownerNickname }}</span>
              </div>
            </div>

            <div class="menu-container" style="position: relative; display: inline-block; margin-right: 12px;">
              <button class="btn-more" @click.stop="toggleMenu(web.id)">⋮</button>
              
              <div v-if="activeMenuId === web.id" class="dropdown-menu">
                  <button v-if="web.role === 'OWNER'" class="delete-opt" @click="confirmDelete(web.id)">
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

.project-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.project-card {
  display: flex;
  justify-content: space-between;
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

/* [위치]: <style scoped> 내의 기존 코드 맨 아래에 추가 */

/* 1. 프로젝트 카드 내부 정렬 */
.project-card {
  display: flex;
  justify-content: space-between; /* 정보는 왼쪽, 버튼들은 오른쪽 끝 [cite: 2026-01-21] */
  align-items: center;
  padding: 1.2rem 1.5rem;
}

/* 2. [핵심] 정보와 버튼 사이를 벌려주는 장치 */
.project-info {
  flex-grow: 1; /* 이 영역이 남는 공간을 다 차지해서 버튼들을 오른쪽으로 밀어냅니다 [cite: 2026-01-21] */
}

/* 3. 점 3개 컨테이너: margin-left: auto를 지우고 간격만 설정 [cite: 2026-01-21] */
.menu-container {
  position: relative;
  display: flex;
  align-items: center;
  margin-left: auto;
  margin-right: 12px; /* ⋮ 버튼과 Open 버튼 사이의 간격 [cite: 2026-01-21] */
}

/* 4. 드롭다운(Delete) 위치: 점 3개 바로 왼쪽 옆 [cite: 2026-01-21] */
.dropdown-menu {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  right: 15px; /* 점 3개에서 왼쪽으로 35px 이동하여 배치 [cite: 2026-01-21] */
  background: transparent;
  border: none;
  z-index: 9999;
}

/* 5. Delete 버튼: Open 버튼과 동일한 크기 (Open 버튼 스타일 참고) [cite: 2026-01-21] */
.delete-opt {
  min-width: 70px;      /* Open 버튼과 동일한 너비 [cite: 2026-01-21] */
  height: 38px;         /* Open 버튼과 동일한 높이 [cite: 2026-01-21] */
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

</style>