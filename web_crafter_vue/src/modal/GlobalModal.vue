<script setup>
import { computed } from 'vue'
import {
  AlertTriangle,
  CheckCircle,
  XCircle,
  CircleHelp,
} from 'lucide-vue-next'

const props = defineProps({
  open: Boolean,
  message: String,
  type: {
    type: String,
    default: 'info', // warning | info | success | error
  },
  icon: {
    type: [Object, Function], // Lucide 아이콘 컴포넌트
    default: null,
  },
})

const emit = defineEmits(['confirm'])

/* type별 기본 아이콘 (icon이 없을 때만 사용) */
const defaultIconMap = {
  warning: AlertTriangle,
  info: CircleHelp,
  success: CheckCircle,
  error: XCircle,
}

/* 상황별 최종 아이콘 결정: icon prop > type 기본 */
const IconComponent = computed(() => {
  return props.icon || defaultIconMap[props.type] || CircleHelp
})
</script>

<template>
  <transition name="modal-fade">
    <div v-if="open" class="modal-backdrop">
      <div class="modal-card">
        <!-- 아이콘 래퍼 -->
        <div class="modal-icon-wrapper" :class="props.type">
          <component :is="IconComponent" class="modal-icon" />
        </div>

        <!-- 메시지 -->
        <p class="modal-message">{{ message }}</p>

        <!-- 확인 버튼 -->
        <button class="modal-btn" @click="$emit('confirm')">확인</button>
      </div>
    </div>
  </transition>
</template>

<style scoped>
/* ===============================
   🌌 배경 오버레이
================================ */
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

/* ===============================
   💎 모달 카드
================================ */
.modal-card {
  background: linear-gradient(
    135deg,
    rgba(15, 15, 30, 0.9) 0%,
    rgba(26, 26, 46, 0.9) 100%
  );
  border: 1.5px solid rgba(0, 212, 255, 0.35);
  border-radius: 18px;
  padding: 2.2rem 2.5rem;
  width: 100%;
  max-width: 360px;
  text-align: center;

  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(0, 212, 255, 0.15),
    0 0 40px rgba(0, 212, 255, 0.25);

  animation: popIn 0.35s ease-out;
}

/* ===============================
   ⚠️ 아이콘
================================ */
/* ===============================
   아이콘 래퍼 (정렬 담당)
================================ */
.modal-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;

  width: 64px;
  height: 64px;
  margin: 0 auto 1.2rem auto;

  border-radius: 50%;
}

/* 타입별 배경 */
.modal-icon-wrapper.warning {
  background: rgba(255, 80, 80, 0.15);
}

.modal-icon-wrapper.info {
  background: rgba(0, 212, 255, 0.15);
}

.modal-icon-wrapper.success {
  background: rgba(34, 197, 94, 0.15);
}

.modal-icon-wrapper.error {
  background: rgba(255, 80, 80, 0.15);
}

/* ===============================
   SVG 아이콘 자체
================================ */
.modal-icon {
  width: 32px;
  height: 32px;
  display: block;
}

/* 색상 분기 */
.modal-icon-wrapper.warning .modal-icon {
  color: #ff6b6b;
}

.modal-icon-wrapper.info .modal-icon {
  color: #00d4ff;
}

.modal-icon-wrapper.success .modal-icon {
  color: #22c55e;
}

.modal-icon-wrapper.error .modal-icon {
  color: #ff6b6b;
}

/* ===============================
   📝 메시지 텍스트
================================ */
.modal-message {
  color: #e5e7eb;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1.8rem;
  word-break: keep-all;
}

/* ===============================
   🔘 확인 버튼
================================ */
.modal-btn {
  width: 100%;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  border: none;

  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  color: #0f0f1e;
  font-size: 0.95rem;
  font-weight: 700;
  letter-spacing: 0.5px;
  cursor: pointer;

  transition: all 0.3s ease;
}

.modal-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.45);
}

/* ===============================
   🎬 트랜지션
================================ */
.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

/* ===============================
   🎥 팝 애니메이션
================================ */
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
