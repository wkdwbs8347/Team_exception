import { defineStore } from 'pinia';
import SockJS from 'sockjs-client';
import Stomp from 'stompjs';
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import api from '@/api/axios';

export const useWebSocketStore = defineStore('websocket', () => {
  const stompClient = ref(null);
  const isConnected = ref(false);

  // destination -> subscription
  const subscriptions = ref({});

  // ✅ 중복 연결/재연결 방지
  const connecting = ref(false);
  const reconnectTimer = ref(null);

  const clearReconnectTimer = () => {
    if (reconnectTimer.value) {
      clearTimeout(reconnectTimer.value);
      reconnectTimer.value = null;
    }
  };

  const scheduleReconnect = (userId) => {
    if (reconnectTimer.value) return; // ✅ 타이머 1개만
    reconnectTimer.value = setTimeout(() => {
      reconnectTimer.value = null;
      connect(userId);
    }, 3000);
  };

  const unsubscribeAll = () => {
    Object.values(subscriptions.value).forEach((sub) => {
      try { sub?.unsubscribe?.(); } catch (e) {}
    });
    subscriptions.value = {};
  };

  // ✅ subscribe (중복 destination 방지)
  const subscribe = (destination, callback) => {
    if (!stompClient.value || !stompClient.value.connected) return;

    if (subscriptions.value[destination]) {
      try { subscriptions.value[destination].unsubscribe(); } catch (e) {}
      delete subscriptions.value[destination];
    }

    const sub = stompClient.value.subscribe(destination, (res) => {
      if (res?.body) callback(res.body);
    });

    subscriptions.value[destination] = sub;
    return sub;
  };

  // ✅ publish
  const publish = (destination, body) => {
    if (stompClient.value?.connected) {
      stompClient.value.send(destination, {}, JSON.stringify(body));
    } else {
      console.warn('⚠️ [WS] not connected, send blocked:', destination);
    }
  };

  // ✅ 전역 구독 설정
  const _setupGlobalSubscriptions = (userId) => {
    const auth = useAuthStore();

    // (0) ✅ 채팅 메시지 수신(진짜 본문 저장은 여기서만!)
    // 서버에서 convertAndSendToUser(..., "/queue/chat", msg) 로 보내는 채널
    subscribe(`/topic/user/${userId}/chat`, (body) => {
      try {
        const msg = JSON.parse(body);
        console.log('📥 [전역 채팅 수신]:', msg);
        auth.pushIncomingChat(msg); // ✅ 여기서만 채팅 저장
      } catch (e) {
        console.error('❌ queue/chat parse fail', e);
      }
    });

    // (1) 🔔 알림 목록(Notifications)
    subscribe(`/topic/user/${userId}/notifications`, (body) => {
      const payload = JSON.parse(body);

      if (Array.isArray(payload)) {
        auth.setNotifications(payload);
      } else {
        const prev = Array.isArray(auth.notifications) ? auth.notifications : [];
        const next = prev.some((n) => n.id === payload.id) ? prev : [payload, ...prev];
        auth.setNotifications(next);
      }
    });

    // (3) 🟢 개인 Presence
    subscribe(`/topic/user/${userId}/presence`, (body) => {
      try {
        const { userId: uid, status } = JSON.parse(body);
        console.log('✅ presence(user):', uid, status);
        auth.updateFriendPresence(String(uid), status);
      } catch (e) {
        console.error('presence(user) 파싱 실패:', e);
      }
    });

    // (4) 🌍 공용 Presence
    subscribe(`/topic/presence`, (body) => {
      try {
        const { userId: uid, status } = JSON.parse(body);
        console.log('✅ presence(global):', uid, status);
        auth.updateFriendPresence(String(uid), status);
      } catch (e) {
        console.error('presence(global) 파싱 실패:', e);
      }
    });

    // (5) 👥 친구 목록 갱신
    subscribe(`/topic/user/${userId}/friends`, async () => {
      console.log('👥 friends refresh 신호 수신');
      try {
        const fres = await api.get('/friends/list');
        if (typeof auth.setFriends === 'function') {
          auth.setFriends(Array.isArray(fres.data) ? fres.data : []);
        } else {
          if (!auth.me) auth.me = {};
          auth.me.friends = Array.isArray(fres.data) ? fres.data : [];
        }
      } catch (e) {
        console.error('친구 목록 갱신 실패', e);
      }
    });
  };

  // ✅ connect (Stomp 연결은 여기서만 1개)
  const connect = (userId) => {
    if (!userId) return;

    if (connecting.value) return;
    if (stompClient.value?.connected) return;

    connecting.value = true;
    clearReconnectTimer();

    const socket = new SockJS('http://localhost:8080/wsproject');
    const client = Stomp.over(socket);
    client.debug = () => {};


    const headers = { 'x-user-id': String(userId) };

    client.connect(
      headers,
      () => {
        stompClient.value = client;
        isConnected.value = true;
        connecting.value = false;

        console.log('🚀 [WS Store] 연결 성공:', userId);

        // ✅ 재연결 찌꺼기 제거 후 재구독
        unsubscribeAll();
        _setupGlobalSubscriptions(userId);
      },
      (error) => {
        console.error('❌ [WS Store] 연결 실패:', error);

        isConnected.value = false;
        connecting.value = false;
        stompClient.value = null;

        scheduleReconnect(userId);
      }
    );
  };

  // ✅ disconnect
  const disconnect = () => {
    clearReconnectTimer();
    connecting.value = false;

    unsubscribeAll();

    if (stompClient.value) {
      try {
        stompClient.value.disconnect(() => {
          console.log('🔌 [WS Store] 연결 해제');
          isConnected.value = false;
          stompClient.value = null;
        });
      } catch (e) {
        isConnected.value = false;
        stompClient.value = null;
      }
    } else {
      isConnected.value = false;
    }
  };

  return {
    isConnected,
    connect,
    disconnect,
    subscribe,
    publish,
  };
});
