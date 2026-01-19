<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth'; // Pinia/Vuex 스토어
import axios from '@/api/axios'; // Axios 인터셉터 설정 파일
import EditProfileModal from '@/modal/EditProfileModal.vue'; // 프로필 수정 모달

const router = useRouter();
const authStore = useAuthStore();

// 1. 데이터 상태 관리 (DB 컬럼 구조 반영)
const myProjects = ref([]);    // 내가 방장인 프로젝트
const sharedProjects = ref([]); // 초대받은 협업 프로젝트
const unreadNotiCount = ref(0); // 읽지 않은 알림 수
const isEditModalOpen = ref(false); // 모달 제어를 위한 상태 변수

// 2. 초기 데이터 로드 (백엔드 API 연동)
onMounted(async () => {
  try {
    // 백엔드 컨트롤러(/api/mypage/data)에서 데이터를 한 번에 가져옴
    const response = await axios.get('/member/me');
    authStore.user = response.data;
    myProjects.value = response.data.myProjects || [];
    sharedProjects.value = response.data.sharedProjects || [];
    unreadNotiCount.value = response.data.unreadNotiCount || 0;
  } catch (error) {
    console.error("데이터 로드 실패:", error);
    // 테스트용 가짜 데이터 (서버 연결 전 확인용)
    // myProjects.value = [{ id: 1, title: '테스트 프로젝트', updateDate: new Date() }];
  }
});

// 3. 페이지 이동 및 액션 핸들러
const enterIDE = (webId) => {
  // 동적 라우팅 /ide/:id 로 이동
  router.push(`/ide/${webId}`);
};

const createNewProject = () => {
  alert('새 프로젝트 생성 모달을 구현할 예정입니다.');
};

const handleLogout = () => {
  if (confirm('로그아웃 하시겠습니까?')) {
    authStore.logout();
    router.push('/login');
  }
};

// 날짜 포맷 함수
const formatDate = (date) => {
  if (!date) return 'Just now';
  return new Date(date).toLocaleDateString();
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
        <div class="stat-card">
          <div class="stat-number">{{ myProjects.length }}</div>
          <div class="stat-label">My Projects</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ sharedProjects.length }}</div>
          <div class="stat-label">Collaborating</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ unreadNotiCount }}</div>
          <div class="stat-label">New Alerts</div>
        </div>
      </section>

      <section class="activity-section">
        <h2 class="activity-title">Your Workspaces</h2>
        
        <div class="project-grid">
          <div v-for="web in myProjects" :key="web.id" class="activity-item project-card">
            <div class="project-info">
              <div class="activity-text">📁 {{ web.title }}</div>
              <div class="activity-time">Owner | Last updated: {{ formatDate(web.updateDate) }}</div>
            </div>
            <button class="btn-sm" @click="enterIDE(web.id)">Open</button>
          </div>

          <div v-for="web in sharedProjects" :key="web.id" class="activity-item project-card shared">
            <div class="project-info">
              <div class="activity-text">🤝 {{ web.title }}</div>
              <div class="activity-time">Editor | From @{{ web.ownerNickname }}</div>
            </div>
            <button class="btn-sm" @click="enterIDE(web.id)">Join</button>
          </div>

          <div v-if="myProjects.length === 0 && sharedProjects.length === 0" class="empty-msg">
            생성하거나 초대받은 프로젝트가 없습니다.
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

</style>