<script setup>
// ==============================
// 1️⃣ 컴포넌트 import 영역
// ==============================

// 메인 히어로 섹션
import HeroSection from '../components/HeroSection.vue'

// 기능 소개 섹션
import FeaturesSection from '../components/FeaturesSection.vue'

// 통계 / 수치 표시 섹션
import StatsSection from '../components/StatsSection.vue'

// Vue의 반응형 상태를 만들기 위한 ref import
import { ref } from 'vue'


// ==============================
// 2️⃣ 반응형 상태 정의
// ==============================

// 현재 스크롤 Y 좌표를 저장할 반응형 변수
// ref(0) → 초기값 0
// scrollY.value 값이 바뀌면 이를 사용하는 컴포넌트도 자동 재렌더링됨
const scrollY = ref(0)


// ==============================
// 3️⃣ 스크롤 이벤트 핸들러 함수
// ==============================

// 스크롤이 발생할 때마다 실행되는 함수
const handleScroll = () => {
  // window.scrollY
  // → 현재 브라우저의 세로 스크롤 위치(px)
  // → 이 값을 scrollY에 저장
  scrollY.value = window.scrollY
}


// ==============================
// 4️⃣ 브라우저 환경 체크 후 이벤트 등록
// ==============================

// Vite는 SSR(서버 사이드 렌더링)도 가능하기 때문에
// window 객체가 존재하는지 먼저 확인
if (typeof window !== 'undefined') {
  // 브라우저에서 스크롤이 발생할 때마다
  // handleScroll 함수 실행
  window.addEventListener('scroll', handleScroll)
}
</script>


<template>
  <!-- ==============================
       5️⃣ 화면 렌더링 영역
       ============================== -->
  <div class="home">


    <!-- 메인 히어로 섹션 -->
    <HeroSection />

    <!-- 기능 소개 섹션 -->
    <FeaturesSection />

    <!-- 통계 / 수치 섹션 -->
    <StatsSection />

  </div>
</template>


<style scoped>
/* ==============================
   6️⃣ 스타일 영역
   ============================== */

/* 홈 전체 레이아웃 */
.home {
  width: 100%;          /* 전체 가로폭 사용 */
  overflow-x: hidden;  /* 가로 스크롤 방지 (애니메이션/배경 확장 시 필수) */
}
</style>