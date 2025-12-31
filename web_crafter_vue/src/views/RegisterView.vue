<script setup>
/*
  ✅ 전체 동작 흐름

  - onBlur → 현재 blur된 input만 말풍선 표시(이전 blur 말풍선 제거)
  - 회원가입 클릭 →
      1) 위에서부터 순서 검증
      2) 가장 첫 번째 문제만 전역 모달 표시
      3) 확인 누르면 해당 input으로 포커스 이동
  - 이메일 인증 →
      1) 이메일 옆 인증 버튼 클릭(로딩/중복방지)
      2) 전송 완료 모달
      3) 인증번호 입력칸 표시 + 확인 버튼
      4) 일치/불일치 모달
      5) 인증 성공 시 인증완료 버튼으로 변경 + 입력칸 제거
*/

import { ref, nextTick, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import GlobalModal from '@/modal/GlobalModal.vue'; // 알림 모달
import api from '@/api/axios'; // 스프링부트 통신
import {
  TriangleAlert,
  CheckCircle,
  XCircle,
  User,
  Tag,
  Mail,
  KeyRound,
  Lock,
  Eye,
  EyeOff,
  Blocks, // 로고(블록 빌더)
  Wand2, // "만들기/생성" 느낌 
  Braces, // 코드/웹 생성 느낌 (설명 카드 리스트용)
  LayoutGrid, // 블록/레이아웃 조합 느낌 (설명 카드 리스트용)
  Monitor, // 미리보기/실행 느낌 (설명 카드 리스트용)
  Play, // 실행 느낌 (설명 카드 리스트용)
  Check,
} from 'lucide-vue-next'; // 아이콘

const router = useRouter();

/* ======================
   입력 데이터
====================== */
const formData = ref({
  firstName: '',
  lastName: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
});

/* =====================
공백 입력 불가능하게 처리
========================*/
// 모든 공백(스페이스/탭/줄바꿈) 제거
const removeAllSpaces = (v) => (v ?? '').replace(/\s+/g, '');

// v-model 값 자체를 "공백 없는 값"으로 강제
const enforceNoSpace = (field) => {
  formData.value[field] = removeAllSpaces(formData.value[field]);
};

/* ======================
   필드별 에러 (말풍선)
====================== */
const fieldErrors = ref({
  firstName: '',
  lastName: '',
  nickname: '',
  email: '',
  password: '',
});

// 말풍선 초기화
const clearAllTooltips = () => {
  Object.keys(fieldErrors.value).forEach((k) => {
    fieldErrors.value[k] = '';
  });
  lastBlurField.value = null;
};

/* ======================
   input ref (포커스 이동용)
====================== */
const firstNameRef = ref(null);
const lastNameRef = ref(null);
const nicknameRef = ref(null);
const emailRef = ref(null);
const passwordRef = ref(null);
const confirmPasswordRef = ref(null);
const verificationCodeRef = ref(null);

/* ======================
   말풍선: 마지막 blur 필드
====================== */
const lastBlurField = ref(null);

// 엔터키로 모달 끌 수 있게
const handleKeydown = (e) => {
  // 모달 열려 있을 때만
  if (!modal.value.open) return;

  // Enter 키
  if (e.key === 'Enter') {
    e.preventDefault();
    e.stopPropagation();
    closeModal();
  }
};

onMounted(() => {
  window.addEventListener('keydown', handleKeydown, true);
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown, true);
});

/* ======================
   전역 모달 상태
====================== */
const modal = ref({
  open: false,
  message: '',
  focusField: null,
  tooltipMessage: null,
  type: 'info', // info | warning | success | error
  icon: null, // lucide icon name
  onConfirm: null,
});

const openModal = (
  message,
  field = null,
  type = 'info',
  onConfirm = null,
  icon = null
) => {
  // 모달을 띄우기 전에 기존 말풍선 정리
  clearAllTooltips();

  modal.value.open = true;
  modal.value.message = message;
  modal.value.focusField = field;
  modal.value.tooltipMessage = field ? message : null;
  modal.value.type = type;
  modal.value.onConfirm = onConfirm;
  modal.value.icon = icon;
};

const closeModal = async () => {
  modal.value.open = false;
  await nextTick();

  // 안내/성공 모달에서 후처리 동작(페이지 이동 등)
  if (modal.value.onConfirm) {
    const fn = modal.value.onConfirm;
    modal.value.onConfirm = null;
    fn();
    return;
  }

  // 경고 모달: 해당 input으로 포커스 이동
  const focusMap = {
    firstName: firstNameRef,
    lastName: lastNameRef,
    nickname: nicknameRef,
    email: emailRef,
    password: passwordRef,
    confirmPassword: confirmPasswordRef,
    verificationCode: verificationCodeRef,
  };

  const field = modal.value.focusField;
  const tooltip = modal.value.tooltipMessage;

  if (field) {
    // ✅ 1) 포커스 먼저
    focusMap[field]?.value?.focus();

    // ✅ 2) 포커스가 잡힌 다음 말풍선 띄우기(타이밍 안정화)
    await nextTick();
    if (tooltip) {
      fieldErrors.value[field] = tooltip;
      lastBlurField.value = field;
    }
  }
};

/* ======================
   상태 관리
====================== */
const isLoading = ref(false);
// ✅ 회원가입 요청 처리중 여부
// - true면 회원가입 버튼 비활성화 + "계정 생성중..." 로딩 UI 표시
// - handleRegister() 시작에서 true, finally에서 false로 복구

const errorMessage = ref('');
// ✅ 폼 하단에 띄울 커스텀 말풍선 “전체 에러 메시지”용 문자열
// - v-if="errorMessage"로 화면에 보여줄 때 사용
// - 현재 코드에선 거의 안 쓰고(대부분 openModal 사용), 필요하면 catch에서 설정 가능

const showPassword = ref(false);
// ✅ 비밀번호 input 표시/숨김 토글 상태
// - true면 type="text", false면 type="password"
// - Eye/EyeOff 아이콘 토글에 사용

const showConfirmPassword = ref(false);
// ✅ 비밀번호 확인 input 표시/숨김 토글 상태
// - 동작은 showPassword와 동일하지만 confirmPassword용

const passwordStrength = ref(0);
// ✅ 비밀번호 강도 점수(0~4)
// - validatePassword()에서 계산
// - 강도 바(%)와 라벨(Weak~Very Strong) 표시 기준 값

const isNicknameChecking = ref(false);
// ✅ 닉네임 중복체크 API 요청중인지 여부
// - true면 버튼 disabled + 로딩 스피너/“확인중” 표시
// - checkNickname() try에서 true, finally에서 false

const nicknameChecked = ref(false);
// ✅ 사용자가 “중복체크를 한 적이 있는가” 상태
// - 닉네임 입력이 바뀌면 false로 초기화(handleNicknameInput)
// - 중복체크 API 성공 응답 오면 true로 변경

const nicknameAvailable = ref(false);
// ✅ 현재 닉네임이 “사용 가능한 상태인지” 여부
// - 중복체크 결과로 결정(res.data.available)
// - true면 버튼 라벨 "사용가능" 표시 + 버튼 disabled + 초록색 처리
// - 닉네임을 다시 수정하면 false로 초기화(handleNicknameInput)

// 닉네임 입력이 바뀌면 중복체크 초기화
const handleNicknameInput = () => {
  fieldErrors.value.nickname = '';
  nicknameChecked.value = false;
  nicknameAvailable.value = false;
};

// 닉네임 중복체크 API호출
const checkNickname = async () => {
  if (isNicknameChecking.value) return;
  if (nicknameAvailable.value) return; // 이미 사용가능이면 재요청 막기

  const nick = formData.value.nickname?.trim();

  if (!nick) {
    fieldErrors.value.nickname = '닉네임을 입력해주세요.';
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning');
  }

  // 닉네임 입력값 간단 규칙
  if (nick.length < 2 || nick.length > 20) {
    fieldErrors.value.nickname = '닉네임은 2~20자여야 합니다.';
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning');
  }
  if (nick.includes(' ')) {
    fieldErrors.value.nickname = '닉네임에는 공백을 사용할 수 없습니다.';
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning');
  }

  try {
    isNicknameChecking.value = true;

    const res = await api.get('/member/nicknameCheck', {
      params: { nickname: nick },
    });

    nicknameChecked.value = true;
    nicknameAvailable.value = !!res.data.available;

    if (nicknameAvailable.value) {
      // 모달만 (말풍선 없음)
      openModal(
        '사용 가능한 닉네임입니다',
        null,
        'success',
        async () => {
          await nextTick();
          emailRef.value?.focus();
        },
        CheckCircle
      );
    } else {
      // 모달만 (말풍선 없음)
      openModal(
        '이미 사용중인 닉네임입니다',
        null,
        'error',
        async () => {
          await nextTick();
          nicknameRef.value?.focus();
        },
        XCircle
      );
    }
  } catch (e) {
    nicknameChecked.value = false;
    nicknameAvailable.value = false;
    const msg = e?.response?.data?.message || '닉네임 확인 실패';
    openModal(msg, 'nickname', 'warning');
  } finally {
    isNicknameChecking.value = false;
  }
};

// 모달 열린상태로 엔터 누를때 닉네임 중복체크 중복요청 방지
const onEnterNickname = () => {
  if (modal.value.open) return;
  if (nicknameAvailable.value) return; // 사용가능일때 막기
  checkNickname();
};

// 상황에 따른 닉네임 버튼 라벨
const getNicknameButtonLabel = () => {
  if (nicknameAvailable.value) return '사용가능';
  return '중복체크';
};

// 비밀번호 입력중일때
const handlePasswordInput = () => {
  validatePassword();
  fieldErrors.value.password = '';
};

// 비밀번호 강도 계산
const validatePassword = () => {
  const password = formData.value.password || '';

  // 8자 미만이면 어떤 조합이든 Good(2) 이상 못 찍게 강제
  if (password.length < 8) {
    let strength = 0;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++;
    else if (/[0-9]/.test(password)) strength++;
    else if (/[^a-zA-Z0-9]/.test(password)) strength++;

    passwordStrength.value = Math.min(strength, 1); // 최대 1까지만
    return;
  }

  // 8자 이상일 때만 정상 강도 계산
  let strength = 0;
  if (password.length >= 8) strength++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++;
  if (/[0-9]/.test(password)) strength++;
  if (/[^a-zA-Z0-9]/.test(password)) strength++;

  passwordStrength.value = strength;
};

// 비밀번호 강도기준
const getPasswordStrengthLabel = () =>
  ['Weak', 'Fair', 'Good', 'Strong', 'Very Strong'][passwordStrength.value] ||
  'Weak';

const getPasswordStrengthColor = () =>
  ['#ff6b6b', '#ffa500', '#ffd700', '#90ee90', '#00d4ff'][
    passwordStrength.value
  ] || '#ff6b6b';

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value;
};
const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value;
};

/* ======================
   이메일 인증 상태
====================== */
const isEmailSending = ref(false); // 전송중(버튼 disable)
const showVerificationInput = ref(false); // 인증번호 입력칸 노출
const verificationCodeInput = ref(''); // 사용자가 입력한 인증번호
const emailVerified = ref(false); // 인증 완료 여부
const isValidEmailFormat = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email); // 이메일 형식
// 인증번호 입력 타이머 (5분)
const expiresInSec = ref(0); // 남은 초
let timerId = null;

const formatTime = (sec) => {
  const m = String(Math.floor(sec / 60)).padStart(2, '0');
  const s = String(sec % 60).padStart(2, '0');
  return `${m}:${s}`;
};

const countdownText = computed(() => formatTime(expiresInSec.value));
const isExpired = computed(() => expiresInSec.value <= 0);

const startCountdown = (seconds = 300) => {
  // 기존 타이머 정리
  if (timerId) clearInterval(timerId);

  expiresInSec.value = seconds;

  timerId = setInterval(() => {
    expiresInSec.value -= 1;

    if (expiresInSec.value <= 0) {
      expiresInSec.value = 0;
      clearInterval(timerId);
      timerId = null;

      // 만료 처리 UX
      openModal(
        '인증 시간이 만료되었습니다. 재요청 해주세요.',
        null,
        'warning'
      );
    }
  }, 1000);
};

const stopCountdown = () => {
  if (timerId) clearInterval(timerId);
  timerId = null;
  expiresInSec.value = 0;
};

// 페이지 떠날 때 타이머 정리
onBeforeUnmount(() => {
  stopCountdown();
});

/* ======================
   이메일 input 변경 시 초기화
   (템플릿에서 multiline @input 쓰지 말고 함수로!)
====================== */
const handleEmailInput = () => {
  fieldErrors.value.email = '';

  // 이메일이 바뀌면 인증 상태 초기화(안전)
  emailVerified.value = false;
  showVerificationInput.value = false;
  verificationCodeInput.value = '';
  stopCountdown(); // 타이머 초기화
};

/* ======================
   이메일 인증 요청
====================== */
const requestEmailVerification = async () => {
  if (emailVerified.value) return;
  if (isEmailSending.value) return;

  // 이메일 기본 검증
  if (!formData.value.email) {
    fieldErrors.value.email = '이메일을 입력해주세요.';
    return openModal(fieldErrors.value.email, 'email', 'warning');
  }
  if (!isValidEmailFormat(formData.value.email)) {
    fieldErrors.value.email = '이메일 형식이 올바르지 않습니다.';
    return openModal(fieldErrors.value.email, 'email', 'warning');
  }

  // 재요청이면 입력칸 값 초기화
  verificationCodeInput.value = '';

  isEmailSending.value = true;

  // 이메일 전송
  try {
    await api.post('/member/emailSend', { email: formData.value.email });

    showVerificationInput.value = true;
    startCountdown(300); // 타이머 5분 시작

    openModal('인증번호가 전송되었습니다.', null, 'info', async () => {
      await nextTick();
      verificationCodeRef.value?.focus();
    });
  } catch (e) {
    const msg = e?.response?.data?.message || '인증번호 전송 실패';
    openModal(msg, 'email', 'warning');
  } finally {
    isEmailSending.value = false;
  }
};

// 엔터로 이메일 인증 중복요청 방지
const onEnterEmail = () => {
  // 모달 떠 있으면 → 닫기만 (전역 keydown에서 처리됨)
  if (modal.value.open) return;

  // 이미 인증 완료면 아무 것도 안 함
  if (emailVerified.value) return;

  requestEmailVerification();
};

/* ======================
   인증번호 확인
====================== */
const confirmVerificationCode = async () => {
  if (!showVerificationInput.value) return;

  if (!verificationCodeInput.value) {
    return openModal('인증번호를 입력해주세요.', 'verificationCode', 'warning');
  }

  try {
    await api.post('/member/emailVerify', {
      email: formData.value.email,
      code: verificationCodeInput.value.trim(),
    });

    // 인증 성공 상태
    emailVerified.value = true;
    showVerificationInput.value = false;
    verificationCodeInput.value = '';
    stopCountdown(); // 인증 성공 시 타이머 종료

    openModal('인증이 완료되었습니다.', null, 'success', async () => {
      await nextTick();
      passwordRef.value?.focus();
    });
  } catch (e) {
    const msg = e?.response?.data?.message || '인증번호 확인 실패';
    openModal(msg, 'verificationCode', 'warning');
  }
};

// 엔터로 인증번호 입력 재요청 방지
const onEnterVerificationCode = () => {
  if (modal.value.open) return;
  confirmVerificationCode();
};

/* ======================
   단일 필드 검증
   - mode: 'blur' | 'submit'
   - submit일 때만 이메일 인증여부까지 체크
====================== */
const validateField = (field, mode = 'blur') => {
  // 모달이 열려있을 때 blur로 인한 말풍선 생성 방지
  if (mode === 'blur' && modal.value.open) return true;
  // 이전 blur에서 뜬 말풍선 제거
  if (mode === 'blur' && lastBlurField.value && lastBlurField.value !== field) {
    fieldErrors.value[lastBlurField.value] = '';
  }

  const value = formData.value[field];
  let message = '';

  switch (field) {
    case 'firstName':
      if (!value) message = '성을 입력해주세요.';
      break;

    case 'lastName':
      if (!value) message = '이름을 입력해주세요.';
      break;

    case 'nickname':
      if (!value) message = '닉네임을 입력해주세요.';
      else if (value.trim().length < 2 || value.trim().length > 20)
        message = '닉네임은 2~20자여야 합니다.';
      else if (value.includes(' '))
        message = '닉네임에는 공백을 사용할 수 없습니다.';
      else if (mode === 'submit') {
        if (!nicknameChecked.value) message = '닉네임 중복체크를 해주세요.';
        else if (!nicknameAvailable.value)
          message = '사용 가능한 닉네임으로 변경해주세요.';
      }
      break;

    case 'email':
      if (!value) message = '이메일을 입력해주세요.';
      else if (!isValidEmailFormat(value))
        message = '이메일 형식이 올바르지 않습니다.';
      else if (mode === 'submit' && !emailVerified.value)
        message = '이메일 인증을 완료해주세요.';
      break;

    case 'password':
      if (!value) message = '비밀번호를 입력해주세요.';
      break;
  }

  fieldErrors.value[field] = message;

  if (mode === 'blur') {
    lastBlurField.value = field;
  }

  return !message;
};

/* ======================
   회원가입버튼 클릭시 검증 및 진행
====================== */
const handleRegister = async () => {
  // 모달 떠있으면 Enter로 재submit 방지
  if (modal.value.open) return;

  if (!validateField('firstName', 'submit'))
    return openModal(fieldErrors.value.firstName, 'firstName', 'warning');

  if (!validateField('lastName', 'submit'))
    return openModal(fieldErrors.value.lastName, 'lastName', 'warning');

  if (!validateField('nickname', 'submit'))
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning');

  if (!validateField('email', 'submit'))
    return openModal(fieldErrors.value.email, 'email', 'warning');

  if (!validateField('password', 'submit'))
    return openModal(fieldErrors.value.password, 'password', 'warning');

  if (formData.value.confirmPassword !== formData.value.password) {
    return openModal(
      '비밀번호가 일치하지 않습니다.',
      null,
      'warning',
      async () => {
        formData.value.password = '';
        formData.value.confirmPassword = '';
        passwordStrength.value = 0; // 강도 초기화도 같이

        await nextTick();
        passwordRef.value?.focus();
      }
    );
  }

  // password 필드 기본 검증 통과 후, 강도 Good(2) 미만이면 막기
  if (passwordStrength.value < 2) {
    formData.value.password = '';
    formData.value.confirmPassword = '';
    passwordStrength.value = 0;

    // confirmPassword 말풍선 미리 제거
    fieldErrors.value.confirmPassword = '';

    return openModal(
      '비밀번호는 8자 이상, 영문·숫자·특수문자 조합으로 입력해주세요.',
      'password',
      'warning'
    );
  }

  try {
    isLoading.value = true;

    const payload = {
      firstName: formData.value.firstName.trim(),
      lastName: formData.value.lastName.trim(),
      nickname: formData.value.nickname.trim(),
      email: formData.value.email.trim(),
      password: formData.value.password.trim(),
    };

    await api.post('/member/register', payload);

    openModal('회원가입이 완료되었습니다.', null, 'success', () =>
      router.push('/')
    );
  } catch (e) {
    const msg = e?.response?.data?.message || '회원가입 실패';
    openModal(msg, null, 'warning');
  } finally {
    isLoading.value = false;
  }
};

const handleLoginRedirect = () => {
  router.push('/login');
};

/* ======================
   이메일 버튼 라벨
====================== */
const getEmailButtonLabel = () => {
  if (emailVerified.value) return '인증완료';
  if (showVerificationInput.value) return '재요청';
  return '인증';
};
</script>

<template>
  <div class="register-container">
    <div class="register-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="register-wrapper">
      <div class="register-card">
        <!-- Form -->
        <form
          class="register-form"
          @submit.prevent="handleRegister"
          novalidate
          autocomplete="off"
        >
          <!-- Header -->
          <div class="register-header">
            <div class="logo-section">
              <span class="logo-icon"><Blocks :size="28" /></span>
              <h1 class="logo-text">Web Crafter</h1>
            </div>
            <p class="subtitle">계정을 생성하고 작업실을 시작하세요!</p>
          </div>
          <!-- Name Row -->
          <div class="form-row">
            <div class="form-group">
              <label for="firstName" class="form-label">성</label>
              <div class="input-wrapper">
                <span class="input-icon"><User :size="18" /></span>
                <input
                  id="firstName"
                  ref="firstNameRef"
                  v-model="formData.firstName"
                  type="text"
                  placeholder="CHA"
                  class="form-input"
                  @blur="validateField('firstName', 'blur')"
                  @input="
                    enforceNoSpace('firstName');
                    fieldErrors.firstName = '';
                  "
                />
                <div v-if="fieldErrors.firstName" class="error-tooltip">
                  <TriangleAlert class="tooltip-icon" :size="14" />
                  <span>{{ fieldErrors.firstName }}</span>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label for="lastName" class="form-label">이름</label>
              <div class="input-wrapper">
                <span class="input-icon"><User :size="18" /></span>
                <input
                  id="lastName"
                  ref="lastNameRef"
                  v-model="formData.lastName"
                  type="text"
                  placeholder="EUNWOO"
                  class="form-input"
                  @blur="validateField('lastName', 'blur')"
                  @input="
                    enforceNoSpace('lastName');
                    fieldErrors.lastName = '';
                  "
                />
                <div v-if="fieldErrors.lastName" class="error-tooltip">
                  <TriangleAlert class="tooltip-icon" :size="14" />
                  <span>{{ fieldErrors.lastName }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Nickname Input + 중복체크 버튼 -->
          <div class="form-group">
            <label for="nickname" class="form-label">닉네임</label>
            <div class="input-wrapper">
              <span class="input-icon"><Tag :size="18" /></span>

              <input
                id="nickname"
                ref="nicknameRef"
                v-model="formData.nickname"
                type="text"
                placeholder="닉네임을 입력하세요"
                class="form-input has-right-btn"
                @blur="validateField('nickname', 'blur')"
                @input="
                  enforceNoSpace('nickname');
                  handleNicknameInput();
                "
                @keydown.enter.prevent="onEnterNickname"
              />

              <button
                type="button"
                class="email-verify-btn"
                :class="{ 'is-available': nicknameAvailable }"
                :disabled="isNicknameChecking || nicknameAvailable"
                @click="checkNickname"
              >
                <span v-if="!isNicknameChecking">{{
                  getNicknameButtonLabel()
                }}</span>
                <span v-else class="email-btn-loading">
                  <span class="mini-spinner"></span>
                  확인중
                </span>
              </button>

              <div v-if="fieldErrors.nickname" class="error-tooltip">
                <TriangleAlert class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.nickname }}</span>
              </div>
            </div>
          </div>

          <!-- Email Input + 인증 버튼 -->
          <div class="form-group">
            <label for="email" class="form-label">이메일 주소</label>
            <div class="input-wrapper">
              <span class="input-icon"><Mail :size="18" /></span>
              <input
                id="email"
                ref="emailRef"
                v-model="formData.email"
                type="text"
                placeholder="you@example.com"
                class="form-input has-right-btn"
                @blur="validateField('email', 'blur')"
                @input="
                  enforceNoSpace('email');
                  handleEmailInput();
                "
                @keydown.enter.prevent="onEnterEmail"
              />

              <!-- 이메일 인증/재요청/인증완료 버튼 -->
              <button
                type="button"
                class="email-verify-btn"
                :class="{ 'is-verified': emailVerified }"
                :disabled="isEmailSending || emailVerified"
                @click="requestEmailVerification"
                :title="emailVerified ? '이미 인증 완료' : ''"
              >
                <span v-if="!isEmailSending">{{ getEmailButtonLabel() }}</span>
                <span v-else class="email-btn-loading">
                  <span class="mini-spinner"></span>
                  전송중
                </span>
              </button>

              <div v-if="fieldErrors.email" class="error-tooltip">
                <TriangleAlert class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.email }}</span>
              </div>
            </div>

            <!-- 인증번호 입력칸(전송 후 & 인증 전) -->
            <div
              v-if="showVerificationInput && !emailVerified"
              class="verify-row"
            >
              <div class="input-wrapper verify-wrapper">
                <span class="input-icon"><KeyRound :size="18" /></span>

                <input
                  ref="verificationCodeRef"
                  v-model="verificationCodeInput"
                  type="text"
                  inputmode="numeric"
                  placeholder="인증번호 6자리"
                  class="form-input has-right-btn"
                  :disabled="isExpired"
                  maxlength="6"
                  @input="
                    verificationCodeInput = removeAllSpaces(
                      verificationCodeInput
                    )
                  "
                  @keydown.enter.prevent="onEnterVerificationCode"
                />

                <button
                  type="button"
                  class="email-verify-btn"
                  @click="confirmVerificationCode"
                  :disabled="isExpired"
                >
                  확인
                </button>

                <!-- 타이머: 입력칸 옆(버튼 왼쪽 아래) -->
                <div class="verify-timer">
                  <span v-if="!isExpired">{{ countdownText }}</span>
                  <span v-else class="expired">만료</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Password Input -->
          <div class="form-group">
            <label for="password" class="form-label">비밀번호</label>
            <div class="input-wrapper">
              <span class="input-icon"><Lock :size="18" /></span>
              <input
                id="password"
                ref="passwordRef"
                v-model="formData.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="8자 이상, 영문·숫자·특수문자 조합"
                class="form-input"
                @blur="validateField('password', 'blur')"
                @input="
                  enforceNoSpace('password');
                  handlePasswordInput();
                "
              />
              <button
                type="button"
                class="password-toggle"
                @click="togglePasswordVisibility"
                :title="showPassword ? 'Hide password' : 'Show password'"
              >
                <Eye v-if="showPassword" :size="18" />
                <EyeOff v-else :size="18" />
              </button>
              <div v-if="fieldErrors.password" class="error-tooltip">
                <TriangleAlert class="tooltip-icon" :size="14" />
                <span>{{ fieldErrors.password }}</span>
              </div>
            </div>

            <div class="password-strength">
              <div class="strength-bar">
                <div
                  class="strength-fill"
                  :style="{
                    width: (passwordStrength / 4) * 100 + '%',
                    backgroundColor: getPasswordStrengthColor(),
                  }"
                ></div>
              </div>
              <span
                class="strength-label"
                :style="{ color: getPasswordStrengthColor() }"
              >
                {{ getPasswordStrengthLabel() }}
              </span>
            </div>
          </div>

          <!-- Confirm Password Input -->
          <div class="form-group">
            <label for="confirmPassword" class="form-label"
              >비밀번호 확인</label
            >
            <div class="input-wrapper">
              <span class="input-icon"><Lock :size="18" /></span>
              <input
                id="confirmPassword"
                ref="confirmPasswordRef"
                v-model="formData.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="비밀번호 확인"
                class="form-input"
                @input="enforceNoSpace('confirmPassword')"
                required
              />
              <button
                type="button"
                class="password-toggle"
                @click="toggleConfirmPasswordVisibility"
                :title="showConfirmPassword ? 'Hide password' : 'Show password'"
              >
                <Eye v-if="showConfirmPassword" :size="18" />
                <EyeOff v-else :size="18" />
              </button>
            </div>
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <!-- Register Button -->
          <button type="submit" class="register-btn" :disabled="isLoading">
            <span v-if="!isLoading">회원가입</span>
            <span v-else class="loading-spinner">
              <span class="spinner"></span>
              계정 생성중...
            </span>
          </button>
        </form>

        <!-- Login Link -->
        <div class="login-section">
          <p>
            이미 계정이있습니까?
            <button
              type="button"
              class="login-link"
              @click="handleLoginRedirect"
            >
              로그인
            </button>
          </p>
        </div>
      </div>

      <!-- Info Card -->
      <div class="info-card">
        <div class="info-header">
          <span class="info-icon"><Blocks :size="22" /></span>
          <h3>블록으로 웹을 만들어보세요!</h3>
        </div>
        <ul class="info-list">
          <li>
            <span class="check-icon"><Wand2 :size="16" /></span>
            <span>서비스 시작을 위한 계정 생성</span>
          </li>
          <li>
            <span class="check-icon"><LayoutGrid :size="16" /></span>
            <span>코드 블록을 조합해 웹사이트를 제작</span>
          </li>
          <li>
            <span class="check-icon"><Braces :size="16" /></span>
            <span>코딩 지식 없이도 바로 시작 가능</span>
          </li>
          <li>
            <span class="check-icon"><Monitor :size="16" /></span>
            <span>실시간 미리보기로 결과 확인</span>
          </li>
          <li>
            <span class="check-icon"><Play :size="16" /></span>
            <span>내가 만든 웹사이트를 바로 실행</span>
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
    :icon="modal.icon"
    @confirm="closeModal"
  />
</template>

<style scoped>
.register-container {
  position: relative;
  min-height: 100vh;
  margin-top: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 100%);
  overflow: hidden;
}

.register-background {
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

.register-wrapper {
  position: relative;
  z-index: 10;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 2rem;
  max-width: 1000px;
  width: 100%;
}

.register-card {
  background: linear-gradient(
    135deg,
    rgba(15, 15, 30, 0.8) 0%,
    rgba(26, 26, 46, 0.8) 100%
  );
  backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 20px;
  padding: 2.2rem;
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

.register-header {
  margin-bottom: 1.2rem;
  text-align: center;
}

.logo-section {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
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

.register-form {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
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

.password-strength {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-top: 0.5rem;
}

.strength-bar {
  flex: 1;
  height: 4px;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.strength-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  min-width: 60px;
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

.register-btn {
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

.register-btn:hover:not(:disabled) {
  transform: translateY(-3px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.4);
}

.register-btn:disabled {
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

.social-register {
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

.login-section {
  text-align: center;
  margin-top: 1.5rem;
  color: #a0a0a0;
  font-size: 0.9rem;
}

.login-link {
  background: none;
  border: none;
  color: #00d4ff;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.login-link:hover {
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
  .register-wrapper {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .register-card {
    padding: 2rem;
  }

  .form-row {
    grid-template-columns: 1fr;
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

/* 이메일/인증번호 input 오른쪽 버튼 공간 확보 */
.form-input.has-right-btn {
  padding-right: 6.2rem; /* 버튼 폭만큼 여유 */
}

/* 이메일 인증 버튼(인증/재요청/확인/인증완료 공용) */
.email-verify-btn {
  position: absolute;
  right: 0.6rem;
  height: calc(100% - 10px);
  top: 5px;
  padding: 0 0.9rem;
  border-radius: 10px;
  border: 1px solid rgba(0, 212, 255, 0.25);
  background: rgba(0, 212, 255, 0.08);
  color: #e0e0e0;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.25s ease;
}

.email-verify-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  background: rgba(0, 212, 255, 0.14);
  border-color: rgba(0, 212, 255, 0.45);
}

/* 닉네임 중복체크, 이메일 인증버튼 검증완료시 살짝 투명하게 */
.email-verify-btn:disabled {
  opacity: 0.75;
  cursor: not-allowed;
}

/* 닉네임 버튼 사용가능일때 색 변경 */
.email-verify-btn.is-available:disabled {
  color: rgba(51, 255, 153, 1);
}

/* 이메일 인증버튼 인증완료일때 색 변경 */
.email-verify-btn.is-verified:disabled {
  color: rgba(51, 255, 153, 1);
}

/* 전송중 표시 */
.email-btn-loading {
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
}

/* 작은 스피너 */
.mini-spinner {
  width: 12px;
  height: 12px;
  border: 2px solid rgba(255, 255, 255, 0.25);
  border-top-color: rgba(0, 212, 255, 0.9);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

/* 인증번호 입력 wrapper */
.verify-wrapper {
  position: relative;
}

/* 타이머를 입력칸 오른쪽(확인 버튼 왼쪽/아래)에 띄우기 */
.verify-timer {
  position: absolute;
  right: 6.7rem; /* 확인 버튼 영역만큼 왼쪽으로 */
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.78rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: rgba(224, 224, 224, 0.85);
  pointer-events: none;
  opacity: 0.95;
}

.verify-timer .expired {
  color: #ff6b6b;
}

/* 인증번호 입력칸 위 여백 */
.verify-row {
  margin-top: 0.75rem;
}
</style>
