<script setup>
import { ref, onMounted } from 'vue'

const features = ref([
  {
    id: 1,
    icon: '🚀',
    title: 'Lightning Performance',
    description: 'Built with cutting-edge technology for maximum speed and efficiency'
  },
  {
    id: 2,
    icon: '🎯',
    title: 'Precision Design',
    description: 'Every pixel is carefully crafted for an exceptional user experience'
  },
  {
    id: 3,
    icon: '🔧',
    title: 'Easy Integration',
    description: 'Seamlessly integrate with your existing tools and workflows'
  },
  {
    id: 4,
    icon: '📊',
    title: 'Analytics Ready',
    description: 'Comprehensive insights to track and optimize your performance'
  },
  {
    id: 5,
    icon: '🌍',
    title: 'Global Scale',
    description: 'Reliable infrastructure that works anywhere in the world'
  },
  {
    id: 6,
    icon: '🛡️',
    title: 'Enterprise Security',
    description: 'Bank-level security to protect your data and privacy'
  }
])

const visibleFeatures = ref(new Set())

const observeFeatures = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        visibleFeatures.value.add(entry.target.dataset.id)
      }
    })
  }, { threshold: 0.1 })

  const featureElements = document.querySelectorAll('.feature-card')
  featureElements.forEach((el) => observer.observe(el))
}

onMounted(() => {
  setTimeout(observeFeatures, 100)
})
</script>

<template>
  <section id="features" class="features">
    <div class="features-container">
      <div class="section-header">
        <h2 class="section-title">Powerful Features</h2>
        <p class="section-subtitle">Everything you need to succeed</p>
      </div>

      <div class="features-grid">
        <div
          v-for="feature in features"
          :key="feature.id"
          class="feature-card"
          :class="{ 'visible': visibleFeatures.has(feature.id.toString()) }"
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
.features {
  position: relative;
  padding: 6rem 2rem;
  background: linear-gradient(180deg, rgba(15, 15, 30, 0) 0%, rgba(0, 212, 255, 0.03) 100%);
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
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.05) 0%, rgba(0, 153, 204, 0.02) 100%);
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
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.1) 0%, rgba(0, 153, 204, 0.05) 100%);
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
  0%, 100% {
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
