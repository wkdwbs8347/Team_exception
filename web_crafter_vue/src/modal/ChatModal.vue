<script setup>
import { ref, onMounted, onUnmounted, nextTick, computed, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useWebSocketStore } from '@/stores/websocket';

const props = defineProps(['friend', 'myId']);
const emit = defineEmits(['close']);

const auth = useAuthStore();
const ws = useWebSocketStore();

const input = ref('');
const roomId = ref('');
const msgBox = ref(null);

// ✅ roomId 기준으로 스토어 메시지 읽기 (전역 소켓이 넣어주는 데이터를 실시간으로 보여줌)
const messages = computed(() => auth.allChatMessages[roomId.value] || []);

const scrollToBottom = () => {
  nextTick(() => {
    if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight;
  });
};

onMounted(async () => {
  roomId.value = [Number(props.myId), Number(props.friend.id)]
    .sort((a, b) => a - b)
    .join('_');

  auth.openRoom(roomId.value);
  auth.markAsRead(props.friend.id);

  // ✅ DB에서 과거 내역만 가져옴 (실시간은 전역 소켓이 담당)
  await auth.loadChatHistory(roomId.value);

  scrollToBottom();
});

onUnmounted(() => {
  auth.clearRoom(roomId.value);
  // ✅ 이제 여기서 구독 해제할 필요가 없음 (구독 자체를 안 하니까)
  auth.closeRoom();
});

// ✅ 메시지가 추가될 때마다 자동 스크롤은 유지
watch(
  () => messages.value.length,
  () => scrollToBottom(),
  { flush: 'post' }
);

const send = () => {
  const text = input.value.trim();
  if (!text) return;

  auth.sendChatMessage(roomId.value, props.friend.id, text);
  input.value = '';
};
</script>


<template>
  <div class="chat-modal-overlay" @click.self="$emit('close')">
    <div class="chat-window">
      <div class="chat-header">
        <div class="header-info">
          <div class="friend-avatar-small">
            {{ friend.nickname ? friend.nickname[0] : '?' }}
            <div class="status-dot"></div>
          </div>
          <div class="header-text">
            <span class="title">{{ friend.nickname }}</span>
            <span class="status-text">Active Now</span>
          </div>
        </div>
        <button class="close-btn" @click="$emit('close')">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
        </button>
      </div>

      <div class="chat-content" ref="msgBox">
        <div class="date-divider"><span>Today</span></div>
        <div
          v-for="(msg, i) in messages"
          :key="i"
          :class="['msg-row', String(msg.senderId) === String(myId) ? 'me' : 'other']"
        >
          <div v-if="String(msg.senderId) !== String(myId)" class="msg-avatar">
            {{ friend.nickname ? friend.nickname[0] : '?' }}
          </div>
          <div class="bubble">{{ msg.content }}</div>
        </div>
      </div>

      <div class="chat-footer">
        <div class="input-wrapper">
          <input v-model="input" @keyup.enter="send" placeholder="메시지 입력..." autofocus />
          <button class="send-btn" @click="send" :disabled="!input.trim()">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"></line><polygon points="22 2 15 22 11 13 2 9 22 2"></polygon></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ✅ 아래 CSS는 너 코드 그대로 유지 */
.chat-modal-overlay {
  position: fixed;
  top: 0; left: 0;
  width: 100vw; height: 100vh;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.chat-window {
  width: 380px;
  height: 600px;
  background: #1a1d21;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.chat-header {
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-between;
  align-items: center;
  backdrop-filter: blur(10px);
}
.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.friend-avatar-small {
  width: 40px; height: 40px;
  background: linear-gradient(135deg, #64748b, #475569);
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: bold;
  color: white;
  position: relative;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}
.status-dot {
  position: absolute;
  bottom: -2px; right: -2px;
  width: 12px; height: 12px;
  background: #22c55e;
  border: 2px solid #1a1d21;
  border-radius: 50%;
}
.header-text {
  display: flex;
  flex-direction: column;
}
.title {
  font-weight: 700;
  font-size: 1rem;
  color: white;
}
.status-text {
  font-size: 0.75rem;
  color: #22c55e;
  font-weight: 500;
}
.close-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: 0.2s;
}
.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.chat-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: radial-gradient(circle at center, rgba(0, 212, 255, 0.02) 0%, transparent 70%);
}

.chat-content::-webkit-scrollbar { width: 6px; }
.chat-content::-webkit-scrollbar-thumb { background: rgba(255, 255, 255, 0.1); border-radius: 3px; }
.chat-content::-webkit-scrollbar-track { background: transparent; }

.date-divider {
  text-align: center;
  font-size: 0.75rem;
  color: #64748b;
  margin: 10px 0;
  position: relative;
}
.date-divider span {
  background: rgba(255,255,255,0.05);
  padding: 4px 12px;
  border-radius: 20px;
}

.msg-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  animation: fadeIn 0.2s ease-out;
}
.msg-row.me { justify-content: flex-end; }
.msg-row.other { justify-content: flex-start; }

.msg-avatar {
  width: 32px; height: 32px;
  background: #334155;
  border-radius: 10px;
  font-size: 0.8rem;
  display: grid;
  place-items: center;
  color: #cbd5e1;
  margin-bottom: 4px;
}

.bubble {
  max-width: 75%;
  padding: 12px 16px;
  font-size: 0.95rem;
  line-height: 1.5;
  word-break: break-all;
  box-shadow: 0 2px 5px rgba(0,0,0,0.1);
}

.me .bubble {
  background: linear-gradient(135deg, #00d4ff 0%, #00a3ff 100%);
  color: #0f172a;
  border-radius: 18px 18px 2px 18px;
  font-weight: 600;
}
.other .bubble {
  background: #2d3748;
  color: #e2e8f0;
  border-radius: 18px 18px 18px 2px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.chat-footer {
  padding: 16px;
  background: rgba(26, 29, 33, 0.95);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}
.input-wrapper {
  display: flex;
  background: #0f172a;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 6px 6px 6px 16px;
  align-items: center;
  transition: 0.2s;
}
.input-wrapper:focus-within {
  border-color: #00d4ff;
  box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.2);
}

input {
  flex: 1;
  background: transparent;
  border: none;
  color: white;
  outline: none;
  font-size: 0.95rem;
}
.send-btn {
  width: 36px; height: 36px;
  background: #00d4ff;
  color: #0f172a;
  border: none;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: 0.2s;
}
.send-btn:hover:not(:disabled) {
  background: #38bdf8;
  transform: scale(1.05);
}
.send-btn:disabled {
  background: #334155;
  color: #64748b;
  cursor: default;
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
