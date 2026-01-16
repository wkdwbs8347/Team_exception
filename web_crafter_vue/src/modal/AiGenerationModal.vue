<script setup>
import { ref, watch } from 'vue';

const props = defineProps({
  open: Boolean,
});

const emit = defineEmits(['close', 'generate']);

// 상태 변수들
const aiPrompt = ref('');
const isGenerating = ref(false);
const aiPromptError = ref(false);

// 모달 열릴 때 초기화
watch(() => props.open, (isOpen) => {
  if (isOpen) {
    aiPrompt.value = '';
    aiPromptError.value = false;
  }
});

// 🔥 [핵심] 백엔드로 요청 보내는 함수
const handleGenerate = async () => {
  if (!aiPrompt.value.trim()) return;

  isGenerating.value = true;
  aiPromptError.value = false;

  // 1. 요청 시작 로그
  console.log(`%c🚀 [AI 요청 시작] 프롬프트: "${aiPrompt.value}"`, "color: #00d4ff; font-weight: bold;");

  try {
    // 스프링부트 서버로 POST 요청
    const response = await fetch('http://localhost:8080/api/ai/generate', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json' 
      },
      body: JSON.stringify({ prompt: aiPrompt.value })
    });

    if (!response.ok) {
      throw new Error(`서버 오류: ${response.status}`);
    }

    // 2. 응답 데이터 확인
    const data = await response.json();
    
    // 🔥 [디버깅] 여기서 콘솔에 XML을 찍어줍니다!
    console.log("%c✅ [AI 응답 성공] 받은 데이터:", "color: #00ff88; font-weight: bold;", data);

    if (data.xml && data.xml.trim() !== '<xml></xml>') {
      console.log("%c📜 [XML 코드 확인]:", "color: yellow;", data.xml);
      
      emit('generate', data.xml); // 부모에게 XML 전달
      emit('close'); 
    } else {
      console.warn("⚠️ [주의] 서버에서 빈 XML(<xml></xml>)이 반환되었습니다.");
      throw new Error('AI가 유효한 블록 코드를 생성하지 못했습니다.');
    }

  } catch (error) {
    console.error("%c❌ [AI 요청 실패]:", "color: #ff5050; font-weight: bold;", error);
    aiPromptError.value = true;
  } finally {
    isGenerating.value = false;
  }
};
</script>

<template>
  <Teleport to="body">
    <transition name="modal-fade">
      <div v-if="open" class="modal-backdrop">
        <div class="modal-card ai-modal-size">
          
          <div class="modal-header">
            <div class="icon-badge ai-badge">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L12 3Z"/></svg>
            </div>
            <h2 class="modal-title">AI 페이지 생성기</h2>
          </div>

          <p class="modal-desc">
            만들고 싶은 화면을 설명하면 AI가 블록을 조립해줍니다.<br>
            <span style="font-size: 0.8em; opacity: 0.7;">(예: "로그인 화면 만들어줘", "빨간 버튼이 있는 카드")</span>
          </p>

          <div class="ai-input-wrapper">
            <textarea
              v-model="aiPrompt"
              placeholder="요청사항 입력..."
              class="ai-textarea-styled"
              :class="{ 'input-error': aiPromptError }"
              :disabled="isGenerating"
            ></textarea>
          </div>

          <div class="btn-group">
            <button class="modal-btn cancel" @click="$emit('close')" :disabled="isGenerating">취소</button>
            <button 
              class="modal-btn apply ai-generate-btn" 
              @click="handleGenerate"
              :disabled="isGenerating || !aiPrompt.trim()"
            >
              <span v-if="isGenerating">🔄 생성 중...</span>
              <span v-else>블록 생성하기 ✨</span>
            </button>
          </div>

        </div>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
/* 모달 스타일 유지 */
.modal-backdrop {
  position: fixed; inset: 0;
  background: rgba(10, 15, 30, 0.75);
  backdrop-filter: blur(8px);
  display: flex; align-items: center; justify-content: center;
  z-index: 100000;
}

.modal-card {
  background: linear-gradient(135deg, rgba(15, 15, 30, 0.95), rgba(26, 26, 46, 0.95));
  border: 1.5px solid rgba(156, 39, 176, 0.3);
  border-radius: 20px;
  padding: 2rem;
  width: 100%;
  box-shadow: 0 30px 80px rgba(0,0,0,0.6), 0 0 40px rgba(156, 39, 176, 0.15);
  animation: popIn 0.35s ease-out;
  display: flex; flex-direction: column;
}

.ai-modal-size { max-width: 500px; }

.modal-header { display: flex; align-items: center; justify-content: center; gap: 12px; margin-bottom: 0.5rem; }

.ai-badge {
  width: 40px; height: 40px; border-radius: 12px;
  background: rgba(156, 39, 176, 0.15);
  color: #d05ce3;
  display: flex; align-items: center; justify-content: center;
}

.modal-title { font-size: 1.4rem; font-weight: 700; color: #fff; margin: 0; }
.modal-desc { color: #9ca3af; font-size: 0.9rem; text-align: center; margin-bottom: 1.5rem; line-height: 1.5; }

.ai-textarea-styled {
  width: 100%; height: 120px;
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 1rem; color: #fff; font-size: 1rem;
  resize: none; outline: none; transition: 0.2s;
}
.ai-textarea-styled:focus { border-color: #d05ce3; box-shadow: 0 0 0 2px rgba(208, 92, 227, 0.2); }
.input-error { border-color: #f44336; box-shadow: 0 0 0 2px rgba(244, 67, 54, 0.2); }

.btn-group { display: flex; gap: 12px; margin-top: 1.5rem; }
.modal-btn { flex: 1; padding: 0.9rem; border-radius: 10px; border: none; font-weight: 700; cursor: pointer; transition: 0.2s; }

.modal-btn.cancel { background: rgba(255,255,255,0.05); color: #9ca3af; }
.modal-btn.cancel:hover { background: rgba(255,255,255,0.1); color: #fff; }

.ai-generate-btn {
  background: linear-gradient(135deg, #9c27b0 0%, #673ab7 100%);
  color: white;
  box-shadow: 0 4px 15px rgba(156, 39, 176, 0.3);
}
.ai-generate-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 25px rgba(156, 39, 176, 0.5); }
.ai-generate-btn:disabled { opacity: 0.6; cursor: not-allowed; filter: grayscale(0.5); }

@keyframes popIn { from { opacity: 0; transform: scale(0.95) translateY(10px); } to { opacity: 1; transform: scale(1) translateY(0); } }

/* Vue 내장 transition 이름과 매칭 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.3s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>