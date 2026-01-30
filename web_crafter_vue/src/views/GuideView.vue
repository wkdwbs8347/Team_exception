<script setup>
import { useRouter } from 'vue-router'; // 👈 [추가] 라우터 사용
import { 
  Layout, Box, Layers, 
  Type, Image, MousePointer2, 
  Palette, PaintBucket, Scaling, 
  ArrowRight, CheckCircle2, HelpCircle,
  Zap // 👈 [추가] 고급 가이드 아이콘
} from 'lucide-vue-next';

const router = useRouter(); // 👈 [추가]
</script>

<template>
  <div class="guide-wrapper">
    <header class="guide-header">
      <div class="header-content">
        <h1 class="title">Web Crafter 가이드</h1>
        <p class="subtitle">블록을 조립하고, 이름을 붙이고, 꾸며보세요! 코딩 없이 나만의 웹사이트를 만드는 가장 쉬운 방법입니다.</p>
      </div>
    </header>

    <main class="guide-body">
      
      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><Layers :size="24" /></span>
          1. 블록의 3가지 역할
        </h2>
        <p class="section-desc">모든 블록은 딱 세 가지 색깔(역할)로 나뉩니다. 이 세 가지만 기억하세요!</p>

        <div class="card-grid">
          <div class="guide-card structure">
            <div class="card-icon"><Layout :size="32" /></div>
            <h3>구조 (Layout)</h3>
            <p class="role">웹페이지의 <strong>뼈대</strong>를 만듭니다.</p>
            <ul class="example-list">
              <li><Box :size="14" /> <strong>박스:</strong> 내용을 담는 네모 상자</li>
              <li><Layout :size="14" /> <strong>전체 영역:</strong> 화면 전체 배경</li>
              <li><Layers :size="14" /> <strong>리스트:</strong> 목록 만들기</li>
            </ul>
            <div class="tip-box">🏗️ 다른 블록들을 담는 '그릇' 역할이에요.</div>
          </div>

          <div class="guide-card content">
            <div class="card-icon"><Type :size="32" /></div>
            <h3>콘텐츠 (Content)</h3>
            <p class="role">화면에 보이는 <strong>실제 내용물</strong>입니다.</p>
            <ul class="example-list">
              <li><Type :size="14" /> <strong>제목/텍스트:</strong> 글자 넣기</li>
              <li><MousePointer2 :size="14" /> <strong>버튼:</strong> 클릭 가능한 버튼</li>
              <li><Image :size="14" /> <strong>이미지:</strong> 사진 넣기</li>
            </ul>
            <div class="tip-box">📦 박스(구조) 안에 쏙 넣어주세요.</div>
          </div>

          <div class="guide-card style">
            <div class="card-icon"><Palette :size="32" /></div>
            <h3>스타일 (Style)</h3>
            <p class="role">요소를 예쁘게 <strong>꾸며줍니다.</strong></p>
            <ul class="example-list">
              <li><PaintBucket :size="14" /> <strong>색상:</strong> 배경색, 글자색</li>
              <li><Scaling :size="14" /> <strong>크기:</strong> 너비, 높이 조절</li>
              <li><Palette :size="14" /> <strong>여백:</strong> 간격 띄우기</li>
            </ul>
            <div class="tip-box">🎨 화면에 안 보여요! 꾸미기 전용입니다.</div>
          </div>
        </div>
      </section>

      <div class="divider"></div>

      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><CheckCircle2 :size="24" /></span>
          2. 3단계로 만드는 법
        </h2>

        <div class="steps-container">
          <div class="step-item">
            <div class="step-num">STEP 1</div>
            <div class="step-content">
              <h3>뼈대와 내용 조립하기</h3>
              <p>레고처럼 블록을 연결하세요. <br><strong>박스 블록</strong> 안에 <strong>이미지</strong>나 <strong>글자</strong>를 끼워 넣으면 됩니다.</p>
            </div>
            <div class="step-visual">
              <div class="mock-block layout">박스 (Layout)</div>
              <div class="arrow">⬇️ 내부 연결</div>
              <div class="mock-block content">버튼 (Content)</div>
            </div>
          </div>

          <div class="step-arrow"><ArrowRight :size="32" /></div>

          <div class="step-item highlight">
            <div class="step-num">STEP 2</div>
            <div class="step-content">
              <h3>이름표 붙이기 (중요! ⭐)</h3>
              <p>꾸며주려면 누구인지 알아야겠죠?<br>블록 설정창에서 <strong>영어 이름(ID)</strong>을 꼭 지어주세요.</p>
              <div class="name-example">
                예: <span class="code">login_btn</span>, <span class="code">my_card</span>
              </div>
            </div>
          </div>

          <div class="step-arrow"><ArrowRight :size="32" /></div>

          <div class="step-item">
            <div class="step-num">STEP 3</div>
            <div class="step-content">
              <h3>스타일 입히기</h3>
              <p><strong>스타일 태그</strong> 블록을 꺼내고, 아까 지은 <strong>이름(ID)</strong>을 적으세요.</p>
              <p>그 안에 색상, 크기 블록을 넣으면 적용됩니다!</p>
            </div>
            <div class="step-visual">
              <div class="mock-block style">
                스타일 태그 (Selector: <span class="code">login_btn</span>)
                <div class="mock-block inner">배경색: 파랑</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <div class="divider"></div>

      <section class="guide-section">
        <h2 class="section-title">
          <span class="icon-box"><HelpCircle :size="24" /></span>
          3. 자주 묻는 질문 & 꿀팁
        </h2>
        
        <div class="faq-grid">
          <div class="faq-item">
            <div class="question">Q. 화면 정중앙에 놓고 싶어요!</div>
            <div class="answer">
              감싸고 있는 <strong>부모 박스</strong>에 스타일을 주세요.<br>
              <strong>Flex 컨테이너</strong> 블록을 넣고 <span class="highlight-text">Justify: Center</span>, <span class="highlight-text">Align: Center</span>로 설정하면 마법처럼 가운데로 옵니다! 🎩
            </div>
          </div>

          <div class="faq-item">
            <div class="question">Q. 버튼 3개를 다 다르게 꾸미고 싶어요.</div>
            <div class="answer">
              각각 다른 이름이 필요합니다!<br>
              버튼1(<span class="code">btn1</span>), 버튼2(<span class="code">btn2</span>) 처럼 이름을 따로 짓고,<br>
              스타일 태그도 <strong>각각 따로 만들어야</strong> 합니다. (1:1 규칙)
            </div>
          </div>

          <div class="faq-item">
            <div class="question">Q. 여백(Padding vs Margin) 차이가 뭐예요?</div>
            <div class="answer">
              <ul>
                <li><strong>Padding:</strong> 패딩 입으면 내가 뚱뚱해지죠? <strong>박스 안쪽</strong> 여백입니다.</li>
                <li><strong>Margin:</strong> 사회적 거리두기! <strong>박스 바깥쪽</strong>(다른 요소와의) 간격입니다.</li>
              </ul>
            </div>
          </div>
        </div>
      </section>

      <div class="divider"></div>

      <section class="next-step-section" @click="router.push('/guide/advanced')">
        <div class="next-step-card">
          <div class="next-step-content">
            <h2 class="next-title">
              <Zap :size="28" class="zap-icon" /> 
              더 강력한 기능을 원하시나요?
            </h2>
            <p class="next-desc">자바스크립트 블록으로 <strong>움직이고 반응하는 웹사이트</strong>를 만드는 법을 배워보세요.</p>
          </div>
          <div class="next-action">
            <span>고급 가이드 (Logic) 보러가기</span>
            <ArrowRight :size="20" />
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

/* ... (기존 스타일 그대로 유지) ... */
/* ... (위에 보내드린 스타일 코드를 그대로 두시면 됩니다) ... */

/* 헤더 */
.guide-header {
  background: linear-gradient(180deg, rgba(13, 31, 60, 0.8) 0%, rgba(10, 22, 40, 0) 100%);
  padding: 3rem 2rem;
  text-align: center;
  border-bottom: 1px solid rgba(0, 217, 255, 0.1);
}
.title {
  font-size: 2.5rem;
  font-weight: 800;
  margin-bottom: 0.5rem;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}
.subtitle { color: #94a3b8; font-size: 1.1rem; }
.guide-body { max-width: 1200px; margin: 0 auto; padding: 2rem; }
.guide-section { margin-bottom: 3rem; }
.section-title { font-size: 1.5rem; font-weight: 700; margin-bottom: 1.5rem; display: flex; align-items: center; gap: 10px; color: #fff; }
.icon-box { color: #00d4ff; display: flex; align-items: center; }
.section-desc { margin-bottom: 1.5rem; color: #94a3b8; }
.card-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; }
.guide-card { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.08); border-radius: 16px; padding: 2rem; transition: transform 0.2s, border-color 0.2s; position: relative; overflow: hidden; }
.guide-card:hover { transform: translateY(-5px); }
.structure { border-top: 4px solid #4caf50; } 
.structure .card-icon { color: #4caf50; background: rgba(76, 175, 80, 0.1); }
.content { border-top: 4px solid #ff9800; } 
.content .card-icon { color: #ff9800; background: rgba(255, 152, 0, 0.1); }
.style { border-top: 4px solid #e91e63; } 
.style .card-icon { color: #e91e63; background: rgba(233, 30, 99, 0.1); }
.card-icon { width: 60px; height: 60px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 1rem; }
.guide-card h3 { font-size: 1.25rem; margin-bottom: 0.5rem; color: #fff; }
.role { color: #cbd5e1; margin-bottom: 1rem; height: 3em;}
.example-list { list-style: none; padding: 0; margin-bottom: 1.5rem; }
.example-list li { display: flex; align-items: center; gap: 8px; margin-bottom: 0.5rem; color: #94a3b8; font-size: 0.9rem; }
.tip-box { background: rgba(0, 0, 0, 0.3); padding: 0.8rem; border-radius: 8px; font-size: 0.85rem; color: #ddd; }
.steps-container { display: flex; align-items: center; justify-content: space-between; gap: 1rem; flex-wrap: wrap; }
.step-item { flex: 1; min-width: 280px; background: rgba(255, 255, 255, 0.02); border-radius: 16px; padding: 1.5rem; border: 1px solid rgba(255, 255, 255, 0.05); height: 280px; display: flex; flex-direction: column; }
.step-item.highlight { border-color: #00d4ff; background: rgba(0, 212, 255, 0.05); box-shadow: 0 0 15px rgba(0, 212, 255, 0.1); }
.step-num { font-size: 0.8rem; font-weight: 800; color: #00d4ff; letter-spacing: 0.1em; margin-bottom: 0.5rem; }
.step-content h3 { font-size: 1.1rem; margin-bottom: 0.5rem; color: #fff; }
.step-content p { font-size: 0.9rem; color: #94a3b8; line-height: 1.5; }
.step-arrow { color: #475569; display: flex; align-items: center; }
.mock-block { margin-top: auto; padding: 8px; border-radius: 4px; font-size: 0.8rem; text-align: center; font-weight: 700; color: #fff; width: 100%; }
.mock-block.layout { background: #4caf50; margin-bottom: 4px; }
.mock-block.content { background: #ff9800; }
.mock-block.style { background: #e91e63; text-align: left; }
.mock-block.inner { background: #c2185b; margin-top: 4px; font-size: 0.7rem; }
.arrow { font-size: 0.8rem; color: #aaa; text-align: center; margin: 2px 0; }
.code { background: #1e293b; color: #00d4ff; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
.faq-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.faq-item { background: rgba(255, 255, 255, 0.03); padding: 1.5rem; border-radius: 12px; }
.question { font-weight: 700; font-size: 1.05rem; color: #fff; margin-bottom: 0.8rem; display: flex; align-items: center; gap: 8px; }
.answer { color: #94a3b8; font-size: 0.95rem; line-height: 1.6; }
.highlight-text { color: #ffeb3b; font-weight: bold; }
.answer ul { padding-left: 1.2rem; margin: 0; }
.divider { height: 1px; background: rgba(255, 255, 255, 0.1); margin: 3rem 0; }
@media (max-width: 900px) { .steps-container { flex-direction: column; } .step-arrow { transform: rotate(90deg); margin: 10px 0; } .faq-grid { grid-template-columns: 1fr; } }

/* 🔥 [신규 스타일] 고급 가이드 버튼 스타일 */
.next-step-section {
  margin-top: 4rem;
  cursor: pointer;
}

.next-step-card {
  background: linear-gradient(135deg, rgba(20, 10, 40, 0.6) 0%, rgba(139, 92, 246, 0.15) 100%); /* 보라색 계열 */
  border: 1px solid rgba(139, 92, 246, 0.3);
  border-radius: 20px;
  padding: 3rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.next-step-card:hover {
  transform: translateY(-5px);
  border-color: rgba(139, 92, 246, 0.6);
  box-shadow: 0 10px 30px rgba(139, 92, 246, 0.2);
}

.next-step-content {
  z-index: 1;
}

.next-title {
  font-size: 1.8rem;
  font-weight: 800;
  color: #fff;
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 12px;
}

.zap-icon {
  color: #a78bfa; /* 밝은 보라색 */
  fill: rgba(167, 139, 250, 0.2);
}

.next-desc {
  font-size: 1.1rem;
  color: #d8b4fe;
}

.next-action {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #8b5cf6;
  color: #fff;
  padding: 12px 24px;
  border-radius: 50px;
  font-weight: 700;
  transition: all 0.3s ease;
}

.next-step-card:hover .next-action {
  background: #a78bfa;
  padding-right: 30px; /* 화살표 애니메이션 공간 확보 */
}

@media (max-width: 768px) {
  .next-step-card {
    flex-direction: column;
    text-align: center;
    gap: 2rem;
    padding: 2rem;
  }
  .next-title { justify-content: center; font-size: 1.5rem; }
}
</style>