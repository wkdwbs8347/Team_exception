<script setup>
/**
 * ============================================================
 * ✅ Web Crafter IDE (Blockly + iframe Preview) 핵심 동작 흐름
 * ============================================================
 *
 * [A. 블록 → 코드 → 프리뷰 렌더]
 * 1) Blockly workspace 에서 블록을 조립한다.
 * 2) javascriptGenerator.workspaceToCode(workspace) 로 "렌더 가능한 코드 문자열(raw)"을 만든다.
 * 3) raw는 용도에 따라 두 개로 분리한다.
 *    - generatedCodeForPreview : 프리뷰(srcdoc) 렌더용 원본 코드
 *    - generatedCode          : 사용자 코드보기용(빌더 속성 제거 + script 제거)
 * 4) updatePreview()는 generatedCodeForPreview를 iframe srcdoc으로 주입한다.
 *    - DESIGN 모드: script 제거(removeScripts) + 클릭 동작 차단
 *    - RUN 모드   : script 유지 + 실제 동작 허용
 *
 * [B. 좌표(Free 배치) 저장 정책]
 * - 드래그 좌표는 "사용자 코드 문자열"에 절대 넣지 않는다.
 * - 좌표는 오직 block.data(JSON: {x,y}) 에 저장한다.
 * - preview 내부에서만 applyPositions()가 WC_POSITIONS(map)을 읽어 left/top로 적용한다.
 *
 * [C. 선택 동기화 (Workspace ↔ ObjectList ↔ Preview)]
 * - Blockly에서 선택되면 → handleSelection() → iframe에 highlight_element 메시지 전송
 * - iframe에서 요소 클릭하면 → parent로 select_block 전송 → Blockly 선택 반영
 * - ObjectList 클릭도 handleSelection()을 통해 동일 흐름으로 동기화
 *
 * [D. 드래그/스냅은 iframe 내부에서만 동작]
 * - PointerEvent 기반 drag
 * - computeSmartSnap(): 다른 요소 기준선(좌/중/우, 상/중/하) 근처일 때만 스냅
 * - 점선 가이드라인은 겹치는 구간 또는 요소-요소 사이 gap 구간만 표시
 * - 드래그 종료 시 update_free_position 메시지로 {blockId,x,y}를 parent에 전달
 * - parent는 block.data에 저장 → 페이지 저장 → refreshCodeAndPreview()로 재렌더
 * ============================================================
 */

import { ref, onMounted, nextTick, watch, computed } from 'vue';
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import * as Ko from 'blockly/msg/ko';
import 'blockly/blocks';
import ConfirmModal from '@/modal/ConfirmModal.vue'; // 컨펌(삭제/취소) 모달
import GlobalModal from '@/modal/GlobalModal.vue'; // 단순 안내용 확인 모달

// ===== 카테고리 블록 import =====
//html 블록 정의 및 툴박스 XML
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
//css 블록 정의 및 툴박스 XML
import * as Style from '@/components/style/Style.vue';
import * as Responsive from '@/components/style/Responsive.vue';
import * as Color from '@/components/style/Color.vue';
import * as Flex from '@/components/style/Flex.vue';
import * as Animation from '@/components/style/Animation.vue';
//js 블록 정의 및 툴박스 XML
import * as Interaction from '@/components/js/Interaction.vue';
import * as Flow from '@/components/js/Flow.vue';
import * as Logic from '@/components/js/Logic.vue';

/* ============================================================
 * UI 상태
 * ============================================================ */
const activeParent = ref('structure'); // 상단 탭(구조/스타일/로직)
const activeMode = ref('structure'); // 실제 workspace 저장 키와 연결되는 모드
const activeTab = ref(null); // 좌측 카테고리 열림 상태
const activeRightTab = ref('objects'); // 우측 패널(objects/pages/code)
const previewSrc = ref(''); // iframe srcdoc 문자열
const isRunning = ref(false); // RUN/DESIGN 전환(스크립트 실행 여부)
const isPhone = ref(false); // 프리뷰 뷰포트 토글
const modeOpen = ref(false); // 모드 드롭다운 열림
let workspace = null; // Blockly workspace 인스턴스

const modeList = [
  { id: 'structure', label: '화면구성', icon: '🏗️' },
  { id: 'style', label: '스타일', icon: '🎨' },
  { id: 'logic', label: '로직/데이터', icon: '⚡' },
];

const currentMode = computed(() => {
  return modeList.find((m) => m.id === activeMode.value) || modeList[0];
});

const changeMode = (modeId) => {
  modeOpen.value = false;
  selectParent(modeId);
};

// 페이지 삭제 확인 모달 상태
const confirmModal = ref({
  open: false,
  message: '',
  payload: null, // 삭제할 pageId 저장
});

// 열기
const openDeleteConfirm = (pageId) => {
  confirmModal.value.open = true;
  confirmModal.value.message = '이 페이지를 삭제하시겠습니까?';
  confirmModal.value.payload = { pageId };
};

// 닫기
const closeDeleteConfirm = () => {
  confirmModal.value.open = false;
  confirmModal.value.message = '';
  confirmModal.value.payload = null;
};

// 확인 눌렀을 때 실제 삭제 실행
const confirmDeletePage = () => {
  const pageId = confirmModal.value.payload?.pageId;
  closeDeleteConfirm();
  if (pageId) deletePageNow(pageId);
};

// 전역 안내 모달(단일 확인)
const modal = ref({ open: false, message: '', type: 'info', onConfirm: null });

const openModal = (message, type = 'info', onConfirm = null) => {
  modal.value.open = true;
  modal.value.message = message;
  modal.value.type = type;
  modal.value.onConfirm = onConfirm;
};

const closeModal = () => {
  modal.value.open = false;
  const fn = modal.value.onConfirm;
  modal.value.onConfirm = null;
  fn?.();
};

// 사용자 코드보기 / 프리뷰용 분리
const generatedCode = ref(''); // 코드보기용(빌더 속성 + 좌표 스타일 제거 + script 제거)

// 선택 상태(오브젝트 리스트/블록 선택/프리뷰 하이라이트 동기화)
const selectedBlockId = ref(null);
let isSelectingProgrammatically = false;

// [AI 및 페이지 상태]
const showAiModal = ref(false);
const aiPrompt = ref('');
const isGenerating = ref(false);
const aiPromptError = ref(false);
const vFocus = { mounted: (el) => el.focus() };

// ====================================================
// 유틸: 페이지 관리 (페이지별 모드별 workspace XML 저장)
// ====================================================
function generateUniquePageId() {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `page-${crypto.randomUUID()}`;
    }
  } catch (e) {}
  return `page-${Date.now().toString(36)}`;
}

function createPage(name) {
  return {
    id: generateUniquePageId(),
    name,
    workspaces: {
      structure: '<xml></xml>',
      style: '<xml></xml>',
      logic: '<xml></xml>',
    },
  };
}

const pages = ref([
  createPage('메인화면 (Home)'),
  createPage('로그인 (Login)'),
]);
const selectedPageId = ref(pages.value[0].id);

const objects = ref([]);
const editingPageId = ref(null);
const editingPageName = ref('');

function loadPagesFromStorage() {
  try {
    return JSON.parse(localStorage.getItem('wc_pages'));
  } catch (e) {
    return null;
  }
}
function savePagesToStorage() {
  try {
    localStorage.setItem('wc_pages', JSON.stringify(pages.value));
  } catch (e) {}
}

/* ============================================================
 * 코드보기 정리:
 * - 빌더 전용 data-* 제거
 * - 좌표 관련 스타일(position/left/top/transform)만 제거
 * - script는 removeScripts()로 제거 (DESIGN 기준)
 * ============================================================ */
const cleanCodeForView = (code) => {
  if (!code) return '';

  try {
    const container = document.createElement('div');
    container.innerHTML = code;

    container.querySelectorAll('*').forEach((el) => {
      // 빌더 전용 속성만 제거
      el.removeAttribute('data-block-id');
      el.removeAttribute('data-draggable');
      el.removeAttribute('data-x');
      el.removeAttribute('data-y');
      el.removeAttribute('data-wc-style'); // 생성기 스타일 흔적 제거
      el.removeAttribute('data-wc-block');

      // 좌표 관련 스타일만 제거
      if (el.hasAttribute('style')) {
        el.style.removeProperty('position');
        el.style.removeProperty('left');
        el.style.removeProperty('top');
        el.style.removeProperty('transform');

        // style이 비어 있으면 제거
        if (!el.getAttribute('style')?.trim()) {
          el.removeAttribute('style');
        }
      }
    });

    return container.innerHTML.trim();
  } catch (e) {
    // fallback (DOM 파싱 실패 시)
    return (
      (code || '')
        .replace(/\sdata-block-id="[^"]*"/g, '')
        .replace(/\sdata-draggable="[^"]*"/g, '')
        .replace(/\sdata-x="[^"]*"/g, '')
        .replace(/\sdata-y="[^"]*"/g, '')
        // ⚠️ style 전체 제거 ❌ → 좌표만 제거
        .replace(/position\s*:\s*absolute;?/gi, '')
        .replace(/left\s*:[^;"]+;?/gi, '')
        .replace(/top\s*:[^;"]+;?/gi, '')
        .replace(/transform\s*:[^;"]+;?/gi, '')
        .trim()
    );
  }
};

const removeScripts = (html) => {
  return html ? html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '') : '';
};

// ====================================================
// 좌표: 코드에 주입하지 않고 iframe 내부에서만 적용
// ====================================================
// [수정됨] 좌표 추출 함수 (현재 모드에 따라 소스 분기)
const getPositionsMap = () => {
  const map = {};
  
  // 1. 현재 페이지 데이터 찾기
  const page = pages.value.find(p => p.id === selectedPageId.value);
  if (!page) return map;

  // 좌표 추출 헬퍼 함수
  const extractFromBlocks = (blocks) => {
    blocks.forEach((b) => {
      if (!b.data) return;
      try {
        const p = JSON.parse(b.data);
        // x, y 정보가 있는 경우만 맵에 저장
        if (Number.isFinite(p.x) && Number.isFinite(p.y)) {
          map[b.id] = { x: Number(p.x), y: Number(p.y) };
        }
      } catch (e) {}
    });
  };

  // ✅ [핵심 로직]
  // Case A: 현재 '화면구성(structure)' 탭을 보고 있다면 -> 실시간 workspace에서 가져옴
  if (activeMode.value === 'structure' && workspace) {
    extractFromBlocks(workspace.getAllBlocks(false));
  } 
  // Case B: 다른 탭(스타일/로직)이나 RUN 모드라면 -> 저장된 XML에서 가져옴
  else if (page.workspaces.structure) {
    try {
      // 임시 워크스페이스를 만들어서 XML을 로드하고 정보를 뺍니다.
      const tempWs = new Blockly.Workspace();
      const dom = Blockly.utils.xml.textToDom(page.workspaces.structure);
      Blockly.Xml.domToWorkspace(dom, tempWs);
      
      extractFromBlocks(tempWs.getAllBlocks(false));
      
      tempWs.dispose(); // 메모리 정리
    } catch (e) {
      console.error("좌표 로드 실패:", e);
    }
  }

  return map;
};

// ====================================================
// XML → 코드 생성 유틸 (미리보기 병합용)
// ====================================================
const generateCodeFromXML = (xmlText) => {
  if (!xmlText || xmlText === '<xml></xml>') return '';

  try {
    // 1. 텍스트 XML을 DOM으로 변환
    const dom = Blockly.utils.xml.textToDom(xmlText);

    // 2. 현재 워크스페이스에 잠시 로드하여 코드를 뽑아냅니다.
    // (이 방식이 별도의 가상 워크스페이스를 만드는 것보다 훨씬 안전합니다.)
    const headlessWorkspace = new Blockly.Workspace();
    Blockly.Xml.domToWorkspace(dom, headlessWorkspace);

    const code = javascriptGenerator.workspaceToCode(headlessWorkspace);
    headlessWorkspace.dispose();

    return code;
  } catch (e) {
    return '';
  }
};
// ====================================================
// 선택 동기화 (Blockly <-> Preview <-> Object list)
// ====================================================
const handleSelection = (blockId, fromSource = 'unknown') => {
  if (blockId && selectedBlockId.value === blockId) return;
  selectedBlockId.value = blockId;

  // 1) Blockly 선택 반영
  if (workspace && fromSource !== 'blockly') {
    isSelectingProgrammatically = true;
    workspace.getAllBlocks(false).forEach((b) => b.unselect());
    if (blockId) {
      const block = workspace.getBlockById(blockId);
      block?.select();
    }
    isSelectingProgrammatically = false;
  }

  // 2) iframe 하이라이트 요청
  const iframe = document.getElementById('previewFrame');
  if (iframe?.contentWindow) {
    iframe.contentWindow.postMessage(
      { type: 'highlight_element', blockId },
      '*'
    );
  }
};

const selectObjectFromList = (objId) => {
  handleSelection(objId, 'list');
  workspace?.centerOnBlock(objId);
};

// ====================================================
// 오브젝트 리스트 갱신
// ====================================================
watch(
  objects,
  (newObjects) => {
    if (Interaction.updateObjectList) Interaction.updateObjectList(newObjects);
  },
  { deep: true, immediate: true }
);

const updateObjectListFromWorkspace = () => {
  if (!workspace) return;
  const current = [];
  const blocks = workspace.getAllBlocks(false);
  const ignoredTypes = new Set([
    'event_click',
    'event_page_load',
    'action_alert',
  ]);

  blocks.forEach((block) => {
    if (ignoredTypes.has(block.type)) return;

    if (
      activeMode.value === 'structure' &&
      (block.type.startsWith('style_') || block.type.startsWith('script'))
    )
      return;

    if (activeMode.value === 'style' && !block.type.startsWith('style_'))
      return;

    if (
      activeMode.value === 'logic' &&
      !block.type.startsWith('script') &&
      !block.type.startsWith('logic_')
    )
      return;

    const name = block.getFieldValue('NAME') || block.type;
    current.push({ id: block.id, name, type: block.type });
  });

  objects.value = current;
};

// ====================================================
// 코드 생성 + preview 갱신 (좌표는 HTML에 주입 ❌)
// ====================================================
const refreshCodeAndPreview = () => {
  if (!workspace) return;

  try {
    saveCurrentWorkspaceToPage();

    const page = pages.value.find((p) => p.id === selectedPageId.value);
    if (page) {
      // 1. 현재 어떤 탭(모드)인지 확인합니다.
      const currentXml = page.workspaces[activeMode.value];
      const rawCode = generateCodeFromXML(currentXml);

      // 2. 모드에 따라 처리 방식을 다르게 합니다.
      if (activeMode.value === 'structure') {
        // 화면 구성 탭일 때는 깨끗한 HTML만 출력
        generatedCode.value = cleanCodeForView(rawCode);
      } else if (activeMode.value === 'style') {
        // 디자인 탭일 때는 생성된 CSS 코드를 출력
        generatedCode.value = rawCode; 
      } else {
        // 로직 탭일 때는 자바스크립트 코드를 출력
        generatedCode.value = rawCode;
      }
    }

    updatePreview();
    updateObjectListFromWorkspace();
  } catch (e) {
    console.error(e);
  }
};

// ====================================================
// ✅ 누락되었던 핵심 함수: 세 가지 모드의 코드를 하나로 합칩니다.
// ====================================================
// 1. 외부에서도 호출할 수 있도록 함수 이름을 원상복구합니다.
/**
 * [A. 블록 -> 코드 -> 프리뷰 렌더]
 * 외부(refreshCodeAndPreview 등)에서 이 이름을 직접 호출하므로,
 * 함수를 updatePreview 밖으로 완전히 빼서 정의해야 에러가 나지 않습니다.
 */
// [1] 빌드 함수 롤백 (상단에 배치)
const buildPreviewDocument = (page) => {
  if (!page || !page.workspaces) return '';

  // 각 워크스페이스(구조/스타일/로직)의 XML을 실제 코드로 변환
  const structureCode = generateCodeFromXML(page.workspaces.structure) || '';
  const styleCode = generateCodeFromXML(page.workspaces.style) || '';
  const logicCode = generateCodeFromXML(page.workspaces.logic) || '';

  // iframe에 한 번에 넣을 수 있도록 합쳐서 반환
  return `
    <style>${styleCode}<\/style>
    ${structureCode}
    <script>${logicCode}<\/script>
  `;
};

// [2] 프리뷰 업데이트 함수 롤백
const updatePreview = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  // ✅ [수정] 현재 워크스페이스의 실시간 XML을 가져옵니다.
  const currentXml = workspace ? Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace)) : '';

  // ✅ [핵심] 현재 편집 중인 모드(activeMode)의 데이터는 실시간 currentXml에서 뽑아냅니다.
  const structureCode = activeMode.value === 'structure' 
    ? generateCodeFromXML(currentXml) 
    : generateCodeFromXML(page.workspaces.structure);

  const styleCode = activeMode.value === 'style' 
    ? generateCodeFromXML(currentXml) 
    : generateCodeFromXML(page.workspaces.style);

  const logicCode = activeMode.value === 'logic' 
    ? generateCodeFromXML(currentXml) 
    : generateCodeFromXML(page.workspaces.logic);

  const finalLogicScript = isRunning.value ? logicCode : '';
  const modeClass = isRunning.value ? 'is-running' : 'is-design';
  const positionsJSON = JSON.stringify(getPositionsMap());

  // 3. 에러 방지용 조립 방식 (백틱 기호 충돌 해결)
  const htmlParts = [
    '<!DOCTYPE html><html><head><meta charset="utf-8">',

    // ✅ 기본 CSS
    '<style>',
    '  html, body { margin:0; padding:0; width:100%; min-height:100vh; overflow:hidden; background:#fff; }',
    '  * { box-sizing: border-box; }',
    '  #wrapper { width:100%; min-height:100vh; position:relative; background:#fff; }',
    '  #wrapper > [data-draggable="true"][data-block-id] {',
    '    position: absolute; left: 0; top: 0; transform:none;',
    '    touch-action:none; user-select:none; -webkit-user-select:none; cursor: grab;',
    '  }',
    // (block계열만 부모폭 채우기)
    '  #wrapper > [data-draggable="true"][data-block-id]:is(div, section, article, header, nav, main, aside, footer, form, ul) {',
    '     max-width: 100%;',
    '  }',
    '  .wc-highlight { outline:2px solid #ff4081 !important; z-index: 9999; }',
    '  .wc-dragging { opacity:0.9; box-shadow: 0 10px 20px rgba(0,0,0,0.2); outline: 2px dashed #2196f3 !important; cursor: grabbing; transition:none !important; z-index: 9999; }',
    '  .wc-guide-line { position:absolute; z-index: 10000; pointer-events:none; display:none; border-color: rgba(255, 0, 0, 0.75); border-style: dashed; }',
    '  .wc-guide-v { width:0; border-left-width:1px; }',
    '  .wc-guide-h { height:0; border-top-width:1px; }',
      // ✅ 프리뷰에서만: 빈 컨테이너 안내
    '  [data-wc-block]{',
    '    position: relative;', // ::before/after 위치 기준
    '    min-width: 50px;',
    '    min-height: 50px;',
    '  }',
    '',
    '  /* ⚠️ empty는 "진짜로 텍스트/태그가 아무것도 없을 때"만 먹음 */',
    '  [data-wc-block]:not(:has(*))::after{',
    '    content: "📦 여기에 콘텐츠를 추가하세요";',
    '    color: #aaa;',
    '    font-size: 12px;',
    '    line-height: 1;',
    '    display: flex;',
    '    align-items: center;',
    '    justify-content: center;',
    '    position: absolute;',
    '    inset: 0;',
    '    pointer-events: none;',
    '    opacity: 0.8;',
    '  }',
    '</style>',
    (styleCode || ''),
    '</head>',
    '<body class="' + modeClass + '">',
    '<div id="wrapper">',
    (structureCode || ''),
    '  <div id="wcGuideV" class="wc-guide-line wc-guide-v"></div>',
    '  <div id="wcGuideH" class="wc-guide-line wc-guide-h"></div>',
    '</div>',
    // ✅ RUN일 때만 logic 실행
    finalLogicScript,
    '<script>',
    'const WC_POSITIONS = ' + positionsJSON + ';',
    'const isRunning = ' + isRunning.value + ';',

    'function applyBuilderStyles(){',
    '  const nodes = document.querySelectorAll("[data-wc-style]");',
    '  nodes.forEach(el => { el.style.cssText += ";" + el.getAttribute("data-wc-style"); });',
    '}',

    'function syncClassStyles(){',
    '  const styleText = document.querySelector("style").textContent;',
    '  const classMatches = styleText.match(/\\.([a-zA-Z0-9_-]+)\\s*\\{/g) || [];',
    '  classMatches.forEach(m => {',
    '    const className = m.replace(".", "").replace("{", "").trim();',
    '    document.querySelectorAll("[data-block-id=\'"+className+"\']").forEach(el => el.classList.add(className));',
    '  });',
    '}',

    'function applyBuilderStyles(){',
    '  const root = document.getElementById("wrapper");',
    '  if(!root) return;',
    '  const nodes = root.querySelectorAll("[data-wc-style]");',
    '  nodes.forEach((el) => {',
    '    const css = (el.getAttribute("data-wc-style") || "").trim();',
    '    if(!css) return;',
    '    const cur = (el.getAttribute("style") || "");',
    '    const join = (cur && !cur.trim().endsWith(";")) ? (cur + ";") : cur;',
    '    el.setAttribute("style", join + css);',
    '  });',
    '}',

    'function hideGuides(){',
    '  const v = document.getElementById("wcGuideV");',
    '  const h = document.getElementById("wcGuideH");',
    '  if(v) v.style.display = "none";',
    '  if(h) h.style.display = "none";',
    '}',

    'function showVSeg(x, y1, y2){',
    '  const v = document.getElementById("wcGuideV");',
    '  if(!v) return;',
    '  v.style.left = x + "px"; v.style.top = Math.min(y1,y2) + "px";',
    '  v.style.height = Math.abs(y2 - y1) + "px"; v.style.display = "block";',
    '}',

    'function showHSeg(y, x1, x2){',
    '  const h = document.getElementById("wcGuideH");',
    '  if(!h) return;',
    '  h.style.top = y + "px"; h.style.left = Math.min(x1,x2) + "px";',
    '  h.style.width = Math.abs(x2 - x1) + "px"; h.style.display = "block";',
    '}',

    'function applyPositions(){',
    '  const wrap = document.getElementById("wrapper");',
    '  if(!wrap) return;',
    '  const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\'true\'][data-block-id]"));',
    '  for(const el of els){',
    '    const id = el.getAttribute("data-block-id");',
    '    const p = WC_POSITIONS[id];',
    '    if (p && Number.isFinite(p.x)){ el.style.left = p.x + "px"; el.style.top = p.y + "px"; }',
    '  }',
    '}',

    'function collectGuides(exceptEl){',
    '  const wrap = document.getElementById("wrapper");',
    '  const wrapRect = wrap.getBoundingClientRect();',
    '  const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\'true\'][data-block-id]")).filter(el => el !== exceptEl);',
    '  return {',
    '    wrapRect,',
    '    items: els.map(el => {',
    '      const r = el.getBoundingClientRect();',
    '      const left = r.left - wrapRect.left; const right = r.right - wrapRect.left;',
    '      const top = r.top - wrapRect.top; const bottom = r.bottom - wrapRect.top;',
    '      return { rect: { left, right, top, bottom, width: r.width, height: r.height }, v: [left, (left+right)/2, right], h: [top, (top+bottom)/2, bottom] };',
    '    })',
    '  };',
    '}',

    'function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){',
    '  const curLeft = nextLeft, curRight = nextLeft + width, curTop = nextTop, curBottom = nextTop + height;',
    '  const curCX = (curLeft + curRight) / 2, curCY = (curTop + curBottom) / 2;',
    '  const selfV = [{x:curLeft},{x:curCX},{x:curRight}], selfH = [{y:curTop},{y:curCY},{y:curBottom}];',
    '  let best = { dx: 0, dy: 0, vLine: null, hLine: null, vSeg: null, hSeg: null, vDist: 6, hDist: 6 };',
    '  guides.items.forEach(it => {',
    '    it.v.forEach(gx => selfV.forEach(sv => {',
    '      const d = Math.abs(gx - sv.x);',
    '      if(d < best.vDist){ best.vDist = d; best.dx = gx - sv.x; best.vLine = gx; best.vSeg = { y1: Math.min(curTop, it.rect.top), y2: Math.max(curBottom, it.rect.bottom) }; }',
    '    }));',
    '    it.h.forEach(gy => selfH.forEach(sh => {',
    '      const d = Math.abs(gy - sh.y);',
    '      if(d < best.hDist){ best.hDist = d; best.dy = gy - sh.y; best.hLine = gy; best.hSeg = { x1: Math.min(curLeft, it.rect.left), x2: Math.max(curRight, it.rect.right) }; }',
    '    }));',
    '  });',
    '  return best;',
    '}',

    'function init(){',
    '  applyBuilderStyles();', // 인라인 스타일 적용
    '  syncClassStyles();',    // 클래스 기반 스타일 강제 연결
    '  applyPositions();',     // 좌표 적용

    '  window.addEventListener("message", (e) => {',
    '    if(e.data.type === "highlight_element"){',
    '      document.querySelectorAll(".wc-highlight").forEach(el => el.classList.remove("wc-highlight"));',
    '      const t = document.querySelector("[data-block-id=\'"+e.data.blockId+"\']");',
    '      if(t) t.classList.add("wc-highlight");',
    '    }',
    '  });',

    '  if(isRunning) return;',

    '  const wrap = document.getElementById("wrapper");',
    '  let dragging = null;',
    '  wrap.addEventListener("pointerdown", (ev) => {',
    '    const t = ev.target.closest("#wrapper > [data-draggable=\'true\'][data-block-id]");',
    '    if(!t) return;',
    '    const r = t.getBoundingClientRect(), wr = wrap.getBoundingClientRect();',
    '    dragging = { el: t, baseLeft: r.left - wr.left, baseTop: r.top - wr.top, startX: ev.clientX, startY: ev.clientY, guides: collectGuides(t), pointerId: ev.pointerId };',
    '    t.classList.add("wc-dragging"); t.setPointerCapture(ev.pointerId);',
    '  });',

    '  wrap.addEventListener("pointermove", (ev) => {',
    '    if(!dragging) return;',
    '    const dx = ev.clientX - dragging.startX, dy = ev.clientY - dragging.startY;',
    '    let nextL = dragging.baseLeft + dx, nextT = dragging.baseTop + dy;',
    '    const r = dragging.el.getBoundingClientRect();',
    '    const snap = computeSmartSnap({ nextLeft: nextL, nextTop: nextT, width: r.width, height: r.height, guides: dragging.guides });',
    '    hideGuides();',
    '    if(snap.vLine) showVSeg(snap.vLine, snap.vSeg.y1, snap.vSeg.y2);',
    '    if(snap.hLine) showHSeg(snap.hLine, snap.hSeg.x1, snap.hSeg.x2);',
    '    dragging.el.style.left = (nextL + snap.dx) + "px"; dragging.el.style.top = (nextT + snap.dy) + "px";',
    '  });',

    '  wrap.addEventListener("pointerup", (ev) => {',
    '    if(!dragging) return;',
    '    const t = dragging.el; hideGuides(); t.classList.remove("wc-dragging");',
    '    window.parent.postMessage({ type: "update_free_position", blockId: t.getAttribute("data-block-id"), x: parseInt(t.style.left), y: parseInt(t.style.top) }, "*");',
    '    dragging = null;',
    '  });',
    '}',

    'window.onload = init;',
    '<\/script>',
    '</body></html>',
  ];

  previewSrc.value = '';
  nextTick(() => {
    previewSrc.value = htmlParts.join('\n');
  });
};

/* ============================================================
 * 커스텀 블록 등록
 * ============================================================ */
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
  Animation.defineBlocks();
};

// toolbox / category
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
  animation: Animation.toolbox,
  data: `<xml><category name="변수" custom="VARIABLE" colour="#a55b80"></category></xml>`,
  advanced: `<xml><category name="함수" custom="PROCEDURE" colour="#995ba5"></category></xml>`,
  empty: `<xml><category name="dummy" style="display:none"></category></xml>`,
};

const setToolbox = (xmlText) => {
  let text = (xmlText || '<xml></xml>').trim();

  // 1. 카테고리 감싸기
  if (text.indexOf('<category') === -1) {
    text = text.replace(/^<xml[^>]*>/, '').replace(/<\/xml>$/, '');
    text = `<xml><category name="root_category">${text}</category></xml>`;
  }

  try {
    // 2. 툴박스 업데이트
    const dom = Blockly.utils.xml.textToDom(text);
    workspace.updateToolbox(dom);

    // 3. 회색 사이드바 숨기기
    const workspaceDom = workspace.getParentSvg().parentNode;
    const toolboxDiv = workspaceDom.querySelector('.blocklyToolboxDiv');
    if (toolboxDiv) toolboxDiv.style.display = 'none';

    // 4. 블록창 강제 오픈 & 자동 닫힘 방지
    const toolbox = workspace.getToolbox();
    if (
      toolbox &&
      toolbox.getToolboxItems &&
      toolbox.getToolboxItems().length > 0
    ) {
      toolbox.selectItemByPosition(0);

      const flyout = workspace.getFlyout();
      if (flyout) flyout.autoClose = false;

      // 5. ✨ 애니메이션 수정 (setTimeout 제거 및 즉시 스타일 적용)
      Blockly.svgResize(workspace);

      const allFlyouts = document.querySelectorAll('.blocklyFlyout');

      allFlyouts.forEach((flyoutSvg) => {
        const blocks = flyoutSvg.querySelector('.blocklyBlockCanvas');
        const bg = flyoutSvg.querySelector('.blocklyFlyoutBackground');

        // [핵심] 1. 브라우저가 그리기 전에 강제로 먼저 숨깁니다 (Flicker 방지)
        if (blocks) {
          blocks.style.opacity = '0';
          // transform은 Blockly 스크롤 좌표와 충돌할 수 있으므로 opacity로만 깜빡임 제어
        }
        if (bg) {
          bg.style.opacity = '0';
        }

        // [핵심] 2. 다음 프레임에 애니메이션 즉시 실행 (딜레이 없음)
        requestAnimationFrame(() => {
          // [블록]: 왼쪽 -> 제자리
          if (blocks) {
            const animations = blocks.getAnimations();
            animations.forEach((anim) => anim.cancel());

            blocks.animate(
              [
                { transform: 'translate(-300px, 0)', opacity: 0 }, // 시작
                { transform: 'translate(0, 0)', opacity: 1 }, // 끝
              ],
              {
                duration: 300,
                easing: 'ease',
                fill: 'forwards',
                composite: 'add',
              }
            );
          }

          // [배경]: Fade In
          if (bg) {
            const bgAnims = bg.getAnimations();
            bgAnims.forEach((anim) => anim.cancel());

            bg.animate([{ opacity: 0 }, { opacity: 1 }], {
              duration: 250,
              easing: 'linear',
              fill: 'forwards',
            });
          }
        });
      });
    }
  } catch (e) {
    console.error('🚨 툴박스 오류:', e);
  }
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
    items: ['style', 'color', 'animation', 'responsive', 'flex'],
  },
  {
    id: 'logic',
    label: '로직/데이터',
    icon: '⚡',
    color: '#2196f3',
    items: ['interaction', 'flow', 'logic', 'data', 'advanced'],
  },
];
const currentSubItems = computed(() => {
  const group = categoryGroups.find((g) => g.id === activeParent.value);
  return group ? group.items : [];
});

const categories = {
  layout: Layout.category,
  content: Content.category,
  form: Form.category,
  component: { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  style: Style.category,
  color: Color.category,
  flex: Flex.category,
  responsive: Responsive.category,
  animation: Animation.category,
  interaction: Interaction.category,
  flow: Flow.category,
  logic: Logic.category,
  data: { label: '데이터', color: '#26a69a', icon: '💾' },
  advanced: { label: '고급', color: '#424242', icon: '⚙️' },
};

const selectParent = (modeId) => {
  // 같은 모드면 무시
  if (activeMode.value === modeId) return;

  // 1️⃣ 현재 workspace 저장
  saveCurrentWorkspaceToPage();

  // 2️⃣ 모드 변경 (UI + 엔진)
  activeMode.value = modeId;
  activeParent.value = modeId;
  activeTab.value = null;

  // 3️⃣ workspace 교체
  if (!workspace) return;
  workspace.clear();

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  const xml = page?.workspaces?.[modeId];

  if (xml) {
    try {
      Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(xml), workspace);
    } catch (e) {
      console.error('XML 로드 에러:', e);
    }
  }

  // 4️⃣ 툴박스 초기화 (빈 상태)
  setToolbox(toolboxXMLs.empty);
  refreshCodeAndPreview();
};
const selectCategory = (key) => {
  if (!workspace) return;
  if (activeTab.value === key) {
    activeTab.value = null;
    setToolbox(toolboxXMLs.empty);
    return;
  }
  activeTab.value = key;
  setToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
  setTimeout(() => Blockly.svgResize(workspace), 300);
};

// ====================================================
// 9) 페이지 저장/로드
// ====================================================
const saveCurrentWorkspaceToPage = () => {
  if (!workspace) return;

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  const xmlText = Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace));

  // ✅ 현재 단계에서는 structure만 사용
  page.workspaces[activeMode.value] = xmlText;

  savePagesToStorage();
};

const loadPageById = (pageId) => {
  if (!workspace) return;
  const page = pages.value.find((p) => p.id === pageId);
  if (!page) return;

  selectedPageId.value = page.id;
  workspace.clear();

  const xml = page.workspaces?.[activeMode.value];
  if (xml) {
    try {
      Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(xml), workspace);
    } catch (e) {}
  }

  refreshCodeAndPreview();
  handleSelection(null);
};

const selectPage = (pageId) => {
  // 1️⃣ 현재 작업 중인 모드 workspace 저장
  saveCurrentWorkspaceToPage();

  // 2️⃣ 페이지 이동 시 기본 모드로 리셋
  activeMode.value = 'structure';
  activeParent.value = 'structure';
  activeTab.value = null;

  // 3️⃣ 페이지 로드
  loadPageById(pageId);
};

const deletePageNow = (pageId) => {
  if (pages.value.length <= 1) {
    openModal('최소 하나의 페이지는 있어야 합니다.', 'info');
    return;
  }

  const idx = pages.value.findIndex((p) => p.id === pageId);
  if (idx !== -1) {
    pages.value.splice(idx, 1);
    savePagesToStorage();
    if (selectedPageId.value === pageId) loadPageById(pages.value[0].id);
  }
};

// 클릭 시에는 confirm 모달만 띄움
const deletePage = (pageId) => {
  if (pages.value.length <= 1) {
    openModal('최소 하나의 페이지는 있어야 합니다.', 'info');
    return;
  }
  openDeleteConfirm(pageId);
};

const addPage = () => {
  const page = createPage(`새 페이지 ${pages.value.length + 1}`);
  pages.value.push(page);
  savePagesToStorage();
  selectPage(page.id);
};

const startEditPageName = (page) => {
  editingPageId.value = page.id;
  editingPageName.value = page.name;
};
const commitEditPageName = (pageId) => {
  const page = pages.value.find((p) => p.id === pageId);
  if (page) {
    page.name = editingPageName.value;
    savePagesToStorage();
  }
  editingPageId.value = null;
};
const cancelEditPageName = () => {
  editingPageId.value = null;
  editingPageName.value = '';
};

// ====================================================
// 10) RUN/디바이스 토글
// ====================================================
const toggleRun = async () => {
  isRunning.value = !isRunning.value;
  await nextTick();
  updatePreview();
};
const changeModel = () => {
  isPhone.value = !isPhone.value;
  updatePreview();
};

// ====================================================
// 11) onMounted: Blockly 주입 + 이벤트 + 메시지 수신
// ====================================================
onMounted(async () => {
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();
  await nextTick();

  workspace = Blockly.inject('blocklyDiv', {
    renderer: 'zelos',
    toolbox: toolboxXMLs.empty,
    move: { scrollbars: true, drag: true, wheel: true },
    zoom: { controls: true, wheel: true, startScale: 0.8 },
    grid: { spacing: 20, length: 3, colour: '#ccc', snap: true },
    trashcan: true,
  });

  let debounceTimer = null;
  workspace.addChangeListener((e) => {
  // 1. 블록 선택 시 하이라이트 (즉시 실행)
  if (e.type === Blockly.Events.SELECTED) {
    if (!isSelectingProgrammatically)
      handleSelection(e.newElementId, 'blockly');
    return;
  }

  // 2. 불필요한 UI 이벤트 무시
  if (e.type === Blockly.Events.UI || e.type === Blockly.Events.CLICK) return;

  // ✨ [추가] 이름 변경(BLOCK_CHANGE)이나 생성/삭제 시 객체 리스트를 즉시 갱신
  // 0.5초를 기다리지 않고 리스트 UI만 먼저 바꿔줍니다.
  if (
    e.type === Blockly.Events.BLOCK_CHANGE || 
    e.type === Blockly.Events.BLOCK_CREATE || 
    e.type === Blockly.Events.BLOCK_DELETE ||
    e.type === Blockly.Events.BLOCK_MOVE
  ) {
    updateObjectListFromWorkspace(); 
  }

  // 3. 코드 생성 및 미리보기 갱신 (무거운 작업이므로 0.5초 디바운스 유지)
  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    refreshCodeAndPreview();
    if (selectedBlockId.value)
      handleSelection(selectedBlockId.value, 'blockly');
  }, 500);
});

  window.addEventListener('message', (event) => {
    if (event.data.type === 'update_free_position') {
      const { blockId, x, y, w, h } = event.data;
      const block = workspace.getBlockById(blockId);
      if (block) {
        block.data = JSON.stringify({
          x: Number(x || 0),
          y: Number(y || 0),
        });
        saveCurrentWorkspaceToPage();
        refreshCodeAndPreview();
      }
    }
    if (event.data.type === 'change_page_request') {
      const targetId = event.data.pageId;
      const targetPage = pages.value.find(
        (p) => p.id === targetId || p.name === targetId
      );

      if (targetPage) {
        selectPage(targetPage.id); // 실제 에디터 페이지 변경
      } else {
        alert('이동할 페이지를 찾을 수 없습니다: ' + targetId);
      }
    }

    if (event.data.type === 'select_block')
      handleSelection(event.data.blockId, 'iframe');
    if (event.data.type === 'deselect_block') handleSelection(null, 'iframe');
  });
  window.WC_GET_PAGES = () => {
    if (!pages.value || pages.value.length === 0) {
      return [['페이지 없음', '']];
    }
    return pages.value.map((p) => [p.name, p.id]);
  };

  const stored = loadPagesFromStorage();

  if (stored && stored.length > 0) {
    pages.value = stored;

    // 1. 현재 선택된 ID가 실제 데이터(pages) 안에 존재하는지 확인합니다.
    const isIdValid = pages.value.some((p) => p.id === selectedPageId.value);

    // 2. ID가 유효하면 그대로 쓰고, 없거나 비어있다면 첫 번째 페이지의 ID를 선택합니다.
    const targetId = isIdValid ? selectedPageId.value : pages.value[0].id;

    // 3. 상태 업데이트 및 로드
    selectedPageId.value = targetId;
    loadPageById(targetId);
  } else {
    // 저장된 데이터가 아예 없는 경우 새 페이지 생성
    addPage();
  }

  // 워크스페이스 크기 조절 리사이저 (유지)
  new ResizeObserver(() => {
    if (workspace) Blockly.svgResize(workspace);
  }).observe(document.getElementById('workspace-area'));
});
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
            class="cursor-pointer inline-flex items-center gap-[5px] text-white hover:text-gray-300"
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
            :sandbox="'allow-same-origin allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox allow-scripts'"
          >
          </iframe>
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
            <span>총 {{ pages.length }}개</span>
            <button class="btn-add-mini" @click="addPage">➕ 추가</button>
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
              <div class="name-container" style="flex: 1">
                <input
                  v-if="editingPageId === page.id"
                  v-model="editingPageName"
                  v-focus
                  class="edit-input"
                  @click.stop
                  @keyup.enter="commitEditPageName(page.id)"
                  @keyup.esc="cancelEditPageName"
                  @blur="commitEditPageName(page.id)"
                />
                <span
                  v-else
                  class="item-name"
                  @dblclick.stop="startEditPageName(page)"
                  title="더블클릭하여 이름 수정"
                >
                  {{ page.name }}
                </span>
              </div>
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
            <li
              v-for="obj in objects"
              :key="obj.id"
              class="list-item"
              :class="{ active: selectedBlockId === obj.id }"
              @click="selectObjectFromList(obj.id)"
            >
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
        <p class="desc">원하는 디자인을 설명하면 블록을 조립해줍니다.</p>
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
    <!--삭제/취소 모달-->
    <ConfirmModal
      :open="confirmModal.open"
      type="warning"
      :message="confirmModal.message"
      confirm-text="삭제"
      cancel-text="취소"
      @confirm="confirmDeletePage"
      @cancel="closeDeleteConfirm"
    />
    <!--단순 안내 모달-->
    <GlobalModal
      :open="modal.open"
      :message="modal.message"
      :type="modal.type"
      @confirm="closeModal"
    />
  </div>
</template>

<style scoped>
.ide-container {
  padding-top: 70px;
  height: 100vh;
  display: flex;
  flex-direction: row;
  background-color: #f0f0f0;
  overflow: visible;
}
.entry-panel {
  background: #f5f5f5;
  border-right: 1px solid #1a1a2e;
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
  overflow: visible;
}
.control-buttons {
  display: flex;
  gap: 6px;
  font-size: 0.85rem;
  align-items: center;
}
.btn-ai,
.btn-toggle,
.btn-deploy {
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
  color: white;
}
.btn-ai {
  background: #9c27b0;
}
.btn-ai:hover {
  background: #7b1fa2;
}
.btn-toggle {
  background: #4caf50;
  display: flex;
  align-items: center;
  gap: 5px;
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
}
.status-slot {
  width: 78px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  flex-shrink: 0;
}
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
.edit-input {
  width: 100%;
  padding: 2px 4px;
  font-size: 0.85rem;
  border: 1px solid #4c97ff;
  border-radius: 4px;
  outline: none;
  background: white;
  color: #333;
}
.name-container {
  display: flex;
  align-items: center;
  overflow: hidden;
}
.ide-main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
:deep(.blocklyScrollbarHorizontal),
:deep(.blocklyScrollbarVertical) {
  display: none;
}
.top-nav-bar {
  height: 60px;
  background: #1a1a2e;
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
  color: #777;
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
.workspace-row {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}
.sub-sidebar {
  width: 70px;
  background: #1a1a2e;
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

:deep(.blocklyToolbox) {
  display: none !important; /* ⭕ 회색 사이드바 영구 숨김 */
}
:deep(.blocklyFlyoutBackground) {
  fill: #c0c0c0 !important;
  fill-opacity: 0.2 !important;
}
.workspace-wrapper:not(.drawer-open) :deep(.blocklyToolboxDiv) {
  transform: translateX(-100%);
  opacity: 0;
  pointer-events: none;
}
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

.mode-dropdown {
  position: relative;
  margin-left: 10px;
}



.mode-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 6px;
  background: #1a1a2e;
  border: 1px solid #333;
  border-radius: 6px;
  min-width: 140px;
  z-index: 99999; /* 🔥 중요 */
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.4);
}

.mode-item {
  padding: 8px 12px;
  cursor: pointer;
  color: #ccc;
  font-size: 0.8rem;
}

.mode-item:hover,
.mode-item.active {
  background: #252535;
  color: white;
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
