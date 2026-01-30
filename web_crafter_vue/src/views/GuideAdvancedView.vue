<script setup>
import { ref } from 'vue';
import { 
  Zap, MousePointerClick, MessageSquare, // 이벤트 & 액션
  GitBranch, RefreshCcw, Database, // 제어 & 변수
  Play, Terminal, Code2, // 실행 관련
  ArrowRight, Lightbulb
} from 'lucide-vue-next';
</script>

<template>
  <div class="guide-wrapper">
    <header class="guide-header">
      <div class="header-content">
        <h1 class="title">고급 가이드: 인터랙션 & 로직</h1>
        <p class="subtitle">
          클릭하면 반응하고, 계산하고, 움직이는 웹사이트! <br>
          <strong>자바스크립트(JS) 블록</strong>을 사용하여 웹사이트에 생명을 불어넣으세요.
        </p>
      </div>
    </header>

    <main class="guide-body">
      
      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><Zap :size="24" /></span>
          1. 움직이는 웹의 원리 (Event & Action)
        </h2>
        <p class="section-desc">모든 로직은 <strong>"언제(When)"</strong>와 <strong>"무엇을(What)"</strong>의 결합입니다.</p>

        <div class="flow-container">
          <div class="flow-card">
            <div class="flow-icon"><MousePointerClick :size="32" /></div>
            <h3>1. 이벤트 (Trigger)</h3>
            <p>"언제 실행할까?"</p>
            <div class="code-badge">버튼을 클릭했을 때</div>
          </div>

          <div class="flow-arrow"><ArrowRight :size="32" /></div>

          <div class="flow-card highlight">
            <div class="flow-icon"><Code2 :size="32" /></div>
            <h3>2. 로직 (Logic)</h3>
            <p>"무슨 일을 할까?"</p>
            <div class="code-badge">경고창 띄우기 / 글자 바꾸기</div>
          </div>
        </div>
      </section>

      <div class="divider"></div>

      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><Terminal :size="24" /></span>
          2. 로직 블록 살펴보기
        </h2>
        
        <div class="card-grid">
          <div class="guide-card event">
            <div class="card-icon"><Zap :size="32" /></div>
            <h3>이벤트 (Events)</h3>
            <p class="role">로직을 <strong>시작</strong>하는 방아쇠입니다.</p>
            <ul class="example-list">
              <li><MousePointerClick :size="14" /> <strong>클릭 시 (On Click):</strong> 요소를 눌렀을 때</li>
              <li><Play :size="14" /> <strong>페이지 로드 (On Load):</strong> 화면이 켜지자마자</li>
              <li><RefreshCcw :size="14" /> <strong>마우스 오버:</strong> 마우스를 올렸을 때</li>
            </ul>
          </div>

          <div class="guide-card control">
            <div class="card-icon"><GitBranch :size="32" /></div>
            <h3>제어 & 판단 (Control)</h3>
            <p class="role">상황에 따라 <strong>결정</strong>을 내립니다.</p>
            <ul class="example-list">
              <li><GitBranch :size="14" /> <strong>만약 (If/Else):</strong> 조건이 맞으면 실행</li>
              <li><RefreshCcw :size="14" /> <strong>반복 (Loop):</strong> 여러 번 실행하기</li>
              <li><Database :size="14" /> <strong>비교 (=, >):</strong> 값이 같은지 확인</li>
            </ul>
          </div>

          <div class="guide-card action">
            <div class="card-icon"><MessageSquare :size="32" /></div>
            <h3>액션 (Actions)</h3>
            <p class="role">실제로 <strong>변화</strong>를 만듭니다.</p>
            <ul class="example-list">
              <li><MessageSquare :size="14" /> <strong>알림 (Alert):</strong> 팝업창 띄우기</li>
              <li><Terminal :size="14" /> <strong>로그 (Console):</strong> 개발자 도구에 기록</li>
              <li><Code2 :size="14" /> <strong>요소 변경:</strong> 글자나 색상 바꾸기</li>
            </ul>
          </div>
        </div>
      </section>

      <div class="divider"></div>

      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><Lightbulb :size="24" /></span>
          3. 따라해보기: "인사하는 버튼" 만들기
        </h2>
        <p class="section-desc">버튼을 누르면 "환영합니다!"라는 알림창이 뜨게 만들어봅시다.</p>

        <div class="tutorial-box">
          <div class="tutorial-step">
            <span class="step-badge">STEP 1</span>
            <p><strong>[이벤트]</strong> 탭에서 <code>요소를 클릭했을 때</code> 블록을 꺼냅니다.</p>
          </div>
          <div class="tutorial-step">
            <span class="step-badge">STEP 2</span>
            <p>블록의 ID 입력칸에 버튼의 이름(예: <code>login_btn</code>)을 적습니다.</p>
          </div>
          <div class="tutorial-step">
            <span class="step-badge">STEP 3</span>
            <p><strong>[액션]</strong> 탭에서 <code>알림창 띄우기(Alert)</code> 블록을 꺼내서 <strong>안쪽에 끼워 넣습니다.</strong></p>
          </div>
          <div class="tutorial-step last">
            <span class="step-badge">STEP 4</span>
            <p>내용 칸에 <code>"환영합니다!"</code>라고 적으면 끝! 🎉</p>
          </div>

          <div class="block-visual">
            <div class="visual-block event">
              👇 클릭 이벤트 (ID: <span class="code">login_btn</span>)
              <div class="visual-block action">
                📢 알림창 띄우기 ("환영합니다!")
              </div>
            </div>
          </div>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
.guide-wrapper {
  padding-top: 76px;
  min-height: 100vh;
  background: #0a1628;
  color: #e0e6ed;
  font-family: 'Inter', sans-serif;
  padding-bottom: 4rem;
}

/* 헤더 (보라색 테마) */
.guide-header {
  background: linear-gradient(180deg, rgba(20, 10, 40, 0.8) 0%, rgba(10, 22, 40, 0) 100%);
  padding: 3rem 2rem;
  text-align: center;
  border-bottom: 1px solid rgba(139, 92, 246, 0.2); /* 보라색 라인 */
}
.title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, #a78bfa 0%, #d8b4fe 100%); /* 보라색 그라데이션 */
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.subtitle { color: #cbd5e1; font-size: 1.1rem; line-height: 1.6; }

/* 공통 레이아웃 */
.guide-body { max-width: 1200px; margin: 0 auto; padding: 2rem; }
.guide-section { margin-bottom: 4rem; }
.section-title {
  font-size: 1.5rem; font-weight: 700; margin-bottom: 1.5rem;
  display: flex; align-items: center; gap: 10px; color: #fff;
}
.icon-box { color: #a78bfa; display: flex; align-items: center; } /* 아이콘 보라색 */
.section-desc { margin-bottom: 2rem; color: #94a3b8; font-size: 1.05rem; }

/* 1. 플로우 차트 스타일 */
.flow-container {
  display: flex; align-items: center; justify-content: center; gap: 2rem; flex-wrap: wrap;
  background: rgba(255, 255, 255, 0.02); padding: 2rem; border-radius: 20px;
}
.flow-card {
  background: #1e1e2e; padding: 2rem; border-radius: 16px; text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1); width: 280px;
  transition: transform 0.3s;
}
.flow-card.highlight {
  border-color: #a78bfa; background: rgba(139, 92, 246, 0.1);
  box-shadow: 0 0 20px rgba(139, 92, 246, 0.2);
}
.flow-icon { color: #a78bfa; margin-bottom: 1rem; display: inline-block; }
.flow-card h3 { color: #fff; margin-bottom: 0.5rem; font-size: 1.2rem; }
.code-badge {
  background: #0f172a; color: #38bdf8; padding: 6px 12px; border-radius: 6px;
  font-family: monospace; font-size: 0.9rem; margin-top: 1rem; display: inline-block;
}
.flow-arrow { color: #64748b; }

/* 2. 카드 그리드 */
.card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.guide-card {
  background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px; padding: 2rem; transition: transform 0.2s;
}
.guide-card:hover { transform: translateY(-5px); }

/* 카드별 테마 */
.event { border-top: 4px solid #fbbf24; } /* 노랑 (번개) */
.event .card-icon { color: #fbbf24; background: rgba(251, 191, 36, 0.1); }

.control { border-top: 4px solid #38bdf8; } /* 파랑 (제어) */
.control .card-icon { color: #38bdf8; background: rgba(56, 189, 248, 0.1); }

.action { border-top: 4px solid #f472b6; } /* 분홍 (액션) */
.action .card-icon { color: #f472b6; background: rgba(244, 114, 182, 0.1); }

.card-icon { width: 50px; height: 50px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 1rem; }
.guide-card h3 { font-size: 1.25rem; margin-bottom: 0.5rem; color: #fff; }
.role { color: #cbd5e1; margin-bottom: 1.5rem; height: 2.5em; }
.example-list { list-style: none; padding: 0; }
.example-list li { display: flex; align-items: center; gap: 8px; margin-bottom: 0.8rem; color: #94a3b8; font-size: 0.95rem; }

/* 3. 튜토리얼 박스 */
.tutorial-box {
  background: #111827; border: 1px solid rgba(255,255,255,0.1); border-radius: 16px; padding: 2rem;
}
.tutorial-step {
  display: flex; align-items: center; gap: 1rem; margin-bottom: 1.5rem;
  position: relative;
}
.tutorial-step:not(.last)::after {
  content: ''; position: absolute; left: 16px; top: 32px; bottom: -20px;
  width: 2px; background: rgba(255,255,255,0.1);
}
.step-badge {
  background: #334155; color: #fff; font-size: 0.75rem; font-weight: 800;
  padding: 4px 8px; border-radius: 6px; width: 60px; text-align: center; flex-shrink: 0;
}
.tutorial-step p { color: #e2e8f0; margin: 0; }
.tutorial-step code { background: #1e293b; color: #a78bfa; padding: 2px 6px; border-radius: 4px; font-family: monospace; }

/* 블록 시각화 */
.block-visual {
  margin-top: 2rem; padding: 2rem; background: #0f172a; border-radius: 12px;
  display: flex; justify-content: center;
}
.visual-block {
  padding: 10px 15px; border-radius: 8px; font-weight: 700; color: #fff;
  font-size: 0.9rem; box-shadow: 0 4px 6px rgba(0,0,0,0.3);
}
.visual-block.event { background: #d97706; /* 진한 노랑 */ }
.visual-block.action { background: #db2777; /* 진한 분홍 */ margin-top: 8px; margin-left: 15px; }
.code { color: #fbbf24; background: rgba(0,0,0,0.2); padding: 0 4px; border-radius: 4px; }

.divider { height: 1px; background: rgba(255, 255, 255, 0.1); margin: 3rem 0; }

@media (max-width: 768px) {
  .flow-container { flex-direction: column; gap: 1rem; }
  .flow-arrow { transform: rotate(90deg); margin: 0.5rem 0; }
}
</style>