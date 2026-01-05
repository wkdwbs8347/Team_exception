<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue';
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import * as Ko from 'blockly/msg/ko';
import 'blockly/blocks';

// ===== 카테고리 블록 import =====
//block 컴포넌트 블록
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
//js 컴포넌트 블록
import * as Interaction from '@/components/js/Interaction.vue';
import * as Flow from '@/components/js/Flow.vue';
import * as Logic from '@/components/js/Logic.vue';
//style 컴포넌트 블록
import * as Style from '@/components/style/Style.vue';
import * as Responsive from '@/components/style/Responsive.vue';
import * as Color from '@/components/style/Color.vue';
import * as Flex from '@/components/style/Flex.vue';

// ===== 상태 관리 =====
const activeParent = ref('structure'); // 초기값: 구조
const activeTab = ref(null);
const generatedCode = ref('');
const previewSrc = ref('');
const activeRightTab = ref('objects');
const isRunning = ref(false);
const isPhone = ref(false);
let workspace = null;

// [AI 관련 상태]
const showAiModal = ref(false);
const aiPrompt = ref('');
const isGenerating = ref(false);
const aiPromptError = ref(false);

// 페이지 및 객체 상태
const generateUniquePageId = () => {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `page-${crypto.randomUUID()}`;
    }
  } catch (e) {}
  return `page-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
};

const pages = ref([
  { id: generateUniquePageId(), name: '메인화면 (Home)', xml: '<xml></xml>' },
  { id: generateUniquePageId(), name: '로그인 (Login)', xml: '<xml></xml>' },
]);
const selectedPageId = ref(pages.value[0].id);
const objects = ref([]);

const editingPageId = ref(null);
const editingPageName = ref('');

// 로컬 스토리지
const loadPagesFromStorage = () => {
  try {
    return JSON.parse(localStorage.getItem('wc_pages'));
  } catch (e) {
    return null;
  }
};
const savePagesToStorage = () => {
  try {
    localStorage.setItem('wc_pages', JSON.stringify(pages.value));
  } catch (e) {}
};

watch(
  objects,
  (newObjects) => {
    if (Interaction.updateObjectList) Interaction.updateObjectList(newObjects);
  },
  { deep: true, immediate: true }
);

// 객체 업데이트
const updateObjectListFromWorkspace = () => {
  if (!workspace) return;
  const currentObjects = [];
  const blocks = workspace.getAllBlocks(false);
  const ignoredTypes = new Set([
    'event_click',
    'event_page_load',
    'action_alert',
  ]);

  blocks.forEach((block) => {
    let name = '';
    let type = block.type;
    if (ignoredTypes.has(block.type)) return;
    if (block.type.startsWith('style_')) return;

    const nameField = block.getFieldValue && block.getFieldValue('NAME');
    if (nameField) {
      name = nameField;
    } else {
      switch (block.type) {
        case 'layout_div':
          name = block.getFieldValue('ELEMENT_CLASS')
            ? `DIV (${block.getFieldValue('ELEMENT_CLASS')})`
            : 'DIV';
          break;
        case 'content_button':
          name = `버튼 (${block.getFieldValue('LABEL')})`;
          break;
        case 'content_text':
          name = `텍스트 (${(block.getFieldValue('TEXT') || '').substring(0, 10)}...)`;
          break;
        case 'content_image':
          name = `이미지 (${(block.getFieldValue('SRC') || '').substring(0, 15)}...)`;
          break;
        default:
          name = block.type;
          break;
      }
    }
    currentObjects.push({ id: block.id, name: name, type: type });
  });
  objects.value = currentObjects;
  if (Interaction.updateObjectList)
    Interaction.updateObjectList(currentObjects);
};

// ===== 카테고리 정의 =====
const categories = {
  layout: Layout.category,
  content: Content.category,
  form: Form.category,
  component: { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  style: Style.category,
  color: Color.category,
  flex: Flex.category,
  responsive: Responsive.category,
  animation: { label: '애니메이션', color: '#ff6f00', icon: '✨' },
  interaction: Interaction.category,
  flow: Flow.category,
  logic: Logic.category,
  data: { label: '데이터', color: '#26a69a', icon: '💾' },
  seo: { label: 'SEO', color: '#607d8b', icon: '🔍' },
  advanced: { label: '고급', color: '#424242', icon: '⚙️' },
};

const categoryGroups = [
  {
    id: 'structure',
    label: '화면 구성',
    icon: '🏗️',
    color: '#4caf50',
    items: ['layout', 'content', 'form', 'component'],
  },
  {
    id: 'style',
    label: '디자인',
    icon: '🎨',
    color: '#e91e63',
    items: ['style', 'color', 'responsive', 'animation', 'flex'],
  },
  {
    id: 'logic',
    label: '로직/데이터',
    icon: '⚡',
    color: '#2196f3',
    items: ['interaction', 'flow', 'logic', 'data', 'seo', 'advanced'],
  },
];

const currentSubItems = computed(() => {
  const group = categoryGroups.find((g) => g.id === activeParent.value);
  return group ? group.items : [];
});

const toolboxXMLs = {
  layout: Layout.toolbox,
  content: Content.toolbox,
  style: Style.toolbox,
  color: Color.toolbox,
  flex: Flex.toolbox,
  interaction: Interaction.toolbox,
  flow: Flow.toolbox,
  logic: Logic.toolbox,
  form: Form.toolbox,
  responsive: Responsive.toolbox,
  empty: `<xml></xml>`,
};

const defineCustomBlocks = () => {
  Layout.defineBlocks();
  Content.defineBlocks();
  Style.defineBlocks();
  Color.defineBlocks();
  Flex.defineBlocks();
  Interaction.defineBlocks();
  Flow.defineBlocks();
  Logic.defineBlocks();
  Form.defineBlocks();
  Responsive.defineBlocks();
};

// AI, Code Clean, Remove Scripts
const callOpenAI = async () => {
  /* ... 기존 로직 유지 ... */
};
const cleanCodeForView = (code) => {
  if (!code) return '';
  return code
    .replace(/\s!important/g, '')
    .replace(/data-block-id="[^"]*"/g, '')
    .replace(/data-x="[^"]*"/g, '')
    .replace(/data-y="[^"]*"/g, '')
    .replace(/style="[^"]*"/g, '')
    .replace(/\s{2,}/g, ' ')
    .replace(/\s>/g, '>')
    .replace(/;\s+}/g, ';\n}')
    .trim();
};
const removeScripts = (html) => {
  if (!html) return '';
  return html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '');
};

const updatePreview = () => {
  const displayCode = isRunning.value
    ? generatedCode.value
    : removeScripts(generatedCode.value);

previewSrc.value = `<!DOCTYPE html><html><head><meta charset="utf-8">
<style>
  /* ✅ [추가] 브라우저 기본 여백 제거 및 가로 스크롤 방지 */
  html, body {
    margin: 0;
    padding: 0;
    width: 100%;
    overflow-x: hidden; /* 가로 스크롤 강제 숨김 */
  }

  /* ✅ [추가] 테두리와 패딩이 너비를 늘리지 않도록 설정 */
  * {
    box-sizing: border-box;
  }

  /* ✅ [추가] 래퍼 설정: 가로폭 100% 유지, 넘치는 내용 숨김 */
  #wrapper {
    width: 100%;
    min-height: 100vh;
    position: relative;
    overflow-x: hidden; 
  }

  /* ✅ [추가] 이미지가 화면 밖으로 튀어나가는 것 방지 */
  img {
    max-width: 100%;
    height: auto;
  }
</style>
<script src="https://cdn.jsdelivr.net/npm/interactjs/dist/interact.min.js"><\/script>
</head><body>
<div id="wrapper">${displayCode}</div>

<script>
function init(){
  if(typeof interact==='undefined'){setTimeout(init,50);return;}

  const isRunning = ${isRunning.value};

  // ✅ DESIGN 모드에서만 클릭 차단 (드래그는 그대로)
  document.addEventListener('click', function(e){
    if(isRunning) return;

    const a = e.target.closest && e.target.closest('a[href]');
    const btn = e.target.closest && e.target.closest('button');
    const inputBtn = e.target.closest && e.target.closest('input[type="button"], input[type="submit"]');

    if(a || btn || inputBtn){
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();
    }
  }, true);

  // ✅ RUNNING이면: (동작 카테고리에서 처리할 거니까) 여기서는 아무 것도 안함
  if(isRunning){
    return;
  }

  // ✅ DESIGN: 드래그 가능
  interact('#wrapper > [data-block-id]').draggable({
    inertia:false,
    autoScroll:true,
    listeners:{
      move(event){
        const target=event.target;
        const x=(parseFloat(target.getAttribute('data-x'))||0)+event.dx;
        const y=(parseFloat(target.getAttribute('data-y'))||0)+event.dy;
        target.style.transform='translate('+x+'px, '+y+'px)';
        target.setAttribute('data-x',x);
        target.setAttribute('data-y',y);
      },
      end(event){
        const target=event.target;
        const blockId=target.getAttribute('data-block-id');
        const x=Math.round(parseFloat(target.getAttribute('data-x'))||0);
        const y=Math.round(parseFloat(target.getAttribute('data-y'))||0);
        window.parent.postMessage({type:'update_position',blockId:blockId,x:x,y:y},'*');
      }
    }
  });
}
if(document.readyState==='loading'){document.addEventListener('DOMContentLoaded',init);}
else{init();}
<\/script>

</body></html>`;
};

const toggleRun = async () => {
  isRunning.value = !isRunning.value;
  await nextTick();
  updatePreview();
};
const changeModel = () => {
  isPhone.value = !isPhone.value;
  updatePreview();
};

// ✅ toolbox 문자열 -> DOM으로 변환해서 updateToolbox (가장 안정)
const setToolbox = (xmlText) => {
  try {
    const text = (xmlText || '<xml></xml>').trim();
    const dom = Blockly.utils.xml.textToDom(text);
    workspace.updateToolbox(dom);
    Blockly.svgResize(workspace);
  } catch (e) {
    console.error('❌ toolbox parse/update failed:', e, xmlText);
    // 실패시라도 빈 toolbox로 복구
    try {
      workspace.updateToolbox(Blockly.utils.xml.textToDom('<xml></xml>'));
      Blockly.svgResize(workspace);
    } catch {}
  }
};

// 탭 선택 로직 (교체)
const selectParent = (parentId) => {
  activeParent.value = parentId;
  activeTab.value = null;
  if (!workspace) return;
  setToolbox(toolboxXMLs.empty);
};

// 카테고리 선택 로직 (교체)
const selectCategory = (key) => {
  if (!workspace) return;

  if (activeTab.value === key) {
    activeTab.value = null;
    setToolbox(toolboxXMLs.empty);
    return;
  }

  activeTab.value = key;
  setToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
};

onMounted(async () => {
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();
  if (Interaction.updateObjectList) Interaction.updateObjectList(objects.value);
  await nextTick();

  const blocklyDiv = document.getElementById('blocklyDiv');
  workspace = Blockly.inject(blocklyDiv, {
    renderer: 'zelos',
    toolbox: toolboxXMLs.empty,
    move: { scrollbars: true, drag: true, wheel: true },
    zoom: { controls: true, wheel: true, startScale: 0.8 },
    grid: { spacing: 20, length: 3, colour: '#ccc', snap: true },
    trashcan: true,
  });

  workspace.addChangeListener((e) => {
    if (e.type === Blockly.Events.UI) return;
    try {
      javascriptGenerator.init(workspace);
      generatedCode.value = javascriptGenerator.workspaceToCode(workspace);
      updatePreview();
      updateObjectListFromWorkspace();
    } catch (err) {}
  });

  window.addEventListener('message', (event) => {
    // 1. 드래그가 끝났다는 신호를 받음
    if (event.data.type === 'update_position') {
      const { blockId, x, y } = event.data;

      // 2. 해당 블록을 찾음
      const block = workspace.getBlockById(blockId);

      if (block) {
        // [핵심] 블록 자체의 'data' 속성에 좌표를 JSON 문자열로 영구 저장 💾
        // 이렇게 해야 나중에 텍스트를 수정해서 코드가 재생성되어도 이 좌표를 기억함!
        block.data = JSON.stringify({ x, y });

        // 3. (선택사항) 즉시 코드를 재생성하여 위치를 확정지음
        // 이걸 안 하면, 다음 번 수정 때 깜빡거릴 수 있음
        generatedCode.value = javascriptGenerator.workspaceToCode(workspace);

        // 4. 페이지 저장 (새로고침 대비)
        saveCurrentWorkspaceToPage();
      }
    }
  });

  const stored = loadPagesFromStorage();
  if (stored && stored.length > 0) pages.value = stored;
  if (pages.value.length > 0) loadPageById(selectedPageId.value);

  new ResizeObserver(() => {
    if (workspace) Blockly.svgResize(workspace);
  }).observe(document.getElementById('workspace-area'));
});

// 기타 함수들 (기존 유지)
const saveCurrentWorkspaceToPage = () => {
  /*...*/
};
const loadPageById = (pageId) => {
  if (!workspace) return;
  /*...*/ javascriptGenerator.init(workspace);
  generatedCode.value = javascriptGenerator.workspaceToCode(workspace);
  updatePreview();
};
const selectPage = (pageId) => {
  saveCurrentWorkspaceToPage();
  loadPageById(pageId);
};
const deletePage = (pageId) => {
  /*...*/
};
const addPage = () => {
  /*...*/
};
const startEditPageName = (page) => {
  /*...*/
};
const commitEditPageName = (pageId) => {
  /*...*/
};
const cancelEditPageName = () => {
  /*...*/
};
</script>

<template>
  <div class="ide-container">
    <aside
      :class="isPhone ? 'phone-size' : 'pc-size'"
      class="entry-panel transition-all duration-300 ease-in-out"
    >
      <div class="preview-section">
        <div class="panel-title">
          <span
            @click="changeModel"
            class="cursor-pointer inline-flex items-center gap-[5px] text-white hover:text-gray-300 transition-colors"
          >
            <span v-if="isPhone" class="text-xl">📱</span>
            <img
              v-else
              src="https://img.icons8.com/?size=100&id=13352&format=png&color=FFFFFF"
              alt="PC"
              class="w-[20px] h-[20px]"
            />
          </span>

          <div class="control-buttons">
            <button
              class="btn-ai"
              :class="isPhone ? 'phone-hide' : ''"
              @click="showAiModal = true"
            >
              ✨ AI
            </button>
            <button
              class="btn-toggle"
              :class="{ running: isRunning }"
              @click="toggleRun"
            >
              {{ isRunning ? '⏹ 정지' : '▶ 시작' }}
            </button>
            <button
              class="btn-deploy"
              :class="isPhone ? 'phone-hide' : ''"
              @click="alert(generatedCode)"
            >
              🚀 배포
            </button>
          </div>

          <div class="status-slot">
            <span class="live-badge" v-if="isRunning">RUNNING</span>
            <span class="stop-badge" v-else>DESIGN</span>
          </div>
        </div>

        <div class="browser-mockup">
          <div class="url-bar">https://web-crafter.app/preview</div>
          <iframe
            :key="`${isRunning}-${selectedPageId}-${isPhone}`"
            id="previewFrame"
            :srcdoc="previewSrc"
            frameborder="0"
            :sandbox="'allow-scripts allow-same-origin allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox'"
          ></iframe>
        </div>
      </div>

      <div class="manager-section">
        <div class="manager-tabs" :class="isPhone ? 'phone-font' : ''">
          <button
            :class="{ active: activeRightTab === 'objects' }"
            @click="activeRightTab = 'objects'"
          >
            📦 객체
          </button>
          <button
            :class="{ active: activeRightTab === 'pages' }"
            @click="activeRightTab = 'pages'"
          >
            🗂️ 페이지
          </button>
          <button
            :class="{ active: activeRightTab === 'code' }"
            @click="activeRightTab = 'code'"
          >
            💻 코드
          </button>
        </div>

        <div v-if="activeRightTab === 'pages'" class="tab-content">
          <div class="list-header">
            <span>총 {{ pages.length }}개</span
            ><button class="btn-add-mini" @click="addPage">➕ 추가</button>
          </div>
          <ul class="item-list">
            <li
              v-for="page in pages"
              :key="page.id"
              class="list-item"
              :class="{ active: selectedPageId === page.id }"
              @click="selectPage(page.id)"
            >
              <span class="item-icon">📄</span>
              <span class="item-name">{{ page.name }}</span>
              <button class="btn-del" @click.stop="deletePage(page.id)">
                ✕
              </button>
            </li>
          </ul>
        </div>
        <div v-if="activeRightTab === 'objects'" class="tab-content">
          <div class="empty-msg" v-if="objects.length === 0">
            <p>배치된 요소가 없습니다.</p>
            <p class="text-sm text-gray-500 mt-2">
              블록을 사용하여 요소를 추가해 보세요!
            </p>
          </div>
          <ul class="item-list" v-else>
            <li v-for="obj in objects" :key="obj.id" class="list-item">
              <span class="item-icon">💠</span>
              <span class="item-name">{{ obj.name }}</span>
              <span class="item-type">{{ obj.type }}</span>
            </li>
          </ul>
        </div>
        <div v-if="activeRightTab === 'code'" class="tab-content code-view">
          <pre>{{ cleanCodeForView(generatedCode) }}</pre>
        </div>
      </div>
    </aside>

    <div class="ide-main-area">
      <nav class="top-nav-bar">
        <div
          v-for="group in categoryGroups"
          :key="group.id"
          class="top-tab-item"
          :class="{ active: activeParent === group.id }"
          @click="selectParent(group.id)"
          :style="{
            borderBottomColor:
              activeParent === group.id ? group.color : 'transparent',
          }"
        >
          <span class="tab-icon">{{ group.icon }}</span>
          <span class="tab-label">{{ group.label }}</span>
        </div>
      </nav>

      <div class="workspace-row">
        <nav class="sub-sidebar">
          <div
            v-for="itemKey in currentSubItems"
            :key="itemKey"
            class="sub-item"
            :class="{ active: activeTab === itemKey }"
            @click.stop="selectCategory(itemKey)"
          >
            <div class="icon">{{ categories[itemKey]?.icon || '?' }}</div>
            <div class="label">{{ categories[itemKey]?.label || itemKey }}</div>
            <div
              class="indicator"
              :style="{ backgroundColor: categories[itemKey]?.color || '#ccc' }"
            ></div>
          </div>
        </nav>

        <div
          id="workspace-area"
          class="workspace-wrapper"
          :class="{ 'drawer-open': activeTab }"
        >
          <div id="blocklyDiv"></div>
        </div>
      </div>
    </div>

    <div v-if="showAiModal" class="modal-overlay">
      <div class="modal-content">
        <h3>✨ AI로 페이지 만들기</h3>
        <p class="desc">
          원하는 디자인을 설명하면 블록을 조립해줍니다.<br />(예: "로그인 버튼이
          있는 파란색 섹션을 만들어줘")
        </p>
        <textarea
          v-model="aiPrompt"
          placeholder="요청사항 입력..."
          class="ai-textarea"
          :class="{ 'input-error': aiPromptError }"
        ></textarea>
        <div class="modal-actions">
          <button @click="showAiModal = false" class="btn-cancel">취소</button>
          <button
            @click="callOpenAI"
            class="btn-generate"
            :disabled="isGenerating"
          >
            {{ isGenerating ? '생성 중...' : '생성하기' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 🎨 [기존 스타일 복구 및 신규 레이아웃 통합] */

/* 레이아웃 컨테이너 */
.ide-container {
  padding-top: 70px;
  height: 100vh;
  display: flex;
  flex-direction: row;
  background-color: #f0f0f0;
  overflow: hidden;
}

/* 1. 좌측 패널 (Entry Panel) - 기존 스타일 유지 */
.entry-panel {
  background: #f5f5f5;
  border-right: 1px solid #1a1a2e; /* 경계선 강조 */
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  z-index: 30;
  height: 100%;
}
.phone-size {
  width: 213px;
}
.pc-size {
  width: 672px;
}

/* 미리보기 섹션 - 다크 테마 복구 */
.preview-section {
  flex: 1;
  background: #1a1a2e;
  padding: 10px;
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid #252535;
}
.panel-title {
  font-weight: bold;
  margin-bottom: 8px;
  font-size: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: white;
  height: 32px;
}

/* 버튼 스타일 복구 */
.control-buttons {
  display: flex;
  gap: 6px;
  font-size: 0.85rem;
  align-items: center;
}
.btn-ai {
  background: #9c27b0;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}
.btn-ai:hover {
  background: #7b1fa2;
}
.btn-toggle {
  background: #4caf50;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 5px;
  transition: 0.2s;
}
.btn-toggle:hover {
  background: #43a047;
}
.btn-toggle.running {
  background: #f44336;
}
.btn-toggle.running:hover {
  background: #d32f2f;
}
.btn-deploy {
  background: #2196f3;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}

.status-slot {
  width: 78px; /* 배지 들어갈 고정 폭(원하는 값으로) */
  display: flex;
  justify-content: flex-end; /* 오른쪽 정렬 */
  align-items: center;
  flex-shrink: 0;
}

/* 배지 애니메이션 복구 */
.live-badge {
  background: #ff5252;
  color: white;
  font-size: 0.6rem;
  padding: 2px 6px;
  border-radius: 4px;
  animation: pulse 1.5s infinite;
  font-weight: bold;
}
.stop-badge {
  background: #9e9e9e;
  color: white;
  font-size: 0.6rem;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
}

.browser-mockup {
  flex: 1;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
}
.url-bar {
  background: #f1f3f4;
  padding: 5px 10px;
  font-size: 0.7rem;
  color: #555;
  border-bottom: 1px solid #ddd;
}
iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 관리자 섹션 - 기존 디자인 */
.manager-section {
  height: 45%;
  display: flex;
  flex-direction: column;
  background: white;
  border-top: 1px solid #ddd;
}
.manager-tabs {
  display: flex;
  background: #1a1a2e;
  border-bottom: 1px solid #ddd;
}
.manager-tabs button {
  flex: 1;
  padding: 10px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #aaa;
  font-size: 0.85rem;
  border-bottom: 3px solid transparent;
  transition: 0.2s;
  white-space: nowrap;
}
.manager-tabs button.active {
  background: white;
  border-bottom-color: #4c97ff;
  font-weight: bold;
  color: #1a1a2e;
}
.phone-font button {
  font-size: 0.75rem;
  padding: 8px;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  color: #252535;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 0.8rem;
  color: #666;
}
.btn-add-mini {
  background: #4c97ff;
  color: white;
  border: none;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.7rem;
}
.item-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.list-item {
  display: flex;
  align-items: center;
  padding: 8px;
  background: #f9f9f9;
  border: 1px solid #eee;
  margin-bottom: 5px;
  border-radius: 4px;
  cursor: pointer;
  transition: 0.1s;
}
.list-item:hover {
  background: #f0f7ff;
  border-color: #cce5ff;
}
.list-item.active {
  background: #eaf4ff;
  border-color: #4c97ff;
  box-shadow: inset 4px 0 0 #4c97ff;
}
.list-item.active .item-name {
  font-weight: 700;
  color: #0b3d91;
}
.item-icon {
  margin-right: 8px;
  font-size: 1.1rem;
}
.item-name {
  flex: 1;
  font-size: 0.85rem;
  font-weight: 500;
}
.item-type {
  font-size: 0.7rem;
  color: #999;
  margin-right: 5px;
}
.btn-del {
  background: none;
  border: none;
  color: #ccc;
  cursor: pointer;
}
.btn-del:hover {
  color: red;
}
.code-view pre {
  margin: 0;
  font-family: monospace;
  font-size: 0.8rem;
  white-space: pre-wrap;
  color: #333;
}
.empty-msg {
  text-align: center;
  color: #999;
  margin-top: 20px;
  font-size: 0.85rem;
}

/* 2. 메인 작업 영역 */
.ide-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
:deep(.blocklyScrollbarHorizontal), 
:deep(.blocklyScrollbarVertical) {
  display: none; /* 또는 opacity: 0; */
}
/* [상단 탭] - 다크 테마 적용 */
.top-nav-bar {
  height: 60px;
  background: #1a1a2e; /* 기존 사이드바 색상 */
  display: flex;
  align-items: center;
  padding-left: 10px;
  border-bottom: 1px solid #000;
  flex-shrink: 0;
}
.top-tab-item {
  height: 100%;
  padding: 0 25px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #777; /* 비활성 색상 */
  cursor: pointer;
  border-bottom: 4px solid transparent;
  transition: all 0.2s;
  font-weight: 500;
  position: relative;
}
.top-tab-item:hover {
  background: #252535;
  color: white;
}
.top-tab-item.active {
  background: #202030;
  color: white;
  font-weight: bold;
}
.tab-icon {
  font-size: 1.2rem;
}
.tab-label {
  font-size: 0.9rem;
}

/* 작업 공간 로우 */
.workspace-row {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* [좌측 소분류 사이드바] - 기존 스타일 복구 */
.sub-sidebar {
  width: 70px;
  background: #1a1a2e; /* 다크 테마 */
  border-right: 1px solid #000;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  z-index: 20;
}
.sub-sidebar::-webkit-scrollbar {
  width: 0px;
}

.sub-item {
  height: 70px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #777;
  cursor: pointer;
  position: relative;
  border-bottom: 1px solid #252535;
  transition: 0.2s;
}
.sub-item:hover {
  background: #252535;
  color: white;
}
.sub-item.active {
  background: #202030;
  color: white;
}
.sub-item .icon {
  font-size: 1.6rem;
  margin-bottom: 4px;
}
.sub-item .label {
  font-size: 0.7rem;
}
.indicator {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  display: none;
}
.sub-item.active .indicator {
  display: block;
}

/* 워크스페이스 */
.workspace-wrapper {
  flex: 1;
  position: relative;
  background: #fff;
  transition: all 0.3s ease;
}
#blocklyDiv {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}
.phone-hide {
  display: none !important;
}
:deep(.blocklyToolboxDiv) {
  background-color: #f9f9f9;
  border-right: 1px solid #ddd;
  width: auto !important;
  max-width: 300px;
  min-width: 50px;
  opacity: 0.5;
  display: block !important;
  transform: scaleX(1);
  transform-origin: left top;
  transition:
    transform 0.3s ease,
    opacity 0.2s ease;
}
.workspace-wrapper:not(.drawer-open) :deep(.blocklyToolboxDiv) {
  display: none !important;
  transform: scaleX(0);
  width: 0px !important;
  border: none !important;
}

/* AI 모달 스타일 복구 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.3);
}
.desc {
  font-size: 0.85rem;
  color: #666;
  margin-bottom: 15px;
}
.ai-textarea {
  width: 100%;
  height: 100px;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  resize: none;
  margin-bottom: 15px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn-cancel {
  background: #ddd;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}
.btn-generate {
  background: #9c27b0;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}
.btn-generate:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.input-error {
  border-color: #f44336;
  box-shadow: 0 0 0 2px rgba(244, 67, 54, 0.2);
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}
</style>
