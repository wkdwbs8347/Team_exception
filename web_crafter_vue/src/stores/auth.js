import { defineStore } from 'pinia';
import api from '@/api/axios';
import { useWebSocketStore } from '@/stores/websocket';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthed: false,
    me: null,
    bootstrapped: false,

    notifications: [],
    friendPresence: {},

    // ✅ 읽지않음 (반응성 위해 Set 교체 방식)
    unreadSenders: new Set(),

    // ✅ 모든 채팅 메시지 저장소 (roomId -> array)
    allChatMessages: {},

    activeRoomId: null,
    isChatOpen: false,

    // ✅ 채팅 빨간점
    hasNewChat: false,
  }),

  getters: {
    nickname: (s) => s.me?.nickname || '',
    unreadCount: (s) => s.notifications.length,

    hasUnread: (state) => (friendId) => state.unreadSenders.has(friendId),
    hasAnyUnread: (state) => state.unreadSenders.size > 0,
  },

  actions: {
    async fetchMe() {
      const res = await api.get('/member/me');
      const me = res.data?.member ?? res.data;
      this.me = me;
      this.isAuthed = !!me?.id;

      // ✅ 로그인 상태면 websocketStore로 연결
      if (this.isAuthed) {
        this.connectChat();
      }
      return me;
    },

    async bootstrap() {
      if (this.bootstrapped) return this.isAuthed;
      try {
        await this.fetchMe();
      } catch (e) {
        this.isAuthed = false;
        this.me = null;
      } finally {
        this.bootstrapped = true;
      }
      return this.isAuthed;
    },

    async login({ email, password, rememberMe }) {
      await api.post('/member/login', { email, password, rememberMe });
      await this.fetchMe();
    },

    async logout() {
      await api.post('/member/logout');

      // ✅ websocketStore 연결 해제
      const ws = useWebSocketStore();
      ws.disconnect();

      this.isAuthed = false;
      this.me = null;
      this.unreadSenders = new Set();
      this.allChatMessages = {};
      this.hasNewChat = false;
      this.activeRoomId = null;
      this.isChatOpen = false;
    },

    // -----------------------------------------------------------
    // ✅ websocket 연결 (실제 연결은 websocketStore가 함)
    // -----------------------------------------------------------
    connectChat() {
      const ws = useWebSocketStore();
      ws.connect(this.me?.id);
    },

    // -----------------------------------------------------------
    // ✅ [추가] 채팅 히스토리 로드용 유틸/액션
    // -----------------------------------------------------------
    ensureRoom(roomId) {
      if (!roomId) return;
      if (this.allChatMessages?.[roomId]) return;

      this.allChatMessages = {
        ...(this.allChatMessages || {}),
        [roomId]: [],
      };
    },

    async loadChatHistory(roomId) {
      if (!roomId) return;
      this.ensureRoom(roomId);

      try {
        const res = await api.get(`/chat/history/${roomId}`);
        const history = Array.isArray(res.data) ? res.data : [];

        const current = this.allChatMessages[roomId] || [];

        // ✅ 중복 제거하면서 병합
        const mergedMap = new Map();
        const keyOf = (m) =>
          m?.id ? `id:${m.id}` : `k:${m.senderId}|${m.content}|${m.regDate}`;

        [...current, ...history].forEach((m) => mergedMap.set(keyOf(m), m));
        this.allChatMessages[roomId] = Array.from(mergedMap.values());
      } catch (e) {
        console.error('채팅 히스토리 로드 실패:', e);
        // 실패해도 배열은 유지
        this.ensureRoom(roomId);
      }
    },

    // ✅ 채팅방 열림/닫힘
    openRoom(roomId) {
      this.activeRoomId = roomId;
      this.isChatOpen = true;
      this.ensureRoom(roomId);
    },

    closeRoom() {
      this.isChatOpen = false;
      this.activeRoomId = null;
    },

    clearRoom(roomId) {
  if (!roomId) return;

  const copy = { ...this.allChatMessages };
  delete copy[roomId];
  this.allChatMessages = copy;

  // active room이면 같이 닫기
  if (this.activeRoomId === roomId) {
    this.activeRoomId = null;
    this.isChatOpen = false;
  }
},

    // ✅ 수신 메시지 저장 (websocketStore의 /user/queue/chat 에서 호출됨)
pushIncomingChat(msg) {
  const rid = msg?.roomId;
  if (!rid) return;

  if (!this.allChatMessages[rid]) {
    this.allChatMessages = { ...this.allChatMessages, [rid]: [] };
  }

  // ✅ 1) 서버 echo가 왔고, 내가 보낸 메시지면 기존 temp 제거
  if (String(msg.senderId) === String(this.me?.id)) {
    const arr = this.allChatMessages[rid];

    // (A) 서버가 tempId를 같이 보내주는 경우: 정확히 제거
    if (msg.tempId) {
      this.allChatMessages[rid] = arr.filter(m => m.tempId !== msg.tempId);
    } else {
      // (B) tempId가 없으면: 내용 기준으로 최근 temp 하나 제거
      const idx = arr.findIndex(m =>
        m._temp === true &&
        String(m.senderId) === String(msg.senderId) &&
        String(m.receiverId) === String(msg.receiverId) &&
        m.content === msg.content
      );
      if (idx !== -1) arr.splice(idx, 1);
    }
  }

  // ✅ 2) 중복 방지 (id가 있으면 id 우선)
  if (msg?.id && this.allChatMessages[rid].some(m => m.id === msg.id)) return;

  const keyOf = (m) => m?.id ? `id:${m.id}` : `k:${m.senderId}|${m.receiverId}|${m.content}|${m.regDate}`;
  const incomingKey = keyOf(msg);
  const exists = this.allChatMessages[rid].some((m) => keyOf(m) === incomingKey);

  if (!exists) {
    this.allChatMessages[rid].push(msg);
  }

  // ✅ 3) 내가 보낸게 아니면 unread 처리
  if (String(msg.senderId) !== String(this.me?.id)) {
    const updated = new Set(this.unreadSenders);
    updated.add(msg.senderId);
    this.unreadSenders = updated;
  }
},


    // ✅ 읽음 처리
    markAsRead(friendId) {
      const updated = new Set(this.unreadSenders);
      updated.delete(friendId);
      this.unreadSenders = updated;
    },

    // ✅ 전송 (optimistic push 포함: 내가 보낸게 내 화면에 바로 보이게)
    sendChatMessage(roomId, receiverId, content) {
      const text = (content ?? '').trim();
      if (!text) return;

      this.ensureRoom(roomId);

       const tempId = `tmp_${Date.now()}_${Math.random().toString(16).slice(2)}`;

      // ✅ 1) 내 화면에 먼저 추가
      const myMsg = {
        id: 0,
        tempId,
        roomId,
        senderId: this.me?.id,
        receiverId,
        content: text,
        regDate: new Date().toISOString(),
        _temp: true,
      };

      this.allChatMessages[roomId].push(myMsg);

      // ✅ 2) 서버 전송
      const ws = useWebSocketStore();
      ws.publish('/app/chat/send', {
        roomId,
        senderId: this.me?.id,
        receiverId,
        content: text,
        tempId,
      });
    },

    // ✅ chat 알림 빨간점
    setHasNewChat(v) {
      this.hasNewChat = !!v;
    },

    // ✅ 알림 목록
    setNotifications(data) {
      this.notifications = Array.isArray(data) ? data : [];
    },

    // ✅ presence
    updateFriendPresence(userId, status) {
      this.friendPresence = {
        ...(this.friendPresence || {}),
        [String(userId)]: status,
      };
    },
  },
});
