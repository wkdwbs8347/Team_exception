<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python' 
import * as Ko from 'blockly/msg/ko'
import 'blockly/blocks'

// ===== 카테고리 블록 import =====
import * as Layout from '@/components/categories/Layout.vue'
import * as Content from '@/components/categories/Content.vue'
import * as Style from '@/components/categories/Style.vue'
import * as Interaction from '@/components/categories/Interaction.vue'
import * as Flow from '@/components/categories/Flow.vue'
import * as Logic from '@/components/categories/Logic.vue'
import * as Form from '@/components/categories/Form.vue'
import * as Responsive from '@/components/categories/Responsive.vue'

// ===== 상태 관리 =====
const activeTab = ref(null)
const generatedCode = ref('')
const previewSrc = ref('')
const activeRightTab = ref('objects')
const isRunning = ref(false)
let workspace = null

// [AI 관련 상태 (프록시 사용)]
const showAiModal = ref(false)
const aiPrompt = ref('')
const isGenerating = ref(false)
const aiPromptError = ref(false);

// 페이지 및 객체 상태
// 고유한 페이지 ID 생성기
const generateUniquePageId = () => {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `page-${crypto.randomUUID()}`
    }
  } catch (e) {
    // ignore and fallback
  }
  return `page-${Date.now().toString(36)}-${Math.random().toString(36).slice(2,8)}`
}

const pages = ref([
  { id: generateUniquePageId(), name: '메인화면 (Home)', xml: '<xml></xml>' },
  { id: generateUniquePageId(), name: '로그인 (Login)', xml: '<xml></xml>' }
])
const selectedPageId = ref(pages.value[0].id)
const objects = ref([])

const editingPageId = ref(null)
const editingPageName = ref('')

// 로컬 스토리지 유틸
const loadPagesFromStorage = () => {
  try {
    const raw = localStorage.getItem('wc_pages')
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    console.warn('loadPagesFromStorage failed', e)
    return null
  }
}
const savePagesToStorage = () => {
  try {
    localStorage.setItem('wc_pages', JSON.stringify(pages.value))
  } catch (e) {
    console.warn('savePagesToStorage failed', e)
  }
}

watch(objects, (newObjects) => {
  if (Interaction.updateObjectList) {
    Interaction.updateObjectList(newObjects);
  }
}, { deep: true, immediate: true });

// Blockly 워크스페이스에서 객체 목록 업데이트
const updateObjectListFromWorkspace = () => {
  if (!workspace) return;
  const currentObjects = [];
  const blocks = workspace.getAllBlocks(false); // 최상위 블록만 가져오기

  // interaction(스크립트) 블록 타입은 객체 목록에서 제외
  const ignoredTypes = new Set(['event_click', 'event_page_load', 'action_alert']);

  blocks.forEach(block => {
    let name = '';
    let type = block.type;

    // 1. [기존] interaction 블록 무시
    if (ignoredTypes.has(block.type)) return;

    // 2. [추가됨] 스타일 블록(style_로 시작)도 객체 목록에서 숨김 처리 ✨
    if (block.type.startsWith('style_')) return; 

    // 우선 NAME 필드가 있으면 사용
    const nameField = block.getFieldValue && block.getFieldValue('NAME')
    if (nameField) {
      name = nameField
    } else {
      switch (block.type) {
        case 'layout_div':
          name = block.getFieldValue('ELEMENT_CLASS') ? `DIV (${block.getFieldValue('ELEMENT_CLASS')})` : 'DIV';
          break;
        case 'content_button':
          name = `버튼 (${block.getFieldValue('LABEL')})`;
          break;
        case 'content_text':
          name = `텍스트 (${(block.getFieldValue('TEXT')||'').substring(0, 10)}...)`;
          break;
        case 'content_image':
          name = `이미지 (${(block.getFieldValue('SRC')||'').substring(0, 15)}...)`;
          break;
        default:
          name = block.type;
          break;
      }
    }
    currentObjects.push({ id: block.id, name: name, type: type });
  });
  objects.value = currentObjects;
  if (Interaction.updateObjectList) {
    Interaction.updateObjectList(currentObjects);
  }
};

// ===== 카테고리 정의 =====
const categories = {
  layout: Layout.category,      // 구조 (📏)
  content: Content.category,    // 콘텐츠 (🧩)
  style: Style.category,        // 스타일 (🎨)
  flow: Flow.category,          // 흐름 (🔄)
  logic: Logic.category,        // 조건 (🔗)
  interaction: Interaction.category, // 상호작용 (🖱️)
  form: Form.category,          // 폼 (📝)
  data: { label: '데이터', color: '#26a69a', icon: '💾' },
  responsive: Responsive.category, // 반응형 (📱)
  animation: { label: '애니메이션', color: '#ff6f00', icon: '✨' },
  component: { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  seo: { label: 'SEO', color: '#607d8b', icon: '🔍' },
  advanced: { label: '고급', color: '#424242', icon: '⚙️' }
}

const toolboxXMLs = {
  layout: Layout.toolbox,
  content: Content.toolbox,
  style: Style.toolbox,
  interaction: Interaction.toolbox,
  flow: Flow.toolbox,
  logic: Logic.toolbox,
  form: Form.toolbox,
  responsive: Responsive.toolbox,
  empty: `<xml></xml>`
}

const defineCustomBlocks = () => {
  Layout.defineBlocks()
  Content.defineBlocks()
  Style.defineBlocks()
  Interaction.defineBlocks()
  Flow.defineBlocks()
  Logic.defineBlocks()
  Form.defineBlocks()
  Responsive.defineBlocks()
}

// ============================================================
// [AI 핵심 로직] OpenAI API 호출 및 블록 생성
// ============================================================

const callOpenAI = async () => {
  aiPromptError.value = false;

  if (!aiPrompt.value) {
    aiPromptError.value = true;
    return;
  }

  isGenerating.value = true;

  const systemPrompt = `You are a Blockly XML generator assistant. User will describe a web page structure. You must output ONLY valid Blockly-compatible XML starting with <xml>. Do NOT include markdown formatting.`;
  const promptText = `${systemPrompt}\n\nUser: Create XML blocks for: ${aiPrompt.value}`;

  try {
    // 서버 프록시 엔드포인트로 요청
    const resp = await fetch('/api/gemini/generate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ prompt: promptText })
    });

    if (!resp.ok) {
      const err = await resp.text();
      throw new Error(err || `Proxy request failed: ${resp.status}`);
    }

    const data = await resp.json();
    const xmlText = (data && data.xml) ? data.xml : '';
    if (!xmlText) throw new Error('Proxy returned empty xml');

    const cleaned = xmlText.replace(/```xml/g, '').replace(/```/g, '').trim();
    const xmlDom = Blockly.utils.xml.textToDom(cleaned);
    Blockly.Xml.domToWorkspace(xmlDom, workspace);

    showAiModal.value = false;
    aiPrompt.value = '';
  } catch (error) {
    console.error(error);
    alert('AI 생성 실패: ' + (error.message || JSON.stringify(error)));
  } finally {
    isGenerating.value = false;
  }
}

// ============================================================
// 기존 로직 (JS 제어 및 미리보기)
// ============================================================

const removeScripts = (html) => {
  if (!html) return "";
  return html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, "");
}

const updatePreview = () => {
  const fullCode = generatedCode.value;

  if (isRunning.value) {
    // [실행 모드] 모든 코드 적용 (스크립트 실행됨)
    previewSrc.value = fullCode;
  } else {
    // [디자인 모드] 복잡한 줄 단위 필터링 제거! 
    // removeScripts 함수가 <script> 태그만 깔끔하게 지워줍니다.
    // 따라서 <style>과 HTML은 그대로 남아서 화면에 보입니다.
    previewSrc.value = removeScripts(fullCode);
  }
}

const toggleRun = () => {
  isRunning.value = !isRunning.value;
  updatePreview();
}

// ===== 초기화 =====
onMounted(async () => {
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();

  if (Interaction.updateObjectList) {
    Interaction.updateObjectList(objects.value);
  }

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

  let saveTimer = null;
  const scheduleSave = () => {
    clearTimeout(saveTimer);
    saveTimer = setTimeout(() => {
      saveCurrentWorkspaceToPage();
    }, 600);
  }

workspace.addChangeListener((e) => {
  if (e.type === Blockly.Events.UI) return;
  try {
    let code = pythonGenerator.workspaceToCode(workspace);
    
    // [추가] 모든 한 줄 주석(//)과 그 뒤의 내용을 삭제합니다.
    code = code.replace(/\/\/.*$/gm, ''); 
    
    // [추가] 빈 줄이 너무 많아지면 정리 (선택 사항)
    code = code.replace(/^\s*[\r\n]/gm, '');

    generatedCode.value = code;
    updatePreview();
    updateObjectListFromWorkspace();
    scheduleSave();
  } catch (e) {
    console.warn(e);
  }
});

  // 로컬 스토리지에서 페이지 로드 (있으면 덮어씀)
  const stored = loadPagesFromStorage();
  if (stored && Array.isArray(stored) && stored.length > 0) {
    // 정규화: 각 페이지에 고유한 id를 보장합니다.
    const seen = new Set();
    const normalized = stored.map(p => {
      const copy = { ...p };
      if (!copy.id || seen.has(copy.id)) {
        let newId = generateUniquePageId();
        while (seen.has(newId)) {
          newId = generateUniquePageId();
        }
        copy.id = newId;
      }
      seen.add(copy.id);
      return copy;
    });
    pages.value = normalized;
    // 보정된 id를 저장하여 이후 중복 문제를 예방
    savePagesToStorage();
  }
  // 선택된 페이지 초기값
  if (!selectedPageId.value && pages.value.length > 0) selectedPageId.value = pages.value[0].id;

  // 선택된 페이지 불러오기
  if (pages.value.length > 0) {
    const initial = pages.value.find(p => p.id === selectedPageId.value) || pages.value[0];
    try {
      const xmlDom = Blockly.utils.xml.textToDom(initial.xml || '<xml></xml>');
      Blockly.Xml.domToWorkspace(xmlDom, workspace);
    } catch (e) {
      console.warn('Failed to load initial page xml', e);
    }
  }

  // 초기 로드 시에도 객체 목록 업데이트
  updateObjectListFromWorkspace();
  generatedCode.value = pythonGenerator.workspaceToCode(workspace);
  updatePreview();

  new ResizeObserver(() => Blockly.svgResize(workspace)).observe(document.getElementById('workspace-area'));
});

const toggleCategory = (key) => {
  if (activeTab.value === key) {
    activeTab.value = null;
    try {
      workspace.updateToolbox(toolboxXMLs.empty);
    } catch (e) {
      // workspace may not be ready yet
    }
  } else {
    activeTab.value = key;
    workspace.updateToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
  }
}

const closeCategory = (key) => {
  if (activeTab.value !== key) return;
  activeTab.value = null;
  try {
    workspace.updateToolbox(toolboxXMLs.empty);
  } catch (e) {
    // ignore if workspace not ready
  }
}

// 현재 워크스페이스 XML을 선택된 페이지에 저장
const saveCurrentWorkspaceToPage = () => {
  if (!workspace || !selectedPageId.value) return;
  try {
    const dom = Blockly.Xml.workspaceToDom(workspace);
    const xmlText = Blockly.Xml.domToText(dom);
    const idx = pages.value.findIndex(p => p.id === selectedPageId.value);
    if (idx !== -1) {
      pages.value[idx].xml = xmlText;
      savePagesToStorage();
    }
  } catch (e) {
    console.warn('saveCurrentWorkspaceToPage failed', e);
  }
}

const loadPageById = (pageId) => {
  if (!workspace) return;
  const page = pages.value.find(p => p.id === pageId);
  if (!page) return;
  try {
    workspace.clear();
    const xmlText = page.xml || '<xml></xml>';
    const xmlDom = Blockly.utils.xml.textToDom(xmlText);
    Blockly.Xml.domToWorkspace(xmlDom, workspace);
    selectedPageId.value = pageId;
    updateObjectListFromWorkspace();
    generatedCode.value = pythonGenerator.workspaceToCode(workspace);
    updatePreview();
  } catch (e) {
    console.warn('loadPageById failed', e);
  }
}

const selectPage = (pageId) => {
  if (pageId === selectedPageId.value) return;
  // 현재 저장
  saveCurrentWorkspaceToPage();
  loadPageById(pageId);
}

const deletePage = (pageId) => {
  const idx = pages.value.findIndex(p => p.id === pageId);
  if (idx === -1) return;
  if (pages.value.length <= 1) {
    alert('최소 하나의 페이지가 필요합니다.');
    return;
  }
  pages.value.splice(idx, 1);
  savePagesToStorage();
  if (selectedPageId.value === pageId) {
    const next = pages.value[0];
    if (next) loadPageById(next.id);
  }
}

const addPage = () => {
  // 현재 워크스페이스 저장
  saveCurrentWorkspaceToPage();
  let newId = generateUniquePageId();
  while (pages.value.find(p => p.id === newId)) {
    newId = generateUniquePageId();
  }
  const newPage = { id: newId, name: `새 페이지`, xml: '<xml></xml>' };
  pages.value.push(newPage);
  savePagesToStorage();
  loadPageById(newId);
}

const startEditPageName = (page) => {
  editingPageId.value = page.id
  editingPageName.value = page.name
  // focus the newly rendered input
  nextTick(() => {
    const el = document.querySelector('.page-edit-input')
    if (el) el.focus()
  })
}

const commitEditPageName = (pageId) => {
  const id = pageId || editingPageId.value
  console.log('commitEditPageName', id, editingPageName.value)
  const idx = pages.value.findIndex(p => p.id === id)
  if (idx === -1) {
    editingPageId.value = null
    editingPageName.value = ''
    return
  }
  pages.value[idx].name = editingPageName.value || '새 페이지'
  savePagesToStorage()
  editingPageId.value = null
  editingPageName.value = ''
}

const cancelEditPageName = () => {
  editingPageId.value = null
  editingPageName.value = ''
}
</script>

<template>
  <div class="ide-container">
    <div class="ide-main">
        <aside class="entry-panel">
        
        <div class="preview-section">
          <div class="panel-title">
            <span>📱 미리보기</span>
            
            <div class="control-buttons">
              <button class="btn-ai" @click="showAiModal = true">✨ AI</button>

              <button
                class="btn-toggle"
                :class="{ 'running': isRunning }"
                @click="toggleRun"
              >
                {{ isRunning ? '⏹ 정지' : '▶ 시작' }}
              </button>
              
              <button class="btn-deploy" @click="alert(generatedCode)">🚀 배포</button>
            </div>

            <span class="live-badge" v-if="isRunning">RUNNING</span>
            <span class="stop-badge" v-else>DESIGN</span>
          </div>
          
          <div class="browser-mockup">
            <div class="url-bar">https://web-crafter.app/preview</div>
            <iframe id="previewFrame" :srcdoc="previewSrc" frameborder="0" sandbox="allow-same-origin allow-scripts allow-modals"></iframe>
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
              <li v-for="page in pages" :key="page.id" class="list-item" :class="{ 'active': selectedPageId === page.id }" @click="editingPageId === page.id ? null : selectPage(page.id)">
                <span class="item-icon">📄</span>
                <template v-if="editingPageId === page.id">
                  <input
                    class="page-edit-input"
                    v-model="editingPageName"
                    @blur="commitEditPageName()"
                    @click.stop
                    @keydown.enter.prevent="commitEditPageName()"
                    @keydown.esc.prevent="cancelEditPageName()"
                    ref="pageEdit"
                    />
                </template>
                <template v-else>
                  <span class="item-name" @dblclick.stop="startEditPageName(page)">{{ page.name }}</span>
                </template>
                <button class="btn-del" @click.stop="deletePage(page.id)">✕</button>
              </li>
            </ul>
          </div>

          <div v-if="activeRightTab === 'objects'" class="tab-content">
            <div class="empty-msg" v-if="objects.length === 0">
              <p>배치된 요소가 없습니다.</p>
              <p class="text-sm text-gray-500 mt-2">블록을 사용하여 요소를 추가해 보세요!</p>
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
          <button v-if="activeTab === key" class="cat-close" @click.stop="closeCategory(key)">✕</button>
        </div>
      </nav>

      <div id="workspace-area" class="workspace-wrapper" :class="{ 'drawer-open': activeTab }">
        <div id="blocklyDiv"></div>
      </div>
    </div>

    <div v-if="showAiModal" class="modal-overlay">
      <div class="modal-content">
        <h3>✨ AI로 페이지 만들기</h3>
        <p class="desc">원하는 디자인을 설명하면 블록을 조립해줍니다.<br>(예: "로그인 버튼이 있는 파란색 섹션을 만들어줘")</p>
        
        <p class="desc">서버 프록시를 사용합니다. API 키는 서버에서 관리하세요.</p>
        
        <textarea 
          v-model="aiPrompt" 
          placeholder="요청사항 입력..." 
          class="ai-textarea" 
          :class="{ 'input-error': aiPromptError }"
        ></textarea>
        <p v-if="aiPromptError" class="error-message">만들고 싶은 내용을 적어주세요!</p>
        
        <div class="modal-actions">
          <button @click="showAiModal = false" class="btn-cancel">취소</button>
          <button @click="callOpenAI" class="btn-generate" :disabled="isGenerating">
            {{ isGenerating ? '생성 중...' : '생성하기' }}
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
/* (기존 스타일 유지) */
.ide-container { padding-top: 81px; height: 100vh; display: flex; flex-direction: column; background-color: #f0f0f0; overflow: hidden; }
.ide-main { display: flex; flex: 1; height: 100%; overflow: hidden; }
.sidebar { width: 70px; background: #1a1a2e; display: flex; flex-direction: column; flex-shrink: 0; border-right: 1px solid #000; overflow-y: auto; z-index: 20; }
.sidebar::-webkit-scrollbar { width: 0px; }
.cat-item { height: 70px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #777; cursor: pointer; position: relative; border-bottom: 1px solid #252535; }
.cat-item:hover { background: #252535; color: white; }
.cat-item.active { background: #202030; color: white; }
.cat-item .icon { font-size: 1.6rem; margin-bottom: 4px; }
.cat-item .label { font-size: 0.7rem; }
.indicator { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; display: none; }
.cat-item.active .indicator { display: block; }

.cat-close { position: absolute; right: 6px; top: 6px; background: transparent; border: none; color: #ccc; cursor: pointer; padding: 4px; border-radius: 4px; font-size: 0.8rem; }
.cat-close:hover { background: rgba(255,255,255,0.06); color: white; }

.workspace-wrapper { flex: 1; position: relative; background: #fff; transition: all 0.3s ease; }
#blocklyDiv { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }

:deep(.blocklyToolboxDiv) { background-color: #f9f9f9; border-right: 1px solid #ddd; transform-origin: left top; width: auto !important; max-width: 300px; min-width: 50px; opacity: 1; overflow: hidden; white-space: nowrap; display: block !important; transform: scaleX(1); transition: transform 280ms cubic-bezier(0.2, 0.8, 0.2, 1), opacity 200ms ease-in-out, width 260ms ease-in-out; }
.workspace-wrapper:not(.drawer-open) :deep(.blocklyToolboxDiv) { min-width: 0px !important; width: 0px !important; max-width: 0px !important; transform: scaleX(0); padding: 0 !important; border: none !important; opacity: 0; pointer-events: none; background: transparent !important; }

.entry-panel { width: 360px; background: #f5f5f5; border-left: 1px solid #252535; display: flex; flex-direction: column; flex-shrink: 0; z-index: 30; }
.preview-section { flex: 1; background: #1a1a2e; padding: 10px; display: flex; flex-direction: column; border-bottom: 1px solid #252535; }
.panel-title { font-weight: bold; margin-bottom: 8px; font-size: 0.9rem; display: flex; justify-content: space-between; align-items: center; color: white; }

/* 버튼 스타일 */
.control-buttons { display: flex; gap: 6px; }

/* AI 버튼 스타일 추가 */
.btn-ai { background: #9c27b0; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-weight: bold; font-size: 0.8rem; transition: 0.2s; }
.btn-ai:hover { background: #7b1fa2; }

.btn-toggle { background: #4caf50; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-weight: bold; font-size: 0.8rem; transition: background 0.2s; display: flex; align-items: center; gap: 5px; }
.btn-toggle:hover { background: #43a047; }
.btn-toggle.running { background: #f44336; }
.btn-toggle.running:hover { background: #d32f2f; }
.btn-deploy { background: #2196f3; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer; font-size: 0.8rem; }

.live-badge { background: #ff5252; color: white; font-size: 0.6rem; padding: 2px 6px; border-radius: 4px; animation: pulse 1.5s infinite; }
.stop-badge { background: #9e9e9e; color: white; font-size: 0.6rem; padding: 2px 6px; border-radius: 4px; }

.browser-mockup { flex: 1; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.1); display: flex; flex-direction: column; }
.url-bar { background: #f1f3f4; padding: 5px 10px; font-size: 0.7rem; color: #555; border-bottom: 1px solid #ddd; }
iframe { flex: 1; width: 100%; height: 100%; border: none; background: white; }

.manager-section { height: 50%; display: flex; flex-direction: column; background: white; }
.manager-tabs { display: flex; background: #1a1a2e; border-bottom: 1px solid #ddd; }
.manager-tabs button { flex: 1; padding: 10px; border: none; background: transparent; cursor: pointer; font-size: 0.85rem; border-bottom: 3px solid transparent; color: #aaa; }
.manager-tabs button.active { background: white; border-bottom-color: #4c97ff; font-weight: bold; color: #1a1a2e; }

.tab-content { flex: 1; overflow-y: auto; padding: 10px; color: #252535;}
.list-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; font-size: 0.8rem; color: #666; }
.btn-add-mini { background: #4c97ff; color: white; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer; font-size: 0.7rem; }
.item-list { list-style: none; padding: 0; margin: 0; }
.list-item { display: flex; align-items: center; padding: 8px; background: #f9f9f9; border: 1px solid #eee; margin-bottom: 5px; border-radius: 4px; cursor: pointer; }
.list-item:hover { background: #f0f7ff; border-color: #cce5ff; }
.item-icon { margin-right: 8px; font-size: 1.1rem; }
.item-name { flex: 1; font-size: 0.85rem; font-weight: 500; }
.item-type { font-size: 0.7rem; color: #999; margin-right: 5px; }
.btn-del { background: none; border: none; color: #ccc; cursor: pointer; }
.btn-del:hover { color: red; }
.code-view pre { margin: 0; font-family: monospace; font-size: 0.8rem; white-space: pre-wrap; color: #333; }

/* [추가됨] AI 모달 스타일 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.6); display: flex; justify-content: center; align-items: center; z-index: 999; }
.modal-content { background: white; padding: 20px; border-radius: 8px; width: 400px; box-shadow: 0 4px 15px rgba(0,0,0,0.3); }
.desc { font-size: 0.85rem; color: #666; margin-bottom: 15px; }
.api-input { width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid #ddd; border-radius: 4px; font-size: 0.8rem; background: #f9f9f9; }
.ai-textarea { width: 100%; height: 100px; padding: 10px; border: 1px solid #ddd; border-radius: 4px; resize: none; margin-bottom: 15px; }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel { background: #ddd; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
.btn-generate { background: #9c27b0; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; font-weight: bold; }
.btn-generate:disabled { background: #ccc; cursor: not-allowed; }

.input-error { border-color: #f44336; box-shadow: 0 0 0 2px rgba(244, 67, 54, 0.2); }
.error-message { color: #f44336; font-size: 0.75rem; margin-top: -8px; margin-bottom: 10px; }

.page-edit-input { width: 100%; padding: 6px 8px; border: 1px solid #ccc; border-radius: 4px; }

.list-item.active { background: #eaf4ff; border-color: #4c97ff; box-shadow: inset 4px 0 0 #4c97ff; }
.list-item.active .item-name { font-weight: 700; color: #0b3d91; }

@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
</style>