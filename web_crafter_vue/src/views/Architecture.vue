<script setup>
import { Database, Server, Layers, Code2, Cpu, Network, ArrowLeftRight, ArrowDown } from 'lucide-vue-next';
import { onMounted } from 'vue'

const nodes = [
  {
    id: 'frontend',
    title: '프론트엔드',
    description: 'Vue 3 + Vite + Blockly',
    color: 'text-green-600',
    bgColor: 'bg-green-50 border-green-200',
    borderColor: 'hover:border-green-400',
    iconComponent: Code2
  },
  {
    id: 'backend',
    title: '백엔드 & AI',
    description: 'Spring Boot + Gemini API',
    color: 'text-blue-600',
    bgColor: 'bg-blue-50 border-blue-200',
    borderColor: 'hover:border-blue-400',
    iconComponent: Server
  },
  {
    id: 'database',
    title: '데이터베이스',
    description: 'MySQL',
    color: 'text-orange-600',
    bgColor: 'bg-orange-50 border-orange-200',
    borderColor: 'hover:border-orange-400',
    iconComponent: Database
  }
];

onMounted(() => {
  window.scrollTo(0, 0);
});
</script>

<template>
  <div class="w-full min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 p-6 flex items-center justify-center overflow-hidden">
    <div class="max-w-7xl mx-auto w-full">
      
      <div class="text-center mb-12">
        <div class="inline-flex items-center gap-3 mb-2">
          <Layers class="size-10 text-slate-700" />
          <h1 class="text-4xl font-extrabold text-slate-800 tracking-tight">Web Crafter 아키텍처</h1>
        </div>
        <p class="text-slate-500 font-medium text-lg">실시간 협업 웹 빌더 플랫폼 데이터 흐름도</p>
      </div>

      <div class="flex flex-col lg:flex-row items-stretch justify-center gap-4 mb-16">
        
        <template v-for="(node, index) in nodes" :key="node.id">
          
          <div 
            :class="[
              node.bgColor, 
              node.borderColor,
              'flex-1 min-w-[300px] border-2 rounded-2xl p-6 shadow-lg hover:shadow-2xl transition-all duration-300 transform hover:-translate-y-1 flex flex-col'
            ]"
          >
            <div class="flex items-center gap-4 mb-5">
              <div :class="[node.color, 'bg-white p-3 rounded-xl shadow-md']">
                <component :is="node.iconComponent" class="size-8" />
              </div>
              <div>
                <h2 class="text-2xl font-bold text-slate-800">{{ node.title }}</h2>
                <p :class="[node.color, 'font-bold text-sm']">{{ node.description }}</p>
              </div>
            </div>

            <div class="mt-auto space-y-3 bg-white/50 p-4 rounded-xl">
              <template v-if="index === 0">
                <div class="stack-item"><span class="dot bg-green-500"></span> Vue 3 (Composition)</div>
                <div class="stack-item"><span class="dot bg-green-500"></span> Google Blockly</div>
                <div class="stack-item"><span class="dot bg-green-500"></span> Pinia Store</div>
              </template>

              <template v-if="index === 1">
                <div class="stack-item"><span class="dot bg-blue-500"></span> Spring Boot</div>
                <div class="stack-item"><span class="dot bg-purple-500"></span> <span class="font-bold text-purple-700">WebSocket (Stomp)</span></div>
                <div class="stack-item"><span class="dot bg-indigo-500"></span> Gemini AI</div>
              </template>

              <template v-if="index === 2">
                <div class="stack-item"><span class="dot bg-orange-500"></span> MySQL (RDB)</div>
                <div class="stack-item"><span class="dot bg-orange-500"></span> MyBatis</div>
                <div class="stack-item"><span class="dot bg-orange-500"></span> JSON 데이터 영속화</div>
              </template>
            </div>
          </div>

          <div v-if="index < nodes.length - 1" class="flex flex-col lg:flex-row items-center justify-center py-2 lg:py-0 lg:px-2 text-slate-400">
            <div class="lg:hidden flex flex-col items-center gap-1 animate-bounce">
              <span class="text-xs font-bold text-slate-400">Flow</span>
              <ArrowDown class="size-8" />
            </div>
            
            <div class="hidden lg:flex flex-col items-center gap-1">
               <span class="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-1">
                 {{ index === 0 ? 'WebSocket / API' : 'JDBC / SQL' }}
               </span>
               <div class="flex items-center text-slate-600">
                 <div class="w-8 h-[2px] bg-slate-300"></div>
                 <div class="bg-white p-1.5 rounded-full border-2 border-slate-200 shadow-sm z-10">
                   <ArrowLeftRight class="size-5 text-slate-500" />
                 </div>
                 <div class="w-8 h-[2px] bg-slate-300"></div>
               </div>
            </div>
          </div>

        </template>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div class="layer-card hover:border-green-400 group">
          <h3 class="layer-title group-hover:text-green-600">
            <Layers class="size-5 text-green-600" /> 프레젠테이션 계층
          </h3>
          <ul class="layer-list">
            <li>• 실시간 협업 에디터 UI</li>
            <li>• Blockly 기반 시각적 코딩</li>
            <li>• 반응형 미리보기 (Iframe)</li>
          </ul>
        </div>

        <div class="layer-card hover:border-blue-400 group">
          <h3 class="layer-title group-hover:text-blue-600">
            <Layers class="size-5 text-blue-600" /> 애플리케이션 계층
          </h3>
          <ul class="layer-list">
            <li>• WebSocket 메시지 브로커</li>
            <li>• JWT 인증 및 보안</li>
            <li>• AI 코드 생성 및 변환 로직</li>
          </ul>
        </div>

        <div class="layer-card hover:border-orange-400 group">
          <h3 class="layer-title group-hover:text-orange-600">
            <Layers class="size-5 text-orange-600" /> 데이터 계층
          </h3>
          <ul class="layer-list">
            <li>• 사용자 및 프로젝트 메타데이터</li>
            <li>• 블록 상태(XML/JSON) 영속화</li>
            <li>• MyBatis 동적 쿼리 매핑</li>
          </ul>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.stack-item {
  @apply flex items-center gap-2 text-slate-700 text-sm font-medium;
}
.dot {
  @apply w-2 h-2 rounded-full;
}
.layer-card {
  @apply bg-white rounded-xl p-5 shadow-md border-2 border-slate-200 transition-colors duration-300;
}
.layer-title {
  @apply font-bold text-slate-800 mb-3 text-base flex items-center gap-2 transition-colors;
}
.layer-list {
  @apply space-y-2 text-sm text-slate-600 font-medium;
}
</style>