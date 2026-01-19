<script setup>
import { reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import axios from '@/api/axios';

const props = defineProps(['user']);
const emit = defineEmits(['close', 'updated']);
const authStore = useAuthStore();
const router = useRouter();

// 1. 수정 데이터 상태 관리
const editData = reactive({
  nickname: props.user?.nickname || '',
  bio: props.user?.bio || '',
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 2. 수정 요청 실행
const handleUpdate = async () => {
  // 닉네임 기본 검사
  if (!editData.nickname.trim()) return alert('닉네임은 필수입니다.');
  
  // 비밀번호 관련 입력이 하나라도 있는 경우 검사 시작
  if (editData.newPassword || editData.currentPassword) {
    // 1) 현재 비밀번호 입력 여부 확인
    if (!editData.currentPassword) return alert('현재 비밀번호를 입력해야 합니다.');
    
    // 2) 새 비밀번호 자릿수 검사 (8자 이상이면 통과) [cite: 2026-01-19]
    if (editData.newPassword) {
      if (editData.newPassword.length < 8) {
        return alert('새 비밀번호는 최소 8자 이상이어야 합니다.');
      }
      
      // 3) 새 비밀번호와 확인 입력 일치 여부
      if (editData.newPassword !== editData.confirmPassword) {
        return alert('새 비밀번호가 일치하지 않습니다.');
      }
    }
  }

  try {
    const response = await axios.put('/member/profile', editData);
    
    // Pinia 스토어 갱신
    if (authStore.user) {
      authStore.user = { ...authStore.user, ...response.data };
    } else {
      authStore.user = response.data;
    }
    
    alert('프로필이 성공적으로 수정되었습니다.'); 
    emit('updated');
    emit('close');
  } catch (error) {
    console.error("수정 실패:", error);
    // 백엔드에서 던지는 "현재 비밀번호 불일치" 등의 메시지 출력
    const errorMsg = error.response?.data?.message || '수정 중 오류가 발생했습니다.';
    alert(errorMsg);
  }
};
</script>

<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>Edit Profile</h2>
        <button class="close-x" @click="$emit('close')">&times;</button>
      </div>
      
      <div class="modal-body custom-scroll">
        <div class="section-title">General</div>
        <div class="input-group">
          <label>Nickname</label>
          <input v-model="editData.nickname" type="text" placeholder="닉네임 입력" />
        </div>

        <div class="input-group">
          <label>Status Message</label>
          <textarea v-model="editData.bio" rows="3" placeholder="나를 표현하는 한마디"></textarea>
        </div>

        <hr class="divider" />

        <div class="section-title">Security</div>
        <div class="input-group">
          <label>Current Password</label>
          <input v-model="editData.currentPassword" type="password" placeholder="현재 비밀번호" />
        </div>
        <div class="input-group">
          <label>New Password</label>
          <input v-model="editData.newPassword" type="password" placeholder="새 비밀번호 (변경 시에만)" />
        </div>
        <div class="input-group">
          <label>Confirm New Password</label>
          <input v-model="editData.confirmPassword" type="password" placeholder="새 비밀번호 확인" />
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="$emit('close')">Cancel</button>
        <button class="btn-save" @click="handleUpdate">Save Changes</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 1. 전체 오버레이: 배경을 매우 어둡게 처리 */
.modal-overlay {
  position: fixed; top: 0; left: 0; width: 100%; height: 100%;
  background: rgba(0, 0, 0, 0.85); display: flex; align-items: center; justify-content: center; z-index: 2000;
}

/* 2. 모달 컨테이너: 어두운 남색 배경과 하늘색 테두리 */
.modal-content {
  background: #0d1f3c; 
  border: 1px solid rgba(0, 217, 255, 0.2);
  padding: 2rem; border-radius: 16px; width: 420px; 
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.5);
}

/* 3. 모달 헤더 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
  
  /* 👈 이 부분이 핵심입니다! */
  background: transparent !important; /* 배경색을 투명하게 해서 뒤의 진한 남색이 보이게 함 */
  border: none !important;            /* 혹시 있을지 모를 테두리 제거 */
  padding: 0.5rem 0;                  /* 상하 여백만 살짝 조절 */
}
.modal-header h2 { color: #00d9ff; font-size: 1.5rem; margin: 0; }
.close-x { background: none; border: none; color: #7a8a99; font-size: 2rem; cursor: pointer; }

/* 4. 🔥 흰색 판 제거 핵심: 바디 배경을 투명하게 설정 */
.modal-body {
  background: transparent !important; 
  padding: 0.5rem 0;
}

.section-title { color: #00d9ff; font-size: 0.85rem; font-weight: 700; text-transform: uppercase; margin: 1.5rem 0 0.8rem; letter-spacing: 1px; }

/* 5. 입력창 그룹: 배경 투명화 */
.input-group { margin-bottom: 1.5rem; background: transparent !important; }
.input-group label { display: block; color: #b0b8c1; margin-bottom: 0.6rem; font-size: 0.9rem; }

/* 6. 입력창 자체 스타일: 어두운 배경과 흰색 글자 고정 */
.input-group input, .input-group textarea {
  width: 100%; 
  background: #1a2a4a !important; 
  border: 1px solid rgba(0, 217, 255, 0.2);
  padding: 0.8rem; 
  color: #ffffff !important; 
  border-radius: 8px; 
  outline: none; 
  font-size: 1rem;
}

/* 포커스 시 테두리 강조 */
.input-group input:focus, .input-group textarea:focus {
  border-color: #00d9ff;
  background: #243454 !important;
}

.divider { border: none; border-top: 1px solid rgba(0, 217, 255, 0.1); margin: 2rem 0 1rem; }

/* 7. 하단 버튼 영역 */
.modal-footer { display: flex; justify-content: flex-end; gap: 1rem; margin-top: 2rem; }
.btn-cancel { background: transparent; border: 1px solid #7a8a99; color: #7a8a99; padding: 0.7rem 1.5rem; border-radius: 8px; cursor: pointer; }
.btn-save { background: #00d9ff; border: none; color: #0a1628; padding: 0.7rem 1.5rem; border-radius: 8px; cursor: pointer; font-weight: 700; }

/* 스크롤바 커스텀 */
.custom-scroll::-webkit-scrollbar { width: 4px; }
.custom-scroll::-webkit-scrollbar-thumb { background: rgba(0, 217, 255, 0.2); border-radius: 10px; }
</style>