<script setup>
// ==============================
// 1️⃣ 컴포넌트 및 라이브러리 import
// ==============================
import { ref, onMounted, onBeforeUnmount } from 'vue' // onUnmount를 onBeforeUnmount로 수정
import { useRouter } from 'vue-router'
import api from '@/api/axios' 
import { useAuthStore } from '@/stores/auth'

// 섹션 컴포넌트
import HeroSection from '../components/HeroSection.vue'
import FeaturesSection from '../components/FeaturesSection.vue'
import StatsSection from '../components/StatsSection.vue'

const router = useRouter()
const auth = useAuthStore()

// ==============================
// 2️⃣ 반응형 상태 정의
// ==============================
const scrollY = ref(0)

// ==============================
// 3️⃣ 핵심 로직: 프로젝트 생성 및 페이지 이동
// ==============================
const handleCreateProject = async () => {
  try {
    // 1. 백엔드 API 호출 (ProjectController.java의 /create 실행)
    const res = await api.post('/projects/create')
    const newWebId = res.data // DB에서 생성된 자동 증가 ID

    // 2. 유저 닉네임 확보 (로그인 정보가 없으면 guest로 처리)
    const nickname = auth.member?.nickname || 'guest'

    // 3. 에디터 페이지로 이동 (라우터 규칙: /ide/:nickname/:webId)
    // 예: /ide/jjy/7
    router.push(`/ide/${nickname}/${newWebId}`)
    
  } catch (error) {
    console.error("프로젝트 생성 중 오류 발생:", error)
    
    // 401 에러(권한 없음) 발생 시 로그인 페이지로 유도
    if (error.response?.status === 401) {
      alert("로그인이 필요한 서비스입니다.")
      router.push('/login')
    } else {
      alert("서버와의 통신에 실패했습니다. 다시 시도해주세요.")
    }
  }
}

// ==============================
// 4️⃣ 스크롤 이벤트 관리 (에러 수정 완료)
// ==============================
const handleScroll = () => {
  scrollY.value = window.scrollY
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll)
})

// Vue 3 표준 명칭인 onBeforeUnmount로 교체하여 SyntaxError 해결
onBeforeUnmount(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<template>
  <div class="home">
    <HeroSection @create="handleCreateProject" />

    <FeaturesSection />
    <StatsSection />
  </div>
</template>

<style scoped>
/* 홈 전체 레이아웃 */
.home {
  width: 100%;
  overflow-x: hidden;
  /* App.vue에서 설정한 그라데이션 배경이 보이도록 투명 설정 */
  background: transparent; 
}
</style>