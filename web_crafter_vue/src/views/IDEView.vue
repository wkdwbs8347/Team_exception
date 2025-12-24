<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python' // 이름은 python이지만 HTML을 생성하게 설정할 것입니다.
import * as Ko from 'blockly/msg/ko'
import 'blockly/blocks'

// ===== 상태 관리 =====
const activeTab = ref(null)
const generatedCode = ref('')
const activeRightTab = ref('objects') // 우측 패널 탭 (objects | pages | code)
let workspace = null

// 더미 데이터 (페이지 및 오브젝트)
const pages = ref([
  { id: 'page1', name: '메인화면 (Home)' },
  { id: 'page2', name: '로그인 (Login)' }
])
const objects = ref([
  { id: 'obj1', name: '헤더 섹션', type: 'section' },
  { id: 'obj2', name: '가입 버튼', type: 'button' }
])

// ===== 카테고리 정의 =====
const categories = {
  layout:      { label: '구조',     color: '#4c97ff', icon: '📐' },
  content:     { label: '콘텐츠',   color: '#00c853', icon: '🧩' },
  style:       { label: '스타일',   color: '#ab47bc', icon: '🎨' },
  interaction: { label: '동작',     color: '#ff7043', icon: '⚡' },
  flow:        { label: '흐름',     color: '#ffab19', icon: '🔁' },
  logic:       { label: '조건',     color: '#4ca454', icon: '❓' },
  page:        { label: '페이지',   color: '#8d6e63', icon: '🗂️' },
  form:        { label: '폼',       color: '#43a047', icon: '📝' },
  data:        { label: '데이터',   color: '#26a69a', icon: '🔗' },
  responsive:  { label: '반응형',   color: '#0091ea', icon: '📱' },
  animation:   { label: '애니메이션', color: '#ff6f00', icon: '✨' },
  component:   { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  seo:         { label: 'SEO',      color: '#607d8b', icon: '🔍' },
  advanced:    { label: '고급',     color: '#424242', icon: '⚙️' }
}

// ===== 툴박스 XML (서랍 내용) =====
const toolboxXMLs = {
  layout: `<xml>
    <block type="layout_div"></block>
  </xml>`,

  content: `
  <xml>
    <block type="content_button"></block>
    <block type="content_text"></block>
    <block type="content_image"></block>
  </xml>
  `,

  

  empty: `<xml></xml>`
}





// ===== 커스텀 블록 및 제너레이터 정의 =====
const defineCustomBlocks = () => {
  // [예시 1] DIV 박스
  if (!Blockly.Blocks['layout_div']) {
    Blockly.Blocks['layout_div'] = {
      init() {
        this.appendDummyInput().appendField("📦 박스(Div)");
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#4c97ff');
      }
    };
  }
  // HTML 생성
  pythonGenerator.forBlock['layout_div'] = (block) => {
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="border:1px solid #ccc; padding:10px; margin:5px;">\n${content}</div>\n`;
  };

  // [예시 2] 버튼
  if (!Blockly.Blocks['content_button']) {
    Blockly.Blocks['content_button'] = {
      init() {
        this.appendDummyInput()
            .appendField("🆗 버튼")
            .appendField(new Blockly.FieldTextInput("클릭"), "LABEL");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }
  pythonGenerator.forBlock['content_button'] = (block) => {
    const label = block.getFieldValue('LABEL');
    return `<button class="preview-btn">${label}</button>\n`;
  };

  // [예시 3] 텍스트
  if (!Blockly.Blocks['content_text']) {
    Blockly.Blocks['content_text'] = {
      init() {
        this.appendDummyInput()
            .appendField("📝 텍스트")
            .appendField(new Blockly.FieldTextInput("내용"), "TEXT");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }
  pythonGenerator.forBlock['content_text'] = (block) => {
    const text = block.getFieldValue('TEXT');
    return `<p>${text}</p>\n`;
  };

  // [4] 이미지 (추가됨)
  if (!Blockly.Blocks['content_image']) {
  Blockly.Blocks['content_image'] = {
    init() {
      this.appendDummyInput()
          .appendField("🖼️ 이미지")
          .appendField(new Blockly.FieldTextInput("https://via.placeholder.com/150"), "SRC");
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#00c853');
    }
  }; 
}
  pythonGenerator.forBlock['content_image'] = (block) => {
    const src = block.getFieldValue('SRC');
    return `<img src="${src}" style="max-width: 100%; border-radius: 8px;" />\n`;
    };

    

}




onMounted(async () => {
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();
  await nextTick();

  const blocklyDiv = document.getElementById('blocklyDiv');

  workspace = Blockly.inject(blocklyDiv, {
    renderer: 'zelos',
    toolbox: toolboxXMLs.empty,
    move: { scrollbars: false, drag: true, wheel: true },
    zoom: { controls: true, wheel: true, startScale: 0.8 },
    grid: { spacing: 20, length: 3, colour: '#ccc', snap: true },
    trashcan: true
  });

  // 코드 변경 감지 -> 미리보기 업데이트
  workspace.addChangeListener(() => {
    try {
      // HTML 생성
      const code = pythonGenerator.workspaceToCode(workspace);
      generatedCode.value = code;
    } catch (e) {
      console.warn(e);
    }
  });

  // 리사이즈 옵저버
  new ResizeObserver(() => Blockly.svgResize(workspace)).observe(document.getElementById('workspace-area'));
});

// 사이드바 토글
const toggleCategory = (key) => {
  if (activeTab.value === key) {
    activeTab.value = null;
  } else {
    activeTab.value = key;
    workspace.updateToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
  }
}

// 페이지 추가 기능
const addPage = () => {
  const newId = `page${pages.value.length + 1}`;
  pages.value.push({ id: newId, name: `새 페이지 ${pages.value.length + 1}` });
}
</script>

<template>
  <div class="ide-container">
    <div class="ide-main">
        <aside class="entry-panel">
        
        <div class="preview-section">
          <div class="panel-title">
            <span>📱 미리보기</span>
            <button class="btn-action">💾 저장</button>
            <button class="btn-run" @click="alert(generatedCode)">🚀 배포하기</button>
            <span class="live-badge">LIVE</span>
          </div>
          <div class="browser-mockup">
            <div class="url-bar">https://web-crafter.app/preview</div>
            <iframe id="previewFrame" :srcdoc="generatedCode || '<h3>블록을 조립해보세요!</h3>'" frameborder="0"></iframe>
          </div>
        </div>
        <div class="manager-section">
          <div class="manager-tabs">
            <button :class="{ active: activeRightTab === 'objects' }" @click="activeRightTab = 'objects'">📦 객체</button>
            <button :class="{ active: activeRightTab === 'pages' }" @click="activeRightTab = 'pages'">🗂️ 페이지</button>
            <button :class="{ active: activeRightTab === 'code' }" @click="activeRightTab = 'code'">💻 코드</button>
          </div>
          <div v-if="activeRightTab === 'pages'" class="tab-content">
            <div class="list-header">
              <span>총 {{ pages.length }}개</span>
              <button class="btn-add-mini" @click="addPage">➕ 추가</button>
            </div>
            <ul class="item-list">
              <li v-for="page in pages" :key="page.id" class="list-item">
                <span class="item-icon">📄</span>
                <span class="item-name">{{ page.name }}</span>
                <button class="btn-del">✕</button>
              </li>
            </ul>
          </div>
          <div v-if="activeRightTab === 'objects'" class="tab-content">
            <div class="empty-msg" v-if="objects.length === 0">배치된 요소가 없습니다.</div>
            <ul class="item-list" v-else>
              <li v-for="obj in objects" :key="obj.id" class="list-item">
                <span class="item-icon">💠</span>
                <span class="item-name">{{ obj.name }}</span>
                <span class="item-type">{{ obj.type }}</span>
              </li>
            </ul>
          </div>
          <div v-if="activeRightTab === 'code'" class="tab-content code-view">
            <pre>{{ generatedCode }}</pre>
          </div>
        </div>
      </aside>
      <nav class="sidebar">
        <div v-for="(cat, key) in categories" :key="key"
             class="cat-item" :class="{ active: activeTab === key }"
             @click="toggleCategory(key)">
          <div class="icon">{{ cat.icon }}</div>
          <div class="label">{{ cat.label }}</div>
          <div class="indicator" :style="{ backgroundColor: cat.color }"></div>
        </div>
      </nav>
      <div id="workspace-area" class="workspace-wrapper" :class="{ 'drawer-open': activeTab }">
        <div id="blocklyDiv"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 전체 레이아웃 */
.ide-container {
  padding-top: 81px; /* 헤더 높이 */
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;
  overflow: hidden;
}

.ide-header {
  position: fixed; top: 0; left: 0; right: 0; height: 50px;
  background: #2a2d3e; color: white;
  display: flex; justify-content: space-between; align-items: center;
  padding: 0 1rem; z-index: 100;
}
.header-controls { display: flex; gap: 10px; }
.btn-run { background: #4c97ff; border: none; padding: 6px 12px; color: white; border-radius: 4px; cursor: pointer; font-weight: bold;}

.ide-main {
  display: flex;
  flex: 1;
  height: 100%;
  overflow: hidden;
}

/* 1. 좌측 사이드바 */
.sidebar {
  width: 70px;
  background: #1a1a2e;
  display: flex; flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid #000;
  overflow-y: auto;
  z-index: 20;
}
.sidebar::-webkit-scrollbar { width: 0px; } /* 스크롤바 숨김 */

.cat-item {
  height: 70px; display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #777; cursor: pointer; position: relative; border-bottom: 1px solid #252535;
}
.cat-item:hover { background: #252535; color: white; }
.cat-item.active { background: #202030; color: white; }
.cat-item .icon { font-size: 1.6rem; margin-bottom: 4px; }
.cat-item .label { font-size: 0.7rem; }
.indicator { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; display: none; }
.cat-item.active .indicator { display: block; }

/* 2. 중앙 워크스페이스 */
.workspace-wrapper {
  flex: 1;
  position: relative;
  background: #fff;
  transition: all 0.3s ease;
}
#blocklyDiv { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }

/* Blockly 서랍 애니메이션 */
/* =========================================
   [수정 완료] 서랍만 왼쪽으로 들어갔다 나오기
   ========================================= */

/* 1. 서랍(Toolbox) 기본 상태 */
:deep(.blocklyToolboxDiv) {
  background-color: #f9f9f9;
  border-right: 1px solid #ddd;
  
  /* 너비, 투명도, 변형을 모두 부드럽게 */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  transform-origin: left top;
  
  /* 열렸을 때는 내용물에 맞게 표시 */
  width: auto !important;
  max-width: 300px; /* 최대 너비 제한 */
  min-width: 50px;  /* 열렸을 땐 최소 너비 유지 */
  
  opacity: 1;
  overflow: hidden;
  white-space: nowrap;
  display: block !important;
  transform: scaleX(1);
}

/* 2. 서랍이 닫혔을 때 (activeTab이 없을 때) */
.workspace-wrapper:not(.drawer-open) :deep(.blocklyToolboxDiv) {
  /* [핵심 해결책] 최소 너비까지 0으로 죽여야 닫힙니다! */
  min-width: 0px !important;
  width: 0px !important;
  max-width: 0px !important;
  
  /* 왼쪽으로 접히는 효과 */
  transform: scaleX(0);
  
  /* 찌꺼기 제거 */
  padding: 0 !important;
  border: none !important;
  opacity: 0;
  pointer-events: none;
}

/* 3. 우측 엔트리 패널 (핵심 변경) */
.entry-panel {
  width: 360px; /* 고정 너비 */
  background: #f5f5f5;
  border-left: 1px solid #ddd;
  display: flex; flex-direction: column;
  flex-shrink: 0;
  z-index: 30;
}

/* (A) 미리보기 섹션 (상단 50%) */
.preview-section {
  flex: 1;
  background: #e0e0e0;
  padding: 10px;
  display: flex; flex-direction: column;
  border-bottom: 1px solid #ccc;
}
.panel-title { font-weight: bold; margin-bottom: 8px; font-size: 0.9rem; display: flex; justify-content: space-between; }
.live-badge { background: red; color: white; font-size: 0.6rem; padding: 2px 6px; border-radius: 4px; animation: pulse 2s infinite; }

.browser-mockup {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0,0,0,0.1);
  display: flex; flex-direction: column;
}
.url-bar { background: #f1f3f4; padding: 5px 10px; font-size: 0.7rem; color: #555; border-bottom: 1px solid #ddd; }
iframe { flex: 1; width: 100%; height: 100%; border: none; background: white; }

/* (B) 관리자 섹션 (하단 50%) */
.manager-section {
  height: 50%; /* 하단 절반 차지 */
  display: flex; flex-direction: column;
  background: white;
}

.manager-tabs {
  display: flex; background: #eee; border-bottom: 1px solid #ddd;
}
.manager-tabs button {
  flex: 1; padding: 10px; border: none; background: transparent; cursor: pointer; font-size: 0.85rem; border-bottom: 3px solid transparent;
}
.manager-tabs button.active { background: white; border-bottom-color: #4c97ff; font-weight: bold; color: #4c97ff; }

.tab-content { flex: 1; overflow-y: auto; padding: 10px; }

/* 리스트 스타일 */
.list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 0.8rem; color: #666; }
.btn-add-mini { background: #4c97ff; color: white; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 0.7rem; }

.item-list { list-style: none; padding: 0; margin: 0; }
.list-item {
  display: flex; align-items: center; padding: 8px; background: #f9f9f9; 
  border: 1px solid #eee; margin-bottom: 5px; border-radius: 4px; cursor: pointer;
}
.list-item:hover { background: #f0f7ff; border-color: #cce5ff; }
.item-icon { margin-right: 8px; font-size: 1.1rem; }
.item-name { flex: 1; font-size: 0.85rem; font-weight: 500; }
.item-type { font-size: 0.7rem; color: #999; margin-right: 5px; }
.btn-del { background: none; border: none; color: #ccc; cursor: pointer; }
.btn-del:hover { color: red; }

.code-view pre { margin: 0; font-family: monospace; font-size: 0.8rem; white-space: pre-wrap; color: #333; }

@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
</style>