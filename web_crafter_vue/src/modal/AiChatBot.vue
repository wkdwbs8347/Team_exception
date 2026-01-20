<script setup>
import { ref, nextTick } from 'vue';
import { MessageCircle, Minimize2, Bot, User, Send, MessageSquare, BrickWall } from 'lucide-vue-next';

// ✅ [해결] 부모로부터 workspaces를 받기 위해 props를 정의합니다.
const props = defineProps({
  workspaces: {
    type: Object,
    default: () => ({ structure: '', style: '', logic: '' })
  }
});

const emit = defineEmits(['generate']);

// 상태 관리
const isOpen = ref(false);
const input = ref('');
const isLoading = ref(false);
const isEditMode = ref(false); 
const currentMode = ref('gen'); 

const messages = ref([
  { 
    id: 1, 
    role: 'ai', 
    text: '안녕하세요! 👋\n[생성 모드]에서는 블록을 만들어드리고,\n[대화 모드]에서는 코딩 조언을 해드립니다.' 
  }
]);
const chatBody = ref(null);

const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) scrollToBottom();
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight;
};

const setMode = (mode) => {
  currentMode.value = mode;
};

// 🔥 수정된 메시지 전송 로직
const sendMessage = async () => {
  if (!input.value.trim() || isLoading.value) return;

  const userText = input.value;
  const editModeActive = isEditMode.value; 
  
  // ✅ [중요] AI에게 현재 모든 탭의 정보를 넘겨주기 위한 컨텍스트 구성
  const currentContext = {
    structure: props.workspaces.structure,
    style: props.workspaces.style,
    logic: props.workspaces.logic
  };

  messages.value.push({ id: Date.now(), role: 'user', text: userText });
  input.value = '';
  isLoading.value = true;
  scrollToBottom();

  try {
    console.log(`%c🚀 [AI 요청] 모드: ${currentMode.value}, 수정: ${editModeActive}`, "color: #00d4ff; font-weight: bold;");

    const response = await fetch('http://localhost:8080/api/ai/generate', { 
       method: 'POST',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify({ 
          prompt: userText,
          mode: currentMode.value,
          isEditMode: editModeActive,
          // ✅ 수정 모드일 때만 현재 블록 정보(context)를 보냅니다.
          context: editModeActive ? currentContext : null 
       })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error?.message || `API 오류: ${response.status}`);
    }

    const data = await response.json();

    // 1️⃣ [대화 모드] 처리
    if (currentMode.value === 'chat') {
        const replyText = data.message || data.text || "답변이 없습니다.";
        messages.value.push({ id: Date.now() + 1, role: 'ai', text: replyText });
        return; 
    }

    // 2️⃣ [생성 모드] 처리
    if (currentMode.value === 'gen') {
        if (data.xml && data.xml.includes('<xml')) {
            // ✅ 부모(IDEView)에게 XML과 수정 모드 여부를 함께 전달합니다.
            emit('generate', data.xml, editModeActive); 

            messages.value.push({ 
              id: Date.now() + 2,
              role: 'ai', 
              text: data.message || `✅ ${editModeActive ? '수정' : '생성'} 작업을 완료했습니다.`
            });
            
            isEditMode.value = false; 
        } else {
            throw new Error("AI가 유효한 블록 코드를 반환하지 않았습니다.");
        }
    }

  } catch (e) {
    console.error("🔥 에러 발생:", e);
    messages.value.push({ id: Date.now() + 1, role: 'ai', text: `❌ ${e.message}` });
  } finally {
    isLoading.value = false;
    scrollToBottom();
  }
};
</script>

<template>
  <div class="ai-chatbot-container">
    <button v-if="!isOpen" class="float-btn" @click="toggleChat">
      <MessageCircle :size="28" />
      <span class="btn-label">AI</span>
    </button>

    <div v-else class="chat-window">
      <div class="chat-header">
        <div class="header-left">
          <Bot :size="20" />
          <span class="title">Web Crafter AI</span>
        </div>
        <div class="header-right">
          <button @click="toggleChat" class="icon-btn">
            <Minimize2 :size="18" />
          </button>
        </div>
      </div>

      <div class="chat-body" ref="chatBody">
        <div v-for="msg in messages" :key="msg.id" class="message-row" :class="msg.role">
          <template v-if="msg.role !== 'system'">
            <div class="avatar" v-if="msg.role === 'ai'"><Bot :size="16" /></div>
            <div class="bubble">{{ msg.text }}</div>
            <div class="avatar" v-if="msg.role === 'user'"><User :size="16" /></div>
          </template>
        </div>
        <div v-if="isLoading" class="message-row ai">
          <div class="avatar"><Bot :size="16" /></div>
          <div class="bubble loading"><span>.</span><span>.</span><span>.</span></div>
        </div>
      </div>

      <div class="mode-tabs">
        <button class="mode-btn" :class="{ active: currentMode === 'chat' }" @click="setMode('chat')">
          <MessageSquare :size="14" /> 대화/질문
        </button>
        <button class="mode-btn" :class="{ active: currentMode === 'gen' }" @click="setMode('gen')">
          <BrickWall :size="14" /> 블록 생성
        </button>
      </div>

      <div class="chat-footer">
        <label v-if="currentMode === 'gen'" class="edit-mode-label">
          <input type="checkbox" v-model="isEditMode">
          <span class="edit-text">수정</span>
        </label>
        <textarea 
          v-model="input" 
          :placeholder="currentMode === 'gen' ? '예: 로그인 화면 만들어줘' : '예: Vue에서 변수는 어떻게 써?'"
          @keydown.enter.prevent="sendMessage"
        ></textarea>
        <button class="send-btn" :disabled="!input.trim() || isLoading" @click="sendMessage">
          <Send :size="18" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 기존 스타일 그대로 사용 (이전 답변 참고) */
.ai-chatbot-container { position: fixed; bottom: 20px; right: 100px; z-index: 9999; }
.float-btn { background: linear-gradient(135deg, #6200ea, #9c27b0); color: white; border: none; border-radius: 50px; padding: 15px 20px; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.3); cursor: pointer; }
.chat-window { width: 350px; height: 500px; background: white; border-radius: 16px; box-shadow: 0 5px 25px rgba(0,0,0,0.2); display: flex; flex-direction: column; overflow: hidden; border: 1px solid #eee; }
.chat-header { background: #1a1a2e; color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 8px; font-weight: bold; }
.chat-body { flex: 1; padding: 15px; background: #f8f9fa; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }
.message-row { display: flex; gap: 8px; align-items: flex-end; }
.message-row.user { justify-content: flex-end; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 12px; font-size: 0.9rem; white-space: pre-wrap; }
.ai .bubble { background: white; border: 1px solid #e0e0e0; border-bottom-left-radius: 2px; color: #303030; }
.user .bubble { background: #4caf50; color: white; border-bottom-right-radius: 2px; }
.mode-tabs { display: flex; background: #f1f3f4; padding: 4px; gap: 4px; }
.mode-btn { flex: 1; border: none; background: transparent; padding: 8px; font-size: 0.85rem; cursor: pointer; border-radius: 6px; display: flex; align-items: center; justify-content: center; gap: 6px; }
.mode-btn.active { background: white; color: #4c51bf; font-weight: bold; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
.chat-footer { padding: 10px; background: white; border-top: 1px solid #eee; display: flex; gap: 8px; align-items: center; }
.edit-mode-label { display: flex; flex-direction: column; align-items: center; gap: 2px; cursor: pointer; min-width: 35px; }
.edit-mode-label input { width: 16px; height: 16px; accent-color: #4c51bf; }
.edit-text { font-size: 0.65rem; color: #666; font-weight: bold; }
textarea { flex: 1; border: 1px solid #ddd; border-radius: 20px; padding: 10px 15px; font-size: 0.9rem; resize: none; height: 44px; outline: none; color: #303030; }
.send-btn { width: 40px; height: 40px; border-radius: 50%; border: none; background: #4c51bf; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.loading span { display: inline-block; animation: bounce 1.4s infinite ease-in-out both; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
</style>