<script setup>
import { ref, onMounted } from 'vue';

// Web Crafter의 핵심 기능으로 내용 변경
const features = ref([
  {
    id: 1,
    icon: '🧩',
    title: '블록 코딩 인터페이스',
    description:
      '레고를 조립하듯 블록을 드래그 앤 드롭하여 웹페이지 구조를 손쉽게 설계하세요.',
  },
  {
    id: 2,
    icon: '⚡',
    title: '실시간 미리보기',
    description:
      '코드를 수정하는 즉시 결과물이 어떻게 보이는지 실시간으로 확인하며 작업할 수 있습니다.',
  },
  {
    id: 3,
    icon: '🤖',
    title: 'AI 코드 어시스턴트',
    description:
      '복잡한 로직이 필요할 땐 AI에게 물어보세요. 필요한 블록 구조를 제안해줍니다.',
  },
  {
    id: 4,
    icon: '📱',
    title: '반응형 디자인 지원',
    description:
      'PC, 태블릿, 모바일 등 다양한 기기 환경에 맞춰 자동으로 최적화된 웹을 만듭니다.',
  },
  {
    id: 5,
    icon: '🌏',
    title: '프로젝트 공유',
    description:
      '다른 사용자들이 만든 멋진 프로젝트를 구경하고, 아이디어를 얻어보세요.',
  },
  {
    id: 6,
    icon: '🔄',
    title: '프로젝트 복사 & 리메이크',
    description:
      '마음에 드는 프로젝트를 내 작업실로 가져와서 나만의 스타일로 수정할 수 있습니다.',
  },
]);

const visibleFeatures = ref(new Set());

const observeFeatures = () => {
  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          visibleFeatures.value.add(entry.target.dataset.id);
        }
      });
    },
    { threshold: 0.1 }
  );

  const featureElements = document.querySelectorAll('.feature-card');
  featureElements.forEach((el) => observer.observe(el));
};

onMounted(() => {
  setTimeout(observeFeatures, 100);
});
</script>

<template>
  <section id="features" class="features">
    <div class="features-container">
      <div class="section-header">
        <h2 class="section-title">핵심 기능</h2>
        <p class="section-subtitle">웹 개발의 모든 과정을 쉽고 재미있게</p>
      </div>

      <div class="features-grid">
        <div
          v-for="feature in features"
          :key="feature.id"
          class="feature-card"
          :class="{ visible: visibleFeatures.has(feature.id.toString()) }"
          :data-id="feature.id"
        >
          <div class="feature-icon">{{ feature.icon }}</div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-description">{{ feature.description }}</p>
          <div class="feature-border"></div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* CSS는 기존 코드 그대로 사용 (내용만 바뀌었으므로 스타일은 완벽 호환됨) */
/* ... 기존 FeaturesSection의 style 전체 복사 ... */
.features {
  position: relative;
  padding: 6rem 2rem;
  background: linear-gradient(
    180deg,
    rgba(15, 15, 30, 0) 0%,
    rgba(0, 212, 255, 0.03) 100%
  );
}
.features-container {
  max-width: 1200px;
  margin: 0 auto;
}
.section-header {
  text-align: center;
  margin-bottom: 4rem;
}
.section-title {
  font-size: 3rem;
  font-weight: 800;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.section-subtitle {
  font-size: 1.25rem;
  color: #a0a0a0;
}
.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}
.feature-card {
  position: relative;
  padding: 2rem;
  background: linear-gradient(
    135deg,
    rgba(0, 212, 255, 0.05) 0%,
    rgba(0, 153, 204, 0.02) 100%
  );
  border: 1px solid rgba(0, 212, 255, 0.1);
  border-radius: 15px;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateY(30px);
  overflow: hidden;
}
.feature-card.visible {
  animation: slideInUp 0.6s ease-out forwards;
}
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.feature-card:hover {
  background: linear-gradient(
    135deg,
    rgba(0, 212, 255, 0.1) 0%,
    rgba(0, 153, 204, 0.05) 100%
  );
  border-color: rgba(0, 212, 255, 0.3);
  transform: translateY(-10px);
  box-shadow: 0 20px 40px rgba(0, 212, 255, 0.15);
}
.feature-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  display: inline-block;
  animation: iconFloat 3s ease-in-out infinite;
}
@keyframes iconFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}
.feature-title {
  font-size: 1.5rem;
  font-weight: 700;
  margin-bottom: 0.75rem;
  color: #e0e0e0;
}
.feature-description {
  color: #a0a0a0;
  line-height: 1.6;
  font-size: 0.95rem;
}
.feature-border {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 3px;
  background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
  transition: width 0.3s ease;
}
.feature-card:hover .feature-border {
  width: 100%;
}
@media (max-width: 768px) {
  .features {
    padding: 4rem 1rem;
  }
  .section-title {
    font-size: 2rem;
  }
  .section-subtitle {
    font-size: 1rem;
  }
  .features-grid {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }
  .feature-card {
    padding: 1.5rem;
  }
}
</style>
