<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import api from '@/api/axios';
import GlobalModal from '@/modal/GlobalModal.vue'; // 알림용 모달

const router = useRouter();
const authStore = useAuthStore();
// 🔥 [신규] "이 프로젝트가 내 것인가?" 판단하는 변수
const isMyProject = computed(() => {
  // 1. 로그인 안 했거나, 선택된 프로젝트가 없으면 '내 것' 아님
  if (!authStore.isAuthed || !authStore.me || !selectedProject.value) {
    return false;
  }
  
  // 2. 닉네임 비교 (내 정보 구조가 가끔 달라서 안전하게 처리)
  const myNickname = authStore.me.nickname || authStore.me.member?.nickname;
  const ownerNickname = selectedProject.value.ownerNickname;

  return myNickname === ownerNickname;
});

// 🔥 [신규] 버튼 하나로 '수정'과 '리메이크'를 분기 처리하는 함수
const handleMainAction = () => {
  // A. 내 프로젝트면 -> 바로 IDE로 이동 (수정)
  if (isMyProject.value) {
    goToProject(selectedProject.value);
    return;
  }

  // B. 남의 프로젝트면 -> 리메이크 실행
  handleRemake();
};
// --- 상태 관리 ---
const allProjects = ref([]);
const searchQuery = ref('');
const isLoading = ref(false);

// --- 페이지네이션 ---
const page = ref(0);
const size = 12; 
const hasMore = ref(true);

// --- 모달 관련 상태 ---
const selectedProject = ref(null); // 현재 선택된 프로젝트 (미리보기용)
const isPreviewOpen = ref(false);  // 모달 열림 여부
const isRemaking = ref(false);     // 리메이크 로딩 상태

// 전역 모달 (에러/알림용)
const globalModal = ref({ open: false, message: '', type: 'info', onConfirm: null });
const openGlobalModal = (msg, type = 'info', confirmFn = null) => {
  globalModal.value = { open: true, message: msg, type, onConfirm: confirmFn };
};
const closeGlobalModal = () => {
  if(globalModal.value.onConfirm) globalModal.value.onConfirm();
  globalModal.value.open = false;
};

// HTML 요소 참조
const loadTrigger = ref(null);
const scrollContainer = ref(null); 
let observer = null;

// --- 데이터 로드 ---
const fetchProjects = async (isReset = false) => {
  if (isLoading.value || (!hasMore.value && !isReset)) return;
  try {
    isLoading.value = true;
    if (isReset) {
      page.value = 0;
      allProjects.value = [];
      hasMore.value = true;
    }
    const params = { page: page.value, size: size, keyword: searchQuery.value };
    const response = await api.get('/projects/explore', { params });
    const newProjects = response.data;

    if (newProjects && newProjects.length > 0) {
      allProjects.value = [...allProjects.value, ...newProjects];
      page.value++; 
      if (newProjects.length < size) hasMore.value = false;
    } else {
      hasMore.value = false;
    }
  } catch (error) {
    console.error("❌ 로드 실패:", error);
  } finally {
    isLoading.value = false;
  }
};

let searchTimeout = null;
watch(searchQuery, () => {
  if (searchTimeout) clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => fetchProjects(true), 500);
});

onMounted(async () => {
  await fetchProjects(true);
  observer = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting && hasMore.value && !isLoading.value) {
      fetchProjects(false);
    }
  }, { root: scrollContainer.value, threshold: 0.1 });
  if (loadTrigger.value) observer.observe(loadTrigger.value);
});

onUnmounted(() => { if (observer) observer.disconnect(); });


// 🔥 [신규] 리메이크(복제) 버튼 핸들러
const handleRemake = async () => {
  if (!authStore.isAuthed) {
    openGlobalModal('로그인이 필요한 기능입니다.', 'warning', () => router.push('/login'));
    return;
  }
  
  if (!selectedProject.value) return;

  try {
    isRemaking.value = true;
    // 1. 서버에 복제 요청
    const res = await api.post(`/projects/${selectedProject.value.id}/remake`);
    const newWebId = res.data; // 생성된 내 프로젝트 ID

    // 2. 성공 시 내 IDE로 이동
    const myNickname = authStore.me?.nickname || 'me';
    openGlobalModal('프로젝트가 복제되었습니다! 작업실로 이동합니다.', 'success', () => {
       router.push(`/ide/${myNickname}/${newWebId}`);
    });

  } catch (e) {
    console.error(e);
    openGlobalModal('프로젝트 복제에 실패했습니다.', 'error');
  } finally {
    isRemaking.value = false;
  }
};

// HTML 주입 헬퍼 (모달에서도 사용)
const getPreviewHtml = (project) => {
  if (!project) return '';
  let rawHtml = project.previewHtml || '';
  if (!rawHtml) {
    return `<html><body style="margin:0;display:flex;justify-content:center;align-items:center;height:100vh;background:#f8f9fa;color:#ccc;font-family:sans-serif;"><div style="text-align:center;">Empty</div></body></html>`;
  }
  const styleInjection = `<style>html,body{margin:0;padding:0;width:100%;height:100vh;overflow:hidden;background-color:#fff;}::-webkit-scrollbar{display:none;}</style>`;
  return rawHtml.includes('</head>') ? rawHtml.replace('</head>', `${styleInjection}</head>`) : styleInjection + rawHtml;
};

const formatDate = (date) => date ? new Date(date).toLocaleDateString() : '';
// --- 포맷팅 & 기능 함수 ---

// 1. 숫자가 1000이 넘으면 K 단위로 변환 (예: 1200 -> 1.2K)
const formatViews = (count) => {
  if (!count) return '0';
  if (count >= 1000) {
    return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
  }
  return count.toString();
};

// 2. 조회수 업데이트 API 호출
const updateProjectHit = async (projectId) => {
  try {
    // 백엔드 주소가 /projects/hit/${id} 임을 확인하세요.
    await api.patch(`/projects/hit/${projectId}`);
    
    // 로컬 데이터에도 즉시 반영 (다시 로드하지 않아도 숫자가 올라가 보이게)
    const project = allProjects.value.find(p => p.id === projectId);
    if (project) project.views = (project.views || 0) + 1;
  } catch (err) {
    console.warn("조회수 증가 실패:", err);
  }
};

// 🔥 [수정] 카드 클릭 시 실행되는 함수에 조회수 증가 추가
const openPreviewModal = (project) => {
  selectedProject.value = project;
  isPreviewOpen.value = true;
  
  // 모달을 열 때 조회수 증가 API 호출
  updateProjectHit(project.id);
};
// 🔥 [누락된 함수 추가] 내 프로젝트 수정하러 이동하기
const goToProject = (project) => {
  if (!project) return;
  
  // 내 닉네임 가져오기 (로그인 상태라고 가정)
  const myNickname = authStore.me?.nickname || authStore.me?.member?.nickname;
  
  if (myNickname && project.id) {
    router.push(`/ide/${myNickname}/${project.id}`);
  } else {
    openGlobalModal('프로젝트 정보를 찾을 수 없습니다.', 'error');
  }
};
</script>

<template>
  <div class="explore-wrapper">
    <header>
      <div class="header-container">
        <div class="logo"><div class="logo-icon">&lt;/&gt;</div><span>Web Crafter</span> <span class="badge">Explore</span></div>
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
          <input v-model="searchQuery" type="text" placeholder="Search for projects...">
        </div>
      </section>

      <section class="grid-section" ref="scrollContainer">
        <div class="card-grid">
          <div 
            v-for="project in allProjects" 
            :key="project.id" 
            class="project-card"
            @click="openPreviewModal(project)" 
          >
            <div class="preview-window">
              <div class="iframe-container">
                <iframe :srcdoc="getPreviewHtml(project)" sandbox="allow-scripts allow-same-origin" loading="lazy" class="scaled-iframe" scrolling="no"></iframe>
              </div>
              <div class="overlay"><button class="view-btn">View Details</button></div>
            </div>
            <div class="card-info">
              <div class="info-header">
                <h3 class="title">{{ project.title || 'Untitled Project' }}</h3>
                <span class="author">@{{ project.ownerNickname }}</span>
              </div>
              <div class="info-footer">
                <div class="stats"><span>👀 {{ formatViews(project.views) }}</span></div>
                <span class="date">{{ formatDate(project.updateDate) }}</span>
              </div>
            </div>
          </div>
        </div>
        <div ref="loadTrigger" class="scroll-trigger">
           <div v-if="isLoading" class="loading-msg"><span class="spinner">⏳</span> Loading...</div>
        </div>
      </section>
    </main>

    <div v-if="isPreviewOpen && selectedProject" class="preview-modal-overlay" @click.self="isPreviewOpen = false">
      <div class="preview-modal-content">
        <div class="modal-header">
          <div class="modal-title-area">
            <h2>{{ selectedProject.title || 'Untitled Project' }}</h2>
            <span class="modal-author">Created by {{ selectedProject.ownerNickname }}</span>
          </div>
          <button class="close-btn" @click="isPreviewOpen = false">✕</button>
        </div>

        <div class="modal-body">
          <div class="live-preview-box">
             <iframe 
                :srcdoc="getPreviewHtml(selectedProject)" 
                sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
                class="live-iframe"
             ></iframe>
          </div>
        </div>

        <div class="modal-footer">
          <div class="footer-left">
             <span class="date-badge">Last updated: {{ formatDate(selectedProject.updateDate) }}</span>
          </div>
          <div class="footer-right">
             <button class="action-btn cancel" @click="isPreviewOpen = false">닫기</button>
             
             <button 
               class="action-btn remake" 
               @click="handleMainAction" 
               :disabled="isRemaking"
             >
               <span v-if="isRemaking">⏳ 처리 중...</span>
               
               <span v-else-if="isMyProject">✏️ 이어서 만들기 (수정)</span>
               
               <span v-else>⚡ 리메이크 (내 걸로 가져오기)</span>
             </button>
          </div>
        </div>
      </div>
    </div>

    <GlobalModal
      :open="globalModal.open"
      :message="globalModal.message"
      :type="globalModal.type"
      @confirm="closeGlobalModal"
    />
  </div>
</template>

<style scoped>
/* (기존 Explore 스타일은 그대로 유지하시고, 아래 모달 스타일만 추가하세요) */
.explore-wrapper { min-height: 100vh; background: linear-gradient(135deg, #0a1628 0%, #0d1f3c 100%); color: #fff; font-family: 'Inter', sans-serif; overflow: hidden; }
header { background: rgba(10, 22, 40, 0.95); padding: 1rem 2rem; position: sticky; top: 0; z-index: 100; border-bottom: 1px solid rgba(0, 217, 255, 0.1); }
.header-container { max-width: 1400px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; }
.logo { display: flex; align-items: center; gap: 0.8rem; font-size: 1.5rem; font-weight: 700; }
.logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg, #00d9ff, #0099cc); border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #0a1628; font-weight: 900; }
.badge { font-size: 0.75rem; background: rgba(0, 217, 255, 0.15); color: #00d9ff; padding: 2px 8px; border-radius: 12px; margin-left: 6px; }
nav a { color: #b0b8c1; text-decoration: none; margin-left: 2rem; font-weight: 500; transition: color 0.2s; }
nav a:hover, nav a.router-link-active { color: #00d9ff; }
main { max-width: 1400px; margin: 0 auto; padding: 2rem; height: calc(100vh - 80px); display: flex; flex-direction: column; }
.search-section { text-align: center; margin-bottom: 2rem; flex-shrink: 0; }
.page-title { font-size: 2rem; margin-bottom: 1rem; font-weight: 700; }
.search-box { position: relative; max-width: 500px; margin: 0 auto; }
.search-box input { width: 100%; padding: 0.8rem 1rem 0.8rem 2.5rem; background: rgba(255, 255, 255, 0.05); border: 1px solid rgba(0, 217, 255, 0.2); border-radius: 50px; color: #fff; font-size: 1rem; }
.search-icon { position: absolute; left: 1rem; top: 50%; transform: translateY(-50%); color: #64748b; }
.grid-section { flex-grow: 1; overflow-y: auto; padding: 1rem; background: rgba(0, 0, 0, 0.2); border-radius: 16px; border: 1px solid rgba(0, 217, 255, 0.05); }
.grid-section::-webkit-scrollbar { width: 8px; }
.grid-section::-webkit-scrollbar-thumb { background: rgba(0, 217, 255, 0.3); border-radius: 10px; }
.grid-section::-webkit-scrollbar-track { background: transparent; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 1.5rem; }
.project-card { background: rgba(255, 255, 255, 0.03); border-radius: 12px; overflow: hidden; border: 1px solid rgba(0, 217, 255, 0.1); transition: all 0.3s ease; cursor: pointer; height: 320px; display: flex; flex-direction: column; position: relative; }
.project-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.3); border-color: rgba(0, 217, 255, 0.5); }
.preview-window { width: 100%; aspect-ratio: 16 / 9; position: relative; overflow: hidden; background: #fff; border-bottom: 1px solid rgba(255, 255, 255, 0.05); }
.iframe-container { width: 400%; height: 400%; transform: scale(0.25); transform-origin: top left; pointer-events: none; background-color: #fff; }
.scaled-iframe { width: 100%; height: 100%; border: none; background-color: #fff; }
.overlay { position: absolute; inset: 0; background: rgba(10, 22, 40, 0.6); backdrop-filter: blur(2px); display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s ease; }
.project-card:hover .overlay { opacity: 1; }
.view-btn { background: #00d9ff; color: #0a1628; border: none; padding: 0.6rem 1.2rem; border-radius: 20px; font-weight: 700; font-size: 0.9rem; cursor: pointer; transform: translateY(10px); transition: transform 0.2s ease; }
.project-card:hover .view-btn { transform: translateY(0); }
.card-info { padding: 1.2rem; flex-grow: 1; background: rgba(13, 31, 60, 0.4); display: flex; flex-direction: column; }
.info-header { margin-bottom: auto; }
.title { font-size: 1rem; font-weight: 700; margin: 0 0 4px 0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.author { font-size: 0.85rem; color: #00d9ff; }
.info-footer { display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; color: #64748b; margin-top: 10px; }
.scroll-trigger { padding: 2rem; text-align: center; color: #64748b; }
.spinner { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { 100% { transform: rotate(360deg); } }

/* ======================================================
   🔥 [NEW] 상세 미리보기 모달 스타일
   ====================================================== */
.preview-modal-overlay {
  position: fixed;
  top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.8);
  backdrop-filter: blur(5px);
  z-index: 2000;
  display: flex;
  justify-content: center;
  align-items: center;
  animation: fadeIn 0.2s ease;
}

.preview-modal-content {
  background: #1a1a2e;
  border: 1px solid rgba(0, 212, 255, 0.3);
  border-radius: 16px;
  width: 90%;
  max-width: 1200px;
  height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px rgba(0,0,0,0.5);
}

.modal-header {
  border-bottom: 1px solid rgba(255,255,255,0.1);
  border-radius: 16px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.modal-title-area h2 { margin: 0; font-size: 1rem; color: #fff; }
.modal-author { font-size: 0.9rem; color: #00d4ff; }

.close-btn {
  background: none; border: none; color: #aaa; font-size: 1.5rem; cursor: pointer;
  padding: 0.5rem; transition: color 0.2s;
}
.close-btn:hover { color: #fff; }

.modal-body {
  flex: 1;
  background: #000;
  position: relative;
  overflow: hidden;
}

.live-preview-box { width: 100%; height: 100%; }
.live-iframe { width: 100%; height: 100%; border: none; background: #fff; }

.modal-footer {
  padding: 1rem 1.5rem;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #151525;
  border-radius: 0 0 16px 16px;
}

.date-badge { font-size: 0.8rem; color: #666; }

.action-btn {
  padding: 0.7rem 1.5rem;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.action-btn.cancel {
  background: transparent;
  color: #aaa;
  margin-right: 10px;
}
.action-btn.cancel:hover { color: #fff; }

.action-btn.remake {
  background: linear-gradient(135deg, #00d4ff 0%, #0077ff 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(0, 212, 255, 0.3);
}
.action-btn.remake:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 212, 255, 0.5);
}
.action-btn.remake:disabled {
  background: #555;
  cursor: not-allowed;
  transform: none;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
</style>