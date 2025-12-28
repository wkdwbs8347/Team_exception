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

import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import GlobalModal from '@/modal/GlobalModal.vue' // 알림 모달
import api from '@/api/axios' // 스프링부트와 통신하기 위한것
import { Sparkles } from 'lucide-vue-next' // 헤더 아이콘

const router = useRouter()

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
})

/* ======================
   필드별 에러 (말풍선)
====================== */
const fieldErrors = ref({
  firstName: '',
  lastName: '',
  nickname: '',
  email: '',
  password: '',
  confirmPassword: '',
})

/* ======================
   input ref (포커스 이동용)
====================== */
const firstNameRef = ref(null)
const lastNameRef = ref(null)
const nicknameRef = ref(null)
const emailRef = ref(null)
const passwordRef = ref(null)
const confirmPasswordRef = ref(null)
const verificationCodeRef = ref(null)

/* ======================
   말풍선: 마지막 blur 필드
====================== */
const lastBlurField = ref(null)

/* ======================
   전역 모달 상태
====================== */
const modal = ref({
  open: false,
  message: '',
  focusField: null,
  type: 'info', // 'warning' | 'info'
  onConfirm: null,
})

const openModal = (message, field = null, type = 'info', onConfirm = null) => {
  modal.value.open = true
  modal.value.message = message
  modal.value.focusField = field
  modal.value.type = type
  modal.value.onConfirm = onConfirm
}

const closeModal = async () => {
  modal.value.open = false
  await nextTick()

  // ✅ 안내/성공 모달에서 후처리 동작(페이지 이동 등)
  if (modal.value.onConfirm) {
    const fn = modal.value.onConfirm
    modal.value.onConfirm = null
    fn()
    return
  }

  // ✅ 경고 모달: 해당 input으로 포커스 이동
  const focusMap = {
    firstName: firstNameRef,
    lastName: lastNameRef,
    nickname: nicknameRef,
    email: emailRef,
    password: passwordRef,
    confirmPassword: confirmPasswordRef,
    verificationCode: verificationCodeRef,
  }

  if (modal.value.focusField) {
    focusMap[modal.value.focusField]?.value?.focus()
  }
}

/* ======================
   상태 관리
====================== */
const isLoading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordStrength = ref(0)
const passwordGuide = ref('') // 안전/위험 안내 말풍선
const isNicknameChecking = ref(false)
const nicknameChecked = ref(false) // 중복체크 했는지
const nicknameAvailable = ref(false) // 사용 가능한지

// 닉네임 입력이 바뀌면 중복체크 초기화
const handleNicknameInput = () => {
  fieldErrors.value.nickname = ''
  nicknameChecked.value = false
  nicknameAvailable.value = false
}

// 닉네임 중복체크 API호출
const checkNickname = async () => {
  if (isNicknameChecking.value) return

  const nick = formData.value.nickname?.trim()

  if (!nick) {
    fieldErrors.value.nickname = '닉네임을 입력해주세요.'
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning')
  }

  // 닉네임 입력값 간단 규칙
  if (nick.length < 2 || nick.length > 20) {
    fieldErrors.value.nickname = '닉네임은 2~20자여야 합니다.'
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning')
  }
  if (nick.includes(' ')) {
    fieldErrors.value.nickname = '닉네임에는 공백을 사용할 수 없습니다.'
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning')
  }

  try {
    isNicknameChecking.value = true

    const res = await api.get('/members/check-nickname', {
      params: { nickname: nick },
    })

    nicknameChecked.value = true
    nicknameAvailable.value = !!res.data.available

    if (nicknameAvailable.value) {
      openModal('사용 가능한 닉네임입니다 ✅', null, 'info')
    } else {
      openModal('이미 사용중인 닉네임입니다 ❌', 'nickname', 'warning')
    }
  } catch (e) {
    nicknameChecked.value = false
    nicknameAvailable.value = false
    const msg = e?.response?.data?.message || '닉네임 확인 실패'
    openModal(msg, 'nickname', 'warning')
  } finally {
    isNicknameChecking.value = false
  }
}

// 비밀번호 입력중일때
const handlePasswordInput = () => {
  validatePassword()
  fieldErrors.value.password = ''

  // ✅ 입력중일때만 안내 말풍선 세팅
  const password = formData.value.password
  if (!password) {
    passwordGuide.value = ''
  } else if (passwordStrength.value <= 1) {
    passwordGuide.value = '안전하지 않은 비밀번호입니다'
  } else {
    passwordGuide.value = '안전한 비밀번호입니다'
  }
}

// 비밀번호 강도 계산
const validatePassword = () => {
  const password = formData.value.password
  let strength = 0

  if (password.length >= 8) strength++
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength++
  if (/[0-9]/.test(password)) strength++
  if (/[^a-zA-Z0-9]/.test(password)) strength++

  passwordStrength.value = strength
}

// 비밀번호 강도기준
const getPasswordStrengthLabel = () =>
  ['Weak', 'Fair', 'Good', 'Strong', 'Very Strong'][passwordStrength.value] ||
  'Weak'

const getPasswordStrengthColor = () =>
  ['#ff6b6b', '#ffa500', '#ffd700', '#90ee90', '#00d4ff'][
    passwordStrength.value
  ] || '#ff6b6b'

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}
const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

/* ======================
   이메일 인증 상태
====================== */
const isEmailSending = ref(false) // 전송중(버튼 disable)
const showVerificationInput = ref(false) // 인증번호 입력칸 노출
const verificationCodeInput = ref('') // 사용자가 입력한 인증번호
const emailVerified = ref(false) // 인증 완료 여부

// (데모용) 서버가 보낸 인증번호라고 가정
const sentVerificationCode = ref('')

const isValidEmailFormat = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)

/* ======================
   ✅ 이메일 input 변경 시 초기화
   (템플릿에서 multiline @input 쓰지 말고 함수로!)
====================== */
const handleEmailInput = () => {
  fieldErrors.value.email = ''

  // 이메일이 바뀌면 인증 상태 초기화(안전)
  emailVerified.value = false
  showVerificationInput.value = false
  verificationCodeInput.value = ''
  sentVerificationCode.value = ''
}

/* ======================
   ✅ 이메일 인증 요청
====================== */
const requestEmailVerification = async () => {
  if (emailVerified.value) return
  if (isEmailSending.value) return

  // 이메일 기본 검증
  if (!formData.value.email) {
    fieldErrors.value.email = '이메일을 입력해주세요.'
    return openModal(fieldErrors.value.email, 'email', 'warning')
  }
  if (!isValidEmailFormat(formData.value.email)) {
    fieldErrors.value.email = '이메일 형식이 올바르지 않습니다.'
    return openModal(fieldErrors.value.email, 'email', 'warning')
  }

  // 재요청이면 입력칸 값 초기화
  verificationCodeInput.value = ''

  isEmailSending.value = true

  // ✅ 여기서 실제 API 호출하면 됨
  // await api.post('/email/send', { email: formData.value.email })
  setTimeout(() => {
    isEmailSending.value = false
    showVerificationInput.value = true

    // 데모용 "서버가 보낸 코드"
    sentVerificationCode.value = '123456'

    openModal('인증번호가 전송되었습니다.', null, 'info', async () => {
      await nextTick()
      verificationCodeRef.value?.focus()
    })
  }, 900)
}

/* ======================
   ✅ 인증번호 확인
====================== */
const confirmVerificationCode = () => {
  if (!showVerificationInput.value) return

  if (!verificationCodeInput.value) {
    return openModal('인증번호를 입력해주세요.', 'verificationCode', 'warning')
  }

  const ok = verificationCodeInput.value.trim() === sentVerificationCode.value

  if (!ok) {
    return openModal(
      '인증번호가 일치하지 않습니다.',
      'verificationCode',
      'warning'
    )
  }

  // ✅ 인증 성공 상태
  emailVerified.value = true
  showVerificationInput.value = false
  verificationCodeInput.value = ''
  sentVerificationCode.value = ''

  openModal('인증이 완료되었습니다.', null, 'info')
}

/* ======================
   단일 필드 검증
   - mode: 'blur' | 'submit'
   - submit일 때만 이메일 인증여부까지 체크
====================== */
const validateField = (field, mode = 'blur') => {
  // ✅ 이전 blur에서 뜬 말풍선 제거
  if (mode === 'blur' && lastBlurField.value && lastBlurField.value !== field) {
    fieldErrors.value[lastBlurField.value] = ''
  }

  const value = formData.value[field]
  let message = ''

  switch (field) {
    case 'firstName':
      if (!value) message = '성을 입력해주세요.'
      break

    case 'lastName':
      if (!value) message = '이름을 입력해주세요.'
      break

    case 'nickname':
      if (!value) message = '닉네임을 입력해주세요.'
      else if (value.trim().length < 2 || value.trim().length > 20)
        message = '닉네임은 2~20자여야 합니다.'
      else if (value.includes(' '))
        message = '닉네임에는 공백을 사용할 수 없습니다.'
      else if (mode === 'submit') {
        if (!nicknameChecked.value) message = '닉네임 중복체크를 해주세요.'
        else if (!nicknameAvailable.value)
          message = '사용 가능한 닉네임으로 변경해주세요.'
      }
      break

    case 'email':
      if (!value) message = '이메일을 입력해주세요.'
      else if (!isValidEmailFormat(value))
        message = '이메일 형식이 올바르지 않습니다.'
      else if (mode === 'submit' && !emailVerified.value)
        message = '이메일 인증을 완료해주세요.'
      break

    case 'password':
      if (!value) message = '비밀번호를 입력해주세요.'

      // ✅ blur 시에는 안내 말풍선 숨김 (입력중에만 보이게)
      if (mode === 'blur') passwordGuide.value = ''
      break

    case 'confirmPassword':
      if (!value) message = '비밀번호 확인을 입력해주세요.'
      else if (value !== formData.value.password)
        message = '비밀번호가 일치하지 않습니다.'
      break
  }

  fieldErrors.value[field] = message

  if (mode === 'blur') {
    lastBlurField.value = field
  }

  return !message
}

/* ======================
   회원가입버튼 클릭시 검증 및 진행 
====================== */
const handleRegister = async () => {
  if (!validateField('firstName', 'submit'))
    return openModal(fieldErrors.value.firstName, 'firstName', 'warning')

  if (!validateField('lastName', 'submit'))
    return openModal(fieldErrors.value.lastName, 'lastName', 'warning')

  if (!validateField('nickname', 'submit'))
    return openModal(fieldErrors.value.nickname, 'nickname', 'warning')

  if (!validateField('email', 'submit'))
    return openModal(fieldErrors.value.email, 'email', 'warning')

  if (!validateField('password', 'submit'))
    return openModal(fieldErrors.value.password, 'password', 'warning')

  if (!validateField('confirmPassword', 'submit'))
    return openModal(
      fieldErrors.value.confirmPassword,
      'confirmPassword',
      'warning'
    )

  try {
    isLoading.value = true

    const payload = {
      firstName: formData.value.firstName,
      lastName: formData.value.lastName,
      nickname: formData.value.nickname.trim(),
      email: formData.value.email,
      password: formData.value.password,
    }

    await api.post('/members/register', payload)

    openModal('회원가입이 완료되었습니다.', null, 'info', () =>
      router.push('/')
    )
  } catch (e) {
    const msg = e?.response?.data?.message || '회원가입 실패'
    openModal(msg, null, 'warning')
  } finally {
    isLoading.value = false
  }
}

const handleLoginRedirect = () => {
  router.push('/login')
}

/* ======================
   이메일 버튼 라벨
====================== */
const getEmailButtonLabel = () => {
  if (emailVerified.value) return '인증완료'
  if (showVerificationInput.value) return '재요청'
  return '인증'
}
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
          <!-- ✅ Header (Login 페이지 스타일) -->
          <div class="register-header">
            <div class="logo-section">
              <span class="logo-icon"><Sparkles :size="28" /></span>
              <h1 class="logo-text">Web Crafter</h1>
            </div>
            <p class="subtitle">계정을 생성하고 작업실을 시작하세요!</p>
          </div>
          <!-- Name Row -->
          <div class="form-row">
            <div class="form-group">
              <label for="firstName" class="form-label">성</label>
              <div class="input-wrapper">
                <span class="input-icon">👤</span>
                <input
                  id="firstName"
                  ref="firstNameRef"
                  v-model="formData.firstName"
                  type="text"
                  placeholder="CHA"
                  class="form-input"
                  @blur="validateField('firstName', 'blur')"
                  @input="fieldErrors.firstName = ''"
                />
                <div v-if="fieldErrors.firstName" class="error-tooltip">
                  ⚠️ {{ fieldErrors.firstName }}
                </div>
              </div>
            </div>

            <div class="form-group">
              <label for="lastName" class="form-label">이름</label>
              <div class="input-wrapper">
                <span class="input-icon">👤</span>
                <input
                  id="lastName"
                  ref="lastNameRef"
                  v-model="formData.lastName"
                  type="text"
                  placeholder="EUNWOO"
                  class="form-input"
                  @blur="validateField('lastName', 'blur')"
                  @input="fieldErrors.lastName = ''"
                />
                <div v-if="fieldErrors.lastName" class="error-tooltip">
                  ⚠️ {{ fieldErrors.lastName }}
                </div>
              </div>
            </div>
          </div>

          <!-- Nickname Input + 중복체크 버튼 -->
          <div class="form-group">
            <label for="nickname" class="form-label">닉네임</label>
            <div class="input-wrapper">
              <span class="input-icon">🏷️</span>

              <input
                id="nickname"
                ref="nicknameRef"
                v-model="formData.nickname"
                type="text"
                placeholder="닉네임을 입력하세요"
                class="form-input has-right-btn"
                @blur="validateField('nickname', 'blur')"
                @input="handleNicknameInput"
                @keyup.enter="checkNickname"
              />

              <button
                type="button"
                class="email-verify-btn"
                :disabled="isNicknameChecking"
                @click="checkNickname"
              >
                <span v-if="!isNicknameChecking">중복체크</span>
                <span v-else class="email-btn-loading">
                  <span class="mini-spinner"></span>
                  확인중
                </span>
              </button>

              <div v-if="fieldErrors.nickname" class="error-tooltip">
                ⚠️ {{ fieldErrors.nickname }}
              </div>
            </div>
          </div>

          <!-- Email Input + 인증 버튼 -->
          <div class="form-group">
            <label for="email" class="form-label">이메일 주소</label>
            <div class="input-wrapper">
              <span class="input-icon">📧</span>
              <input
                id="email"
                ref="emailRef"
                v-model="formData.email"
                type="email"
                placeholder="you@example.com"
                class="form-input has-right-btn"
                @blur="validateField('email', 'blur')"
                @input="handleEmailInput"
              />

              <!-- ✅ 이메일 인증/재요청/인증완료 버튼 -->
              <button
                type="button"
                class="email-verify-btn"
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
                ⚠️ {{ fieldErrors.email }}
              </div>
            </div>

            <!-- ✅ 인증번호 입력칸(전송 후 & 인증 전) -->
            <div
              v-if="showVerificationInput && !emailVerified"
              class="verify-row"
            >
              <div class="input-wrapper">
                <span class="input-icon">🔑</span>
                <input
                  ref="verificationCodeRef"
                  v-model="verificationCodeInput"
                  type="text"
                  inputmode="numeric"
                  placeholder="인증번호 6자리"
                  class="form-input has-right-btn"
                  @keyup.enter="confirmVerificationCode"
                />
                <button
                  type="button"
                  class="email-verify-btn"
                  @click="confirmVerificationCode"
                >
                  확인
                </button>
              </div>
            </div>
          </div>

          <!-- Password Input -->
          <div class="form-group">
            <label for="password" class="form-label">비밀번호</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input
                id="password"
                ref="passwordRef"
                v-model="formData.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Create a strong password"
                class="form-input"
                @blur="validateField('password', 'blur')"
                @input="handlePasswordInput"
              />
              <button
                type="button"
                class="password-toggle"
                @click="togglePasswordVisibility"
                :title="showPassword ? 'Hide password' : 'Show password'"
              >
                {{ showPassword ? '👁️' : '👁️‍🗨️' }}
              </button>
              <div v-if="fieldErrors.password" class="error-tooltip">
                ⚠️ {{ fieldErrors.password }}
              </div>
              <!-- ✅ 안전/위험 안내 말풍선 (에러 없을 때만) -->
              <div v-else-if="passwordGuide" class="error-tooltip">
                {{ passwordGuide }}
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
              <span class="input-icon">🔒</span>
              <input
                id="confirmPassword"
                ref="confirmPasswordRef"
                v-model="formData.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="Confirm your password"
                class="form-input"
                @blur="validateField('confirmPassword', 'blur')"
                @input="fieldErrors.confirmPassword = ''"
                required
              />
              <button
                type="button"
                class="password-toggle"
                @click="toggleConfirmPasswordVisibility"
                :title="showConfirmPassword ? 'Hide password' : 'Show password'"
              >
                {{ showConfirmPassword ? '👁️' : '👁️‍🗨️' }}
              </button>
              <div v-if="fieldErrors.confirmPassword" class="error-tooltip">
                ⚠️ {{ fieldErrors.confirmPassword }}
              </div>
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
              Sign in
            </button>
          </p>
        </div>
      </div>

      <!-- Info Card -->
      <div class="info-card">
        <div class="info-header">
          <span class="info-icon">🚀</span>
          <h3>계정을 생성하세요!</h3>
        </div>
        <ul class="info-list">
          <li>
            <span class="check-icon">✓</span>
            <span>서비스 시작을 위한 계정 생성</span>
          </li>
          <li>
            <span class="check-icon">✓</span>
            <span>결제 수단 등록 필요 없음</span>
          </li>
          <li>
            <span class="check-icon">✓</span>
            <span>모든 도구를 즉시 사용하세요!</span>
          </li>
          <li>
            <span class="check-icon">✓</span>
            <span>끊김 없는 고객 서포트</span>
          </li>
        </ul>
      </div>
    </div>
  </div>

  <!-- ✅ 전역 모달 -->
  <GlobalModal
    :open="modal.open"
    :message="modal.message"
    :type="modal.type"
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

.email-verify-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
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

/* 인증번호 입력칸 위 여백 */
.verify-row {
  margin-top: 0.75rem;
}
</style>
