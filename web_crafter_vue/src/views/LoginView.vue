<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Sparkles,
  Mail,
  Lock,
  Eye,
  EyeOff,
  Palette,
  Check,
} from 'lucide-vue-next'

const router = useRouter()

const email = ref('')
const password = ref('')
const rememberMe = ref(false)
const isLoading = ref(false)
const errorMessage = ref('')
const showPassword = ref(false)

const handleLogin = async () => {
  errorMessage.value = ''

  if (!email.value || !password.value) {
    errorMessage.value = 'Please fill in all fields'
    return
  }

  isLoading.value = true

  // Simulate API call
  setTimeout(() => {
    isLoading.value = false
    // Success - redirect to home
    router.push('/')
  }, 1500)
}

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const handleForgotPassword = () => {
  alert('Password reset functionality would be implemented here')
}

const handleSignUp = () => {
  router.push('/register')
}
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
        <!-- Header -->
        <div class="login-header">
          <div class="logo-section">
            <span class="logo-icon"><Sparkles :size="28" /></span>
            <h1 class="logo-text">Web Crafter</h1>
          </div>
          <p class="subtitle">당신의 작업실에 오신 걸 환영합니다!</p>
        </div>

        <!-- Form -->
        <form class="login-form" @submit.prevent="handleLogin" autocomplete="off">
          <!-- Email Input -->
          <div class="form-group">
            <label for="email" class="form-label">이메일 주소</label>
            <div class="input-wrapper">
              <span class="input-icon"><Mail :size="18" /></span>
              <input
                id="email"
                v-model="email"
                type="email"
                placeholder="you@example.com"
                class="form-input"
                required
              />
            </div>
          </div>

          <!-- Password Input -->
          <div class="form-group">
            <label for="password" class="form-label">비밀번호</label>
            <div class="input-wrapper">
              <span class="input-icon"><Lock :size="18" /></span>
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="Enter your password"
                class="form-input"
                required
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
            </div>
          </div>

          <!-- Remember Me & Forgot Password -->
          <div class="form-options">
            <label class="remember-me">
              <input v-model="rememberMe" type="checkbox" />
              <span>자동 로그인</span>
            </label>
            <button
              type="button"
              class="forgot-password"
              @click="handleForgotPassword"
            >
              비밀번호를 잊으셨나요?
            </button>
          </div>

          <!-- Error Message -->
          <div v-if="errorMessage" class="error-message">
            {{ errorMessage }}
          </div>

          <!-- Login Button -->
          <button type="submit" class="login-btn" :disabled="isLoading">
            <span v-if="!isLoading">로그인</span>
            <span v-else class="loading-spinner">
              <span class="spinner"></span>
              로그인 중...
            </span>
          </button>
        </form>

        <!-- Divider -->
        <div class="divider">
          <span>or</span>
        </div>

        <!-- Social Login -->
        <div class="social-login">
          <button type="button" class="social-btn google">
            <span>🔍</span>
            Google
          </button>
          <button type="button" class="social-btn github">
            <span>💻</span>
            GitHub
          </button>
        </div>

        <!-- Sign Up Link -->
        <div class="signup-section">
          <p>
            아직 계정이 없으신가요?
            <button type="button" class="signup-link" @click="handleSignUp">
              회원가입
            </button>
          </p>
        </div>
      </div>

      <!-- Decorative Card -->
      <div class="info-card">
        <div class="info-header">
          <span class="info-icon"><Palette :size="26" /></span>
          <h3>웹페이지 제작 경험이 없으신가요?</h3>
        </div>
        <ul class="info-list">
          <li>
            <span class="check-icon"><Check :size="14" /></span>
            <span>코드블럭을 활용한 학습과 창작 경험 제공</span>
          </li>
          <li>
            <span class="check-icon"><Check :size="14" /></span>
            <span>쉽게 만드는 나만의 웹사이트</span>
          </li>
          <li>
            <span class="check-icon"><Check :size="14" /></span>
            <span>모든 변화가 즉시 반영</span>
          </li>
          <li>
            <span class="check-icon"><Check :size="14" /></span>
            <span>안심하고 쓰는 보안</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
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

.remember-me input {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #00d4ff;
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
