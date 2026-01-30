<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios' 

// 항목이 3개이므로 인덱스는 [0, 1, 2] 입니다.
const stats = ref([
  { label: '가입 유저', value: 0, suffix: '명', key: 'userCount' },       // index 0
  { label: '생성된 프로젝트', value: 0, suffix: '개', key: 'projectCount' }, // index 1
  { label: '누적 조회수', value: 0, suffix: '회', key: 'totalViews' }     // index 2
])

const displayValues = ref([0, 0, 0]) 
const hasAnimated = ref(false)

const fetchStats = async () => {
  try {
    const res = await api.get('/common/stats') 
    const data = res.data

    // ✅ 요청하신 콘솔 로그: 여기서 실제 백엔드 데이터 확인 가능
    console.log("📡 백엔드에서 받은 원본 데이터:", data); 

    // 1. 일단 변수에 담습니다 (바로 stats에 넣지 말고 로직 처리를 위해)
    let uCount = data.userCount || 0;
    let pCount = data.projectCount || 0;
    let tViews = data.totalViews || 0;

    // 2. 데모 모드 로직 (프로젝트가 0개면 가짜 데이터 보여주기)
    if (pCount === 0) {
        console.warn("⚠️ 데이터가 없어서 데모 모드(가짜 데이터)를 보여줍니다.");
        uCount = 15;
        pCount = 3;
        tViews = 120;
    }

    // 3. 최종 값을 화면 변수(stats)에 주입
    // 🚨 중요: 인덱스 0, 1, 2 순서대로 넣어야 합니다.
    stats.value[0].value = uCount;
    stats.value[1].value = pCount;
    stats.value[2].value = tViews; // (아까 코드는 여기가 [3]이라 에러였음)

  } catch (e) {
    console.warn("API 에러 (임시 데이터 사용):", e)
    stats.value[0].value = 42;
    stats.value[1].value = 128;
    stats.value[2].value = 3500;
  }
}

// ... (animateCounter, observeStats 등 나머지 함수는 기존과 동일) ...

// 숫자 카운팅 애니메이션
const animateCounter = (index, target) => {
  const duration = 2000
  const steps = 60
  const stepDuration = duration / steps
  const increment = target / steps

  let current = 0
  const interval = setInterval(() => {
    current += increment
    if (current >= target) {
      // ✅ [핵심] 애니메이션이 끝나면 formatViews를 적용해서 K 단위를 붙임
      displayValues.value[index] = formatViews(target)
      clearInterval(interval)
    } else {
      // ✅ 애니메이션 중에는 숫자만 보여줌 (K 붙이면 계산 안됨)
      displayValues.value[index] = Math.floor(current).toString()
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

onMounted(async () => {
  await fetchStats() 
  setTimeout(observeStats, 100) 
})
// StatSection.vue 내부
const formatViews = (count) => {
  if (!count) return '0';
  if (count >= 1000) {
    return (count / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
  }
  return count;
};
</script>

<template>
  <section id="stats" class="stats">
    <div class="stats-container">
      <h2 class="stats-title">Web Crafter 현황</h2>

      <div class="stats-grid">
        <div v-for="(stat, index) in stats" :key="index" class="stat-card">
          <div class="stat-value">
            <span class="number">{{ displayValues[index] }}</span>
            <span class="suffix">{{ stat.suffix }}</span>
          </div>
          <p class="stat-label">{{ stat.label }}</p>
          <div class="stat-bar">
            <div class="stat-bar-fill" :style="{ width: (stat.value > 0 ? (parseFloat(displayValues[index].toString().replace('K', '')) / (stat.value >= 1000 ? stat.value/1000 : stat.value)) * 100 : 0) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
/* 기존 스타일 전체 복사 + 한글 폰트 적용 */
/* ... 기존 StatsSection style 전체 ... */
.stats { position: relative; padding: 6rem 2rem; background: linear-gradient(135deg, rgba(0, 212, 255, 0.05) 0%, rgba(0, 153, 204, 0.02) 100%); overflow: hidden; }
.stats::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: radial-gradient(circle at 20% 50%, rgba(0, 212, 255, 0.1) 0%, transparent 50%), radial-gradient(circle at 80% 80%, rgba(0, 153, 204, 0.1) 0%, transparent 50%); pointer-events: none; }
.stats-container { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; }
.stats-title { font-size: 2.5rem; font-weight: 800; text-align: center; margin-bottom: 3rem; background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 2rem; }
.stat-card { padding: 2rem; background: linear-gradient(135deg, rgba(0, 212, 255, 0.08) 0%, rgba(0, 153, 204, 0.03) 100%); border: 1px solid rgba(0, 212, 255, 0.15); border-radius: 15px; text-align: center; transition: all 0.3s ease; position: relative; overflow: hidden; }
.stat-card::before { content: ''; position: absolute; top: 0; left: -100%; width: 100%; height: 100%; background: linear-gradient(90deg, transparent 0%, rgba(0, 212, 255, 0.1) 50%, transparent 100%); transition: left 0.5s ease; }
.stat-card:hover { border-color: rgba(0, 212, 255, 0.3); background: linear-gradient(135deg, rgba(0, 212, 255, 0.12) 0%, rgba(0, 153, 204, 0.06) 100%); transform: translateY(-5px); box-shadow: 0 15px 30px rgba(0, 212, 255, 0.1); }
.stat-card:hover::before { left: 100%; }
.stat-value { display: flex; align-items: baseline; justify-content: center; gap: 0.5rem; margin-bottom: 1rem; }
.number { font-size: 3rem; font-weight: 900; background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%); -webkit-background-clip: text; -webkit-text-fill-color: transparent; background-clip: text; }
.suffix { font-size: 1.5rem; color: #00d4ff; font-weight: 700; }
.stat-label { color: #a0a0a0; font-size: 1rem; margin-bottom: 1.5rem; font-weight: 500; }
.stat-bar { width: 100%; height: 4px; background: rgba(0, 212, 255, 0.1); border-radius: 2px; overflow: hidden; }
.stat-bar-fill { height: 100%; background: linear-gradient(90deg, #00d4ff 0%, #0099cc 100%); border-radius: 2px; transition: width 2s ease-out; box-shadow: 0 0 10px rgba(0, 212, 255, 0.5); }
@media (max-width: 768px) { .stats { padding: 4rem 1rem; } .stats-title { font-size: 1.75rem; } .stats-grid { grid-template-columns: repeat(2, 1fr); gap: 1rem; } .stat-card { padding: 1.5rem; } .number { font-size: 2rem; } .suffix { font-size: 1rem; } }
@media (max-width: 480px) { .stats-grid { grid-template-columns: 1fr; } }
</style>