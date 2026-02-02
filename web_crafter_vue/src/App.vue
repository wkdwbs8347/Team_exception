<script setup>
// ==============================
// 1️⃣ Vue 반응형 API import
// ==============================

import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '@/stores/auth'
import api from '@/api/axios'
import { useWebSocketStore } from '@/stores/websocket'
import NavBar from '@/components/NavBar.vue'
import Footer from '@/components/Footer.vue'

// 전역 로그인상태 관리용
const auth = useAuthStore()
const wsStore = useWebSocketStore()

// ==============================
// 2️⃣ 전역 스크롤 상태 정의
// ==============================

// 현재 스크롤 Y 위치(px)를 저장하는 반응형 변수
// App.vue는 최상위 컴포넌트이므로
// 여기서 스크롤 값을 관리하면
// 하위 페이지 컴포넌트에서도 공유 가능
const scrollY = ref(0)

// ==============================
// 3️⃣ 스크롤 이벤트 핸들러
// ==============================

// 브라우저에서 스크롤이 발생할 때 실행되는 함수
const handleScroll = () => {
  // window.scrollY
  // → 현재 세로 스크롤 위치
  scrollY.value = window.scrollY
}

onMounted(async () => {
  window.addEventListener('scroll', handleScroll)
  await auth.bootstrap() // 여기서만 복원 시도

  if (auth.me?.id) {
    console.log('🔌 [App] 사용자 인증 확인됨, 소켓 연결 시도...');
    wsStore.connect(auth.me.id);
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <!-- ==============================
      5️⃣ 앱 최상위 레이아웃
      ============================== -->
  <div class="app">
    <!--   👉 NavBar 내부에서는
      props.scrollY 값을 이용해
      - 스크롤에 따라 배경 변경
      - 그림자 표시
      - 높이/투명도 변경
      같은 UI 제어가 가능
    -->
    <NavBar :scroll-y="scrollY" />
    <!--
      RouterView
      → 현재 URL에 매칭된 페이지 컴포넌트를 렌더링하는 자리

      예)
      /            → Home.vue
      /login       → Login.vue
      /building/1  → BuildingDetail.vue

      App.vue는 공통 레이아웃 역할만 수행
    -->
    <RouterView />
    <!-- 하단 푸터 -->
    <Footer />
  </div>
</template>

<!-- ==============================
    6️⃣ App.vue 전용 스타일
    (scoped)
    ============================== -->
<style scoped>
.app {
  width: 100%; /* 전체 화면 가로폭 사용 */
  overflow-x: hidden; /* 가로 스크롤 방지 (배경/애니메이션 안정화) */
}
</style>

<!-- ==============================
    7️⃣ 전역 스타일
    (scoped 아님)
    ============================== -->
<style>
/* 모든 요소 기본 여백 제거 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box; /* width 계산 안정화 */
}

/* HTML 전체 스크롤 동작 설정 */
html {
  scroll-behavior: smooth; /* 앵커 이동 시 부드러운 스크롤 */
}

/* body 기본 테마 */
body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;

  /* 전체 배경 그라데이션 */
  background: linear-gradient(135deg, #0f0f1e 0%, #1a1a2e 100%);

  color: #e0e0e0; /* 기본 글자색 */
  line-height: 1.6; /* 가독성 향상 */
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-use-select: none;
  user-select: none;
}

/* Vue 앱 루트 엘리먼트 */
#app {
  width: 100%;
}

/* ==============================
    8️⃣ 커스텀 스크롤바 스타일
    (Chrome / Edge / Safari)
   ============================== */

/* 스크롤바 전체 폭 */
::-webkit-scrollbar {
  width: 10px;
}

/* 스크롤바 트랙(배경) */
::-webkit-scrollbar-track {
  background: #1a1a2e;
}

/* 스크롤바 드래그 영역 */
::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #00d4ff 0%, #0099cc 100%);
  border-radius: 5px;
}

/* 스크롤바 hover 시 */
::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(180deg, #00e5ff 0%, #00aadd 100%);
}
</style>
