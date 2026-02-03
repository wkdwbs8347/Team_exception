<script setup>
import { ref, watch, reactive } from 'vue'
import { 
  Palette, 
  Settings, 
  Check
} from 'lucide-vue-next'
import api from '@/api/axios';

const props = defineProps({
  open: Boolean,
  project: Object,
  currentThemeId: {
    type: String,
    default: 'default'
  }
})

const emit = defineEmits(['close', 'apply'])

/* ============================================================
   🗂️ 카테고리 (사이드바 메뉴)
   ============================================================ */
const categories = [
  { id: 'general', label: '일반 설정', icon: Settings },
  { id: 'theme', label: '테마 디자인', icon: Palette },
]

const activeCategory = ref('general')

/* ============================================================
   🎨 테마 데이터
   ============================================================ */
const themeList = [
  { id: 'default', name: 'Default Dark', toolboxColor: '#dcdcdcba', workspaceColor: '#ffffff' },
  { id: 'midnight', name: 'Midnight Ocean', toolboxColor: '#0f172a', workspaceColor: '#1e293b' },
  { id: 'forest', name: 'Deep Forest', toolboxColor: '#064e3b', workspaceColor: '#ecfdf5' },
  { id: 'cyberpunk', name: 'Cyber Neon', toolboxColor: '#2a0a2e', workspaceColor: '#1a1a2e' },
  { id: 'monochrome', name: 'Clean Gray', toolboxColor: '#4b5563', workspaceColor: '#f3f4f6' },
  { id: 'cherry', name: 'Cherry Blossom', toolboxColor: '#fff0f5', workspaceColor: '#fff' }
]

/* ============================================================
   📝 폼 상태 관리 (언어, 에디터 설정 제거됨)
   ============================================================ */
const formData = reactive({
  projectName: 'My Awesome Project',
  themeId: props.currentThemeId
})

// 모달 열릴 때 props 값으로 초기화
watch(
  () => [props.open, props.project?.title], 
  ([isOpen, newTitle]) => {
    if (isOpen) {
      formData.themeId = props.currentThemeId;
      
      const actualName = newTitle;
      if (actualName) {
        formData.projectName = actualName;
      }
      
      activeCategory.value = 'general';
    }
  }, 
  { immediate: true }
);

const selectTheme = (id) => {
  formData.themeId = id
}

// 최종 저장
const handleSave = async () => {
  try {
    // 1. 서버 DB 저장
    await api.put(`/projects/${props.project.id}/name`, { 
      name: formData.projectName 
    });

    // 2. 부모 컴포넌트에 반영
    const selectedTheme = themeList.find(t => t.id === formData.themeId);
    
    emit('apply', {
      theme: selectedTheme,
      settings: { ...formData }, // 이제 여기엔 projectName과 themeId만 들어감
      newName: formData.projectName
    });

    emit('close');

  } catch (error) {
    console.error("저장 실패:", error);
    alert("서버 저장 중 오류가 발생했습니다.");
  }
};
</script>

<template>
  <transition name="modal-fade">
    <div v-if="open" class="modal-backdrop">
      <div class="modal-card">
        
        <aside class="sidebar">
          <div class="sidebar-header">
            <span class="sidebar-title">설정</span>
          </div>
          
          <nav class="sidebar-nav">
            <button 
              v-for="cat in categories" 
              :key="cat.id"
              class="nav-item"
              :class="{ active: activeCategory === cat.id }"
              @click="activeCategory = cat.id"
            >
              <component :is="cat.icon" :size="18" />
              <span>{{ cat.label }}</span>
            </button>
          </nav>
        </aside>

        <main class="content-area">
          
          <div v-if="activeCategory === 'general'" class="tab-panel">
            <h3 class="panel-title">일반 설정</h3>
            
            <div class="form-group">
              <label>프로젝트 이름</label>
              <input 
                v-model="formData.projectName" 
                type="text" 
                class="input-field" 
                @keyup.enter="handleSave"
              />
            </div>
            </div>

          <div v-if="activeCategory === 'theme'" class="tab-panel">
            <h3 class="panel-title">테마 디자인</h3>
            <p class="panel-desc">작업 환경의 색상 테마를 선택하세요.</p>
            
            <div class="theme-grid">
              <div 
                v-for="theme in themeList" 
                :key="theme.id"
                class="theme-item"
                :class="{ active: formData.themeId === theme.id }"
                @click="selectTheme(theme.id)"
              >
                <div class="preview-box">
                  <div class="preview-toolbox" :style="{ backgroundColor: theme.toolboxColor }"></div>
                  <div class="preview-workspace" :style="{ backgroundColor: theme.workspaceColor }"></div>
                  <div v-if="formData.themeId === theme.id" class="check-overlay">
                    <Check :size="20" stroke-width="3" />
                  </div>
                </div>
                <span class="theme-name">{{ theme.name }}</span>
              </div>
            </div>
          </div>

          <div class="action-footer">
            <button class="btn-cancel" @click="$emit('close')">취소</button>
            <button class="btn-save" @click="handleSave">저장하기</button>
          </div>
        </main>

      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ===============================
   🌌 배경 및 모달 레이아웃
================================ */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 15, 30, 0.75);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100000;
}

.modal-card {
  width: 750px;
  height: 500px;
  background: #1a1a2e;
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 16px;
  display: flex;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  animation: popIn 0.3s ease-out;
}

/* ===============================
   🟢 왼쪽 사이드바
================================ */
.sidebar {
  width: 200px;
  background: rgba(0, 0, 0, 0.2);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  flex-direction: column;
  padding: 1.5rem 1rem;
}

.sidebar-title {
  font-size: 1.2rem;
  font-weight: 800;
  color: #fff;
  margin-bottom: 2rem;
  display: block;
  padding-left: 0.5rem;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 4px;
  background: transparent;
  border: none;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 0.9rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
}

.nav-item.active {
  background: linear-gradient(90deg, rgba(0, 212, 255, 0.15), transparent);
  color: #00d4ff;
  font-weight: 700;
  border-left: 3px solid #00d4ff;
}

/* ===============================
   🔵 오른쪽 콘텐츠 영역
================================ */
.content-area {
  flex: 1;
  padding: 2rem;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(26,26,46,0.95), rgba(20,20,35,0.98));
  position: relative;
}

.tab-panel {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 60px;
}

.tab-panel::-webkit-scrollbar {
  width: 6px;
}
.tab-panel::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.1);
  border-radius: 3px;
}

.panel-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #fff;
  margin-bottom: 0.5rem;
}

.panel-desc {
  color: #6b7280;
  font-size: 0.9rem;
  margin-bottom: 2rem;
}

/* --- 공통 폼 스타일 --- */
.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  color: #cbd5e1;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.input-field {
  width: 100%;
  padding: 0.8rem;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
  outline: none;
  font-size: 0.95rem;
}

.input-field:focus {
  border-color: #00d4ff;
}

/* --- 테마 그리드 스타일 --- */
.theme-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 15px;
}

.theme-item {
  cursor: pointer;
  text-align: center;
}

.preview-box {
  width: 100%;
  aspect-ratio: 4/3;
  border-radius: 8px;
  border: 2px solid rgba(255,255,255,0.1);
  display: flex;
  overflow: hidden;
  position: relative;
  margin-bottom: 8px;
  transition: 0.2s;
}

.preview-toolbox { width: 30%; height: 100%; }
.preview-workspace { width: 70%; height: 100%; }

.theme-item.active .preview-box {
  border-color: #00d4ff;
  box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
}

.check-overlay {
  position: absolute; inset: 0;
  background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
  color: #00d4ff;
}

.theme-name {
  font-size: 0.8rem;
  color: #9ca3af;
}

/* --- 에디터 토글 스위치 스타일 삭제됨 (필요 없음) --- */

/* ===============================
   🔴 하단 액션 버튼
================================ */
.action-footer {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid rgba(255,255,255,0.05);
}

.btn-cancel {
  padding: 0.6rem 1.2rem;
  background: transparent;
  border: 1px solid rgba(255,255,255,0.2);
  color: #9ca3af;
  border-radius: 8px;
  cursor: pointer;
}

.btn-save {
  padding: 0.6rem 1.5rem;
  background: linear-gradient(135deg, #00d4ff, #0099cc);
  border: none;
  color: #0f172a;
  font-weight: 700;
  border-radius: 8px;
  cursor: pointer;
}

.btn-save:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 212, 255, 0.3);
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}
</style>