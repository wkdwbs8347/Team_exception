<script setup>
/**
 *    data-block-id="BLOCK_ID"
 *    data-draggable="true"   (최상위 요소만)
 *
 * - 좌표는 block.data(JSON)에 저장하지만,
 *   ✅ 사용자 코드보기(generatedCode)에는 절대 노출되지 않게
 *   ✅ iframe(프리뷰) 내부에서만 좌표를 적용(applyPositions)
 *
 * - 드래그 대상은:
 *   ✅ #wrapper 직계 자식 중 [data-draggable="true"][data-block-id] 만
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
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
import * as Interaction from '@/components/js/Interaction.vue';
import * as Flow from '@/components/js/Flow.vue';
import * as Logic from '@/components/js/Logic.vue';
import * as Style from '@/components/style/Style.vue';
import * as Responsive from '@/components/style/Responsive.vue';
import * as Color from '@/components/style/Color.vue';
import * as Flex from '@/components/style/Flex.vue';
import * as Animation from '@/components/style/Animation.vue';

// ===== 상태 관리 =====
const activeParent = ref('structure');
const activeTab = ref(null);
const previewSrc = ref('');
const activeRightTab = ref('objects');
const isRunning = ref(false);
const isPhone = ref(false);
let workspace = null;

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

// ✅ 사용자 코드보기 / 프리뷰용 분리
const generatedCode = ref(''); // 코드보기: 빌더용 data-* 제거
const generatedCodeForPreview = ref(''); // 프리뷰: 빌더용 data-* 포함

// ✅ 선택 상태(오브젝트 리스트/블록 선택/프리뷰 하이라이트 동기화)
const selectedBlockId = ref(null);
let isSelectingProgrammatically = false;

// [AI 및 페이지 상태]
const showAiModal = ref(false);
const aiPrompt = ref('');
const isGenerating = ref(false);
const aiPromptError = ref(false);
const vFocus = { mounted: (el) => el.focus() };

// ====================================================
// 1) 유틸: 페이지/로컬스토리지
// ====================================================
function generateUniquePageId() {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) {
      return `page-${crypto.randomUUID()}`;
    }
  } catch (e) {}
  return `page-${Date.now().toString(36)}`;
}

const pages = ref([
  { id: generateUniquePageId(), name: '메인화면 (Home)', xml: '<xml></xml>' },
  { id: generateUniquePageId(), name: '로그인 (Login)', xml: '<xml></xml>' },
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

// ====================================================
// 2) 코드 후처리 유틸 (✅ data-block-id/data-draggable만 제거)
// ====================================================
const cleanCodeForView = (code) => {
  if (!code) return '';

  try {
    const container = document.createElement('div');
    container.innerHTML = code;

    // ✅ 우리가 빌더에서만 쓰는 속성만 제거 (href/type/src 등은 유지)
    const REMOVE_ATTRS = new Set([
      'data-block-id',
      'data-draggable',
      'data-x',
      'data-y',
      'style',
    ]);
    container.querySelectorAll('*').forEach((el) => {
      for (const name of REMOVE_ATTRS) el.removeAttribute(name);
    });

    return container.innerHTML.trim();
  } catch (e) {
    return (code || '')
      .replace(/\sdata-block-id="[^"]*"/g, '')
      .replace(/\sdata-draggable="[^"]*"/g, '')
      .replace(/\sdata-x="[^"]*"/g, '')
      .replace(/\sdata-y="[^"]*"/g, '')
      .replace(/\sstyle="[^"]*"/g, '')
      .trim();
  }
};

const removeScripts = (html) => {
  return html ? html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '') : '';
};

// ====================================================
// 3) ✅ 좌표: 코드에 주입하지 않고 iframe 내부에서만 적용
// ====================================================
const getPositionsMap = () => {
  const map = {};
  if (!workspace) return map;

  const blocks = workspace.getAllBlocks(false);
  blocks.forEach((b) => {
    if (!b.data) return;
    try {
      const p = JSON.parse(b.data);
      map[b.id] = {
        x: Number(p.x || 0),
        y: Number(p.y || 0),
      };
    } catch (e) {}
  });

  return map;
};

// ====================================================
// 4) 선택 동기화 (Blockly <-> Preview <-> Object list)
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
// 5) 오브젝트 리스트 갱신
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
    if (ignoredTypes.has(block.type) || block.type.startsWith('style_')) return;
    if (ignoredTypes.has(block.type) || block.type.startsWith('script')) return;
    const name = block.getFieldValue('NAME') || block.type;
    current.push({ id: block.id, name, type: block.type });
  });
  objects.value = current;
};

// ====================================================
// 6) ✅ 코드 생성 + preview 갱신 (좌표는 HTML에 주입 ❌)
// ====================================================
const refreshCodeAndPreview = () => {
  if (!workspace) return;
  try {
    javascriptGenerator.init(workspace);

    // 1) Blockly -> HTML 생성(프리뷰용: data-* 포함)
    const raw = javascriptGenerator.workspaceToCode(workspace);

    // 2) 프리뷰용 코드 저장
    generatedCodeForPreview.value = raw;

    // 3) 코드보기용: 우리가 붙인 data-*만 제거
    generatedCode.value = cleanCodeForView(raw);

    // 4) preview 갱신
    updatePreview();
    updateObjectListFromWorkspace();
  } catch (e) {}
};

// ====================================================
// 7) ✅ Preview (iframe) - Free 배치 + "기준선 있을 때만" 스냅
//    - wc-drag 없음
//    - 드래그 대상: #wrapper > [data-draggable="true"][data-block-id]
// ====================================================
// ====================================================
// 7) ✅ Preview (iframe) - Free 배치 + "기준선 있을 때만" 스냅
//    - interactjs 제거(의존 X) -> PointerEvent로 드래그 구현
//    - 드래그 대상: #wrapper > [data-draggable="true"][data-block-id]
// ====================================================
const updatePreview = () => {
  let codeToRender = generatedCodeForPreview.value;
  if (!isRunning.value) codeToRender = removeScripts(codeToRender);

  const modeClass = isRunning.value ? 'is-running' : 'is-design';
  const positionsJSON = JSON.stringify(getPositionsMap());

  previewSrc.value = `<!DOCTYPE html><html><head><meta charset="utf-8">
<style>
  html, body { margin:0; padding:0; width:100%; min-height:100vh; overflow:hidden; }
  * { box-sizing: border-box; }

  #wrapper{
    width:100%;
    min-height:100vh;
    position:relative;
    background:#fff;
  }

  img { max-width:100%; height:auto; }

  /* 기본: 좌표 없는 애들은 흐름 유지 */
  #wrapper > [data-draggable="true"][data-block-id]{
    position: absolute ;
    left: 0;             /* 👈 초기 위치 안전장치 */
    top: 0;              /* 👈 초기 위치 안전장치 */
    transform:none;
    touch-action:none;
    user-select:none;
    -webkit-user-select:none;
    cursor: grab;
  }
  /* 하이라이트 */
  .wc-highlight{
    outline:2px solid #ff4081 !important;
    z-index: 9999;
  }

  /* 드래그 중 */
  .wc-dragging{
    opacity:0.9;
    box-shadow: 0 10px 20px rgba(0,0,0,0.2);
    outline: 2px dashed #2196f3 !important;
    cursor: grabbing;
    transition:none !important;
    z-index: 9999;
  }

  /* ✅ 점선 가이드(길이 = 요소-요소 간 구간만 표시) */
  .wc-guide-line{
    position:absolute;
    z-index: 10000;
    pointer-events:none;
    display:none;
    border-color: rgba(255, 0, 0, 0.75);
    border-style: dashed;
  }
  .wc-guide-v{ width:0; border-left-width:1px; }
  .wc-guide-h{ height:0; border-top-width:1px; }

  /* DESIGN 모드에서는 애니메이션 끔 */
  .is-design * { animation:none !important; transition:none !important; }
</style>
</head>

<body class="${modeClass}">
<div id="wrapper">
  ${codeToRender}
  <div id="wcGuideV" class="wc-guide-line wc-guide-v"></div>
  <div id="wcGuideH" class="wc-guide-line wc-guide-h"></div>
</div>

<script>
const WC_POSITIONS = ${positionsJSON};

function hideGuides(){
  const v = document.getElementById('wcGuideV');
  const h = document.getElementById('wcGuideH');
  v.style.display = 'none';
  h.style.display = 'none';
}
function showVSeg(x, y1, y2){
  const v = document.getElementById('wcGuideV');
  v.style.left = x + 'px';
  v.style.top = Math.min(y1,y2) + 'px';
  v.style.height = Math.abs(y2 - y1) + 'px';
  v.style.display = 'block';
}
function showHSeg(y, x1, x2){
  const h = document.getElementById('wcGuideH');
  h.style.top = y + 'px';
  h.style.left = Math.min(x1,x2) + 'px';
  h.style.width = Math.abs(x2 - x1) + 'px';
  h.style.display = 'block';
}

// ✅ 좌표 적용: iframe 내부에서만 absolute/left/top 주입
function applyPositions(){
  const els = Array.from(document.querySelectorAll('#wrapper > [data-draggable="true"][data-block-id]'));

  for(const el of els){
    const id = el.getAttribute('data-block-id');
    const p = WC_POSITIONS[id];

    // 기본은 흐름 유지
    el.removeAttribute('data-has-pos');
    el.style.left = '';
    el.style.top  = '';
    el.style.transform = 'none';

    // ✅ 좌표가 존재하는 경우만 absolute 적용
    if (p && Number.isFinite(p.x) && Number.isFinite(p.y)){
      el.setAttribute('data-has-pos', '1');
      el.style.position = 'absolute';
      el.style.left = p.x + 'px';
      el.style.top  = p.y + 'px';
    }
  }
}

/**
 * ✅ 다른 요소들의 기준선(좌/중/우, 상/중/하) + rect 수집
 */
function collectGuides(exceptEl){
  const wrap = document.getElementById('wrapper');
  const wrapRect = wrap.getBoundingClientRect();
  const els = Array.from(document.querySelectorAll('#wrapper > [data-draggable="true"][data-block-id]'))
    .filter(el => el !== exceptEl);

  const items = [];
  for (const el of els){
    const r = el.getBoundingClientRect();
    const left = r.left - wrapRect.left;
    const right = r.right - wrapRect.left;
    const top = r.top - wrapRect.top;
    const bottom = r.bottom - wrapRect.top;

    items.push({
      el,
      rect: { left, right, top, bottom, width: (right-left), height: (bottom-top) },
      v: [left, (left+right)/2, right],
      h: [top, (top+bottom)/2, bottom]
    });
  }
  return { wrapRect, items };
}

// ✅ “가이드 있을 때만” 스냅 임계값
const SNAP_THRESHOLD = 1; // px

function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){
  const curLeft = nextLeft;
  const curTop  = nextTop;
  const curRight = curLeft + width;
  const curBottom = curTop + height;
  const curCX = (curLeft + curRight) / 2;
  const curCY = (curTop + curBottom) / 2;

  const selfV = [{x:curLeft},{x:curCX},{x:curRight}];
  const selfH = [{y:curTop},{y:curCY},{y:curBottom}];

  let best = {
    dx: 0, dy: 0,
    vLine: null, hLine: null,
    vSeg: null, hSeg: null,
    vDist: SNAP_THRESHOLD + 1,
    hDist: SNAP_THRESHOLD + 1,
  };

  for (const it of guides.items){
    for (const gx of it.v){
      for (const sv of selfV){
        const d = Math.abs(gx - sv.x);
        if (d < best.vDist){
          best.vDist = d;
          best.dx = (gx - sv.x);
          best.vLine = gx;

          const aTop = curTop, aBot = curBottom;
          const bTop = it.rect.top, bBot = it.rect.bottom;
          const overlapTop = Math.max(aTop, bTop);
          const overlapBot = Math.min(aBot, bBot);

          if (overlapBot > overlapTop){
            best.vSeg = { y1: overlapTop, y2: overlapBot };
          } else {
            const gap1 = Math.abs(bTop - aBot);
            const gap2 = Math.abs(aTop - bBot);
            if (gap1 <= gap2) best.vSeg = { y1: aBot, y2: bTop };
            else best.vSeg = { y1: bBot, y2: aTop };
          }
        }
      }
    }
  }

  for (const it of guides.items){
    for (const gy of it.h){
      for (const sh of selfH){
        const d = Math.abs(gy - sh.y);
        if (d < best.hDist){
          best.hDist = d;
          best.dy = (gy - sh.y);
          best.hLine = gy;

          const aL = curLeft, aR = curRight;
          const bL = it.rect.left, bR = it.rect.right;
          const overlapL = Math.max(aL, bL);
          const overlapR = Math.min(aR, bR);

          if (overlapR > overlapL){
            best.hSeg = { x1: overlapL, x2: overlapR };
          } else {
            const gap1 = Math.abs(bL - aR);
            const gap2 = Math.abs(aL - bR);
            if (gap1 <= gap2) best.hSeg = { x1: aR, x2: bL };
            else best.hSeg = { x1: bR, x2: aL };
          }
        }
      }
    }
  }

  if (best.vDist > SNAP_THRESHOLD){ best.dx = 0; best.vLine = null; best.vSeg = null; }
  if (best.hDist > SNAP_THRESHOLD){ best.dy = 0; best.hLine = null; best.hSeg = null; }

  return best;
}

function clamp(n, min, max){ return Math.max(min, Math.min(max, n)); }

function init(){
  const isRunning = ${isRunning.value};

  // ✅ 렌더 직후 좌표 적용
  applyPositions();

  // ✅ 하이라이트 수신 (interact 의존 X)
  window.addEventListener('message', function(e){
    if(e.data && e.data.type === 'highlight_element'){
      document.querySelectorAll('.wc-highlight').forEach(el => el.classList.remove('wc-highlight'));
      if(e.data.blockId){
        const t = document.querySelector('[data-block-id="'+e.data.blockId+'"]');
        if(t) t.classList.add('wc-highlight');
      }
    }
  });

  // ✅ DESIGN 모드 클릭 방지 + 선택 전송
  document.addEventListener('mousedown', function(e){
    if(isRunning) return;
    const target = e.target.closest('[data-block-id]');
    if(target){
      window.parent.postMessage({ type:'select_block', blockId: target.getAttribute('data-block-id') }, '*');
    } else {
      window.parent.postMessage({ type:'deselect_block' }, '*');
    }
  }, true);

  document.addEventListener('click', function(e){
    if(isRunning) return;
    if(e.target.closest('a, button, input')) e.preventDefault();
  }, true);

  if(isRunning) return;

  // ✅ PointerEvent 기반 드래그
  const wrap = document.getElementById('wrapper');
  let dragging = null;

  function onPointerMove(ev){
    if(!dragging) return;

    const t = dragging.el;

    // 누적 이동량
    dragging.dx += (ev.clientX - dragging.lastX);
    dragging.dy += (ev.clientY - dragging.lastY);
    dragging.lastX = ev.clientX;
    dragging.lastY = ev.clientY;

    const baseLeft = dragging.baseLeft;
    const baseTop  = dragging.baseTop;

    // 보정 전
    let nextLeft = baseLeft + dragging.dx;
    let nextTop  = baseTop  + dragging.dy;

    const rectNow = t.getBoundingClientRect();
    const width = rectNow.width;
    const height = rectNow.height;

    // ✅ 스냅
    const snap = computeSmartSnap({ nextLeft, nextTop, width, height, guides: dragging.guides });

    if (snap.vLine != null && snap.vSeg) showVSeg(snap.vLine, snap.vSeg.y1, snap.vSeg.y2);
    else document.getElementById('wcGuideV').style.display = 'none';

    if (snap.hLine != null && snap.hSeg) showHSeg(snap.hLine, snap.hSeg.x1, snap.hSeg.x2);
    else document.getElementById('wcGuideH').style.display = 'none';

    nextLeft += snap.dx;
    nextTop  += snap.dy;

    // ✅ 부모 안으로 제한(대충)
    const wrapRect = wrap.getBoundingClientRect();
    nextLeft = clamp(nextLeft, 0, Math.max(0, wrapRect.width - width));
    nextTop  = clamp(nextTop, 0, Math.max(0, wrapRect.height - height));

    // 적용
    t.style.left = nextLeft + 'px';
    t.style.top  = nextTop  + 'px';
    t.style.transform = 'none';

    // 튐 방지: 누적값 재정렬
    dragging.dx = nextLeft - baseLeft;
    dragging.dy = nextTop  - baseTop;

    ev.preventDefault();
  }

  function onPointerUp(){
    if(!dragging) return;

    const t = dragging.el;
    t.classList.remove('wc-dragging');
    hideGuides();

    const left = parseFloat(t.style.left || '0');
    const top  = parseFloat(t.style.top  || '0');

    window.parent.postMessage({
      type: 'update_free_position',
      blockId: t.getAttribute('data-block-id'),
      x: Math.round(left),
      y: Math.round(top),
    }, '*');

    window.parent.postMessage({ type:'deselect_block' }, '*');

    dragging.el.releasePointerCapture(dragging.pointerId);
    dragging = null;

    window.removeEventListener('pointermove', onPointerMove, true);
    window.removeEventListener('pointerup', onPointerUp, true);
    window.removeEventListener('pointercancel', onPointerUp, true);
  }

    wrap.addEventListener('pointerdown', (ev) => {
    const t = ev.target.closest('#wrapper > [data-draggable="true"][data-block-id]');
    if(!t) return;

    // ✅ wrapper 기준 현재 위치를 left/top로 고정 (static -> absolute 전환 시 튐 방지)
    const wrapRect = wrap.getBoundingClientRect();
    const r = t.getBoundingClientRect();
    const curLeft = r.left - wrapRect.left + wrap.scrollLeft;
    const curTop  = r.top  - wrapRect.top  + wrap.scrollTop;

    // ✅ 이제부터 자유배치 대상으로 전환
    t.setAttribute('data-has-pos', '1');
    t.style.position = 'absolute';
    t.style.left = curLeft + 'px';
    t.style.top  = curTop + 'px';
    t.style.transform = 'none';

    t.classList.add('wc-dragging');

    window.parent.postMessage({
      type:'select_block',
      blockId: t.getAttribute('data-block-id')
    }, '*');

    const baseLeft = parseFloat(t.style.left || '0');
    const baseTop  = parseFloat(t.style.top  || '0');

    dragging = {
      el: t,
      baseLeft,
      baseTop,
      dx: 0,
      dy: 0,
      lastX: ev.clientX,
      lastY: ev.clientY,
      guides: collectGuides(t),
      pointerId: ev.pointerId,
    };

    t.setPointerCapture(ev.pointerId);

    window.addEventListener('pointermove', onPointerMove, true);
    window.addEventListener('pointerup', onPointerUp, true);
    window.addEventListener('pointercancel', onPointerUp, true);

    ev.preventDefault();
  }, true);
}

init();
<\/script>
</body></html>`;
};

// ====================================================
// 8) Blockly 초기화 & 이벤트 처리
// ====================================================
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
  data : `<xml><category name="변수" custom="VARIABLE" colour="#a55b80"></category></xml>`,
  seo: `<xml><label text="SEO 설정 준비중"></label></xml>`,
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
    if (toolbox && toolbox.getToolboxItems && toolbox.getToolboxItems().length > 0) {
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
            animations.forEach(anim => anim.cancel());

            blocks.animate([
              { transform: 'translate(-300px, 0)', opacity: 0 }, // 시작
              { transform: 'translate(0, 0)', opacity: 1 }       // 끝
            ], {
              duration: 300,
              easing: 'ease',
              fill: 'forwards',
              composite: 'add'
            });
          }

          // [배경]: Fade In
          if (bg) {
            const bgAnims = bg.getAnimations();
            bgAnims.forEach(anim => anim.cancel());

            bg.animate([
              { opacity: 0 },
              { opacity: 1 }
            ], {
              duration: 250,
              easing: 'linear',
              fill: 'forwards'
            });
          }
        });
      });
    }

  } catch (e) {
    console.error("🚨 툴박스 오류:", e);
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
    items: ['interaction', 'flow', 'logic', 'data', 'seo', 'advanced'],
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
  seo: { label: 'SEO', color: '#607d8b', icon: '🔍' },
  advanced: { label: '고급', color: '#424242', icon: '⚙️' },
};

const selectParent = (parentId) => {
  // 1. 상위 카테고리 변경
  activeParent.value = parentId;

  // 2. 해당 그룹의 정보 찾기
  const group = categoryGroups.find((g) => g.id === parentId);

  // 3. 하위 아이템이 있다면, 첫 번째 아이템을 자동으로 선택!
  if (group && group.items && group.items.length > 0) {
    const firstItem = group.items[0]; // 첫 번째 아이템 (예: layout)
    selectCategory(firstItem);        // 강제로 선택 함수 실행
  } else {
    // 하위 아이템이 없는 경우만 빈 화면
    activeTab.value = null;
    if (workspace) setToolbox(toolboxXMLs.empty);
  }
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
  const xmlText = Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace));
  const currentPage = pages.value.find((p) => p.id === selectedPageId.value);
  if (currentPage) currentPage.xml = xmlText;
  savePagesToStorage();
};

const loadPageById = (pageId) => {
  if (!workspace) return;
  const page = pages.value.find((p) => p.id === pageId);
  if (!page) return;

  selectedPageId.value = pageId;
  workspace.clear();

  if (page.xml) {
    try {
      Blockly.Xml.domToWorkspace(
        Blockly.utils.xml.textToDom(page.xml),
        workspace
      );
    } catch (e) {}
  }

  refreshCodeAndPreview();
  handleSelection(null);
};

const selectPage = (pageId) => {
  saveCurrentWorkspaceToPage();
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
  const newId = generateUniquePageId();
  pages.value.push({
    id: newId,
    name: `새 페이지 ${pages.value.length + 1}`,
    xml: '<xml></xml>',
  });
  savePagesToStorage();
  selectPage(newId);
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
    if (e.type === Blockly.Events.SELECTED) {
      if (!isSelectingProgrammatically)
        handleSelection(e.newElementId, 'blockly');
      return;
    }
    if (e.type === Blockly.Events.UI || e.type === Blockly.Events.CLICK) return;

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

    if (event.data.type === 'select_block')
      handleSelection(event.data.blockId, 'iframe');
    if (event.data.type === 'deselect_block') handleSelection(null, 'iframe');
  });

  const stored = loadPagesFromStorage();
  if (stored && stored.length > 0) pages.value = stored;
  if (pages.value.length > 0) loadPageById(selectedPageId.value);

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
  overflow: hidden;
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
:deep(.blocklyFlyoutBackground){
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
