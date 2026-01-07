<script setup>
import { computed } from 'vue';
import {
  AlertTriangle,
  CircleHelp,
  CheckCircle,
  XCircle,
} from 'lucide-vue-next';

const props = defineProps({
  open: Boolean,
  message: String,
  type: { type: String, default: 'warning' }, // warning | info | success | error
  icon: { type: [Object, Function], default: null },

  // 버튼 라벨 커스텀
  confirmText: { type: String, default: '삭제' },
  cancelText: { type: String, default: '취소' },
});

const emit = defineEmits(['confirm', 'cancel']);

const defaultIconMap = {
  warning: AlertTriangle,
  info: CircleHelp,
  success: CheckCircle,
  error: XCircle,
};

const IconComponent = computed(
  () => props.icon || defaultIconMap[props.type] || CircleHelp
);

const onKeydown = (e) => {
  if (!props.open) return;
  if (e.key === 'Escape') emit('cancel');
  if (e.key === 'Enter') emit('confirm');
};
</script>

<template>
  <transition name="modal-fade">
    <div
      v-if="open"
      class="modal-backdrop"
      tabindex="-1"
      @keydown="onKeydown"
      @mousedown.self="$emit('cancel')"
    >
      <div class="modal-card" role="dialog" aria-modal="true">
        <div class="modal-icon-wrapper" :class="props.type">
          <component :is="IconComponent" class="modal-icon" />
        </div>

        <p class="modal-message">{{ message }}</p>

        <div class="modal-actions">
          <button class="modal-btn danger" @click="$emit('confirm')">
            {{ confirmText }}
          </button>
          <button class="modal-btn ghost" @click="$emit('cancel')">
            {{ cancelText }}
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
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
  max-width: 380px;
  text-align: center;

  box-shadow:
    0 30px 80px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(0, 212, 255, 0.15),
    0 0 40px rgba(0, 212, 255, 0.25);

  animation: popIn 0.35s ease-out;
}

.modal-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  margin: 0 auto 1.2rem auto;
  border-radius: 50%;
}

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

.modal-icon {
  width: 32px;
  height: 32px;
  display: block;
}
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

.modal-message {
  color: #e5e7eb;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1.8rem;
  word-break: keep-all;
}

.modal-actions {
  display: flex;
  gap: 10px;
}

.modal-btn {
  flex: 1;
  padding: 0.85rem 1rem;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  font-size: 0.95rem;
  font-weight: 800;
  letter-spacing: 0.4px;
  transition: all 0.25s ease;
}

/* 취소(유령 버튼) */
.modal-btn.ghost {
  background: rgba(255, 255, 255, 0.08);
  color: #e5e7eb;
  border: 1px solid rgba(255, 255, 255, 0.12);
}
.modal-btn.ghost:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(255, 255, 255, 0.08);
}

/* 삭제(위험 버튼) */
.modal-btn.danger {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff3b3b 100%);
  color: #0f0f1e;
}
.modal-btn.danger:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 35px rgba(255, 80, 80, 0.35);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.25s ease;
}
.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}

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
