<script setup>
import { ref } from 'vue'

defineProps({
  scrollY: Number
})

const isMenuOpen = ref(false)

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value
}

const scrollToSection = (id) => {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth' })
    isMenuOpen.value = false
  }
}
</script>

<template>
  <nav class="navbar" :class="{ 'scrolled': scrollY > 50 }">
    <div class="navbar-container">
      <div class="navbar-brand">
        <span class="logo-icon">✨</span>
        <span class="logo-text">StyleHub</span>
      </div>

      <button class="menu-toggle" @click="toggleMenu" :class="{ 'active': isMenuOpen }">
        <span></span>
        <span></span>
        <span></span>
      </button>

      <ul class="nav-menu" :class="{ 'active': isMenuOpen }">
        <li><a href="#features" @click.prevent="scrollToSection('features')">Features</a></li>
        <li><a href="#stats" @click.prevent="scrollToSection('stats')">Stats</a></li>
        <li><a href="#cta" @click.prevent="scrollToSection('cta')">Get Started</a></li>
      </ul>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  background: rgba(15, 15, 30, 0.8);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(0, 212, 255, 0.1);
  transition: all 0.3s ease;
}

.navbar.scrolled {
  background: rgba(15, 15, 30, 0.95);
  box-shadow: 0 4px 20px rgba(0, 212, 255, 0.1);
}

.navbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.navbar-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.5rem;
  font-weight: bold;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.logo-icon {
  font-size: 2rem;
}

.logo-text {
  letter-spacing: 1px;
}

.nav-menu {
  display: flex;
  list-style: none;
  gap: 2rem;
}

.nav-menu li a {
  color: #e0e0e0;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
  position: relative;
  padding-bottom: 0.5rem;
}

.nav-menu li a::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%);
  transition: width 0.3s ease;
}

.nav-menu li a:hover {
  color: #00d4ff;
}

.nav-menu li a:hover::after {
  width: 100%;
}

.menu-toggle {
  display: none;
  flex-direction: column;
  background: none;
  border: none;
  cursor: pointer;
  gap: 0.4rem;
}

.menu-toggle span {
  width: 25px;
  height: 3px;
  background: #00d4ff;
  border-radius: 2px;
  transition: all 0.3s ease;
}

.menu-toggle.active span:nth-child(1) {
  transform: rotate(45deg) translate(8px, 8px);
}

.menu-toggle.active span:nth-child(2) {
  opacity: 0;
}

.menu-toggle.active span:nth-child(3) {
  transform: rotate(-45deg) translate(7px, -7px);
}

@media (max-width: 768px) {
  .menu-toggle {
    display: flex;
  }

  .nav-menu {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    flex-direction: column;
    background: rgba(15, 15, 30, 0.95);
    backdrop-filter: blur(10px);
    gap: 0;
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.3s ease;
    border-bottom: 1px solid rgba(0, 212, 255, 0.1);
  }

  .nav-menu.active {
    max-height: 300px;
  }

  .nav-menu li {
    padding: 1rem 2rem;
    border-bottom: 1px solid rgba(0, 212, 255, 0.05);
  }

  .nav-menu li a::after {
    display: none;
  }
}
</style>
