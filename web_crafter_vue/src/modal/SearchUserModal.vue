<script setup>
import { ref } from 'vue';
import { X, Search, UserPlus } from 'lucide-vue-next';
import api from '@/api/axios';
import GlobalModal from '@/modal/GlobalModal.vue';

// 부모 컴포넌트(FriendListModal)에서 열고 닫음을 제어
defineProps(['isOpen']);
defineEmits(['close']);

const keyword = ref('');
const searchResults = ref([]);
const message = ref('');

/* ======================
   ✅ GlobalModal (alert 대체)
====================== */
const modal = ref({
  open: false,
  message: '',
  type: 'info', // warning | info | success | error
});

const openModal = (msg, type = 'info') => {
  modal.value.open = true;
  modal.value.message = msg;
  modal.value.type = type;
};

const closeModal = () => {
  modal.value.open = false;
};

/* ======================
   ✅ 1. 유저 검색 API 호출
====================== */
const handleSearch = async () => {
  if (!keyword.value.trim()) return;

  try {
    const res = await api.get(`/friends/search`, {
      params: { keyword: keyword.value },
    });
    searchResults.value = res.data;

    if (searchResults.value.length === 0) {
      message.value = '검색 결과가 없습니다.';
    } else {
      message.value = '';
    }
  } catch (e) {
    console.error(e);
    message.value = '검색 중 오류가 발생했습니다.';
    // ✅ 모달로도 알려주고 싶으면 유지 (원치 않으면 이 줄 삭제)
    openModal('검색 중 오류가 발생했습니다.', 'error');
  }
};

/* ======================
   ✅ 2. 친구 요청 보내기 API 호출
====================== */
const sendRequest = async (targetId) => {
  try {
    await api.post('/friends/request', { targetId });

    // ✅ alert → GlobalModal
    openModal('친구 요청을 보냈습니다! 💌', 'success');

    // (선택) 요청 보낸 사람은 목록에서 지우기
    searchResults.value = searchResults.value.filter((u) => u.id !== targetId);
  } catch (e) {
    console.error(e);
    // ✅ alert → GlobalModal
    openModal(e.response?.data?.message || '요청 실패', 'error');
  }
};
</script>

<template>
  <div v-if="isOpen" class="search-overlay">
    <div class="search-box">
      <div class="header">
        <h3>Add Crafter</h3>
        <button class="close-btn" @click="$emit('close')">
          <X :size="20" />
        </button>
      </div>

      <div class="input-wrapper">
        <input
          v-model="keyword"
          @keyup.enter="handleSearch"
          placeholder="닉네임 또는 이메일 검색"
          autofocus
        />
        <button class="search-btn" @click="handleSearch">
          <Search :size="18" />
        </button>
      </div>

      <div class="result-list">
        <div v-for="user in searchResults" :key="user.id" class="user-row">
          <div class="info">
            <span class="nick">{{ user.nickname }}</span>
            <span class="email">{{ user.email }}</span>
          </div>
          <button class="add-btn" @click="sendRequest(user.id)">
            <UserPlus :size="16" /> Add
          </button>
        </div>

        <div v-if="message" class="no-result">{{ message }}</div>
      </div>
    </div>

    <!-- ✅ 알림 모달 (네가 쓰는 방식 그대로) -->
    <GlobalModal
      :open="modal.open"
      :message="modal.message"
      :type="modal.type"
      @confirm="closeModal"
    />
  </div>
</template>

<style scoped>
/* 모달 배경 */
.search-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;

  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 120px;

  background: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  backdrop-filter: blur(2px);
  border-radius: 20px;
}

/* 검색 박스 */
.search-box {
  width: 480px;

  background: #1e293b;
  border: 1px solid #00d9ff;
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
  animation: slideDown 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 1.2rem;
  color: #fff;
  align-items: center;
}
.header h3 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 700;
  color: #fff;
}

.close-btn {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: 0.2s;
}
.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.input-wrapper {
  display: flex;
  gap: 10px;
  margin-bottom: 1rem;
}
input {
  flex: 1;
  padding: 12px 14px;
  background: #0f172a;
  border: 1px solid #334155;
  color: white;
  border-radius: 10px;
  font-size: 0.95rem;
  transition: 0.2s;
}
input:focus {
  outline: none;
  border-color: #00d9ff;
  box-shadow: 0 0 0 3px rgba(0, 217, 255, 0.1);
}

.search-btn {
  background: #00d9ff;
  border: none;
  border-radius: 10px;
  width: 48px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0f172a;
  transition: 0.2s;
}
.search-btn:hover {
  background: #60e4ff;
}

/* 결과 리스트 */
.result-list {
  max-height: 300px;
  overflow-y: auto;
}

.user-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  margin-bottom: 8px;
  border-radius: 10px;
  border: 1px solid transparent;
}
.user-row:hover {
  border-color: rgba(0, 217, 255, 0.3);
  background: rgba(0, 217, 255, 0.05);
}

.info {
  display: flex;
  flex-direction: column;
}
.nick {
  color: white;
  font-weight: 700;
  font-size: 0.95rem;
}
.email {
  color: #94a3b8;
  font-size: 0.8rem;
}

.add-btn {
  background: rgba(0, 217, 255, 0.1);
  border: 1px solid rgba(0, 217, 255, 0.3);
  color: #00d9ff;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  font-weight: 600;
  transition: 0.2s;
}
.add-btn:hover {
  background: #00d9ff;
  color: #0f172a;
}

.no-result {
  text-align: center;
  color: #64748b;
  margin-top: 20px;
  font-size: 0.9rem;
}

@keyframes slideDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>