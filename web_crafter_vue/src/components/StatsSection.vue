<script setup>
import { ref, onMounted } from 'vue'

const stats = ref([
  { label: 'Active Users', value: 50, suffix: 'K+' },
  { label: 'Projects Completed', value: 1200, suffix: '+' },
  { label: 'Client Satisfaction', value: 98, suffix: '%' },
  { label: 'Global Reach', value: 150, suffix: '+' }
])

const displayValues = ref([0, 0, 0, 0])
const hasAnimated = ref(false)

const animateCounter = (index, target) => {
  const duration = 2000
  const steps = 60
  const stepDuration = duration / steps
  const increment = target / steps

  let current = 0
  const interval = setInterval(() => {
    current += increment
    if (current >= target) {
      displayValues.value[index] = target
      clearInterval(interval)
    } else {
      displayValues.value[index] = Math.floor(current)
    }
  }, stepDuration)
}

const observeStats = () => {
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting && !hasAnimated.value) {
        hasAnimated.value = true
        stats.value.forEach((stat, index) => {
          animateCounter(index, stat.value)
        })
      }
    })
  }, { threshold: 0.5 })

  const statsSection = document.querySelector('.stats')
  if (statsSection) observer.observe(statsSection)
}

onMounted(() => {
  setTimeout(observeStats, 100)
})
</script>

<template>
  <section id="stats" class="stats">
    <div class="stats-container">
      <h2 class="stats-title">By The Numbers</h2>

      <div class="stats-grid">
        <div v-for="(stat, index) in stats" :key="index" class="stat-card">
          <div class="stat-value">
            <span class="number">{{ displayValues[index] }}</span>
            <span class="suffix">{{ stat.suffix }}</span>
          </div>
          <p class="stat-label">{{ stat.label }}</p>
          <div class="stat-bar">
            <div class="stat-bar-fill" :style="{ width: (displayValues[index] / stat.value) * 100 + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.stats {
  position: relative;
  padding: 6rem 2rem;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.05) 0%, rgba(0, 153, 204, 0.02) 100%);
  overflow: hidden;
}

.stats::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at 20% 50%, rgba(0, 212, 255, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(0, 153, 204, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

.stats-container {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
}

.stats-title {
  font-size: 2.5rem;
  font-weight: 800;
  text-align: center;
  margin-bottom: 3rem;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
}

.stat-card {
  padding: 2rem;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.08) 0%, rgba(0, 153, 204, 0.03) 100%);
  border: 1px solid rgba(0, 212, 255, 0.15);
  border-radius: 15px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent 0%, rgba(0, 212, 255, 0.1) 50%, transparent 100%);
  transition: left 0.5s ease;
}

.stat-card:hover {
  border-color: rgba(0, 212, 255, 0.3);
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.12) 0%, rgba(0, 153, 204, 0.06) 100%);
  transform: translateY(-5px);
  box-shadow: 0 15px 30px rgba(0, 212, 255, 0.1);
}

.stat-card:hover::before {
  left: 100%;
}

.stat-value {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.number {
  font-size: 3rem;
  font-weight: 900;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.suffix {
  font-size: 1.5rem;
  color: #00d4ff;
  font-weight: 700;
}

.stat-label {
  color: #a0a0a0;
  font-size: 1rem;
  margin-bottom: 1.5rem;
  font-weight: 500;
}

.stat-bar {
  width: 100%;
  height: 4px;
  background: rgba(0, 212, 255, 0.1);
  border-radius: 2px;
  overflow: hidden;
}

.stat-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
  border-radius: 2px;
  transition: width 2s ease-out;
  box-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

@media (max-width: 768px) {
  .stats {
    padding: 4rem 1rem;
  }

  .stats-title {
    font-size: 1.75rem;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 1rem;
  }

  .stat-card {
    padding: 1.5rem;
  }

  .number {
    font-size: 2rem;
  }

  .suffix {
    font-size: 1rem;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
