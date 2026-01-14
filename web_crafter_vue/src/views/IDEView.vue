<script setup>
/**
 * ============================================================
 * ✅ Web Crafter IDE (Final Fixed Version)
 * - 순환 참조 오류 해결 (pages 초기화 순서 변경)
 * - URL 중복 방지 로직 적용
 * ============================================================
 */

import { ref, onMounted, nextTick, watch, computed } from 'vue';
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import * as Ko from 'blockly/msg/ko';
import 'blockly/blocks';
import ConfirmModal from '@/modal/ConfirmModal.vue';
import GlobalModal from '@/modal/GlobalModal.vue';

// ===== 카테고리 블록 import =====
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
import * as Style from '@/components/style/Style.vue';
import * as Responsive from '@/components/style/Responsive.vue';
import * as Color from '@/components/style/Color.vue';
import * as Flex from '@/components/style/Flex.vue';
import * as Animation from '@/components/style/Animation.vue';
import * as Interaction from '@/components/js/Interaction.vue';
import * as Flow from '@/components/js/Flow.vue';
import * as Logic from '@/components/js/Logic.vue';

/* ============================================================
 * 🚀 [Page Engine] 로직
 * ============================================================ */

function slugify(name) {
  return (
    "/" +
    name
      .trim()
      .replace(/\s+/g, "-")
      .replace(/[^a-zA-Z0-9\-\uAC00-\uD7A3]+/g, "")
      .toLowerCase()
  );
}

// 2. ✨ [Fix] 안전한 유니크 URL 생성기
function getUniqueRoute(name, excludeId = null) {
  let baseSlug = slugify(name);
  if (baseSlug === '/') baseSlug = '/home';

  let candidate = baseSlug;
  let counter = 1;

  // pages가 아직 초기화 전(undefined)이거나 비어있으면 검사 없이 바로 반환
  if (!pages.value || pages.value.length === 0) return candidate;

  while (pages.value.some(p => p.route === candidate && p.id !== excludeId)) {
    candidate = `${baseSlug}-${counter}`;
    counter++;
  }
  return candidate;
}

// 3. 페이지 생성
function createPage(name) {
  const generatedId = "page_" + (typeof crypto !== 'undefined' ? crypto.randomUUID().slice(0, 6) : Date.now().toString(36));
  
  // pages에 안전하게 접근하여 라우트 생성
  const safeRoute = getUniqueRoute(name);

  return {
    id: generatedId,
    name: name,
    route: safeRoute,
    aliases: [],
    status: "DRAFT",
    workspaces: { structure: '<xml></xml>', style: '<xml></xml>', logic: '<xml></xml>' },
  };
}

/* ============================================================
 * UI 상태 및 초기화
 * ============================================================ */
const activeParent = ref('structure');
const activeMode = ref('structure');
const activeTab = ref(null);
const activeRightTab = ref('objects');
const previewSrc = ref('');
const isRunning = ref(false);
const isPhone = ref(false);
const modeOpen = ref(false);
let workspace = null;

const modeList = [
  { id: 'structure', label: '화면구성', icon: '🏗️' },
  { id: 'style', label: '스타일', icon: '🎨' },
  { id: 'logic', label: '로직/데이터', icon: '⚡' },
];

const currentMode = computed(() => modeList.find((m) => m.id === activeMode.value) || modeList[0]);

const changeMode = (modeId) => {
  modeOpen.value = false;
  selectParent(modeId);
};

// ✨ [핵심 수정] pages 선언과 초기값 주입 분리 (순환 참조 방지)
// 1. 빈 배열로 먼저 선언 (이제 createPage 안에서 pages.value 접근 가능)
const pages = ref([]); 

// 2. 초기 데이터 주입
pages.value.push(createPage('Home'));
pages.value.push(createPage('Login'));

// 3. 선택된 페이지 설정
const selectedPageId = ref(pages.value[0].id);

const currentPageUrl = computed(() => {
  const page = pages.value.find(p => p.id === selectedPageId.value);
  return page ? `https://web-crafter.app${page.route}` : 'https://web-crafter.app/';
});

// 기타 상태
const objects = ref([]);
const editingPageId = ref(null);
const editingPageName = ref('');
const generatedCode = ref('');
const codeCache = ref({ structure: '', style: '', logic: '' });
const selectedBlockId = ref(null);
let isSelectingProgrammatically = false;

// 모달 상태
const showAiModal = ref(false);
const aiPrompt = ref('');
const isGenerating = ref(false);
const aiPromptError = ref(false);
const confirmModal = ref({ open: false, message: '', payload: null });
const modal = ref({ open: false, message: '', type: 'info', onConfirm: null });

const vFocus = { mounted: (el) => el.focus() };

/* ============================================================
 * 카테고리 정의
 * ============================================================ */
const categoryGroups = [
  { id: 'structure', label: '화면 구성', icon: '🏗️', color: '#4caf50', items: ['layout', 'content', 'form', 'component'] },
  { id: 'style', label: '디자인', icon: '🎨', color: '#e91e63', items: ['style', 'color', 'animation', 'responsive', 'flex'] },
  { id: 'logic', label: '로직/데이터', icon: '⚡', color: '#2196f3', items: ['interaction', 'flow', 'logic', 'data', 'advanced'] },
];

const categories = {
  layout: Layout.category, content: Content.category, form: Form.category, component: { label: '컴포넌트', color: '#5c6bc0', icon: '🧱' },
  style: Style.category, color: Color.category, flex: Flex.category, responsive: Responsive.category, animation: Animation.category,
  interaction: Interaction.category, flow: Flow.category, logic: Logic.category, data: { label: '데이터', color: '#26a69a', icon: '💾' }, advanced: { label: '고급', color: '#424242', icon: '⚙️' },
};

const currentSubItems = computed(() => {
  const group = categoryGroups.find((g) => g.id === activeParent.value);
  return group ? group.items : [];
});

/* ============================================================
 * 페이지 관리 함수
 * ============================================================ */
function loadPagesFromStorage() { try { return JSON.parse(localStorage.getItem('wc_pages')); } catch (e) { return null; } }
function savePagesToStorage() { try { localStorage.setItem('wc_pages', JSON.stringify(pages.value)); } catch (e) {} }

const startEditPageName = (page) => { editingPageId.value = page.id; editingPageName.value = page.name; };

const commitEditPageName = (pageId) => {
  const page = pages.value.find((p) => p.id === pageId);
  if (page) {
    page.name = editingPageName.value;
    if (page.status === "DRAFT") {
      page.route = getUniqueRoute(page.name, page.id); // 수정 시에도 중복 체크
    }
    savePagesToStorage();
  }
  editingPageId.value = null;
};

const cancelEditPageName = () => { editingPageId.value = null; editingPageName.value = ''; };

const lockPage = (pageId) => {
  const page = pages.value.find(p => p.id === pageId);
  if (page && page.status !== "LOCKED") { page.status = "LOCKED"; savePagesToStorage(); }
};

const addPage = () => {
  const page = createPage(`Page ${pages.value.length + 1}`);
  pages.value.push(page);
  savePagesToStorage();
  selectPage(page.id);
};

const deletePageNow = (pageId) => {
  if (pages.value.length <= 1) { openModal('최소 하나의 페이지는 있어야 합니다.', 'info'); return; }
  const idx = pages.value.findIndex((p) => p.id === pageId);
  if (idx !== -1) {
    pages.value.splice(idx, 1);
    savePagesToStorage();
    if (selectedPageId.value === pageId) loadPageById(pages.value[0].id);
  }
};

const deletePage = (pageId) => {
  if (pages.value.length <= 1) { openModal('최소 하나의 페이지는 있어야 합니다.', 'info'); return; }
  openDeleteConfirm(pageId);
};

const openDeleteConfirm = (pageId) => { confirmModal.value = { open: true, message: '이 페이지를 삭제하시겠습니까?', payload: { pageId } }; };
const closeDeleteConfirm = () => { confirmModal.value = { ...confirmModal.value, open: false }; };
const confirmDeletePage = () => { const pageId = confirmModal.value.payload?.pageId; closeDeleteConfirm(); if (pageId) deletePageNow(pageId); };
const openModal = (message, type = 'info', onConfirm = null) => { modal.value = { open: true, message, type, onConfirm }; };
const closeModal = () => { modal.value.open = false; modal.value.onConfirm?.(); modal.value.onConfirm = null; };

/* ============================================================
 * 코드/프리뷰 로직
 * ============================================================ */
const cleanCodeForView = (code) => {
  if (!code) return '';
  try {
    const container = document.createElement('div');
    container.innerHTML = code;
    container.querySelectorAll('*').forEach((el) => {
      el.removeAttribute('data-block-id'); el.removeAttribute('data-draggable'); el.removeAttribute('data-x'); el.removeAttribute('data-y'); el.removeAttribute('data-wc-style'); el.removeAttribute('data-wc-block');
      if (el.hasAttribute('style')) {
        el.style.removeProperty('position'); el.style.removeProperty('left'); el.style.removeProperty('top'); el.style.removeProperty('transform');
        if (!el.getAttribute('style')?.trim()) el.removeAttribute('style');
      }
    });
    return container.innerHTML.trim();
  } catch (e) { return (code || '').replace(/\sdata-block-id="[^"]*"/g, '').trim(); }
};

const removeScripts = (html) => html ? html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '') : '';

const getPositionsMap = () => {
  const map = {};
  const page = pages.value.find(p => p.id === selectedPageId.value);
  if (!page) return map;

  const extractFromBlocks = (blocks) => {
    blocks.forEach((b) => {
      if (!b.data) return;
      try { 
        const p = JSON.parse(b.data); 
        if (Number.isFinite(p.x) && Number.isFinite(p.y)) {
          map[b.id] = { x: Number(p.x), y: Number(p.y) }; 
        }
      } catch (e) {}
    });
  };

  // ✅ [핵심 수정] activeMode 조건문을 제거합니다.
  // 현재 브라우저 메모리에 workspace가 살아있다면, 모드와 상관없이 최신 좌표를 가져옵니다.
  if (workspace) { 
    extractFromBlocks(workspace.getAllBlocks(false)); 
  } 

  // ✅ 워크스페이스에 데이터가 없거나, 다른 페이지 로딩 등의 경우에만 XML을 참조합니다.
  if (Object.keys(map).length === 0 && page.workspaces.structure) {
    try {
      const tempWs = new Blockly.Workspace();
      const dom = Blockly.utils.xml.textToDom(page.workspaces.structure);
      Blockly.Xml.domToWorkspace(dom, tempWs);
      extractFromBlocks(tempWs.getAllBlocks(false));
      tempWs.dispose();
    } catch (e) {}
  }
  
  return map;
};

const generateCodeFromXML = (xmlText) => {
  if (!xmlText || xmlText === '<xml></xml>') return '';
  try {
    const dom = Blockly.utils.xml.textToDom(xmlText);
    const headlessWorkspace = new Blockly.Workspace();
    Blockly.Xml.domToWorkspace(dom, headlessWorkspace);
    const code = javascriptGenerator.workspaceToCode(headlessWorkspace);
    headlessWorkspace.dispose();
    return code;
  } catch (e) { return ''; }
};

const handleSelection = (blockId, fromSource = 'unknown') => {
  if (blockId && selectedBlockId.value === blockId) return;
  selectedBlockId.value = blockId;
  if (workspace && fromSource !== 'blockly') {
    isSelectingProgrammatically = true;
    workspace.getAllBlocks(false).forEach((b) => b.unselect());
    if (blockId) workspace.getBlockById(blockId)?.select();
    isSelectingProgrammatically = false;
  }
  const iframe = document.getElementById('previewFrame');
  if (iframe?.contentWindow) iframe.contentWindow.postMessage({ type: 'highlight_element', blockId }, '*');
};

const selectObjectFromList = (objId) => { handleSelection(objId, 'list'); workspace?.centerOnBlock(objId); };

watch(objects, (newObjects) => { if (Interaction.updateObjectList) Interaction.updateObjectList(newObjects); }, { deep: true, immediate: true });

const updateObjectListFromWorkspace = () => {
  if (!workspace) return;
  const current = [];
  const blocks = workspace.getAllBlocks(false);
  const ignoredTypes = new Set(['event_click', 'event_page_load', 'action_alert']);
  blocks.forEach((block) => {
    if (ignoredTypes.has(block.type)) return;
    if (activeMode.value === 'structure' && (block.type.startsWith('style_') || block.type.startsWith('script'))) return;
    if (activeMode.value === 'style' && !block.type.startsWith('style_')) return;
    if (activeMode.value === 'logic' && !block.type.startsWith('script') && !block.type.startsWith('logic_')) return;
    current.push({ id: block.id, name: block.getFieldValue('NAME') || block.type, type: block.type });
  });
  objects.value = current;
};

const refreshCodeAndPreview = () => {
  if (!workspace) return;
  try {
    saveCurrentWorkspaceToPage();
    javascriptGenerator.init(workspace);
    const raw = javascriptGenerator.workspaceToCode(workspace);
    codeCache.value[activeMode.value] = raw;
    const page = pages.value.find((p) => p.id === selectedPageId.value);
    if (page) {
      const currentXml = page.workspaces[activeMode.value];
      const rawCode = generateCodeFromXML(currentXml);
      generatedCode.value = (activeMode.value === 'structure') ? cleanCodeForView(rawCode) : rawCode;
    }
    updatePreview();
    updateObjectListFromWorkspace();
  } catch (e) { console.error(e); }
};

const updatePreview = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  const currentXml = workspace ? Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace)) : '';
  const structureCode = activeMode.value === 'structure' ? generateCodeFromXML(currentXml) : generateCodeFromXML(page.workspaces.structure);
  const styleCode = activeMode.value === 'style' ? generateCodeFromXML(currentXml) : generateCodeFromXML(page.workspaces.style);
  const logicCode = activeMode.value === 'logic' ? generateCodeFromXML(currentXml) : generateCodeFromXML(page.workspaces.logic);

  const safeStyle = (styleCode.trim() && !styleCode.includes('<style')) ? `<style>${styleCode}</style>` : styleCode;
  const safeScript = (logicCode.trim() && !logicCode.includes('<script')) ? `<script>${logicCode}<\/script>` : logicCode;
  const finalLogicScript = isRunning.value ? safeScript : '';

  const positionsJSON = JSON.stringify(getPositionsMap());
  const PAGE_ID = page.id;
  const PAGE_ROUTE = page.route;

  const ANIMATION_DEFS = `
    @keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
    @keyframes bounce { 0%, 20%, 50%, 80%, 100% { transform: translateY(0); } 40% { transform: translateY(-20px); } 60% { transform: translateY(-10px); } }
    @keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
    @keyframes shake { 0%, 100% { transform: translateX(0); } 10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); } 20%, 40%, 60%, 80% { transform: translateX(5px); } }
    @keyframes zoom-in { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
    @keyframes rainbow { 0% { color: #ff0000; } 50% { color: #00ff00; } 100% { color: #ff0000; } }
    @keyframes float { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-15px); } }
`;

  const htmlParts = [
    '<!DOCTYPE html><html><head><meta charset="utf-8">',
    '<style>html, body { margin:0; padding:0; width:100%; min-height:100vh; overflow:hidden; background:#fff; } * { box-sizing: border-box; } #wrapper { width:100%; min-height:100vh; position:relative; background:#fff; } #wrapper > [data-draggable="true"][data-block-id] { position: absolute; left: 0; top: 0; transform:none; touch-action:none; user-select:none; -webkit-user-select:none; cursor: grab; } #wrapper > [data-draggable="true"][data-block-id]:is(div, section, article, header, nav, main, aside, footer, form, ul) { max-width: 100%; } .wc-highlight { outline:2px solid #ff4081 !important; z-index: 9999; } .wc-dragging { opacity:0.9; box-shadow: 0 10px 20px rgba(0,0,0,0.2); outline: 2px dashed #2196f3 !important; cursor: grabbing; transition:none !important; z-index: 9999; } .wc-guide-line { position:absolute; z-index: 10000; pointer-events:none; display:none; border-color: rgba(255, 0, 0, 0.75); border-style: dashed; } .wc-guide-v { width:0; border-left-width:1px; } .wc-guide-h { height:0; border-top-width:1px; } [data-wc-block] { position: relative; min-width: 50px; min-height: 50px; } [data-wc-block]:not(:has(*))::after { content: "📦"; color: #aaa; display: flex; align-items: center; justify-content: center; position: absolute; inset: 0; pointer-events: none; opacity: 0.5; } .is-design * { animation:none !important; transition:none !important; }</style>',
    `<style>${ANIMATION_DEFS}</style>`,
    safeStyle,
    '</head>',
    `<body class="${isRunning.value ? 'is-running' : 'is-design'}">`,
    '<div id="wrapper">', structureCode, '<div id="wcGuideV" class="wc-guide-line wc-guide-v"></div><div id="wcGuideH" class="wc-guide-line wc-guide-h"></div></div>',
    finalLogicScript,
    '<script>',
    `const WC_POSITIONS = ${positionsJSON}; const isRunning = ${isRunning.value}; const PAGE_ID = "${PAGE_ID}"; const PAGE_ROUTE = "${PAGE_ROUTE}";`,
    'function navigateToPage(targetId) { window.parent.postMessage({ type: "NAVIGATE", pageId: targetId }, "*"); }',
    'function redirectToPage(targetId) { window.parent.postMessage({ type: "REDIRECT", pageId: targetId }, "*"); }',
    'function goToPage(targetId) { navigateToPage(targetId); }',
    'function applyBuilderStyles(){ const nodes = document.querySelectorAll("[data-wc-style]"); nodes.forEach(el => { el.style.cssText += ";" + el.getAttribute("data-wc-style"); }); }',
    'function syncClassStyles(){ const styleText = document.querySelector("style")?.textContent || ""; const classMatches = styleText.match(/\\.([a-zA-Z0-9_-]+)\\s*\\{/g) || []; classMatches.forEach(m => { const className = m.replace(".", "").replace("{", "").trim(); document.querySelectorAll("[data-block-id=\'"+className+"\']").forEach(el => el.classList.add(className)); }); }',
    'function hideGuides(){ const v = document.getElementById("wcGuideV"); const h = document.getElementById("wcGuideH"); if(v) v.style.display = "none"; if(h) h.style.display = "none"; }',
    'function showVSeg(x, y1, y2){ const v = document.getElementById("wcGuideV"); if(!v) return; v.style.left = x + "px"; v.style.top = Math.min(y1,y2) + "px"; v.style.height = Math.abs(y2 - y1) + "px"; v.style.display = "block"; }',
    'function showHSeg(y, x1, x2){ const h = document.getElementById("wcGuideH"); if(!h) return; h.style.top = y + "px"; h.style.left = Math.min(x1,x2) + "px"; h.style.width = Math.abs(x2 - x1) + "px"; h.style.display = "block"; }',
    'function clamp(n, min, max) { return Math.max(min, Math.min(max, n)); }',

    // ✅ [수정 1] applyPositions에서도 음수 좌표를 0으로 보정해서 "기존 저장값(-3px)"도 즉시 복구
    'function applyPositions(){ const wrap = document.getElementById("wrapper"); if(!wrap) return; const wrapRect = wrap.getBoundingClientRect(); const targets = wrap.querySelectorAll(":scope > [data-draggable=\'true\']"); targets.forEach(el => { const id = el.getAttribute("data-block-id"); const p = WC_POSITIONS[id]; if(p && typeof p.x === "number"){ const elRect = el.getBoundingClientRect(); const maxX = Math.max(0, wrapRect.width - elRect.width); const maxY = Math.max(0, wrapRect.height - elRect.height); const x = clamp(Number(p.x) || 0, 0, maxX); const y = clamp(Number(p.y) || 0, 0, maxY); el.style.setProperty("position", "absolute", "important"); el.style.setProperty("left", x + "px", "important"); el.style.setProperty("top", y + "px", "important"); el.style.setProperty("transform", "none", "important"); } }); }',

    'function collectGuides(exceptEl){ const wrap = document.getElementById("wrapper"); const wrapRect = wrap.getBoundingClientRect(); const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\'true\'][data-block-id]")).filter(el => el !== exceptEl); return { wrapRect, items: els.map(el => { const r = el.getBoundingClientRect(); const left = r.left - wrapRect.left; const right = r.right - wrapRect.left; const top = r.top - wrapRect.top; const bottom = r.bottom - wrapRect.top; return { rect: { left, right, top, bottom, width: r.width, height: r.height }, v: [left, (left+right)/2, right], h: [top, (top+bottom)/2, bottom] }; }) }; }',
    'function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){ const curLeft = nextLeft, curRight = nextLeft + width, curTop = nextTop, curBottom = nextTop + height; const curCX = (curLeft + curRight) / 2, curCY = (curTop + curBottom) / 2; const selfV = [{x:curLeft},{x:curCX},{x:curRight}], selfH = [{y:curTop},{y:curCY},{y:curBottom}]; let best = { dx: 0, dy: 0, vLine: null, hLine: null, vSeg: null, hSeg: null, vDist: 6, hDist: 6 }; guides.items.forEach(it => { it.v.forEach(gx => selfV.forEach(sv => { const d = Math.abs(gx - sv.x); if(d < best.vDist){ best.vDist = d; best.dx = gx - sv.x; best.vLine = gx; best.vSeg = { y1: Math.min(curTop, it.rect.top), y2: Math.max(curBottom, it.rect.bottom) }; } })); it.h.forEach(gy => selfH.forEach(sh => { const d = Math.abs(gy - sh.y); if(d < best.hDist){ best.hDist = d; best.dy = gy - sh.y; best.hLine = gy; best.hSeg = { x1: Math.min(curLeft, it.rect.left), x2: Math.max(curRight, it.rect.right) }; } })); }); return best; }',

    'function init(){ applyBuilderStyles(); syncClassStyles(); applyPositions(); window.addEventListener("message", (e) => { if(e.data.type === "highlight_element"){ document.querySelectorAll(".wc-highlight").forEach(el => el.classList.remove("wc-highlight")); const t = document.querySelector("[data-block-id=\'"+e.data.blockId+"\']"); if(t) t.classList.add("wc-highlight"); } }); if(isRunning) return; const wrap = document.getElementById("wrapper"); let dragging = null; wrap.addEventListener("pointerdown", (ev) => { const t = ev.target.closest("#wrapper > [data-draggable=\'true\'][data-block-id]"); if(!t) return; const r = t.getBoundingClientRect(), wr = wrap.getBoundingClientRect(); dragging = { el: t, baseLeft: r.left - wr.left, baseTop: r.top - wr.top, startX: ev.clientX, startY: ev.clientY, guides: collectGuides(t), pointerId: ev.pointerId }; t.classList.add("wc-dragging"); t.setPointerCapture(ev.pointerId); window.parent.postMessage({ type: "select_block", blockId: t.getAttribute("data-block-id") }, "*"); });',

    // ✅ [수정 2] pointermove: 스냅 적용 후 clamp로 wrapper 밖 음수/초과 방지
    'wrap.addEventListener("pointermove", (ev) => { if(!dragging) return; const dx = ev.clientX - dragging.startX, dy = ev.clientY - dragging.startY; let nextL = dragging.baseLeft + dx, nextT = dragging.baseTop + dy; const r = dragging.el.getBoundingClientRect(); const snap = computeSmartSnap({ nextLeft: nextL, nextTop: nextT, width: r.width, height: r.height, guides: dragging.guides }); hideGuides(); if(snap.vLine) showVSeg(snap.vLine, snap.vSeg.y1, snap.vSeg.y2); if(snap.hLine) showHSeg(snap.hLine, snap.hSeg.x1, snap.hSeg.x2); const wrapRect = wrap.getBoundingClientRect(); const elRect = dragging.el.getBoundingClientRect(); let finalL = nextL + (snap.dx || 0); let finalT = nextT + (snap.dy || 0); const maxX = Math.max(0, wrapRect.width - elRect.width); const maxY = Math.max(0, wrapRect.height - elRect.height); finalL = clamp(finalL, 0, maxX); finalT = clamp(finalT, 0, maxY); dragging.el.style.left = finalL + "px"; dragging.el.style.top = finalT + "px"; });',

    // ✅ [수정 3] pointerup: 저장 전에 clamp로 음수 저장 방지 (+ 화면값도 정리)
    'wrap.addEventListener("pointerup", (ev) => { if(!dragging) return; const t = dragging.el; hideGuides(); t.classList.remove("wc-dragging"); const wrapRect = wrap.getBoundingClientRect(); const elRect = t.getBoundingClientRect(); const maxX = Math.max(0, wrapRect.width - elRect.width); const maxY = Math.max(0, wrapRect.height - elRect.height); let x = parseFloat(t.style.left) || 0; let y = parseFloat(t.style.top) || 0; x = clamp(x, 0, maxX); y = clamp(y, 0, maxY); t.style.left = x + "px"; t.style.top = y + "px"; window.parent.postMessage({ type: "update_free_position", blockId: t.getAttribute("data-block-id"), x, y }, "*"); dragging = null; }); }',

    'window.onload = init;',
    '<\/script>',
    '</body></html>'
  ];

  previewSrc.value = '';
  nextTick(() => { previewSrc.value = htmlParts.join('\n'); });
};

/* ============================================================
 * 커스텀 블록 등록 및 툴박스
 * ============================================================ */
const defineCustomBlocks = () => {
  Layout.defineBlocks(); Content.defineBlocks(); Style.defineBlocks(); Color.defineBlocks(); Flex.defineBlocks(); Interaction.defineBlocks(); Flow.defineBlocks(); Logic.defineBlocks(); Form.defineBlocks(); Responsive.defineBlocks(); Animation.defineBlocks();
};

const toolboxXMLs = {
  layout: Layout.toolbox, content: Content.toolbox, style: Style.toolbox, color: Color.toolbox, flex: Flex.toolbox, interaction: Interaction.toolbox, flow: Flow.toolbox, logic: Logic.toolbox, form: Form.toolbox, responsive: Responsive.toolbox, animation: Animation.toolbox,
  data: `<xml><category name="변수" custom="VARIABLE" colour="#a55b80"></category></xml>`,
  advanced: `<xml><category name="함수" custom="PROCEDURE" colour="#995ba5"></category></xml>`,
  empty: `<xml><category name="dummy" style="display:none"></category></xml>`,
};

const setToolbox = (xmlText) => {
  let text = (xmlText || '<xml></xml>').trim();
  if (!text.startsWith('<xml')) text = `<xml>${text}</xml>`;

  try {
    const dom = Blockly.utils.xml.textToDom(text);
    if (dom.getElementsByTagName('category').length === 0) {
      const category = Blockly.utils.xml.createElement('category');
      category.setAttribute('name', '요소');
      category.setAttribute('colour', '#5c6bc0');
      while (dom.firstChild) category.appendChild(dom.firstChild);
      dom.appendChild(category);
    }
    workspace.updateToolbox(dom);
    const workspaceDom = workspace.getParentSvg().parentNode;
    const toolboxDiv = workspaceDom.querySelector('.blocklyToolboxDiv');
    if (toolboxDiv) toolboxDiv.style.display = 'none';
    const toolbox = workspace.getToolbox();
    if (toolbox && toolbox.getToolboxItems && toolbox.getToolboxItems().length > 0) {
      toolbox.selectItemByPosition(0);
      workspace.getFlyout().autoClose = false;
      const immediateFlyouts = document.querySelectorAll('.blocklyFlyout');
      immediateFlyouts.forEach(flyout => {
          flyout.style.opacity = '0'; // 즉시 숨김
      });
      setTimeout(() => {
        Blockly.svgResize(workspace);
        const allFlyouts = document.querySelectorAll('.blocklyFlyout');
        allFlyouts.forEach((flyoutSvg) => {
          flyoutSvg.style.opacity = '1';
          const blocks = flyoutSvg.querySelector('.blocklyBlockCanvas');
          const bg = flyoutSvg.querySelector('.blocklyFlyoutBackground');
          if (blocks) blocks.animate([{ transform: 'translate(-300px, 0)', opacity: 0}, { transform: 'translate(0, 0)', opacity: 1}], { duration: 300, easing: 'ease', fill: 'forwards', composite: 'add' });
          if (bg) bg.animate([{ transform: 'translate(-300px, 0)', opacity: 0}, { transform: 'translate(0, 0)', opacity: 1}], { duration: 300, easing: 'ease', fill: 'forwards' })
        });
      }, 100);
    }
  } catch (e) {
    workspace.updateToolbox('<xml></xml>');
  }
};

const saveCurrentWorkspaceToPage = () => {
  if (!workspace) return;
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  // 1. 모든 블록을 순회하며 좌표 데이터 동기화
  workspace.getAllBlocks(false).forEach(block => {
    if (block.data) {
      // ✅ [중요] block.setData 메서드가 존재하는 블록인 경우만 호출하여 에러 방지
      if (typeof block.setData === 'function') {
        block.setData(block.data);
      } else {
        // 메서드가 없는 버전이거나 일반 블록인 경우 속성에 직접 할당
        block.data = block.data;
      }
    }
  });

  try {
    // 2. 워크스페이스를 XML로 변환
    const dom = Blockly.Xml.workspaceToDom(workspace);
    const xmlText = Blockly.Xml.domToText(dom);
    
    // 3. 페이지 데이터 업데이트 및 로컬 스토리지 저장
    page.workspaces[activeMode.value] = xmlText;
    savePagesToStorage();
  } catch (e) {
    console.error("XML 저장 중 오류 발생:", e);
  }
};

const loadPageById = (pageId) => {
  if (!workspace) return;
  const page = pages.value.find((p) => p.id === pageId);
  if (!page) return;
  selectedPageId.value = page.id;
  workspace.clear();
  const xml = page.workspaces?.[activeMode.value];
  if (xml) { try { Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(xml), workspace); } catch (e) {} }
  refreshCodeAndPreview();
  handleSelection(null);
};

const selectPage = (pageId) => {
  saveCurrentWorkspaceToPage();
  codeCache.value = { structure: '', style: '', logic: '' };
  activeMode.value = 'structure';
  activeParent.value = 'structure';
  activeTab.value = null;
  loadPageById(pageId);
};

const selectParent = (modeId) => {
  if (activeMode.value === modeId) return;
  saveCurrentWorkspaceToPage();
  activeMode.value = modeId;
  activeParent.value = modeId;
  activeTab.value = null;

  if (!workspace) return;
  workspace.clear();

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  const xml = page?.workspaces?.[modeId];
  if (xml) { try { Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(xml), workspace); } catch (e) {} }
  setToolbox(toolboxXMLs.empty);

  const group = categoryGroups.find(g => g.id === modeId);
  if (group && group.items && group.items.length > 0) {
    // 첫 번째 아이템(예: layout)을 선택하도록 호출
    selectCategory(group.items[0]);
  } else {
    // 하위 메뉴가 없으면 빈 툴박스
    setToolbox(toolboxXMLs.empty);
  }

  refreshCodeAndPreview();
};

const selectCategory = (key) => {
  if (!workspace) return;
  if (activeTab.value === key) { activeTab.value = null; setToolbox(toolboxXMLs.empty); return; }
  activeTab.value = key;
  setToolbox(toolboxXMLs[key] || toolboxXMLs.empty);
  setTimeout(() => Blockly.svgResize(workspace), 300);
};

const toggleRun = async () => { isRunning.value = !isRunning.value; await nextTick(); updatePreview(); };
const changeModel = () => { isPhone.value = !isPhone.value; updatePreview(); };

onMounted(async () => {
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();
  await nextTick();

  workspace = Blockly.inject('blocklyDiv', {
    renderer: 'zelos', toolbox: toolboxXMLs.empty, move: { scrollbars: true, drag: true, wheel: true }, zoom: { controls: true, wheel: true, startScale: 0.8 }, grid: { spacing: 20, length: 3, colour: '#ccc', snap: true }, trashcan: true,
  });

  let debounceTimer = null;
  workspace.addChangeListener((e) => {
    if (e.type === Blockly.Events.SELECTED) { if (!isSelectingProgrammatically) handleSelection(e.newElementId, 'blockly'); return; }
    if (e.type === Blockly.Events.UI || e.type === Blockly.Events.CLICK) return;
    if ([Blockly.Events.BLOCK_CHANGE, Blockly.Events.BLOCK_CREATE, Blockly.Events.BLOCK_DELETE, Blockly.Events.BLOCK_MOVE].includes(e.type)) { updateObjectListFromWorkspace(); }
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => { refreshCodeAndPreview(); if (selectedBlockId.value) handleSelection(selectedBlockId.value, 'blockly'); }, 500);
  });

window.addEventListener('message', (event) => {
  const data = event.data;
  if (!data) return;

  if (data.type === 'update_free_position') {
    const { blockId, x, y } = data;
    const block = workspace.getBlockById(blockId);
    if (block) {
      // ✅ 좌표를 블록 데이터에 기록
      block.data = JSON.stringify({ x: Number(x || 0), y: Number(y || 0) });
      
      // ✅ [핵심] 위치가 수정되자마자 즉시 XML로 변환하여 저장합니다.
      saveCurrentWorkspaceToPage(); 
      refreshCodeAndPreview();
    }
  }
    if (data.type === 'NAVIGATE' || data.type === 'REDIRECT' || data.type === 'change_page_request') {
      const targetId = data.pageId;
      const targetPage = pages.value.find((p) => p.id === targetId || p.route === targetId || p.name === targetId);
      if (targetPage) { lockPage(targetPage.id); selectPage(targetPage.id); } else { alert('이동할 페이지를 찾을 수 없습니다: ' + targetId); }
    }
    if (data.type === 'select_block') handleSelection(data.blockId, 'iframe');
    if (data.type === 'deselect_block') handleSelection(null, 'iframe');
  });

  window.WC_GET_PAGES = () => {
    if (!pages.value || pages.value.length === 0) return [['페이지 없음', '']];
    return pages.value.map((p) => [p.name, p.id]);
  };

  const stored = loadPagesFromStorage();
  if (stored && stored.length > 0) {
    pages.value = stored;
    const isIdValid = pages.value.some((p) => p.id === selectedPageId.value);
    const targetId = isIdValid ? selectedPageId.value : pages.value[0].id;
    selectedPageId.value = targetId;
    loadPageById(targetId);
  } else {
    // 이미 pages는 초기화되어 있으므로 별도 addPage 필요 없음
    // 저장만 한 번 해줌
    savePagesToStorage();
    loadPageById(pages.value[0].id);
  }

  new ResizeObserver(() => { if (workspace) Blockly.svgResize(workspace); }).observe(document.getElementById('workspace-area'));
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
          <div class="url-bar">
            {{ currentPageUrl }}
          </div>
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

.browser-mockup iframe {
  display: block;
  width: calc(100% + 2px);
  height: calc(100% + 2px);
  margin: -1px;
  border: none;
  background: white;
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
/* 1. 작업 공간을 감싸는 틀 (기준점) */
.workspace-wrapper {
  position: relative !important; /* 자식들의 기준점이 됨 */
  flex: 1;
  width: 100%;
  height: 100%;
  overflow: hidden; /* 튀어나오는 것 잘라냄 */
}

/* 2. Blockly가 들어가는 실제 DIV (핵심!) */
/* 형제 요소(사이드바 등)의 영향을 무시하고 무조건 부모 크기에 맞춤 */
#blocklyDiv {
  position: absolute !important;
  top: 0;
  left: 0;
  width: 100% !important;
  height: 100% !important;
  z-index: 1; /* 가장 아래 깔림 */
}

/* 3. Blockly 내부 SVG 강제 고정 (이중 안전장치) */
:deep(.blocklySvg) {
  position: absolute !important;
  width: 100% !important;
  height: 100% !important;
  top: 0 !important;
  left: 0 !important;
  display: block !important;
}

/* 4. 툴박스(회색 영역)를 유령으로 만듦 (공간 차지 X) */
:deep(.blocklyToolboxDiv) {
  position: absolute !important;
  top: 0 !important;
  left: 0 !important;
  width: 0px !important;    /* 너비 0 */
  height: 100% !important;
  z-index: 90;              /* 워크스페이스보다 위 */
  pointer-events: none;     /* 클릭 통과 */
}

/* 5. 플라이아웃(메뉴창)은 최상단에 띄움 */
:deep(.blocklyFlyout) {
  position: absolute !important;
  z-index: 100 !important;
  pointer-events: auto; /* 메뉴 클릭 가능 */
}
:deep(.blocklyToolbox) {
  display: none !important; /* ⭕ 회색 사이드바 영구 숨김 */
}
:deep(.blocklyFlyoutBackground) {
  position: absolute !important;
  fill: #c0c0c0 !important;
  fill-opacity: 0.2 !important;
}
:deep(.blocklyToolboxFlyout) {
  min-width: 300px !important;
  width: fit-content !important;
  max-width: 300px;
  transition: max-width 0.4s ease-in-out;
  z-index: 100;
}
/* 호버 시 폭을 'auto' 또는 충분히 넓은 값으로 변경 */
:deep(.blocklyToolboxFlyout:hover) {
  max-width: 800px !important;
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
