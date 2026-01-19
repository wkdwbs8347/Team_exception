<script setup>
import { ref, nextTick } from 'vue';
import { MessageCircle, Minimize2, Bot, User, Send, MessageSquare, BrickWall } from 'lucide-vue-next';

const emit = defineEmits(['generate']);

// 상태 관리
const isOpen = ref(false);
const input = ref('');
const isLoading = ref(false);
const currentMode = ref('gen'); // 'chat'(대화) 또는 'gen'(생성)
const messages = ref([
  { 
    id: 1, 
    role: 'ai', 
    text: '안녕하세요! 👋\n[생성 모드]에서는 블록을 만들어드리고,\n[대화 모드]에서는 코딩 조언을 해드립니다.' 
  }
]);
const chatBody = ref(null);

// 채팅창 열기/닫기
const toggleChat = () => {
  isOpen.value = !isOpen.value;
  if (isOpen.value) scrollToBottom();
};

const scrollToBottom = async () => {
  await nextTick();
  if (chatBody.value) chatBody.value.scrollTop = chatBody.value.scrollHeight;
};

// 🔥 모드 변경 핸들러
const setMode = (mode) => {
  currentMode.value = mode;
  // 모드 변경 안내 메시지 (선택사항)
  // messages.value.push({ 
  //   id: Date.now(), 
  //   role: 'system', 
  //   text: mode === 'gen' ? '✨ [생성 모드]로 전환되었습니다.' : '💬 [대화 모드]로 전환되었습니다.' 
  // });
  // scrollToBottom();
};

const sendMessage = async () => {
  if (!input.value.trim() || isLoading.value) return;

  const userText = input.value;
  messages.value.push({ id: Date.now(), role: 'user', text: userText });
  input.value = '';
  isLoading.value = true;
  scrollToBottom();

  try {
    console.log(`%c🚀 [AI 요청 시작] 모드: ${currentMode.value}`, "color: #00d4ff; font-weight: bold;");

    const response = await fetch('http://localhost:8080/api/ai/generate', { 
       method: 'POST',
       headers: { 'Content-Type': 'application/json' },
       body: JSON.stringify({ 
         prompt: userText,
         mode: currentMode.value 
       })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(errorData.error?.message || `API 오류: ${response.status}`);
    }

    const data = await response.json();
    console.log("%c✅ [AI 응답 수신]", "color: #00ff88; font-weight: bold;", data);

    // ============================================================
    // 🔥 [핵심 수정] 모드에 따라 로직을 완전히 격리 (철벽 방어)
    // ============================================================

    // 1️⃣ [대화 모드]인 경우
    // 👉 XML이 있든 없든 절대 쳐다보지 않고, 오직 메시지만 출력하고 끝냅니다.
    if (currentMode.value === 'chat') {
        const replyText = data.message || data.text || "답변이 없습니다.";
        
        messages.value.push({ 
          id: Date.now() + 1, 
          role: 'ai', 
          text: replyText 
        });
        
        return; // ⛔ 여기서 함수 강제 종료! (아래 코드는 실행될 기회조차 없음)
    }

    // 2️⃣ [생성 모드]인 경우
    // 👉 이때만 XML 검사를 수행합니다.
    if (currentMode.value === 'gen') {
        // XML 데이터가 유효한지 검사
        if (data.xml && data.xml.includes('<xml')) {
            console.log("🧩 블록 생성 시작");
            
            // 부모에게 전달 (블록 변환 시도)
            emit('generate', data.xml);

            messages.value.push({ 
              id: Date.now() + 2,
              role: 'ai', 
              text: data.message || `✅ "${userText}" 기능을 생성했습니다.`
            });
        } else {
            // 생성 모드인데 XML이 없으면 에러 처리
            throw new Error("AI가 유효한 블록 코드를 반환하지 않았습니다.");
        }
    }

  } catch (e) {
    console.error("🔥 에러 발생:", e);
    
    let errorMsg = "죄송합니다. 오류가 발생했습니다.";
    if (e.message.includes('429')) errorMsg = "⚠️ 사용량이 초과되었습니다. 잠시 후 다시 시도해주세요.";
    else errorMsg = `❌ ${e.message}`;

    messages.value.push({ id: Date.now() + 1, role: 'ai', text: errorMsg });
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
          <div v-if="msg.role === 'system'" class="system-msg">
            {{ msg.text }}
          </div>
          <template v-else>
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
        <button 
          class="mode-btn" 
          :class="{ active: currentMode === 'chat' }"
          @click="setMode('chat')"
        >
          <MessageSquare :size="14" /> 대화/질문
        </button>
        <button 
          class="mode-btn" 
          :class="{ active: currentMode === 'gen' }"
          @click="setMode('gen')"
        >
          <BrickWall :size="14" /> 블록 생성
        </button>
      </div>

      <div class="chat-footer">
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
/* 기존 스타일 유지 */
.ai-chatbot-container { position: fixed; bottom: 20px; right: 100px; z-index: 99999; font-family: 'Segoe UI', sans-serif; }
.float-btn { background: linear-gradient(135deg, #6200ea, #9c27b0); color: white; border: none; border-radius: 50px; padding: 15px 20px; display: flex; align-items: center; gap: 10px; box-shadow: 0 4px 15px rgba(0,0,0,0.3); cursor: pointer; transition: transform 0.2s; }
.float-btn:hover { transform: scale(1.05); }
.btn-label { font-weight: bold; font-size: 1rem; }
.chat-window { width: 350px; height: 500px; background: white; border-radius: 16px; box-shadow: 0 5px 25px rgba(0,0,0,0.2); display: flex; flex-direction: column; overflow: hidden; animation: slideUp 0.3s ease-out; border: 1px solid #eee; }
@keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
.chat-header { background: linear-gradient(135deg, #1a1a2e, #16213e); color: white; padding: 15px; display: flex; justify-content: space-between; align-items: center; }
.header-left { display: flex; align-items: center; gap: 8px; font-weight: bold; }
.icon-btn { background: none; border: none; color: rgba(255,255,255,0.7); cursor: pointer; }
.icon-btn:hover { color: white; }
.chat-body { flex: 1; padding: 15px; background: #f8f9fa; overflow-y: auto; display: flex; flex-direction: column; gap: 12px; }

/* 메시지 스타일 */
.message-row { display: flex; gap: 8px; align-items: flex-end; }
.message-row.user { justify-content: flex-end; }
.message-row.ai { justify-content: flex-start; }
.avatar { width: 28px; height: 28px; border-radius: 50%; background: #eee; display: flex; align-items: center; justify-content: center; color: #555; font-size: 0.8rem; flex-shrink: 0; }
.ai .avatar { background: #e0e7ff; color: #4c51bf; }
.user .avatar { background: #4caf50; color: white; }
.bubble { max-width: 80%; padding: 10px 14px; border-radius: 12px; font-size: 0.9rem; line-height: 1.4; white-space: pre-wrap; word-break: break-word; }
.ai .bubble { background: white; border: 1px solid #e0e0e0; color: #333; border-bottom-left-radius: 2px; }
.user .bubble { background: #4caf50; color: white; border-bottom-right-radius: 2px; }

/* 시스템 메시지 (모드 변경 알림 등) */
.system-msg { width: 100%; text-align: center; font-size: 0.8rem; color: #999; margin: 5px 0; }

/* 🔥 모드 탭 스타일 */
.mode-tabs {
  display: flex;
  background: #f1f3f4;
  padding: 4px;
  gap: 4px;
  border-top: 1px solid #eee;
}
.mode-btn {
  flex: 1;
  border: none;
  background: transparent;
  padding: 8px;
  font-size: 0.85rem;
  color: #666;
  border-radius: 6px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: 0.2s;
}
.mode-btn:hover { background: rgba(0,0,0,0.05); }
.mode-btn.active {
  background: white;
  color: #4c51bf;
  font-weight: bold;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.chat-footer { color: #333; padding: 10px; background: white; border-top: 1px solid #eee; display: flex; gap: 8px; align-items: center; }
textarea { flex: 1; border: 1px solid #ddd; border-radius: 20px; padding: 10px 15px; font-size: 0.9rem; resize: none; height: 44px; outline: none; }
textarea:focus { border-color: #4c51bf; }
.send-btn { width: 40px; height: 40px; border-radius: 50%; border: none; background: #4c51bf; color: white; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: 0.2s; }
.send-btn:hover { background: #434190; }
.send-btn:disabled { background: #ccc; cursor: not-allowed; }

.loading span { display: inline-block; animation: bounce 1.4s infinite ease-in-out both; margin: 0 1px; }
.loading span:nth-child(1) { animation-delay: -0.32s; }
.loading span:nth-child(2) { animation-delay: -0.16s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1); } }
</style>