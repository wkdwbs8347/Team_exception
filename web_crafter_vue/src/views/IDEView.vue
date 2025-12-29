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

// ===== 상태 관리 =====
const activeTab = ref(null)
const generatedCode = ref('')
const previewSrc = ref('')
const activeRightTab = ref('objects') 
const isRunning = ref(false)
let workspace = null

// [AI 관련 상태]
const showAiModal = ref(false)
const aiPrompt = ref('')
const isGenerating = ref(false)
const apiKey = ref('') // ⚠️ 실제 사용시엔 여기에 키를 넣거나, 모달창에서 입력받으세요.

// 더미 데이터
const pages = ref([
  { id: 'page1', name: '메인화면 (Home)' },
  { id: 'page2', name: '로그인 (Login)' }
])
const objects = ref([
  { id: 'obj1', name: '헤더 섹션', type: 'section' },
  { id: 'obj2', name: '가입 버튼', type: 'button' }
])

watch(objects, (newObjects) => {
  if (Interaction.updateObjectList) {
    Interaction.updateObjectList(newObjects);
  }
}, { deep: true, immediate: true });

// ===== 카테고리 정의 =====
const categories = {
  interaction: Interaction.category,
  layout: Layout.category,
  content: Content.category,
  style: Style.category,
  flow: Flow.category,
  page:        { label: '페이지',   color: '#8d6e63', icon: '🗂️' },
  form:        { label: '폼',      color: '#43a047', icon: '📝' },
  data:        { label: '데이터',   color: '#26a69a', icon: '🔗' },
  responsive:  { label: '반응형',   color: '#0091ea', icon: '📱' },
  animation:   { label: '애니메이션', color: '#ff6f00', icon: '✨' },
  component:   { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  seo:         { label: 'SEO',      color: '#607d8b', icon: '🔍' },
  advanced:    { label: '고급',     color: '#424242', icon: '⚙️' }
}

const toolboxXMLs = {
  layout: Layout.toolbox,
  content: Content.toolbox,
  style: Style.toolbox,
  interaction: Interaction.toolbox,
  flow: Flow.toolbox,
  empty: `<xml></xml>`
}

const defineCustomBlocks = () => {
  Layout.defineBlocks()
  Content.defineBlocks()
  Style.defineBlocks()
  Interaction.defineBlocks()
  Flow.defineBlocks()
}

// ============================================================
// [AI 핵심 로직] OpenAI API 호출 및 블록 생성
// ============================================================

const callOpenAI = async () => {
  if (!aiPrompt.value) return alert("만들고 싶은 내용을 적어주세요!");
  if (!apiKey.value) return alert("OpenAI API Key를 입력해주세요.");

  isGenerating.value = true;

  // [중요] 시스템 프롬프트: AI에게 우리 블록의 'type' 이름을 알려주는 역할
  // 실제 Layout.vue, Content.vue 등에서 정의한 블록 type 이름과 일치해야 합니다.
  const systemPrompt = `
    You are a Blockly XML generator assistant. 
    User will describe a web page structure. 
    You must output ONLY valid XML code compatible with Blockly.
    Do NOT include any markdown formatting (like \`\`\`xml). 
    
    Available Block Types (Use these exact names):
    - Structure: layout_section, layout_div, layout_row, layout_col
    - Elements: content_text, content_button, content_image, content_input
    - Style: style_css
    
    Rules:
    1. Nest blocks correctly using <statement name="DO">.
    2. Set fields using <field name="TEXT"> or appropriate field names.
    3. Output minimal standard Blockly XML starting with <xml>.
  `;

  try {
    const response = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${apiKey.value}`
      },
      body: JSON.stringify({
        model: "gpt-3.5-turbo", 
        messages: [
          { role: "system", content: systemPrompt },
          { role: "user", content: `Create XML blocks for: ${aiPrompt.value}` }
        ],
        temperature: 0.2
      })
    });

    const data = await response.json();
    
    if (data.error) {
      throw new Error(data.error.message);
    }

    let xmlText = data.choices[0].message.content;
    
    // AI가 가끔 마크다운(```xml ... ```)을 붙일 때가 있어서 제거
    xmlText = xmlText.replace(/```xml/g, '').replace(/```/g, '').trim();

    console.log("AI Generated XML:", xmlText); // 디버깅용

    // [핵심] 텍스트를 실제 블록으로 변환하여 워크스페이스에 추가
    const xmlDom = Blockly.utils.xml.textToDom(xmlText);
    Blockly.Xml.domToWorkspace(xmlDom, workspace);
    
    showAiModal.value = false;
    aiPrompt.value = '';

  } catch (error) {
    console.error(error);
    alert("AI 생성 실패: " + error.message);
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
    previewSrc.value = fullCode;
  } else {
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

  workspace.addChangeListener((e) => {
    if (e.type === Blockly.Events.UI) return;
    try {
      const code = pythonGenerator.workspaceToCode(workspace);
      generatedCode.value = code;
      if (!isRunning.value) {
        updatePreview();
      }
    } catch (e) {
      console.warn(e);
    }
  });

  generatedCode.value = pythonGenerator.workspaceToCode(workspace);
  updatePreview();

  new ResizeObserver(() => Blockly.svgResize(workspace)).observe(document.getElementById('workspace-area'));
});

const toggleCategory = (key) => {
  if (activeTab.value === key) {
    activeTab.value = null;
  } else {
    activeTab.value = key;
    workspace.updateToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
  }
}

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
            <iframe id="previewFrame" :srcdoc="previewSrc" frameborder="0"></iframe>
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

    <div v-if="showAiModal" class="modal-overlay">
      <div class="modal-content">
        <h3>✨ AI로 페이지 만들기</h3>
        <p class="desc">원하는 디자인을 설명하면 블록을 조립해줍니다.<br>(예: "로그인 버튼이 있는 파란색 섹션을 만들어줘")</p>
        
        <input v-model="apiKey" type="password" placeholder="OpenAI API Key (sk-...)" class="api-input"/>
        
        <textarea v-model="aiPrompt" placeholder="요청사항 입력..." class="ai-textarea"></textarea>
        
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

.workspace-wrapper { flex: 1; position: relative; background: #fff; transition: all 0.3s ease; }
#blocklyDiv { position: absolute; top: 0; left: 0; right: 0; bottom: 0; }

:deep(.blocklyToolboxDiv) { background-color: #f9f9f9; border-right: 1px solid #ddd; transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1); transform-origin: left top; width: auto !important; max-width: 300px; min-width: 50px; opacity: 1; overflow: hidden; white-space: nowrap; display: block !important; transform: scaleX(1); }
.workspace-wrapper:not(.drawer-open) :deep(.blocklyToolboxDiv) { min-width: 0px !important; width: 0px !important; max-width: 0px !important; transform: scaleX(0); padding: 0 !important; border: none !important; opacity: 0; pointer-events: none; }

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

@keyframes pulse { 0% { opacity: 1; } 50% { opacity: 0.5; } 100% { opacity: 1; } }
</style>