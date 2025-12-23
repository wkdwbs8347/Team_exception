<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const formData = ref({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeTerms: false,
})

// input 입력 검증 에러 상태
const fieldErrors = ref({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
})

// 각 input 필드 단위 검증 함수
const validateField = (field) => {
  const value = formData.value[field]

  if (!value) {
    fieldErrors.value[field] = '이 입력란을 작성하세요.'
    return false
  }

  fieldErrors.value[field] = ''
  return true
}

const isLoading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const passwordStrength = ref(0)

const validatePassword = () => {
  const password = formData.value.password
  let strength = 0

  if (password.length >= 8) strength++
  if (password.match(/[a-z]/) && password.match(/[A-Z]/)) strength++
  if (password.match(/[0-9]/)) strength++
  if (password.match(/[^a-zA-Z0-9]/)) strength++

  passwordStrength.value = strength
}

const getPasswordStrengthLabel = () => {
  const labels = ['Weak', 'Fair', 'Good', 'Strong', 'Very Strong']
  return labels[passwordStrength.value] || 'Weak'
}

const getPasswordStrengthColor = () => {
  const colors = ['#ff6b6b', '#ffa500', '#ffd700', '#90ee90', '#00d4ff']
  return colors[passwordStrength.value] || '#ff6b6b'
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const toggleConfirmPasswordVisibility = () => {
  showConfirmPassword.value = !showConfirmPassword.value
}

const handleRegister = async () => {
  // 1️⃣ 전체 필드 검증
  let isValid = true
  Object.keys(fieldErrors.value).forEach((field) => {
    if (!validateField(field)) isValid = false
  })

  if (!isValid) return

  // 2️⃣ 로딩 시작
  isLoading.value = true

  // 3️⃣ API 호출 시뮬레이션
  setTimeout(() => {
    isLoading.value = false
    router.push('/')
  }, 1500)
}

const handleLoginRedirect = () => {
  router.push('/login')
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
        <form class="register-form" @submit.prevent="handleRegister" novalidate>
          <!-- Name Row -->
          <div class="form-row">
            <div class="form-group">
              <label for="firstName" class="form-label">First Name</label>
              <div class="input-wrapper">
                <span class="input-icon">👤</span>
                <input
                  id="firstName"
                  v-model="formData.firstName"
                  type="text"
                  placeholder="CHA"
                  class="form-input"
                  @blur="validateField('firstName')"
                  @input="fieldErrors.firstName = ''"
                />
                <!-- 👇 커스텀 말풍선 -->
                <div v-if="fieldErrors.firstName" class="error-tooltip">
                  ⚠️ {{ fieldErrors.firstName }}
                </div>
              </div>
            </div>

            <div class="form-group">
              <label for="lastName" class="form-label">Last Name</label>
              <div class="input-wrapper">
                <span class="input-icon">👤</span>
                <input
                  id="lastName"
                  v-model="formData.lastName"
                  type="text"
                  placeholder="EUNWOO"
                  class="form-input"
                  @blur="validateField('lastName')"
                  @input="fieldErrors.lastName = ''"
                />
                <!-- 👇 커스텀 말풍선 -->
                <div v-if="fieldErrors.lastName" class="error-tooltip">
                  ⚠️ {{ fieldErrors.lastName }}
                </div>
              </div>
            </div>
          </div>

          <!-- Email Input -->
          <div class="form-group">
            <label for="email" class="form-label">Email Address</label>
            <div class="input-wrapper">
              <span class="input-icon">📧</span>
              <input
                id="email"
                v-model="formData.email"
                type="email"
                placeholder="you@example.com"
                class="form-input"
                @blur="validateField('email')"
                @input="fieldErrors.email = ''"
              />
              <!-- 👇 커스텀 말풍선 -->
              <div v-if="fieldErrors.email" class="error-tooltip">
                ⚠️ {{ fieldErrors.email }}
              </div>
            </div>
          </div>

          <!-- Password Input -->
          <div class="form-group">
            <label for="password" class="form-label">Password</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input
                id="password"
                v-model="formData.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Create a strong password"
                class="form-input"
                @blur="validateField('password')"
                @input="validatePassword"
              />
              <button
                type="button"
                class="password-toggle"
                @click="togglePasswordVisibility"
                :title="showPassword ? 'Hide password' : 'Show password'"
              >
                {{ showPassword ? '👁️' : '👁️‍🗨️' }}
              </button>
              <!-- 👇 커스텀 말풍선 -->
              <div v-if="fieldErrors.password" class="error-tooltip">
                ⚠️ {{ fieldErrors.password }}
              </div>
            </div>

            <!-- Password Strength Indicator -->
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
              >Confirm Password</label
            >
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input
                id="confirmPassword"
                v-model="formData.confirmPassword"
                :type="showConfirmPassword ? 'text' : 'password'"
                placeholder="Confirm your password"
                class="form-input"
                @blur="validateField('confirmPassword')"
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
              <!-- 👇 커스텀 말풍선 -->
              <div v-if="fieldErrors.confirmPassword" class="error-tooltip">
                ⚠️ {{ fieldErrors.confirmPassword }}
              </div>
            </div>
          </div>

          <!-- Terms and Conditions -->
          <label class="terms-checkbox">
            <input v-model="formData.agreeTerms" type="checkbox" />
            <span>
              <a href="#" class="terms-link">이용약관</a>
            </span>
          </label>

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

        <!-- Divider -->
        <div class="divider">
          <span>or</span>
        </div>

        <!-- Social Register -->
        <div class="social-register">
          <button type="button" class="social-btn google">
            <span>🔍</span>
            Google
          </button>
          <button type="button" class="social-btn github">
            <span>💻</span>
            GitHub
          </button>
        </div>

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
          <h3>오늘 시작하세요!</h3>
        </div>
        <ul class="info-list">
          <li>
            <span class="check-icon">✓</span>
            <span>무료 계정 생성</span>
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
  gap: 3rem;
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

.register-header {
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

.register-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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

.terms-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  color: #a0a0a0;
  font-size: 0.85rem;
  cursor: pointer;
  user-select: none;
}

.terms-checkbox input {
  width: 16px;
  height: 16px;
  margin-top: 0.25rem;
  cursor: pointer;
  accent-color: #00d4ff;
  flex-shrink: 0;
}

.terms-link {
  color: #00d4ff;
  text-decoration: none;
  transition: all 0.3s ease;
}

.terms-link:hover {
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
</style>
