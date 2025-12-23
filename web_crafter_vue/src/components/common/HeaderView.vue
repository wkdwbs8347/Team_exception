<template>
  <header class="main-header">
    <div class="header-content">
      <div class="logo">
        <router-link to="/">
          <!-- <img src="@/assets/logo.png" alt="My Logo" class="logo-image" /> -->
          <h1>Web Crafter</h1>
        </router-link>
      </div>

      <nav class="main-nav">
        <ul>
          <li v-for="menu in menus" :key="menu.path">
            <router-link :to="menu.path">
              {{ menu.name }}
            </router-link>
          </li>
        </ul>
      </nav>

      <div class="user-actions">
        <router-link v-if="!isLoggedIn" to="/login" class="login-button">
          Login
        </router-link>

        <router-link v-if="!isLoggedIn" to="/signup" class="signup-button">
          Sign Up
        </router-link>
        <router-link v-if="!isLoggedIn" to="/test" class="test-button">
          스프링연결테스트
        </router-link>

        <button v-if="isLoggedIn" class="login-button" @click="logout">
          Logout
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { ref } from 'vue'

defineOptions({
  name: 'AppHeader',
})

const menus = [
  { name: 'Home', path: '/' },
  { name: 'About', path: '/about' },
  { name: 'Services', path: '/services' },
  { name: 'Contact', path: '/contact' },
  { name: 'Spring', path: '/test' },
]

// 추후 Pinia / JWT / Session 연동 가능
const isLoggedIn = ref(false)

const logout = () => {
  isLoggedIn.value = false
}
</script>

<style scoped>
.main-header {
  background-color: #2c3e50;
  color: white;
  padding: 15px 30px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.2);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
}

.logo {
  display: flex;
  align-items: center;
}

.logo a {
  display: flex;
  align-items: center;
  text-decoration: none;
  color: white;
}

.logo-image {
  height: 40px;
  margin-right: 10px;
}

.logo h1 {
  font-size: 1.8em;
  margin: 0;
  font-weight: bold;
}

.main-nav ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
}

.main-nav li {
  margin-left: 25px;
}

.main-nav a {
  text-decoration: none;
  color: white;
  font-weight: 500;
  font-size: 1.1em;
  transition: color 0.3s ease;
}

.main-nav a:hover,
.main-nav a.router-link-exact-active {
  color: #42b983;
}

.user-actions {
  display: flex;
  gap: 15px;
}

.login-button,
.signup-button {
  text-decoration: none;
  color: white;
  background-color: #42b983;
  padding: 8px 15px;
  border-radius: 5px;
  font-weight: bold;
  transition: background-color 0.3s ease;
  border: none;
  cursor: pointer;
}

.login-button:hover,
.signup-button:hover {
  background-color: #36a16e;
}

.test-button {
  text-decoration: none;
  color: white;
  background-color: #42b983;
  padding: 8px 15px;
  border-radius: 5px;
  font-weight: bold;
  transition: background-color 0.3s ease;
  border: none;
  cursor: pointer;
}
</style>
