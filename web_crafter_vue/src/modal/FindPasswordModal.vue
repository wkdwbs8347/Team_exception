<script setup>
import { ref, computed, watch, nextTick } from 'vue';
import {
  Mail,
  User,
  X,
  Send,
  AlertTriangle,
  CheckCircle,
  XCircle,
  CircleHelp,
} from 'lucide-vue-next';

const props = defineProps({
  open: Boolean,
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(['close', 'submit']);

const name = ref('');
const email = ref('');

const fieldErrors = ref({
  name: '',
  email: '',
});

const clearErrors = () => {
  fieldErrors.value.name = '';
  fieldErrors.value.email = '';
};

const resetForm = () => {
  name.value = '';
  email.value = '';
  clearErrors();
};

watch(
  () => props.open,
  async (v) => {
    if (v) {
      await nextTick();
      // 열릴 때 초기화는 원하면 켤 수 있음 (기본: 유지)
      // resetForm()
    } else {
      clearErrors();
    }
  }
);

const isValidEmailFormat = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

const validate = () => {
  clearErrors();
  let ok = true;

  if (!name.value.trim()) {
    fieldErrors.value.name = '이름을 입력해주세요.';
    ok = false;
  }
  const e = email.value.trim();
  if (!e) {
    fieldErrors.value.email = '이메일을 입력해주세요.';
    ok = false;
  } else if (!isValidEmailFormat(e)) {
    fieldErrors.value.email = '이메일 형식이 올바르지 않습니다.';
    ok = false;
  }

  return ok;
};

const handleSubmit = () => {
  if (!validate()) return;
  emit('submit', { name: name.value.trim(), email: email.value.trim() });
};

const onBackdrop = (e) => {
  if (e.target?.classList?.contains('modal-backdrop')) emit('close');
};

const onKeydown = (e) => {
  if (!props.open) return;
  if (e.key === 'Escape') emit('close');
  if (e.key === 'Enter') handleSubmit();
};

watch(
  () => props.open,
  (v) => {
    if (v) window.addEventListener('keydown', onKeydown);
    else window.removeEventListener('keydown', onKeydown);
  }
);
</script>

<template>
  <transition name="modal-fade">
    <div v-if="open" class="modal-backdrop" @click="onBackdrop">
      <div class="modal-card">
        <!-- 헤더 -->
        <div class="modal-head">
          <div class="modal-icon-wrapper info">
            <Send class="modal-icon" />
          </div>
          <button
            class="close-btn"
            type="button"
            @click="$emit('close')"
            aria-label="close"
          >
            <X :size="18" />
          </button>
        </div>

        <h3 class="modal-title">비밀번호 찾기</h3>
        <p class="modal-desc">
          가입 시 사용한 <b>이름</b>과 <b>이메일</b>을 입력하면<br />
          해당 이메일로 <b>임시 비밀번호</b>를 전송합니다.
        </p>

        <!-- 폼 -->
        <div class="form">
          <!-- 이름 -->
          <div class="form-group">
            <label class="form-label">이름</label>
            <div class="input-wrapper">
              <span class="input-icon"><User :size="18" /></span>
              <input
                v-model="name"
                class="form-input"
                type="text"
                placeholder="홍길동"
                :disabled="loading"
                @input="fieldErrors.name = ''"
              />
              <div v-if="fieldErrors.name" class="error-tooltip">
                <AlertTriangle class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.name }}</span>
              </div>
            </div>
          </div>

          <!-- 이메일 -->
          <div class="form-group">
            <label class="form-label">이메일</label>
            <div class="input-wrapper">
              <span class="input-icon"><Mail :size="18" /></span>
              <input
                v-model="email"
                class="form-input"
                type="email"
                placeholder="you@example.com"
                :disabled="loading"
                @input="fieldErrors.email = ''"
              />
              <div v-if="fieldErrors.email" class="error-tooltip">
                <AlertTriangle class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.email }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 버튼 -->
        <div class="btn-row">
          <button
            class="btn ghost"
            type="button"
            :disabled="loading"
            @click="$emit('close')"
          >
            취소
          </button>
          <button
            class="btn primary"
            type="button"
            :disabled="loading"
            @click="handleSubmit"
          >
            <span v-if="!loading">찾기</span>
            <span v-else class="loading">
              <span class="spinner"></span>
              전송 중...
            </span>
          </button>
        </div>

        <!-- (선택) 아래 리셋 버튼이 필요하면 주석 해제 -->
        <!--
        <button class="reset-link" type="button" :disabled="loading" @click="resetForm">
          입력 초기화
        </button>
        -->
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ====== 배경 (GlobalModal 느낌 유지) ====== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(10, 15, 30, 0.65);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

/* ====== 카드 ====== */
.modal-card {
  background: linear-gradient(
    135deg,
    rgba(15, 15, 30, 0.92) 0%,
    rgba(26, 26, 46, 0.92) 100%
  );
  border: 1.5px solid rgba(0, 212, 255, 0.35);
  border-radius: 18px;
  padding: 2.1rem 2.2rem;
  width: 100%;
  max-width: 420px;
  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(0, 212, 255, 0.15),
    0 0 40px rgba(0, 212, 255, 0.25);
  animation: popIn 0.35s ease-out;
  position: relative;
}

/* 헤더 */
.modal-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.close-btn {
  border: none;
  background: rgba(255, 255, 255, 0.06);
  color: #e5e7eb;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  cursor: pointer;
  display: grid;
  place-items: center;
  transition: all 0.2s ease;
}
.close-btn:hover {
  background: rgba(0, 212, 255, 0.12);
  transform: translateY(-1px);
}

/* 아이콘 래퍼 */
.modal-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(0, 212, 255, 0.15);
  margin-bottom: 1rem;
}
.modal-icon {
  width: 32px;
  height: 32px;
  color: #00d4ff;
}

/* 타이틀/설명 */
.modal-title {
  color: #e5e7eb;
  font-size: 1.1rem;
  font-weight: 900;
  margin: 0.2rem 0 0.6rem;
}
.modal-desc {
  color: #b9c2d0;
  font-size: 0.9rem;
  line-height: 1.6;
  margin-bottom: 1.4rem;
}
.modal-desc b {
  color: #e5e7eb;
}

/* 폼 */
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}
.form-label {
  color: #d5d9e3;
  font-weight: 700;
  font-size: 0.82rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon {
  position: absolute;
  left: 1rem;
  pointer-events: none;
  color: rgba(0, 212, 255, 0.9);
}
.form-input {
  width: 100%;
  padding: 0.85rem 1rem 0.85rem 2.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 12px;
  color: #e5e7eb;
  font-size: 0.95rem;
  transition: all 0.25s ease;
}
.form-input::placeholder {
  color: #6b7280;
}
.form-input:focus {
  outline: none;
  background: rgba(0, 212, 255, 0.06);
  border-color: rgba(0, 212, 255, 0.55);
  box-shadow: 0 0 22px rgba(0, 212, 255, 0.22);
}
.form-input:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 버튼 */
.btn-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.8rem;
  margin-top: 1.4rem;
}
.btn {
  border: none;
  border-radius: 12px;
  padding: 0.9rem 1rem;
  font-weight: 900;
  cursor: pointer;
  transition: all 0.25s ease;
}
.btn.ghost {
  background: rgba(255, 255, 255, 0.06);
  color: #e5e7eb;
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.btn.ghost:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.1);
  transform: translateY(-2px);
}
.btn.primary {
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  color: #0f0f1e;
}
.btn.primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.45);
}
.btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 로딩 */
.loading {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.55rem;
}
.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(15, 15, 30, 0.25);
  border-top-color: #0f0f1e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 에러 툴팁 (로그인 페이지 느낌 그대로) */
.error-tooltip {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 10px;
  padding: 7px 12px;
  background: #0b1220;
  color: #e5e7eb;
  font-size: 0.72rem;
  font-weight: 500;
  line-height: 1.4;
  border-radius: 8px;
  border: 1.5px solid rgba(0, 212, 255, 0.75);
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 20;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(0, 212, 255, 0.12);
}
.error-tooltip::before {
  content: '';
  position: absolute;
  top: -14px;
  left: 22px;
  border: 7px solid transparent;
  border-bottom-color: rgba(0, 212, 255, 0.85);
}
.error-tooltip::after {
  content: '';
  position: absolute;
  top: -12px;
  left: 22px;
  border: 7px solid transparent;
  border-bottom-color: #0b1220;
}
.tooltip-icon {
  flex-shrink: 0;
  color: rgba(0, 212, 255, 0.95);
}

/* 트랜지션 */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* 팝 애니메이션 */
@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style>
