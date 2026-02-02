<script setup>
import { reactive, ref, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import api from '@/api/axios';
import GlobalModal from '@/modal/GlobalModal.vue';

// ✅ 아이콘 (요구사항: "이미 적용중"일 때 빨간 경고 아이콘)
import { CheckCircle, XCircle, TriangleAlert } from 'lucide-vue-next';

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
  confirmPassword: '',
});

// ✅ "원래 닉네임" 저장 (닉네임 변경 여부 판단용)
const originalNickname = ref(props.user?.nickname || '');

// ✅ 닉네임 중복체크 상태
const isNicknameChecking = ref(false);
const nicknameChecked = ref(false);
const nicknameAvailable = ref(false);

// ✅ 닉네임이 바뀌면 중복체크 초기화
const handleNicknameInput = () => {
  nicknameChecked.value = false;
  nicknameAvailable.value = false;
};

// ✅ 버튼 라벨
const getNicknameButtonLabel = () => {
  if (nicknameAvailable.value) return '사용가능';
  return '중복체크';
};

// ✅ 모달 상태 (alert 대체)
const modal = ref({
  open: false,
  message: '',
  type: 'info', // warning | info | success | error
  focusField: null, // 'nickname' | 'currentPassword' | 'newPassword' | 'confirmPassword' | null
  onConfirm: null,
  icon: null,
});

const openModal = (
  message,
  type = 'info',
  focusField = null,
  onConfirm = null,
  icon = null
) => {
  modal.value.open = true;
  modal.value.message = message;
  modal.value.type = type;
  modal.value.focusField = focusField;
  modal.value.onConfirm = onConfirm;
  modal.value.icon = icon;
};

const closeModal = async () => {
  modal.value.open = false;
  await nextTick();

  // 후처리 콜백이 있으면 실행
  if (modal.value.onConfirm) {
    const fn = modal.value.onConfirm;
    modal.value.onConfirm = null;
    fn?.();
    return;
  }

  // 포커스 이동 (UX)
  const field = modal.value.focusField;
  modal.value.focusField = null;
  if (!field) return;

  const root = document.querySelector('.modal-content');
  if (!root) return;
  const target = root.querySelector(`[data-focus="${field}"]`);
  target?.focus?.();
};

// Enter/Escape로 모달 닫기
const handleKeydown = (e) => {
  if (!modal.value.open) return;
  if (e.key === 'Enter' || e.key === 'Escape') {
    e.preventDefault();
    closeModal();
  }
};

onMounted(() => window.addEventListener('keydown', handleKeydown));
onBeforeUnmount(() => window.removeEventListener('keydown', handleKeydown));

/* =========================
   ✅ 닉네임 중복체크 API
========================= */
const checkNickname = async () => {
  if (isNicknameChecking.value) return;
  if (nicknameAvailable.value) return; // 이미 사용가능이면 재요청 막기

  const nick = (editData.nickname || '').trim();

  if (!nick) {
    return openModal('닉네임을 입력해주세요.', 'warning', 'nickname');
  }

  // 닉네임 입력값 간단 규칙
  if (nick.length < 2 || nick.length > 20) {
    return openModal('닉네임은 2~20자여야 합니다.', 'warning', 'nickname');
  }
  if (nick.includes(' ')) {
    return openModal(
      '닉네임에는 공백을 사용할 수 없습니다.',
      'warning',
      'nickname'
    );
  }

  // ✅ 요구사항: 변경사항 없으면 "이미 적용중" + 빨간 경고 아이콘
  if (nick === (originalNickname.value || '').trim()) {
    // 상태는 "문제 없음"으로 처리해도 되고(아래처럼 true),
    // 굳이 안 남기고 싶으면 false로 바꿔도 됨.
    nicknameChecked.value = true;
    nicknameAvailable.value = true;

    return openModal(
      '이미 적용중인 닉네임입니다.',
      'warning',
      null,
      null,
      TriangleAlert
    );
  }

  try {
    isNicknameChecking.value = true;

    const res = await api.get('/member/nicknameCheck', {
      params: { nickname: nick },
    });

    nicknameChecked.value = true;
    nicknameAvailable.value = !!res.data.available;

    if (nicknameAvailable.value) {
      openModal(
        '사용 가능한 닉네임입니다',
        'success',
        null,
        async () => {
          await nextTick();
        },
        CheckCircle
      );
    } else {
      openModal(
        '이미 사용중인 닉네임입니다',
        'error',
        'nickname',
        async () => {
          await nextTick();
          const root = document.querySelector('.modal-content');
          const target = root?.querySelector(`[data-focus="nickname"]`);
          target?.focus?.();
        },
        XCircle
      );
    }
  } catch (e) {
    nicknameChecked.value = false;
    nicknameAvailable.value = false;
    const msg = e?.response?.data?.message || '닉네임 확인 실패';
    openModal(msg, 'warning', 'nickname');
  } finally {
    isNicknameChecking.value = false;
  }
};

// ✅ 모달 열린 상태면 엔터는 닫기만, 아니면 닉네임 중복체크 실행
const onEnterNickname = () => {
  if (modal.value.open) return;
  if (nicknameAvailable.value) return;
  checkNickname();
};

// 2. 수정 요청 실행
const handleUpdate = async () => {
  // 닉네임 기본 검사
  if (!editData.nickname.trim()) {
    return openModal('닉네임은 필수입니다.', 'warning', 'nickname');
  }

  // ✅ 닉네임이 "변경된 경우"에는 중복체크 강제
  const nick = editData.nickname.trim();
  const nickChanged = nick !== (originalNickname.value || '').trim();

  if (nickChanged) {
    if (!nicknameChecked.value) {
      return openModal('닉네임 중복체크를 해주세요.', 'warning', 'nickname');
    }
    if (!nicknameAvailable.value) {
      return openModal(
        '사용 가능한 닉네임으로 변경해주세요.',
        'warning',
        'nickname'
      );
    }
  }

  // 비밀번호 관련 입력이 하나라도 있는 경우 검사 시작
  if (editData.newPassword || editData.currentPassword) {
    if (!editData.currentPassword) {
      return openModal(
        '현재 비밀번호를 입력해야 합니다.',
        'warning',
        'currentPassword'
      );
    }

    if (editData.newPassword) {
      if (editData.newPassword.length < 8) {
        return openModal(
          '새 비밀번호는 최소 8자 이상이어야 합니다.',
          'warning',
          'newPassword'
        );
      }
      if (editData.newPassword !== editData.confirmPassword) {
        return openModal(
          '새 비밀번호가 일치하지 않습니다.',
          'warning',
          'confirmPassword'
        );
      }
    }
  }

  try {
    const response = await api.put('/member/profile', editData);

    // Pinia 스토어 갱신
    if (authStore.user) {
      authStore.user = { ...authStore.user, ...response.data };
    } else {
      authStore.user = response.data;
    }

    // ✅ 저장 성공 후 "원래 닉네임"도 갱신 (다음에 다시 열었을 때 일관성)
    originalNickname.value = response.data?.nickname ?? editData.nickname;

    openModal('프로필이 성공적으로 수정되었습니다.', 'success', null, () => {
      emit('updated');
      emit('close');
    });
  } catch (error) {
    console.error('수정 실패:', error);
    const errorMsg =
      error.response?.data?.message || '수정 중 오류가 발생했습니다.';
    openModal(errorMsg, 'error');
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

          <!-- ✅ input + 버튼 row -->
          <div class="input-row">
            <input
              v-model="editData.nickname"
              type="text"
              placeholder="닉네임 입력"
              data-focus="nickname"
              class="has-right-btn"
              @input="handleNicknameInput"
              @keydown.enter.prevent="onEnterNickname"
            />

            <button
              type="button"
              class="nickname-check-btn"
              :class="{ 'is-available': nicknameAvailable }"
              :disabled="isNicknameChecking || nicknameAvailable"
              @click="checkNickname"
            >
              <span v-if="!isNicknameChecking">{{
                getNicknameButtonLabel()
              }}</span>
              <span v-else class="btn-loading">
                <span class="mini-spinner"></span>
                확인중
              </span>
            </button>
          </div>
        </div>

        <div class="input-group">
          <label>Status Message</label>
          <textarea
            v-model="editData.bio"
            rows="3"
            placeholder="나를 표현하는 한마디"
          ></textarea>
        </div>

        <hr class="divider" />

        <div class="section-title">Security</div>

        <div class="input-group">
          <label>Current Password</label>
          <input
            v-model="editData.currentPassword"
            type="password"
            placeholder="현재 비밀번호"
            data-focus="currentPassword"
          />
        </div>

        <div class="input-group">
          <label>New Password</label>
          <input
            v-model="editData.newPassword"
            type="password"
            placeholder="새 비밀번호 (변경 시에만)"
            data-focus="newPassword"
          />
        </div>

        <div class="input-group">
          <label>Confirm New Password</label>
          <input
            v-model="editData.confirmPassword"
            type="password"
            placeholder="새 비밀번호 확인"
            data-focus="confirmPassword"
          />
        </div>
      </div>

      <div class="modal-footer">
        <button class="btn-cancel" @click="$emit('close')">Cancel</button>
        <button class="btn-save" @click="handleUpdate">Save Changes</button>
      </div>
    </div>
  </div>

  <!-- ✅ 전역 모달 (alert 대체) -->
  <GlobalModal
    :open="modal.open"
    :message="modal.message"
    :type="modal.type"
    :icon="modal.icon"
    @confirm="closeModal"
  />
</template>

<style scoped>
/* 1. 전체 오버레이: 배경을 매우 어둡게 처리 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

/* 2. 모달 컨테이너: 어두운 남색 배경과 하늘색 테두리 */
.modal-content {
  background: #0d1f3c;
  border: 1px solid rgba(0, 217, 255, 0.2);
  padding: 2rem;
  border-radius: 16px;
  width: 420px;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.5);
}

/* 3. 모달 헤더 */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.2rem;
  background: transparent !important;
  border: none !important;
  padding: 0.25rem 0;
}
.modal-header h2 {
  color: #00d9ff;
  font-size: 1.5rem;
  margin: 0;
}
.close-x {
  background: none;
  border: none;
  color: #7a8a99;
  font-size: 2rem;
  cursor: pointer;
}

/* 4. 바디 배경 투명 */
.modal-body {
  background: transparent !important;
  padding: 0.5rem 0;
}

.section-title {
  color: #00d9ff;
  font-size: 0.85rem;
  font-weight: 700;
  text-transform: uppercase;
  margin: 1.1rem 0 0.5rem;
  letter-spacing: 1px;
}

.input-group {
  margin-bottom: 1rem;
  background: transparent !important;
}
.input-group label {
  display: block;
  color: #b0b8c1;
  margin-bottom: 0.6rem;
  font-size: 0.9rem;
}

.input-group input,
.input-group textarea {
  width: 100%;
  background: #1a2a4a !important;
  border: 1px solid rgba(0, 217, 255, 0.2);
  padding: 0.8rem;
  color: #ffffff !important;
  border-radius: 8px;
  outline: none;
  font-size: 1rem;
}

.input-group input:focus,
.input-group textarea:focus {
  border-color: #00d9ff;
  background: #243454 !important;
}

.divider {
  border: none;
  border-top: 1px solid rgba(0, 217, 255, 0.1);
  margin: 2rem 0 1rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
}
/* ✅ Cancel 버튼: 세련된 레드 톤 (기존 .btn-cancel 교체) */
.btn-cancel {
  background: rgba(255, 77, 77, 0.08); /* 은은한 레드 글로우 */
  border: 1px solid rgba(255, 77, 77, 0.55); /* 세련된 레드 테두리 */
  color: rgba(255, 120, 120, 0.95); /* 너무 쨍하지 않은 레드 텍스트 */
  padding: 0.7rem 1.5rem;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 700;
  letter-spacing: 0.2px;
  transition: all 0.22s ease;
}

.btn-cancel:hover {
  background: rgba(255, 77, 77, 0.14);
  border-color: rgba(255, 77, 77, 0.85);
  color: rgba(255, 160, 160, 1);
  box-shadow: 0 10px 26px rgba(255, 77, 77, 0.18);
  transform: translateY(-1px);
}

.btn-cancel:active {
  transform: translateY(0px);
  box-shadow: 0 6px 18px rgba(255, 77, 77, 0.14);
}

.btn-cancel:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(255, 77, 77, 0.22),
    0 10px 26px rgba(255, 77, 77, 0.14);
}
/* 🔵 Save Changes 버튼 (Cancel 버튼과 애니메이션 완전 통일) */
.btn-save {
  background: linear-gradient(
    135deg,
    rgba(0, 217, 255, 0.9) 0%,
    rgba(0, 170, 220, 0.9) 100%
  );
  border: none;
  color: #0a1628;
  padding: 0.7rem 1.5rem;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 800;
  letter-spacing: 0.4px;
  transition: all 0.28s ease;
  box-shadow: 0 6px 18px rgba(0, 217, 255, 0.25);
}

/* hover: Cancel 버튼과 동일한 "뜸 + 글로우" */
.btn-save:hover {
  transform: translateY(-2px);
  box-shadow:
    0 14px 36px rgba(0, 217, 255, 0.45),
    0 0 0 1px rgba(120, 230, 255, 0.45);
  background: linear-gradient(
    135deg,
    rgba(80, 235, 255, 0.95) 0%,
    rgba(0, 200, 240, 0.95) 100%
  );
}

/* 클릭 */
.btn-save:active {
  transform: translateY(0px);
  box-shadow: 0 8px 20px rgba(0, 217, 255, 0.3);
}

/* 키보드 포커스 접근성 */
.btn-save:focus-visible {
  outline: none;
  box-shadow:
    0 0 0 3px rgba(0, 217, 255, 0.35),
    0 14px 36px rgba(0, 217, 255, 0.4);
}

/* 스크롤바 커스텀 */
.custom-scroll::-webkit-scrollbar {
  width: 4px;
}
.custom-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 217, 255, 0.2);
  border-radius: 10px;
}

/* 닉네임 input + 버튼 한 줄 배치 */
.input-row {
  position: relative;
  display: flex;
  gap: 0.6rem;
  align-items: center;
}

/* 오른쪽 버튼 공간 */
.has-right-btn {
  flex: 1;
}

/* 중복체크 버튼 */
.nickname-check-btn {
  height: 44px;
  padding: 0 0.9rem;
  border-radius: 10px;
  border: 1px solid rgba(0, 217, 255, 0.25);
  background: rgba(0, 217, 255, 0.08);
  color: #e0e0e0;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.nickname-check-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  background: rgba(0, 217, 255, 0.14);
  border-color: rgba(0, 217, 255, 0.45);
}

.nickname-check-btn:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}

/* 사용가능일 때 글자색 */
.nickname-check-btn.is-available:disabled {
  color: rgba(51, 255, 153, 1);
}

/* 로딩 표시 */
.btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

.mini-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: rgba(0, 217, 255, 0.9);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
