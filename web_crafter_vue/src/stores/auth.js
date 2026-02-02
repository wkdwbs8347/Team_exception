import { defineStore } from 'pinia';
import api from '@/api/axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isAuthed: false,
    me: null,
    bootstrapped: false,
    notifications: [],
    friendPresence: {},
    stompClient: null,
    unreadSenders: new Set(),
    // ✅ [추가] 모든 채팅 메시지 저장소 (방 ID를 키로 사용)
    allChatMessages: {}, 
    activeRoomId: null,
    isChatOpen: false,
  }),

  getters: {
    nickname: (s) => s.me?.nickname || '',
    unreadCount: (s) => s.notifications.length,
    isSocketConnected: (s) => !!s.stompClient && s.stompClient.connected,
    hasUnread: (state) => (friendId) => state.unreadSenders.has(friendId),
    hasAnyUnread: (state) => state.unreadSenders.size > 0,
  },

  actions: {
    async fetchMe() {
      const res = await api.get('/member/me');
      const me = res.data?.member ?? res.data;
      this.me = me;
      this.isAuthed = !!me?.id;
      
      if (this.isAuthed && !this.stompClient) {
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
      if (this.stompClient) {
        this.stompClient.disconnect();
        this.stompClient = null;
      }
      this.isAuthed = false;
      this.me = null;
      this.unreadSenders = new Set();
      this.allChatMessages = {}; // 로그아웃 시 대화 내역 초기화
    },

    // -----------------------------------------------------------
    // 📡 [핵심] 상시 메시지 수신 로직
    // -----------------------------------------------------------
    connectChat() {
  if (this.stompClient?.connected) return;

  const socket = new window.SockJS('http://localhost:8080/wsproject');
  this.stompClient = window.Stomp.over(socket);

  // 디버그 켜기
  this.stompClient.debug = (msg) => console.log('[STOMP]', msg);

  const headers = { 'x-user-id': String(this.me?.id) };

  this.stompClient.connect(
    headers,
    () => {
      console.log('✅ 실시간 서버 연결 성공!');

      // ✅ 1) 채팅 메시지 전용: 개인 큐 (채팅창 안 열어도 무조건 받음)
      this.stompClient.subscribe('/user/queue/chat', (res) => {
        if (!res.body) return;
        const msg = JSON.parse(res.body);
        this.pushIncomingChat(msg);
      });

      // ✅ 2) 기존 알림 채널은 "알림 전용"으로만 쓰기
      this.stompClient.subscribe(`/topic/notifications/${this.me.id}`, (res) => {
        if (!res.body) return;
        const payload = JSON.parse(res.body);

        // 여기서는 "빨간점"만 올리거나, 알림 목록만 갱신
        // 메시지 본문까지 여기로 섞지 마!
        if (payload?.senderId) {
          const updated = new Set(this.unreadSenders);
          updated.add(payload.senderId);
          this.unreadSenders = updated;
        }
      });
    },
    (error) => {
      console.error('❌ STOMP 에러:', error);
    }
  );
},

// ✅ 채팅방 열림 상태 기록
openRoom(roomId) {
  this.activeRoomId = roomId;
  this.isChatOpen = true;
},

// ✅ 채팅방 닫힘
closeRoom() {
  this.isChatOpen = false;
  this.activeRoomId = null;
},

// ✅ 실제 메시지 저장 + unread 처리
pushIncomingChat(msg) {
  const rid = msg.roomId;
  if (!rid) return;

  if (!this.allChatMessages[rid]) this.allChatMessages[rid] = [];
  this.allChatMessages[rid].push(msg);

  if (String(msg.senderId) !== String(this.me?.id)) {
    const updated = new Set(this.unreadSenders);
    updated.add(msg.senderId);
    this.unreadSenders = updated;
  }
},


    // ✅ 읽음 처리 (채팅방 열 때 호출)
    markAsRead(friendId) {
      const updatedSet = new Set(this.unreadSenders);
      updatedSet.delete(friendId);
      this.unreadSenders = updatedSet;
    },

    // ✅ 전송 함수
    sendChatMessage(roomId, receiverId ,content) {
      if (this.stompClient && this.stompClient.connected) {
        const payload = JSON.stringify({
          roomId: roomId,
          senderId: this.me.id,
          receiverId,
          content: content
        });
        this.stompClient.send(`/app/chat/send`, {}, payload);
      } else {
        console.warn("소켓이 연결되지 않아 전송할 수 없습니다.");
      }
    },

    setNotifications(data) {
      this.notifications = Array.isArray(data) ? data : [];
    },
    
    updateFriendPresence(userId, status) {
      this.friendPresence = {
        ...(this.friendPresence || {}),
        [String(userId)]: status,
      };
    },
  },
});