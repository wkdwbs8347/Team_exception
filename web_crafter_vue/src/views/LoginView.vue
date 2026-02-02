<script setup>
/*
  동작 흐름 (전역 모달 기반, 이메일 로그인)

  [입력 검증]
  1) 로그인 클릭
    - 이메일 비었음 → 모달(경고) + 이메일 focus
    - 이메일 형식 오류 → 모달(경고) + 이메일 focus
    - 비밀번호 비었음 → 모달(경고) + 비밀번호 focus

  [서버 로그인]
  2) POST /api/member/login 요청
    - 존재하지 않는 이메일 → 모달(경고) + email 비움 + 이메일 focus
    - 비밀번호 불일치 → 모달(경고) + password 비움 + 비밀번호 focus

  [성공]
  3) 성공 시 응답에서 nickname 꺼냄
  4) “[nickname]님 환영합니다.” 모달 띄움
  5) 모달 확인 누르면 "/" 이동
*/

import { ref, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import GlobalModal from '@/modal/GlobalModal.vue';
import FindPasswordModal from '@/modal/FindPasswordModal.vue';
import api from '@/api/axios'; // baseURL: http://localhost:8080/api (※ 다른 곳에서 쓸 수 있으니 유지)
import {
  TriangleAlert,
  Mail,
  Lock,
  Eye,
  EyeOff,
  Blocks, // 로고
  LayoutGrid, // 블록 조합
  Braces, // 코드 생성/구조
  Monitor, // 미리보기
  Settings2, // 설정 없이 바로 시작
  Play, // 실행
} from 'lucide-vue-next';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const router = useRouter();

// 엔터키로 모달 끌 수 있게
const handleKeydown = (e) => {
  // 모달 열려 있을 때만
  if (!modal.value.open) return;

  // Enter 키
  if (e.key === 'Enter') {
    e.preventDefault();
    closeModal();
  }
};

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown);
});

/* ======================
   입력 상태
====================== */
const email = ref('');
const password = ref('');
const rememberMe = ref(false); // UI만 유지(로직은 지금 무시)
const isLoading = ref(false);
const showPassword = ref(false);

/* ======================
   input ref (포커스 이동용)
====================== */
const emailRef = ref(null);
const passwordRef = ref(null);

/* ======================
   커스텀 말풍선 에러 상태 (회원가입 페이지 스타일)
====================== */
const fieldErrors = ref({
  email: '',
  password: '',
});

/* ======================
   말풍선: 마지막 blur 필드 (회원가입 페이지 방식)
====================== */
const lastBlurField = ref(null);

// 말풍선 전체 초기화
const clearAllTooltips = () => {
  Object.keys(fieldErrors.value).forEach((k) => {
    fieldErrors.value[k] = '';
  });
  lastBlurField.value = null;
};

/* ======================
   전역 모달 상태
====================== */
const modal = ref({
  open: false,
  message: '',
  type: 'info', // info | warning | success | error
  focusField: null, // 'email' | 'password'
  onConfirm: null, // 확인 누른 뒤 실행할 함수(성공 후 이동 등)
});

/* 모달 열기 */
const openModal = (
  message,
  type = 'info',
  focusField = null,
  onConfirm = null
) => {
  // 모달 띄우기 전: 기존 말풍선 싹 제거
  clearAllTooltips();

  // 포커스 줄 필드가 있으면 그 필드에만 말풍선 표시
  if (
    focusField &&
    message &&
    (focusField === 'email' || focusField === 'password')
  ) {
    fieldErrors.value[focusField] = message;
    lastBlurField.value = focusField;
  }

  modal.value.open = true;
  modal.value.message = message;
  modal.value.type = type;
  modal.value.focusField = focusField;
  modal.value.onConfirm = onConfirm;
};

/* 모달 닫기: onConfirm 우선 실행 → 아니면 focus 이동 */
const closeModal = async () => {
  modal.value.open = false;
  await nextTick();

  // 성공 모달 등 후처리
  if (modal.value.onConfirm) {
    const fn = modal.value.onConfirm;
    modal.value.onConfirm = null;
    fn();
    return;
  }

  // 경고/에러 모달은 해당 input으로 포커스
  if (modal.value.focusField === 'email') emailRef.value?.focus();
  if (modal.value.focusField === 'password') passwordRef.value?.focus();
  modal.value.focusField = null;
};

// 비밀번호 찾기 모달
const isFindPwOpen = ref(false);
const isFindPwLoading = ref(false);

const openFindPwModal = () => {
  clearAllTooltips();
  isFindPwOpen.value = true;
};

const closeFindPwModal = () => {
  isFindPwOpen.value = false;
};

// ✅ 임시 비밀번호 발송 요청
const submitFindPassword = async ({ name, email }) => {
  if (isFindPwLoading.value) return;

  isFindPwLoading.value = true;

  try {
    /**
     * ✅ 백엔드 엔드포인트는 너가 정하면 됨
     * 예시로 /member/password/temp 발송 API 가정
     *
     * 요청 바디:
     * { name: "...", email: "..." }
     */
    await api.post('/member/password/find', { name, email });

    closeFindPwModal();
    openModal('임시 비밀번호를 이메일로 전송했습니다.', 'success');
  } catch (e) {
    const msg =
      e?.response?.data?.message || '임시 비밀번호 전송에 실패했습니다.';
    // 모달은 유지하고, 알림은 GlobalModal로
    openModal(msg, 'error');
  } finally {
    isFindPwLoading.value = false;
  }
};

/* ======================
   유틸: 이메일 형식 체크
====================== */
const isValidEmailFormat = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v);

/* ======================
   단일 필드 검증 (회원가입 페이지 방식)
   - onBlur 시: 이전에 blur 되었던 필드 말풍선은 제거하고, 현재 필드만 표시
====================== */
const validateField = (field, mode = 'blur') => {
  // blur 검증일 때만 "이전 blur 말풍선 제거" 동작
  if (mode === 'blur' && lastBlurField.value && lastBlurField.value !== field) {
    fieldErrors.value[lastBlurField.value] = '';
  }

  let message = '';

  if (field === 'email') {
    const trimmedEmail = email.value.trim();
    if (!trimmedEmail) message = '이메일을 입력해주세요.';
    else if (!isValidEmailFormat(trimmedEmail))
      message = '이메일 형식이 올바르지 않습니다.';
  }

  if (field === 'password') {
    if (!password.value) message = '비밀번호를 입력해주세요.';
  }

  fieldErrors.value[field] = message;

  if (mode === 'blur') {
    lastBlurField.value = field;
  }

  return !message;
};

/* ======================
   로그인 요청
====================== */
const handleLogin = async () => {
  // 모달 떠 있으면 Enter로 재submit 방지
  if (modal.value.open) return;

  const trimmedEmail = email.value.trim();

  // 프론트 검증: 전부 모달로 처리
  if (!validateField('email', 'submit')) {
    return openModal(fieldErrors.value.email, 'warning', 'email');
  }
  if (!validateField('password', 'submit')) {
    return openModal(fieldErrors.value.password, 'warning', 'password');
  }

  isLoading.value = true;

  try {
    // Pinia 전역 상태로 로그인 처리 (로그인 직후 NavBar 즉시 반영)
    await auth.login({
      email: trimmedEmail,
      password: password.value,
      rememberMe: rememberMe.value, // 자동로그인 체크 유무
    });

    const nickname = auth.nickname || '회원';

    openModal(`${nickname}님 환영합니다.`, 'success', null, () => {
      router.push('/');
    });
  } catch (e) {
    // 실패: 서버 메시지 기반 분기
    const msg = e?.response?.data?.message || '로그인 실패';

    // 존재하지 않는 이메일
    if (msg.includes('존재') && msg.includes('이메일')) {
      fieldErrors.value.email = '';
      openModal('존재하지 않는 이메일입니다.', 'warning', 'email');
      return;
    }

    // 비밀번호 불일치
    if (msg.includes('비밀번호') && msg.includes('일치')) {
      password.value = '';
      fieldErrors.value.password = '';
      openModal('비밀번호가 일치하지 않습니다.', 'warning', 'password');
      return;
    }

    // 기타 에러
    openModal(msg, 'error');
  } finally {
    isLoading.value = false;
  }
};

/* ======================
   비밀번호 토글 UI 동작
====================== */
const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};

/* 이메일 찾기 / 비밀번호 찾기 라우팅 */
const goFindPw = () => openFindPwModal();

const handleSignUp = () => router.push('/register');
</script>

<template>
  <div class="login-container">
    <div class="login-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="login-wrapper">
      <div class="login-card">
        <!-- 헤더 -->
        <div class="login-header">
          <div class="logo-section">
            <span class="logo-icon"><Blocks :size="28" /></span>
            <h1 class="logo-text">Web Crafter</h1>
          </div>
          <p class="subtitle">당신의 작업실에 오신 걸 환영합니다!</p>
        </div>

        <!-- 로그인 폼 -->
        <form
          class="login-form"
          @submit.prevent="handleLogin"
          autocomplete="off"
          novalidate
        >
          <!-- 이메일 입력 -->
          <div class="form-group">
            <label for="email" class="form-label">이메일 주소</label>
            <div class="input-wrapper">
              <span class="input-icon"><Mail :size="18" /></span>
              <input
                id="email"
                ref="emailRef"
                v-model="email"
                type="email"
                placeholder="you@example.com"
                class="form-input"
                @blur="validateField('email', 'blur')"
                @input="fieldErrors.email = ''"
              />
              <div v-if="fieldErrors.email" class="error-tooltip">
                <TriangleAlert class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.email }}</span>
              </div>
            </div>
          </div>

          <!-- 비밀번호 입력 -->
          <div class="form-group">
            <label for="password" class="form-label">비밀번호</label>
            <div class="input-wrapper">
              <span class="input-icon"><Lock :size="18" /></span>
              <input
                id="password"
                ref="passwordRef"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="password"
                class="form-input"
                @blur="validateField('password', 'blur')"
                @input="fieldErrors.password = ''"
              />
              <div v-if="fieldErrors.password" class="error-tooltip">
                <TriangleAlert class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.password }}</span>
              </div>
              <button
                type="button"
                class="password-toggle"
                @click="togglePasswordVisibility"
                :title="showPassword ? 'Hide password' : 'Show password'"
              >
                <Eye v-if="showPassword" :size="18" />
                <EyeOff v-else :size="18" />
              </button>
            </div>
          </div>

          <!-- 자동 로그인 및 찾기 링크 -->
          <div class="form-options">
            <label class="remember-me">
              <input v-model="rememberMe" type="checkbox" />
              <span>자동 로그인</span>
            </label>

            <!-- 이메일 찾기 및 비밀번호 찾기 -->
            <div class="find-links">
              <button type="button" class="forgot-password" @click="goFindPw">
                비밀번호 찾기
              </button>
            </div>
          </div>

          <!-- 로그인 버튼 -->
          <button type="submit" class="login-btn" :disabled="isLoading">
            <span v-if="!isLoading">로그인</span>
            <span v-else class="loading-spinner">
              <span class="spinner"></span>
              로그인 중...
            </span>
          </button>
        </form>

        <!-- 회원가입 링크 -->
        <div class="signup-section">
          <p>
            아직 계정이 없으신가요?
            <button type="button" class="signup-link" @click="handleSignUp">
              회원가입
            </button>
          </p>
        </div>
      </div>

      <!-- 안내 카드 -->
      <div class="info-card">
        <div class="info-header">
          <span class="info-icon"><Blocks :size="26" /></span>
          <h3>이제, 웹을 직접 만들어볼 차례입니다</h3>
        </div>
        <ul class="info-list">
          <li>
            <span class="check-icon"><LayoutGrid :size="14" /></span>
            <span>블록을 조합해 웹페이지 구성</span>
          </li>
          <li>
            <span class="check-icon"><Braces :size="14" /></span>
            <span>직접 만들며 배우는 웹 구조</span>
          </li>
          <li>
            <span class="check-icon"><Settings2 :size="14" /></span>
            <span>복잡한 설정 없이 바로 시작</span>
          </li>
          <li>
            <span class="check-icon"><Monitor :size="14" /></span>
            <span>창작에만 집중할 수 있는 환경</span>
          </li>
          <li>
            <span class="check-icon"><Play :size="14" /></span>
            <span>당신만의 웹 프로젝트를 시작하세요</span>
          </li>
        </ul>
      </div>
    </div>
  </div>

  <!-- 전역 모달 -->
  <GlobalModal
    :open="modal.open"
    :message="modal.message"
    :type="modal.type"
    @confirm="closeModal"
  />

  <FindPasswordModal
    :open="isFindPwOpen"
    :loading="isFindPwLoading"
    @close="closeFindPwModal"
    @submit="submitFindPassword"
  />
</template>

<style scoped>
/* =========================================
   🔥 브라우저 자동완성(Autofill) 스타일 강제 수정 (가장 중요)
   - 크롬, 엣지 등에서 배경이 하얗게/파랗게 변하는 것 방지
========================================= */
input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
input:-webkit-autofill:active {
  /* 1. 글자 색상 */
  -webkit-text-fill-color: #e0e0e0 !important;
  
  /* 2. 배경색 덮어쓰기 (내부 그림자) - 다크 테마 유지 */
  -webkit-box-shadow: 0 0 0px 1000px #1a1a2e inset !important;
  
  /* 3. 배경 전환 애니메이션 딜레이 */
  transition: background-color 5000s ease-in-out 0s;
  
  /* 4. 보더 스타일 강제 지정 (Register.vue와 동일하게) */
  border: 1px solid rgba(0, 212, 255, 0.2) !important;
}

/* (선택사항) 자동완성된 상태에서 포커스(클릭) 했을 때 보더 색상 */
input:-webkit-autofill:focus {
    border-color: rgba(0, 212, 255, 0.5) !important;
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.2), inset 0 0 0px 1000px #1a1a2e !important;
}


/* ======================
   👇 기존 스타일 유지 👇
====================== */
.error-tooltip {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 10px;
  padding: 7px 12px;
  background: #0b1220; /* 딥 네이비 다크 */
  color: #e5e7eb; /* 소프트 화이트 */
  font-size: 0.72rem;
  font-weight: 500;
  line-height: 1.4;
  border-radius: 8px;
  border: 1.5px solid rgba(0, 212, 255, 0.75);
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 20;
  letter-spacing: 0.01em;
  box-shadow:
    0 8px 24px rgba(0, 0, 0, 0.45),
    0 0 0 1px rgba(0, 212, 255, 0.12);
}

/* 🔻 바깥 테두리 삼각형 */
.error-tooltip::before {
  content: '';
  position: absolute;
  top: -14px;
  left: 22px;
  border: 7px solid transparent;
  border-bottom-color: rgba(0, 212, 255, 0.85);
  z-index: 1;
}

/* 🔻 안쪽 배경 삼각형 */
.error-tooltip::after {
  content: '';
  position: absolute;
  top: -12px;
  left: 22px;
  border: 7px solid transparent;
  border-bottom-color: #0b1220;
  z-index: 2;
}

.tooltip-icon {
  flex-shrink: 0;
  color: rgba(0, 212, 255, 0.95); /* 전역모달 경고 아이콘 색과 맞추기 */
}

.find-links {
  display: inline-flex;
  align-items: center;
  gap: 0.6rem;
}

.divider-dot {
  color: #606060;
  user-select: none;
}

.login-container {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 100%);
  overflow: hidden;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 0;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.2;
  animation: float 20s infinite ease-in-out;
}

.orb-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #00d4ff 0%, transparent 70%);
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.orb-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #0099cc 0%, transparent 70%);
  bottom: 100px;
  left: -50px;
  animation-delay: 5s;
}

.orb-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, #00ffff 0%, transparent 70%);
  top: 50%;
  right: 10%;
  animation-delay: 10s;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(30px);
  }
}

.login-wrapper {
  position: relative;
  z-index: 10;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3rem;
  max-width: 1000px;
  width: 100%;
}

.login-card {
  background: linear-gradient(
    135deg,
    rgba(15, 15, 30, 0.8) 0%,
    rgba(26, 26, 46, 0.8) 100%
  );
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 20px;
  padding: 3rem;
  box-shadow: 0 20px 60px rgba(0, 212, 255, 0.1);
  animation: slideInLeft 0.6s ease-out;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.login-header {
  margin-bottom: 2rem;
  text-align: center;
}

.logo-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-bottom: 1rem;
}

.logo-icon {
  font-size: 2.5rem;
}

.logo-text {
  font-size: 2rem;
  font-weight: 800;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #a0a0a0;
  font-size: 0.95rem;
  margin-top: 0.5rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-label {
  color: #e0e0e0;
  font-weight: 600;
  font-size: 0.9rem;
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
  font-size: 1.2rem;
  pointer-events: none;
}

.form-input {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 2.75rem;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  color: #e0e0e0;
  font-size: 0.95rem;
  transition: all 0.3s ease;
}

.form-input::placeholder {
  color: #606060;
}

.form-input:focus {
  outline: none;
  background: rgba(0, 212, 255, 0.05);
  border-color: rgba(0, 212, 255, 0.5);
  box-shadow: 0 0 20px rgba(0, 212, 255, 0.2);
}

.password-toggle {
  position: absolute;
  right: 1rem;
  background: none;
  border: none;
  color: #00d4ff;
  cursor: pointer;
  font-size: 1.2rem;
  transition: all 0.3s ease;
  padding: 0.25rem;
}

.password-toggle:hover {
  transform: scale(1.1);
}

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
}

.remember-me {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #a0a0a0;
  cursor: pointer;
  user-select: none;
}

/* ✅ 체크박스 색상 수정 (Register.vue와 동일) */
.remember-me input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #00d4ff; /* 여기 색상을 변경 */
}

.forgot-password {
  background: none;
  border: none;
  color: #00d4ff;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
}

.forgot-password:hover {
  color: #00ffff;
  text-decoration: underline;
}

.error-message {
  padding: 0.75rem 1rem;
  background: rgba(255, 77, 77, 0.1);
  border: 1px solid rgba(255, 77, 77, 0.3);
  border-radius: 8px;
  color: #ff6b6b;
  font-size: 0.85rem;
  text-align: center;
}

.login-btn {
  padding: 1rem;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  color: #0f0f1e;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-top: 0.5rem;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.4);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(15, 15, 30, 0.3);
  border-top-color: #0f0f1e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.divider {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: #606060;
  font-size: 0.85rem;
  margin: 1rem 0;
}

.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(0, 212, 255, 0.1);
}

.social-login {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.875rem;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 10px;
  color: #e0e0e0;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 600;
  font-size: 0.9rem;
}

.social-btn:hover {
  background: rgba(0, 212, 255, 0.1);
  border-color: rgba(0, 212, 255, 0.4);
  transform: translateY(-2px);
}

.social-btn span {
  font-size: 1.2rem;
}

.signup-section {
  text-align: center;
  margin-top: 1.5rem;
  color: #a0a0a0;
  font-size: 0.9rem;
}

.signup-link {
  background: none;
  border: none;
  color: #00d4ff;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.signup-link:hover {
  color: #00ffff;
  text-decoration: underline;
}

.info-card {
  background: linear-gradient(
    135deg,
    rgba(0, 212, 255, 0.05) 0%,
    rgba(0, 153, 204, 0.02) 100%
  );
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 20px;
  padding: 2rem;
  animation: slideInRight 0.6s ease-out;
}

@keyframes slideInRight {
  from {
    opacity: 0;
    transform: translateX(30px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.info-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
}

.info-icon {
  font-size: 2rem;
}

.info-header h3 {
  color: #e0e0e0;
  font-size: 1.25rem;
  font-weight: 700;
}

.info-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.info-list li {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  color: #a0a0a0;
  font-size: 0.95rem;
}

.check-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: rgba(0, 212, 255, 0.2);
  border-radius: 50%;
  color: #00d4ff;
  font-weight: bold;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .login-wrapper {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .login-card {
    padding: 2rem;
  }

  .logo-text {
    font-size: 1.5rem;
  }

  .info-card {
    display: none;
  }

  .gradient-orb {
    filter: blur(60px);
  }

  .orb-1 {
    width: 250px;
    height: 250px;
  }

  .orb-2 {
    width: 200px;
    height: 200px;
  }

  .orb-3 {
    width: 220px;
    height: 220px;
  }
}
</style>