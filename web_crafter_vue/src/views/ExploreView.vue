<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import api from '@/api/axios';

const router = useRouter();
const authStore = useAuthStore();

// 상태 관리
const allProjects = ref([]);
const searchQuery = ref('');
const isLoading = ref(true);
const hoverProjectId = ref(null); // 마우스 오버한 카드 추적

// 초기 데이터 로드
onMounted(async () => {
  try {
    // 📡 백엔드 API 호출 (MyBatis로 수정한 로직)
    // 파라미터로 page, size를 조절할 수 있습니다. (기본값 사용)
    const response = await api.get('/projects/explore');
    allProjects.value = response.data;
  } catch (error) {
    console.error("프로젝트 목록 로드 실패:", error);
  } finally {
    isLoading.value = false;
  }
});

// 검색 필터링 (제목 또는 작성자)
const filteredProjects = computed(() => {
  if (!searchQuery.value) return allProjects.value;
  const query = searchQuery.value.toLowerCase();
  return allProjects.value.filter(p => 
    p.title.toLowerCase().includes(query) || 
    p.ownerNickname.toLowerCase().includes(query)
  );
});

// 상세 페이지(IDE)로 이동
const goToProject = (project) => {
  const nickname = project.ownerNickname || 'guest';
  router.push(`/ide/${nickname}/${project.id}`);
};

// 💡 [핵심] DB에서 가져온 HTML/CSS 코드로 미리보기용 HTML 문자열 생성
const getPreviewHtml = (project) => {
  let html = project.htmlContent || '';
  let css = project.cssContent || '';

  // 🛡️ 예외 처리: 만약 저장된 데이터가 순수 HTML이 아니라 JSON(Blockly 상태)이라면?
  // (DB에 HTML 스냅샷을 따로 저장하지 않는 한, Blockly JSON은 바로 보여줄 수 없습니다)
  if (html.trim().startsWith('{') || html.trim().startsWith('<xml')) {
     // 코드가 JSON이나 XML이면 미리보기 불가 메시지 출력 (또는 기본 이미지)
     return `
       <html>
         <body style="display:flex;justify-content:center;align-items:center;height:100%;background:#fff;margin:0;">
           <div style="text-align:center;color:#888;">
             <div style="font-size:40px;margin-bottom:10px;">🧩</div>
             <div style="font-size:12px;">No Preview Available</div>
           </div>
         </body>
       </html>
     `;
  }

  // 정상적인 HTML이라면 Iframe용 전체 문서 조립
  return `
    <!DOCTYPE html>
    <html>
      <head>
        <style>
          /* 미리보기용 기본 스타일 초기화 */
          body { margin: 0; padding: 0; overflow: hidden; background-color: #fff; }
          /* 스크롤바 숨김 (깔끔하게 보이기 위해) */
          ::-webkit-scrollbar { display: none; }
          /* 사용자 정의 CSS 주입 */
          ${css}
        </style>
      </head>
      <body>
        ${html}
      </body>
    </html>
  `;
};

// 날짜 포맷
const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleDateString();
};
</script>

<template>
  <div class="explore-wrapper">
    <header>
      <div class="header-container">
        <div class="logo">
          <div class="logo-icon">&lt;/&gt;</div>
          <span>Web Crafter</span> <span class="badge">Explore</span>
        </div>
        <nav>
          <router-link to="/explore" class="active">Explore</router-link>
          <router-link to="/mypage">My Page</router-link>
          <router-link to="/ide/new">New Project</router-link>
          <a v-if="authStore.isAuthed" href="#" @click.prevent="authStore.logout()">Logout</a>
          <router-link v-else to="/login">Login</router-link>
        </nav>
      </div>
    </header>

    <main>
      <section class="search-section">
        <h1 class="page-title">Discover Projects</h1>
        <div class="search-box">
          <span class="search-icon">🔍</span>
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="Search for projects, tags, or creators..."
          >
        </div>
      </section>

      <section class="grid-section">
        <div v-if="isLoading" class="loading-msg">
          <span class="spinner">⏳</span> 로딩 중입니다...
        </div>
        
        <div v-else-if="filteredProjects.length > 0" class="card-grid">
          <div 
            v-for="project in filteredProjects" 
            :key="project.id" 
            class="project-card"
            @mouseenter="hoverProjectId = project.id"
            @mouseleave="hoverProjectId = null"
            @click="goToProject(project)"
          >
            <div class="preview-window">
              <div class="iframe-container" v-if="hoverProjectId === project.id">
                <iframe 
                  :srcdoc="getPreviewHtml(project)"
                  frameborder="0"
                  scrolling="no"
                  class="scaled-iframe"
                ></iframe>
              </div>
              
              <div v-else class="placeholder">
                <span class="code-icon">⚡</span>
                <span class="placeholder-text">Hover to Preview</span>
              </div>

              <div class="overlay">
                <button class="view-btn">Open Project</button>
              </div>
            </div>

            <div class="card-info">
              <div class="info-header">
                <h3 class="title">{{ project.title }}</h3>
                <span class="author">@{{ project.ownerNickname }}</span>
              </div>
              <div class="info-footer">
                <div class="stats">
                  <span>👀 {{ project.views || 0 }}</span>
                  </div>
                <span class="date">{{ formatDate(project.updateDate) }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-msg">
          검색 결과가 없습니다.
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
/* ===============================
   전체 레이아웃 및 헤더
================================ */
.explore-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #0a1628 0%, #0d1f3c 100%);
  color: #fff;
  font-family: 'Inter', sans-serif;
}

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
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  font-size: 1.5rem;
  font-weight: 700;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, #00d9ff, #0099cc);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0a1628;
  font-weight: 900;
}

.badge {
  font-size: 0.75rem;
  background: rgba(0, 217, 255, 0.15);
  color: #00d9ff;
  padding: 2px 8px;
  border-radius: 12px;
  margin-left: 6px;
}

nav a {
  color: #b0b8c1;
  text-decoration: none;
  margin-left: 2rem;
  font-weight: 500;
  transition: color 0.2s;
}

nav a:hover, nav a.router-link-active {
  color: #00d9ff;
}

/* ===============================
   메인 컨텐츠
================================ */
main {
  max-width: 1400px;
  margin: 0 auto;
  padding: 3rem 2rem;
}

.search-section {
  text-align: center;
  margin-bottom: 3.5rem;
}

.page-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  background: linear-gradient(to right, #fff, #00d9ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.search-box {
  position: relative;
  max-width: 600px;
  margin: 0 auto;
}

.search-box input {
  width: 100%;
  padding: 1rem 1rem 1rem 3rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 217, 255, 0.2);
  border-radius: 50px;
  color: white;
  font-size: 1.1rem;
  transition: all 0.3s ease;
}

.search-box input:focus {
  outline: none;
  border-color: #00d9ff;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 3px rgba(0, 217, 255, 0.1);
}

.search-icon {
  position: absolute;
  left: 1.2rem;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 1.2rem;
}

/* ===============================
   카드 그리드
================================ */
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 2rem;
}

.project-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(0, 217, 255, 0.1);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  position: relative;
}

.project-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.3);
  border-color: rgba(0, 217, 255, 0.4);
}

/* 1. 미리보기 창 */
.preview-window {
  height: 200px;
  background: #000;
  position: relative;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

/* 🚀 CSS Scale: 내용을 50%로 축소하여 보여줌 */
.iframe-container {
  width: 200%;  /* 실제 너비 2배 */
  height: 200%; /* 실제 높이 2배 */
  transform: scale(0.5); /* 0.5배 축소 */
  transform-origin: top left;
  pointer-events: none; /* iframe 내부 클릭 방지 */
}

.scaled-iframe {
  width: 100%;
  height: 100%;
  border: none;
  background-color: #fff;
}

/* 플레이스홀더 */
.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: radial-gradient(circle at center, rgba(0,217,255,0.08), transparent);
}

.code-icon {
  font-size: 2.2rem;
  opacity: 0.6;
}

.placeholder-text {
  font-size: 0.85rem;
  color: #64748b;
}

/* 오버레이 */
.overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.project-card:hover .overlay {
  opacity: 1;
}

.view-btn {
  background: #00d9ff;
  color: #0a1628;
  border: none;
  padding: 0.7rem 1.5rem;
  border-radius: 20px;
  font-weight: 700;
  font-size: 0.9rem;
  cursor: pointer;
  transform: translateY(10px);
  transition: transform 0.3s ease;
}

.project-card:hover .view-btn {
  transform: translateY(0);
}

/* 2. 카드 정보 */
.card-info {
  padding: 1.2rem;
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.info-header {
  margin-bottom: 0.8rem;
}

.title {
  font-size: 1.1rem;
  font-weight: 700;
  color: #fff;
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.author {
  font-size: 0.85rem;
  color: #00d9ff;
}

.info-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  color: #64748b;
  font-size: 0.8rem;
}

.loading-msg, .empty-msg {
  text-align: center;
  font-size: 1.2rem;
  color: #7a8a99;
  padding: 4rem;
}

.spinner {
  display: inline-block;
  animation: spin 1.5s linear infinite;
}

@keyframes spin { 100% { transform: rotate(360deg); } }
</style>