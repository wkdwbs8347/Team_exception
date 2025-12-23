<script setup>
import { ref, onMounted } from 'vue'

const isVisible = ref(false)

onMounted(() => {
  isVisible.value = true
})
</script>

<template>
  <section class="hero">
    <div class="hero-background">
      <div class="gradient-orb orb-1"></div>
      <div class="gradient-orb orb-2"></div>
      <div class="gradient-orb orb-3"></div>
    </div>

    <div class="hero-content" :class="{ 'visible': isVisible }">
      <h1 class="hero-title">
        <span class="title-line">Welcome to</span>
        <span class="title-highlight">StyleHub</span>
      </h1>

      <p class="hero-subtitle">
        Experience the future of modern web design with cutting-edge technology and stunning visual aesthetics.
      </p>

      <div class="hero-buttons">
        <button class="btn btn-primary">Get Started</button>
        <button class="btn btn-secondary">Learn More</button>
      </div>

      <div class="hero-features">
        <div class="feature-item">
          <span class="feature-icon">⚡</span>
          <span class="feature-text">Lightning Fast</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🎨</span>
          <span class="feature-text">Beautiful Design</span>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🔒</span>
          <span class="feature-text">Secure & Reliable</span>
        </div>
      </div>
    </div>

    <div class="hero-scroll-indicator">
      <div class="scroll-dot"></div>
      <p>Scroll to explore</p>
    </div>
  </section>
</template>

<style scoped>
.hero {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  padding-top: 80px;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: -1;
}

.gradient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
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
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(30px);
  }
}

.hero-content {
  position: relative;
  z-index: 10;
  text-align: center;
  max-width: 800px;
  padding: 0 2rem;
  opacity: 0;
  animation: fadeInUp 1s ease-out forwards;
}

.hero-content.visible {
  animation: fadeInUp 1s ease-out forwards;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-title {
  font-size: 4rem;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 1.5rem;
  letter-spacing: -2px;
}

.title-line {
  display: block;
  color: #a0a0a0;
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.title-highlight {
  display: block;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 50%, #00ffff 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-size: 4rem;
  animation: shimmer 3s ease-in-out infinite;
}

@keyframes shimmer {
  0%, 100% {
    filter: brightness(1);
  }
  50% {
    filter: brightness(1.2);
  }
}

.hero-subtitle {
  font-size: 1.25rem;
  color: #b0b0b0;
  margin-bottom: 2.5rem;
  line-height: 1.8;
}

.hero-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 3rem;
  flex-wrap: wrap;
}

.btn {
  padding: 0.875rem 2rem;
  border: none;
  border-radius: 50px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.btn-primary {
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  color: #0f0f1e;
  box-shadow: 0 8px 25px rgba(0, 212, 255, 0.3);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 12px 35px rgba(0, 212, 255, 0.5);
}

.btn-secondary {
  background: transparent;
  color: #00d4ff;
  border: 2px solid #00d4ff;
}

.btn-secondary:hover {
  background: rgba(0, 212, 255, 0.1);
  transform: translateY(-3px);
}

.hero-features {
  display: flex;
  gap: 2rem;
  justify-content: center;
  flex-wrap: wrap;
  margin-top: 2rem;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1.5rem;
  background: rgba(0, 212, 255, 0.05);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 50px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(0, 212, 255, 0.1);
  border-color: rgba(0, 212, 255, 0.4);
  transform: translateY(-2px);
}

.feature-icon {
  font-size: 1.5rem;
}

.feature-text {
  font-weight: 500;
  color: #e0e0e0;
}

.hero-scroll-indicator {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  animation: bounce 2s infinite;
}

.scroll-dot {
  width: 8px;
  height: 8px;
  background: #00d4ff;
  border-radius: 50%;
  animation: scrollBounce 2s infinite;
}

@keyframes bounce {
  0%, 100% {
    transform: translateX(-50%) translateY(0);
  }
  50% {
    transform: translateX(-50%) translateY(10px);
  }
}

@keyframes scrollBounce {
  0%, 100% {
    opacity: 1;
    transform: translateY(0);
  }
  50% {
    opacity: 0.5;
    transform: translateY(10px);
  }
}

.hero-scroll-indicator p {
  font-size: 0.875rem;
  color: #808080;
  text-transform: uppercase;
  letter-spacing: 1px;
}

@media (max-width: 768px) {
  .hero {
    min-height: 80vh;
    padding-top: 60px;
  }

  .hero-title {
    font-size: 2.5rem;
  }

  .title-line {
    font-size: 1.25rem;
  }

  .title-highlight {
    font-size: 2.5rem;
  }

  .hero-subtitle {
    font-size: 1rem;
  }

  .hero-buttons {
    flex-direction: column;
  }

  .btn {
    width: 100%;
  }

  .hero-features {
    flex-direction: column;
    gap: 1rem;
  }

  .feature-item {
    width: 100%;
    justify-content: center;
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
