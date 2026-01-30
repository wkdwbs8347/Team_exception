<script setup>
import { ref, computed, watch } from 'vue';
import { UserPlus, X, MessageCircle, UserMinus, Send } from 'lucide-vue-next'; 
import SearchUserModal from '@/modal/SearchUserModal.vue'; 
import api from '@/api/axios';

// ✅ [핵심] 부모(NavBar)에서 보내준 mode('invite' or 'manage')와 webId를 받습니다.
const props = defineProps(['isOpen', 'currentUser', 'mode', 'webId', 'isOwner']); 
defineEmits(['close']);

const isSearchOpen = ref(false); 
const friends = ref([]); 
const projectMemberIds = ref(new Set()); // 이미 멤버
const pendingInviteIds = ref(new Set()); // 초대장 보낸 사람

// 닉네임 계산
const myNickname = computed(() => {
  const u = props.currentUser;
  if (!u) return 'Guest';
  return u.member?.nickname || u.nickname || 'Guest';
});

// 이메일 계산
const myEmail = computed(() => {
  const u = props.currentUser;
  if (!u) return '로그인이 필요합니다';
  const email = u.member?.email || u.email;
  if (email) return email;
  if (myNickname.value !== 'Guest') return 'No Email';
  return '로그인이 필요합니다';
});

// 친구 목록 불러오기 (+ 초대 모드면 멤버 목록도 같이 로드)
const loadFriends = async () => {
  if (myNickname.value === 'Guest') return; 
  try {
    // 1. 친구 목록
    const res = await api.get('/friends/list');
    friends.value = res.data; 

    // 2. 초대 모드일 때: 멤버 목록 + ✅ [추가] 대기 목록 가져오기
    if (props.mode === 'invite' && props.webId) {
       try {
         // (1) 이미 멤버인 사람들
         const memberRes = await api.get(`/projects/${props.webId}/members`);
         projectMemberIds.value = new Set(memberRes.data);

         // (2) ✅ [추가] 초대를 보낸 사람들 (Pending)
         const pendingRes = await api.get(`/projects/${props.webId}/pending-invites`);
         pendingInviteIds.value = new Set(pendingRes.data);

       } catch (err) {
         console.error("상태 목록 로드 실패:", err);
       }
    }
  } catch (e) {
    console.error("친구 목록 로드 실패", e);
  }
};

// 멤버 추방 함수
const kickMember = async (friendId, nickname) => {
  if (!confirm(`정말로 '${nickname}' 님을 프로젝트에서 추방하시겠습니까?`)) return;

  try {
    // 백엔드 추방 API 호출
    await api.delete(`/projects/${props.webId}/members/${friendId}`);
    
    // 성공 시: '참여중' 목록에서 제거 (즉시 '초대' 버튼으로 바뀜)
    projectMemberIds.value.delete(friendId);
    
    alert(`'${nickname}' 님을 추방했습니다.`);
  } catch (e) {
    console.error(e);
    alert(e.response?.data || "추방 실패");
  }
};

// 친구 삭제 함수
const deleteFriend = async (friendId, nickname) => {
  if (!confirm(`정말로 '${nickname}' 님을 친구 목록에서 삭제하시겠습니까?`)) return;
  try {
    await api.delete(`/friends/${friendId}`);
    friends.value = friends.value.filter(u => u.id !== friendId);
  } catch (e) {
    console.error(e);
    alert("삭제 실패: " + (e.response?.data?.message || "서버 오류"));
  }
};

const isMember = (userId) => projectMemberIds.value.has(userId);   // 참여중
const isPending = (userId) => pendingInviteIds.value.has(userId); // 초대됨(대기)

// ✅ [추가] 프로젝트 초대 함수
const inviteProject = async (friendId, nickname) => {
  if (!confirm(`'${nickname}'님을 현재 프로젝트에 초대하시겠습니까?`)) return;

  try {
    // 백엔드 초대 API 호출
    await api.post('/projects/invite', {
      targetId: friendId,
      webId: props.webId 
    });
    
    alert(`'${nickname}'님에게 초대장을 보냈습니다! 💌`);
  } catch (e) {
    console.error(e);
    alert(e.response?.data?.message || "초대 실패 (이미 멤버이거나 오류)");
  }
};

const handleSearchClose = () => {
  isSearchOpen.value = false;
  loadFriends();
};

watch(
  [() => props.isOpen, () => props.currentUser], 
  ([isOpen, user]) => {
    if (!user || myNickname.value === 'Guest') {
      friends.value = []; 
      return;
    }
    if (isOpen) {
      loadFriends();
    }
  },
  { immediate: true }
);
</script>

<template>
  <div v-if="isOpen" class="modal-backdrop" @click.self="$emit('close')">
    
    <div class="modal-window">
      
      <header class="window-header">
        <div class="header-left">
           <h2 class="title">{{ mode === 'invite' ? 'Invite Friends' : 'Connections' }}</h2>
           
           <button v-if="mode !== 'invite'" class="action-btn add" @click="isSearchOpen = true" title="친구 찾기">
             <UserPlus :size="20"/>
           </button>
        </div>

        <button class="action-btn close" @click="$emit('close')" title="닫기">
           <X :size="20"/>
        </button>
      </header>

      <div class="window-body">
        
        <div class="my-profile-card">
          <div class="avatar-area">
            <div class="avatar me">ME</div>
            <div class="status-dot online"></div>
          </div>
          <div class="info-area">
            <div class="user-name">
                {{ myNickname }}
                <span class="badge" v-if="myNickname !== 'Guest'">Me</span>
            </div>
            <div class="user-bio" v-if="myEmail && myEmail !== 'No Email' && myEmail !== '로그인이 필요합니다'">
                {{ myEmail }}
            </div>
            </div>
        </div>

        <div class="list-section">
          <h3 class="section-title">Friends List <span class="count">{{ friends.length }}</span></h3>
          
          <div v-if="friends.length === 0" class="empty-state">
            <p>아직 친구가 없습니다.</p>
            <p class="sub">친구를 추가해서 프로젝트에 초대해보세요!</p>
          </div>

          <div v-else class="friend-list">
            <div v-for="u in friends" :key="u.id" class="friend-item">
              
              <div class="avatar-wrapper">
                <div class="avatar">{{ u.nickname ? u.nickname[0] : '?' }}</div>
                <div class="status-dot" :class="u.connectStatus || 'offline'"></div>
              </div>

              <div class="friend-info">
                <div class="name">{{ u.nickname }}</div>
                <div class="email" v-if="u.email">{{ u.email }}</div>
              </div>
              
              <div class="action-group">
                
                <template v-if="mode === 'invite'">
                  
                  <template v-if="isMember(u.id)">
                    <button 
                      v-if="isOwner" 
                      class="icon-btn kick" 
                      @click.stop="kickMember(u.id, u.nickname)" 
                      title="멤버 추방"
                    >
                        <UserMinus :size="18" />
                    </button>
                    
                    <div v-else class="member-badge">
                      참여중
                    </div>
                </template>

                <div v-else-if="isPending(u.id)" class="pending-badge">
                  초대됨
                </div>

                  <button 
                    v-else
                    class="icon-btn invite" 
                    @click.stop="inviteProject(u.id, u.nickname)" 
                    title="프로젝트 초대 보내기"
                  >
                     <Send :size="18" />
                  </button>

                </template>

                <template v-else>
                  <button class="icon-btn chat" title="채팅 보내기">
                      <MessageCircle :size="18"/>
                  </button>
                  <button class="icon-btn delete" @click.stop="deleteFriend(u.id, u.nickname)" title="친구 삭제">
                      <UserMinus :size="18"/>
                  </button>
                </template>

              </div>
            </div>
          </div>
        </div>

      </div>
    </div>

    <SearchUserModal 
      :isOpen="isSearchOpen" 
      @close="handleSearchClose" 
    />
  </div>
</template>

<style scoped>
/* 모달 레이아웃 */
.modal-backdrop {
  position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
  background: rgba(0, 0, 0, 0.6); 
  backdrop-filter: blur(3px);
  z-index: 1100; 
  display: flex; justify-content: center; align-items: center;
  animation: fadeIn 0.2s ease-out;
}

.modal-window {
  width: 600px; 
  height: 700px; 
  max-height: 90vh;
  background: #151922; 
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px; 
  box-shadow: 0 30px 60px -12px rgba(0, 0, 0, 0.6);
  display: flex; flex-direction: column;
  overflow: hidden;
  animation: zoomIn 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

/* 헤더 */
.window-header {
  height: 70px;
  padding: 0 24px;
  display: flex; justify-content: space-between; align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.header-left { display: flex; align-items: center; gap: 12px; }

.title { font-size: 1.3rem; font-weight: 800; color: #fff; margin: 0; letter-spacing: -0.5px; }

.action-btn {
  width: 38px; height: 38px; border-radius: 10px; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: 0.2s;
}
.action-btn.add { background: rgba(0, 212, 255, 0.1); color: #00d4ff; }
.action-btn.add:hover { background: rgba(0, 212, 255, 0.2); transform: translateY(-2px); }

.action-btn.close { background: transparent; color: #64748b; }
.action-btn.close:hover { background: rgba(255, 255, 255, 0.1); color: #fff; }

.window-body { flex: 1; padding: 24px; overflow-y: auto; color: #e2e8f0; }

/* 내 프로필 카드 */
.my-profile-card {
  display: flex; align-items: center; gap: 18px;
  padding: 20px; margin-bottom: 30px;
  background: linear-gradient(145deg, rgba(255,255,255,0.03), rgba(255,255,255,0.01));
  border-radius: 16px; border: 1px solid rgba(255,255,255,0.06);
}
.avatar-area { position: relative; }
.avatar.me {
  width: 54px; height: 54px; background: #00d4ff; color: #0f172a;
  font-weight: 900; font-size: 1.1rem; border-radius: 18px;
  display: grid; place-items: center;
  box-shadow: 0 0 15px rgba(0, 212, 255, 0.2);
}
.status-dot {
  position: absolute; bottom: -2px; right: -2px; width: 14px; height: 14px;
  background: #22c55e; border: 3px solid #151922; border-radius: 50%;
}
.user-name { font-weight: 700; font-size: 1.1rem; color: #fff; display: flex; align-items: center; gap: 8px; }
.badge { font-size: 0.7rem; background: rgba(0, 212, 255, 0.2); color: #00d4ff; padding: 3px 8px; border-radius: 6px; font-weight: 800; }
.user-bio { font-size: 0.9rem; color: #94a3b8; margin-top: 4px; }

/* 리스트 섹션 */
.section-title { font-size: 0.85rem; color: #64748b; font-weight: 700; margin-bottom: 12px; display: flex; justify-content: space-between; text-transform: uppercase; letter-spacing: 1px; }
.count { background: #334155; color: #fff; padding: 2px 8px; border-radius: 10px; font-size: 0.75rem; }

.empty-state {
  text-align: center; padding: 60px 0; color: #64748b;
  background: rgba(255,255,255,0.01); border-radius: 12px; border: 2px dashed rgba(255,255,255,0.05);
}
.empty-state .sub { font-size: 0.9rem; margin-top: 10px; color: #94a3b8; }

/* 친구 리스트 아이템 */
.friend-item {
  display: flex; align-items: center; gap: 14px; padding: 14px;
  border-radius: 14px; margin-bottom: 8px; cursor: pointer;
  transition: 0.2s; border: 1px solid transparent;
  position: relative; 
}
.friend-item:hover { background: rgba(255, 255, 255, 0.04); border-color: rgba(255,255,255,0.05); }

.avatar-wrapper { position: relative; width: 44px; height: 44px; }
.avatar {
  width: 100%; height: 100%; background: #334155; border-radius: 14px;
  display: grid; place-items: center; color: #fff; font-weight: 600; font-size: 1.1rem;
}
.friend-item .status-dot {
  position: absolute; bottom: -2px; right: -2px; width: 12px; height: 12px;
  border-radius: 50%; border: 2px solid #151922; background: #64748b;
}
.friend-item .status-dot.online { background: #22c55e; box-shadow: 0 0 8px rgba(34, 197, 94, 0.4); }

.friend-info { flex: 1; }
.friend-info .name { font-weight: 600; color: #f1f5f9; font-size: 1rem; margin-bottom: 2px; }
.friend-info .email { font-size: 0.85rem; color: #64748b; }

/* 액션 버튼 그룹 (호버 시 표시) */
.action-group {
  display: flex; gap: 8px; opacity: 0; transition: opacity 0.2s ease;
}
.friend-item:hover .action-group { opacity: 1; }

.icon-btn {
  width: 32px; height: 32px; border-radius: 8px; border: none;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: all 0.2s;
  background: rgba(255, 255, 255, 0.05); color: #64748b;
}

.icon-btn.chat:hover { background: rgba(0, 212, 255, 0.15); color: #00d4ff; transform: translateY(-2px); }
.icon-btn.delete:hover { background: rgba(239, 68, 68, 0.15); color: #ef4444; transform: translateY(-2px); }

/* ✅ [추가] 초대 버튼 스타일 */
.icon-btn.invite { color: #22c55e; }
.icon-btn.invite:hover { 
  background: rgba(34, 197, 94, 0.15); 
  color: #22c55e; 
  transform: translateY(-2px);
  box-shadow: 0 0 10px rgba(34, 197, 94, 0.2);
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes zoomIn { from { transform: scale(0.95); opacity: 0; } to { transform: scale(1); opacity: 1; } }

/* 참여중 (파랑) */
.member-badge {
  font-size: 0.75rem; font-weight: 700;
  color: #00d4ff; background: rgba(0, 212, 255, 0.1);
  padding: 4px 8px; border-radius: 6px;
  border: 1px solid rgba(0, 212, 255, 0.2);
  white-space: nowrap; cursor: default;
}

/* 초대됨 (노랑/주황) */
.pending-badge {
  font-size: 0.75rem; font-weight: 700;
  color: #fbbf24; /* Amber-400 */
  background: rgba(251, 191, 36, 0.1);
  padding: 4px 8px; border-radius: 6px;
  border: 1px solid rgba(251, 191, 36, 0.2);
  white-space: nowrap; cursor: default;
}
/*추방 버튼 스타일 (빨간색) */
.icon-btn.kick {
  color: #ef4444; /* Red */
  border: 1px solid rgba(239, 68, 68, 0.3);
}
.icon-btn.kick:hover {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
  transform: translateY(-2px);
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.2);
}
</style>