<script setup>
/**

 * ============================================================

 * ✅ Web Crafter IDE (Final Fixed Version)

 * - 순환 참조 오류 해결 (pages 초기화 순서 변경)

 * - URL 중복 방지 로직 적용

 * ============================================================

 */
import JSZip from 'jszip';
import { ref, onMounted, nextTick, watch, computed, reactive } from 'vue';

import * as Blockly from 'blockly';

import { javascriptGenerator } from 'blockly/javascript';

import * as Ko from 'blockly/msg/ko';

import 'blockly/blocks';

import ConfirmModal from '@/modal/ConfirmModal.vue';

import GlobalModal from '@/modal/GlobalModal.vue';

// ===== 카테고리 블록 import =====
//blockly 블록 정의 및 툴박스 XML을 각각의 모듈에서 가져옵니다.
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
import * as ContentAttr from '@/components/block/ContentAttr.vue';
//style 관련 블록
import * as Style from '@/components/style/Style.vue';
import * as Responsive from '@/components/style/Responsive.vue';
import * as Color from '@/components/style/Color.vue';
import * as Flex from '@/components/style/Flex.vue';
import * as Animation from '@/components/style/Animation.vue';
//js/logic 관련 블록
import * as Interaction from '@/components/js/Interaction.vue';
import * as Flow from '@/components/js/Flow.vue';
import * as Logic from '@/components/js/Logic.vue';
//modal
import { Settings } from 'lucide-vue-next'
// 1. 컴포넌트 임포트
import AiChatBot from '@/modal/AiChatBot.vue';

const props = defineProps({
  nickname: {
    type: String,
    default: ''
  },
  webId: {
    type: [String, Number],
    default: ''
  }
});
// ✨ [추가] 기존 XML 문자열에 새로운 DOM 노드들을 합쳐주는 함수
const mergeBlockXml = (originalXmlText, newXmlDom) => {
  // 1. 새로운 블록이 없으면 기존 것 그대로 반환
  if (!newXmlDom || newXmlDom.children.length === 0) return originalXmlText;

  // 2. 기존 XML이 비어있으면 새 것만 반환
  if (!originalXmlText || originalXmlText === '<xml></xml>') {
    return Blockly.Xml.domToText(newXmlDom);
  }

  // 3. 기존 XML을 DOM으로 변환
  let originalDom = null;
  try {
    originalDom = Blockly.utils.xml.textToDom(originalXmlText);
  } catch (e) {
    // 혹시 파싱 에러나면 그냥 새거 덮어쓰기
    return Blockly.Xml.domToText(newXmlDom);
  }

  // 4. 새 블록들을 기존 DOM 끝에 붙이기 (이사시키기)
  const newBlocks = Array.from(newXmlDom.children);
  newBlocks.forEach((blockNode) => {
    // cloneNode(true)를 써서 복사본을 넣어야 안전함
    originalDom.appendChild(blockNode.cloneNode(true));
  });

  // 5. 합쳐진 DOM을 다시 글자로 바꿔서 반환
  return Blockly.Xml.domToText(originalDom);
};
const wrapperWidth = ref(600);
const wrapperHeight = ref(800);
// 3. AI가 만든 XML을 받아서 카테고리별로 나눠 담는 핸들러 (수정됨)
const handleAiBlockGeneration = (xmlText) => {
  if (!workspace || !xmlText) return;

  try {
    // 1. AI가 준 텍스트를 DOM으로 변환
    const parser = new DOMParser();
    const xmlDom = Blockly.utils.xml.textToDom(xmlText);
    
    // 2. 각 모드별로 담을 임시 컨테이너 생성
    const structureXml = document.createElement('xml');
    const styleXml = document.createElement('xml');
    const logicXml = document.createElement('xml');

    // 3. 블록 하나하나 검사해서 방 배정 (Dispatcher)
    const blocks = Array.from(xmlDom.children);
    
    blocks.forEach((blockNode) => {
      // <block> 태그가 아니면 패스 (주석이나 텍스트 노드 등)
      if (blockNode.nodeName.toLowerCase() !== 'block') return;

      const type = blockNode.getAttribute('type') || '';

      // 🔥 [핵심 수정] 제공해주신 블록 리스트 기반의 정밀 분류
      // 1. 화면 구성 (Structure) & 속성 (Attributes) -> structureXml
      if (
        type.startsWith('layout_') ||    // layout_area, layout_box 등
        type.startsWith('content_') ||   // content_heading, content_button 등
        type.startsWith('form_') ||      // form_container, form_input 등 (layout_form과 중복 주의)
        type.startsWith('wc_attr_') ||   // wc_attr_id, wc_attr_class 등 (속성도 요소와 함께 배치)
        type.startsWith('component_')    // component_ (만약 있다면)
      ) {
        structureXml.appendChild(blockNode);
      } 
      // 2. 스타일링 (Styling) -> styleXml
      else if (
        type.startsWith('style_') ||     // style_size, style_color 등
        type.startsWith('effect_') ||    // effect_entrance, effect_emphasis 등
        type.startsWith('anim_')         // anim_duration, anim_delay 등
      ) {
        styleXml.appendChild(blockNode);
      } 
      // 3. 로직 및 이벤트 (Logic, Events, Flow, Ops) -> logicXml
      else if (
        type.startsWith('event_') ||     // event_click, event_page_load
        type.startsWith('action_') ||    // action_alert, action_navigate
        type.startsWith('dom_') ||       // dom_change_text
        type.startsWith('script_') ||    // script_tag
        type.startsWith('flow_') ||      // flow_if, flow_repeat
        type.startsWith('logic_') ||     // logic_compare, logic_and
        type.startsWith('value_')        // value_text, value_number
      ) {
        logicXml.appendChild(blockNode);
      }
      // 4. 분류되지 않은 블록은 기본적으로 로직으로 보내거나, 에러 로그 출력
      else {
        console.warn(`분류되지 않은 블록 타입 발견: ${type}. 로직 탭으로 이동합니다.`);
        logicXml.appendChild(blockNode);
      }
    });

    // 4. 현재 선택된 페이지 찾기
    const page = pages.value.find((p) => p.id === selectedPageId.value);
    if (!page) return;

    // 5. 페이지 데이터(workspaces)에 각각 저장 (덮어쓰기)
    // 기존 데이터가 있다면 유지하면서 추가하고 싶다면, 기존 XML을 파싱해서 합치는 로직이 필요하지만
    // 여기서는 AI 생성이 "새로운 제안"이라고 가정하고 덮어쓰거나, 비어있지 않은 경우만 업데이트합니다.
    
    // [중요] 각 XML 컨테이너에 자식 노드가 하나라도 있을 때만 해당 탭의 데이터를 갱신합니다.
    // 이렇게 하면 AI가 스타일만 줬을 때, 기존의 화면 구성은 날아가지 않습니다.
    if (structureXml.children.length > 0) {
      // page.workspaces.structure = Blockly.Xml.domToText(structureXml); // ❌ (삭제)
      page.workspaces.structure = mergeBlockXml(page.workspaces.structure, structureXml); // ⭕ (수정)
    }
    if (styleXml.children.length > 0) {
      page.workspaces.style = Blockly.Xml.domToText(styleXml);
    }
    if (logicXml.children.length > 0) {
      page.workspaces.logic = Blockly.Xml.domToText(logicXml);
    }

    // 6. 데이터 저장이 끝났으니, 로컬스토리지 저장
    savePagesToStorage(); 
    
    // 7. 현재 보고 있는 탭(activeMode)에 맞는 데이터로 워크스페이스 다시 그리기
    // 사용자가 현재 'structure' 탭을 보고 있다면, structureXml 내용이 화면에 나타납니다.
    // 만약 AI가 style만 생성했다면, 현재 화면(structure)은 변하지 않을 수 있습니다. 
    // 이를 위해 알림창으로 어떤 데이터가 갱신되었는지 알려주면 좋습니다.
    
    const currentModeXml = page.workspaces[activeMode.value];
    workspace.clear();
    if (currentModeXml) {
       Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(currentModeXml), workspace);
    }
    
    // 프리뷰(Iframe) 및 코드창 업데이트
    refreshCodeAndPreview();
    
    let msg = "AI 코드 적용 완료!\n";
    if (structureXml.children.length > 0) msg += "- 화면 구성 탭 갱신됨\n";
    if (styleXml.children.length > 0) msg += "- 스타일 탭 갱신됨\n";
    if (logicXml.children.length > 0) msg += "- 로직 탭 갱신됨";
    
    console.log("✅ AI 블록 분류 및 적용 완료!");

  } catch (e) {
    console.error("블록 변환 중 오류:", e);
    alert("AI 코드를 블록으로 변환하는데 실패했습니다.");
  }
};
import ThemeSettingsModal from '@/modal/ThemeSettingsModal.vue';
//기본 테마 설정
const isThemeModalOpen = ref(false);
const currentTheme = reactive({
id: 'default',
toolboxColor: '#dcdcdcba',
workspaceColor: '#ffffff'
})
/* ============================================================

 * 🚀 [Page Engine] 로직

 * ============================================================ */

function slugify(name) {
  return (
    '/' +
    name

      .trim()

      .replace(/\s+/g, '-')

      .replace(/[^a-zA-Z0-9\-\uAC00-\uD7A3]+/g, '')

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

  while (pages.value.some((p) => p.route === candidate && p.id !== excludeId)) {
    candidate = `${baseSlug}-${counter}`;

    counter++;
  }

  return candidate;
}

// 3. 페이지 생성

function createPage(name) {
  const generatedId =
    'page_' +
    (typeof crypto !== 'undefined'
      ? crypto.randomUUID().slice(0, 6)
      : Date.now().toString(36));

  // pages에 안전하게 접근하여 라우트 생성

  const safeRoute = getUniqueRoute(name);

  return {
    id: generatedId,

    name: name,

    route: safeRoute,

    aliases: [],

    status: 'DRAFT',

    workspaces: {
      structure: '<xml></xml>',
      style: '<xml></xml>',
      logic: '<xml></xml>',
    },
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
const isLandscape = ref(false);
const modeOpen = ref(false);
let workspace = null;
let isFlyoutOpened = false;
const modeList = [
  { id: 'structure', label: '화면구성', icon: '🏗️' },

  { id: 'style', label: '스타일', icon: '🎨' },

  { id: 'logic', label: '로직/데이터', icon: '⚡' },
];

const currentMode = computed(
  () => modeList.find((m) => m.id === activeMode.value) || modeList[0]
);

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
  const page = pages.value.find((p) => p.id === selectedPageId.value);

  return page
    ? `https://web-crafter.app${page.route}`
    : 'https://web-crafter.app/';
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

const confirmModal = ref({ open: false, message: '', payload: null });

const modal = ref({ open: false, message: '', type: 'info', onConfirm: null });

const vFocus = { mounted: (el) => el.focus() };

/* ============================================================

 * 카테고리 정의

 * ============================================================ */

const categoryGroups = [
  {
    id: 'structure',
    label: '화면 구성',
    icon: '🏗️',
    color: '#4caf50',
    items: ['layout', 'content', 'contentAttr', 'form', 'component'],
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

const categories = {
  layout: Layout.category,
  content: Content.category,
  contentAttr: ContentAttr.category,
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

const currentSubItems = computed(() => {
  const group = categoryGroups.find((g) => g.id === activeParent.value);

  return group ? group.items : [];
});

/* ============================================================

 * 페이지 관리 함수

 * ============================================================ */

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

const startEditPageName = (page) => {
  editingPageId.value = page.id;
  editingPageName.value = page.name;
};

const commitEditPageName = (pageId) => {
  const page = pages.value.find((p) => p.id === pageId);

  if (page) {
    page.name = editingPageName.value;

    if (page.status === 'DRAFT') {
      page.route = getUniqueRoute(page.name, page.id); // 수정 시에도 중복 체크
    }

    savePagesToStorage();
  }

  editingPageId.value = null;
};

const cancelEditPageName = () => {
  editingPageId.value = null;
  editingPageName.value = '';
};

const lockPage = (pageId) => {
  const page = pages.value.find((p) => p.id === pageId);

  if (page && page.status !== 'LOCKED') {
    page.status = 'LOCKED';
    savePagesToStorage();
  }
};

const addPage = () => {
  const page = createPage(`Page ${pages.value.length + 1}`);

  pages.value.push(page);

  savePagesToStorage();

  selectPage(page.id);
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

const deletePage = (pageId) => {
  if (pages.value.length <= 1) {
    openModal('최소 하나의 페이지는 있어야 합니다.', 'info');
    return;
  }

  openDeleteConfirm(pageId);
};

const openDeleteConfirm = (pageId) => {
  confirmModal.value = {
    open: true,
    message: '이 페이지를 삭제하시겠습니까?',
    payload: { pageId },
  };
};

const closeDeleteConfirm = () => {
  confirmModal.value = { ...confirmModal.value, open: false };
};

const confirmDeletePage = () => {
  const pageId = confirmModal.value.payload?.pageId;
  closeDeleteConfirm();
  if (pageId) deletePageNow(pageId);
};

const openModal = (message, type = 'info', onConfirm = null) => {
  modal.value = { open: true, message, type, onConfirm };
};

const closeModal = () => {
  modal.value.open = false;
  modal.value.onConfirm?.();
  modal.value.onConfirm = null;
};

/* ============================================================

 * 코드/프리뷰 로직

 * ============================================================ */

const cleanCodeForView = (code) => {
  if (!code) return '';

  try {
    const container = document.createElement('div');

    container.innerHTML = code;

    container.querySelectorAll('*').forEach((el) => {
      el.removeAttribute('data-block-id');
      el.removeAttribute('data-draggable');
      el.removeAttribute('data-x');
      el.removeAttribute('data-y');
      el.removeAttribute('data-wc-style');
      el.removeAttribute('data-wc-block');

      if (el.hasAttribute('style')) {
        el.style.removeProperty('position');
        el.style.removeProperty('left');
        el.style.removeProperty('top');
        el.style.removeProperty('transform');

        if (!el.getAttribute('style')?.trim()) el.removeAttribute('style');
      }
    });

    return container.innerHTML.trim();
  } catch (e) {
    return (code || '').replace(/\sdata-block-id="[^"]*"/g, '').trim();
  }
};

const removeScripts = (html) =>
  html ? html.replace(/<script\b[^>]*>([\s\S]*?)<\/script>/gim, '') : '';

const getPositionsMap = () => {
  const map = {};

  const page = pages.value.find((p) => p.id === selectedPageId.value);

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
  } catch (e) {
    return '';
  }
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

  if (iframe?.contentWindow)
    iframe.contentWindow.postMessage(
      { type: 'highlight_element', blockId },
      '*'
    );
};

const selectObjectFromList = (objId) => {
  handleSelection(objId, 'list');
  workspace?.centerOnBlock(objId);
};

watch(
  objects,
  (newObjects) => {
    if (Interaction.updateObjectList) Interaction.updateObjectList(newObjects);
  },
  { deep: true, immediate: true }
);

// 기존 updateObjectListFromWorkspace 함수를 이걸로 덮어씌우세요!
const updateObjectListFromWorkspace = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  let targetBlocks = [];
  let tempWorkspace = null;

  // 1. 현재 탭이 '화면 구성(structure)'이면 -> 라이브 워크스페이스 사용
  if (activeMode.value === 'structure' && workspace) {
    targetBlocks = workspace.getAllBlocks(false);
  } 
  // 2. 다른 탭(스타일, 로직)이면 -> 저장된 화면 구성 XML을 파싱해서 사용
  else {
    try {
      const structureXml = page.workspaces.structure;
      if (structureXml && structureXml !== '<xml></xml>') {
        // 임시 워크스페이스를 만들어서 블록 정보를 읽어옵니다.
        tempWorkspace = new Blockly.Workspace();
        const dom = Blockly.utils.xml.textToDom(structureXml);
        Blockly.Xml.domToWorkspace(dom, tempWorkspace);
        targetBlocks = tempWorkspace.getAllBlocks(false);
      }
    } catch (e) {
      console.error("객체 목록 로드 실패:", e);
    }
  }

  const current = [];

  // 3. 가져온 블록들 중에서 "화면 요소"만 골라내기
  targetBlocks.forEach((block) => {
    const type = block.type;

    // 🔥 [필터] 오직 화면 구성용 블록만 목록에 넣습니다.
    // (이벤트, 스타일, 로직 블록 등은 제외)
    if (
      type.startsWith('layout_') || 
      type.startsWith('content_') || 
      type.startsWith('form_') || 
      type.startsWith('component_')
    ) {
      current.push({
        id: block.id,
        name: block.getFieldValue('NAME') || type, // 블록에 이름 필드가 있으면 그걸 쓰고, 없으면 타입명
        type: type,
      });
    }
  });

  // 4. 결과 적용
  objects.value = current;

  // 5. 메모리 정리 (임시 워크스페이스 삭제)
  if (tempWorkspace) {
    tempWorkspace.dispose();
  }
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

      generatedCode.value =
        activeMode.value === 'structure' ? cleanCodeForView(rawCode) : rawCode;
    }

    updatePreview();

    updateObjectListFromWorkspace();
  } catch (e) {
    console.error(e);
  }
};

const updatePreview = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  // ✅ 프리뷰에는 "style_tag/style_tag_all이 만든 <style>...</style>만" 적용되게
  const extractStyleTagsOnly = (raw) => {
    if (!raw) return '';
    const matches = raw.match(/<style[^>]*>[\s\S]*?<\/style>/gi);
    return matches ? matches.join('\n') : '';
  };

  // 1. 현재 워크스페이스의 XML 가져오기
  const currentXml = workspace
    ? Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace))
    : '';

  // 2. 각 모드별 코드 생성
  const structureCode =
    activeMode.value === 'structure'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.structure);

  const styleCodeRaw =
    activeMode.value === 'style'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.style);

  const logicCode =
    activeMode.value === 'logic'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.logic);

  // 3. ✅ 코드보기(사용자 코드)는 그대로 보여줌 (단독 STYLE 속성도 여기엔 보임)
  const viewScript = logicCode.trim() ? `${logicCode}` : '';
  const viewHtml = cleanCodeForView(structureCode);
  const viewStyle = styleCodeRaw.trim() ? `${styleCodeRaw}` : '';
  const deviceClass = isPhone.value ? 'is-mobile-mode' : 'is-pc-mode';
  const orientationClass = (isPhone.value && isLandscape.value) ? 'is-landscape' : '';
  const finalBodyClass = `${isRunning.value ? 'is-running' : 'is-design'} ${deviceClass} ${orientationClass}`;

  generatedCode.value = [viewScript, viewHtml, viewStyle].filter(Boolean).join('\n\n');

  // 4. ✅ 프리뷰용 스타일은 "style 태그만" 추출해서 적용
  // - style_tag/style_tag_all이 만든 <style>...</style>만 남음
  // - style_size 같은 단독 속성 블록은 프리뷰에 절대 영향 없음
  const styleCodeForPreview = extractStyleTagsOnly(styleCodeRaw);

  // 로직 스크립트는 기존대로
  const safeScript =
    logicCode.trim() && !logicCode.includes('<script')
      ? `<script>${logicCode}<\/script>`
      : logicCode;

  const finalLogicScript = isRunning.value ? safeScript : '';

  const positionsJSON = JSON.stringify(getPositionsMap());
  const PAGE_ID = page.id;
  const PAGE_ROUTE = page.route;

// 5. Iframe HTML 조립 (전체 수정 버전)
  const htmlParts = [
    '<!DOCTYPE html><html><head><meta charset="utf-8">',
    
    // 🔥 [수정 1] CSS 오타 수정 및 스크롤/높이 설정 완벽 적용
    '<style>',
    'html, body { margin:0; padding:0; width:100%; height:100%; overflow-y: auto; overflow-x: hidden; background:#fff; }',
    '* { box-sizing: border-box; }',
    '#wrapper { width:100%; min-height:100vh; position:relative; background:#fff; }',
    
    // 드래그 및 하이라이트 스타일
    '#wrapper > [data-draggable="true"][data-block-id] { position: absolute; left: 0; top: 0; transform:none; touch-action:none; user-select:none; -webkit-user-select:none; cursor: grab; }',
    '#wrapper > [data-draggable="true"][data-block-id]:is(div, section, article, header, nav, main, aside, footer, form, ul) { max-width: 100%; }',
    '.wc-highlight { outline:2px solid #ff4081 !important; z-index: 9999; }',
    '.wc-dragging { opacity:0.9; box-shadow: 0 10px 20px rgba(0,0,0,0.2); outline: 2px dashed #2196f3 !important; cursor: grabbing; transition:none !important; z-index: 9999; }',
    '.wc-guide-line { position:absolute; z-index: 10000; pointer-events:none; display:none; border-color: rgba(255, 0, 0, 0.75); border-style: dashed; }',
    '.wc-guide-v { width:0; border-left-width:1px; }',
    '.wc-guide-h { height:0; border-top-width:1px; }',
    '[data-wc-block] { position: relative; min-width: 50px; min-height: 50px; }',
    '[data-wc-block]:not(:has(*))::after { content: "📦"; color: #aaa; display: flex; align-items: center; justify-content: center; position: absolute; inset: 0; pointer-events: none; opacity: 0.5; }',
    '</style>',

    `<style id="anim-defs">${Animation.Animation.ANIMATION_KEYFRAMES}</style>`,
    '<style>body.is-design * { animation: none !important; transition: none !important; }</style>',

    // 사용자 정의 스타일 (여기만 프리뷰용 스타일 적용)
    styleCodeForPreview,

    '</head>',
    `<body class="${isRunning.value ? 'is-running' : 'is-design'} ${finalBodyClass}">`,
    '<div id="wrapper">',
    structureCode,
    '<div id="wcGuideV" class="wc-guide-line wc-guide-v"></div><div id="wcGuideH" class="wc-guide-line wc-guide-h"></div></div>',
    finalLogicScript,
    
    '<script>',
    `const WC_POSITIONS = ${positionsJSON}; const isRunning = ${isRunning.value}; const PAGE_ID = "${PAGE_ID}"; const PAGE_ROUTE = "${PAGE_ROUTE}";`,
    
    // 기본 헬퍼 함수들
    'function navigateToPage(targetId) { window.parent.postMessage({ type: "NAVIGATE", pageId: targetId }, "*"); }',
    'function redirectToPage(targetId) { window.parent.postMessage({ type: "REDIRECT", pageId: targetId }, "*"); }',
    'function goToPage(targetId) { navigateToPage(targetId); }',
    'function applyBuilderStyles(){ const nodes = document.querySelectorAll("[data-wc-style]"); nodes.forEach(el => { el.style.cssText += ";" + el.getAttribute("data-wc-style"); }); }',
    'function parseTargetToSelector(raw){const s=(raw||"").trim();if(!s)return"";if((s.startsWith("[")&&s.endsWith("]"))||s.startsWith("#"))return s;const p=s.split(":"),m=(p.length>=2?p.slice(1).join(":"):p[0]).trim();if(!m)return"";if(m.startsWith(".")||m.startsWith("#")||(m.startsWith("[")&&m.endsWith("]")))return m;return"."+m;}',
    'function applyContentAttrs(){const metas=[...document.querySelectorAll("[data-wc-block=\'wc_attrs\'][data-wc-attrs]")];metas.forEach(m=>{let p;try{p=JSON.parse(m.getAttribute("data-wc-attrs")||"{}")}catch(e){p=null}if(!p||!p.target||!Array.isArray(p.ops)){m.remove();return}const sel=parseTargetToSelector(p.target);if(!sel){m.remove();return}[...document.querySelectorAll(sel)].forEach(el=>{p.ops.forEach(o=>{if(!o||!o.t)return;switch(o.t){case"id":o.v&&(el.id=o.v);break;case"class_add":o.v&&el.classList.add(o.v);break;case"data":o.k&&el.setAttribute("data-"+o.k,o.v??"");break;case"aria":o.k&&el.setAttribute("aria-"+o.k,o.v??"");break;case"placeholder":el.setAttribute("placeholder",o.v??"");break;case"value":el.setAttribute("value",o.v??"");break;case"required":el.setAttribute("required","");break;case"disabled":el.setAttribute("disabled","");break;case"readonly":el.setAttribute("readonly","");break;case"target_blank":el.setAttribute("target","_blank");break;case"rel_noopener":{const r=(el.getAttribute("rel")||"").split(/\\s+/).filter(Boolean);r.includes("noopener")||r.push("noopener");r.includes("noreferrer")||r.push("noreferrer");el.setAttribute("rel",r.join(" "));break}case"for":o.v&&el.setAttribute("for",o.v);break;case"server_field":o.v&&(el.setAttribute("name",o.v),el.setAttribute("data-wc-field",o.v));break;case"style":if(o.v){const prev=el.getAttribute("data-wc-style")||"";el.setAttribute("data-wc-style",(prev&&(prev.trim().endsWith(";")?prev:prev+";"))+o.v)}break;case"dup_target":o.v&&(el.setAttribute("data-wc-action","duplicate-check"),el.setAttribute("data-wc-target",o.v));break}})});m.remove()})}',
    'function syncClassStyles(){ const styleText = document.querySelector("style")?.textContent || ""; const classMatches = styleText.match(/\\.([a-zA-Z0-9_-]+)\\s*\\{/g) || []; classMatches.forEach(m => { const className = m.replace(".", "").replace("{", "").trim(); document.querySelectorAll("[data-block-id=\'"+className+"\']").forEach(el => el.classList.add(className)); }); }',
    'function hideGuides(){ const v = document.getElementById("wcGuideV"); const h = document.getElementById("wcGuideH"); if(v) v.style.display = "none"; if(h) h.style.display = "none"; }',
    'function showVSeg(x, y1, y2){ const v = document.getElementById("wcGuideV"); if(!v) return; v.style.left = x + "px"; v.style.top = Math.min(y1,y2) + "px"; v.style.height = Math.abs(y2 - y1) + "px"; v.style.display = "block"; }',
    'function showHSeg(y, x1, x2){ const h = document.getElementById("wcGuideH"); if(!h) return; h.style.top = y + "px"; h.style.left = Math.min(x1,x2) + "px"; h.style.width = Math.abs(x2 - x1) + "px"; h.style.display = "block"; }',
    'function applyPositions(){ const wrap = document.getElementById("wrapper"); if(!wrap) return; const targets = wrap.querySelectorAll(":scope > [data-draggable=\'true\']"); targets.forEach(el => { const id = el.getAttribute("data-block-id"); const p = WC_POSITIONS[id]; if(p && typeof p.x === "number"){ el.style.setProperty("position", "absolute", "important"); el.style.setProperty("left", p.x + "px", "important"); el.style.setProperty("top", p.y + "px", "important"); el.style.setProperty("transform", "none", "important"); } }); }',
    'function collectGuides(exceptEl){ const wrap = document.getElementById("wrapper"); const wrapRect = wrap.getBoundingClientRect(); const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\'true\'][data-block-id]")).filter(el => el !== exceptEl); return { wrapRect, items: els.map(el => { const r = el.getBoundingClientRect(); const left = r.left - wrapRect.left; const right = r.right - wrapRect.left; const top = r.top - wrapRect.top; const bottom = r.bottom - wrapRect.top; return { rect: { left, right, top, bottom, width: r.width, height: r.height }, v: [left, (left+right)/2, right], h: [top, (top+bottom)/2, bottom] }; }) }; }',
    'function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){ const curLeft = nextLeft, curRight = nextLeft + width, curTop = nextTop, curBottom = nextTop + height; const curCX = (curLeft + curRight) / 2, curCY = (curTop + curBottom) / 2; const selfV = [{x:curLeft},{x:curCX},{x:curRight}], selfH = [{y:curTop},{y:curCY},{y:curBottom}]; let best = { dx: 0, dy: 0, vLine: null, hLine: null, vSeg: null, hSeg: null, vDist: 6, hDist: 6 }; guides.items.forEach(it => { it.v.forEach(gx => selfV.forEach(sv => { const d = Math.abs(gx - sv.x); if(d < best.vDist){ best.vDist = d; best.dx = gx - sv.x; best.vLine = gx; best.vSeg = { y1: Math.min(curTop, it.rect.top), y2: Math.max(curBottom, it.rect.bottom) }; } })); it.h.forEach(gy => selfH.forEach(sh => { const d = Math.abs(gy - sh.y); if(d < best.hDist){ best.hDist = d; best.dy = gy - sh.y; best.hLine = gy; best.hSeg = { x1: Math.min(curLeft, it.rect.left), x2: Math.max(curRight, it.rect.right) }; } })); }); return best; }',
    
    // 🔥 [수정 2] 화면 높이 자동 조절 함수 (updateWrapperHeight)
    'function updateWrapperHeight() {',
    '  const wrap = document.getElementById("wrapper");',
    '  const els = wrap.querySelectorAll("[data-block-id]");',
    '  let maxBottom = 1080; // 기본 높이',
    '  els.forEach(el => {',
    '    const bottom = el.offsetTop + el.offsetHeight;',
    '    if(bottom > maxBottom) maxBottom = bottom;',
    '  });',
    '  wrap.style.minHeight = (maxBottom + 50) + "px";',
    '  document.body.style.minHeight = (maxBottom + 50) + "px";',
    '}',

    // 초기화 및 이벤트 리스너 등록
    'function init(){',
    '  applyBuilderStyles();',
    '  applyContentAttrs();',
    '  syncClassStyles();',
    '  applyPositions();',
    
    // 높이 조절 실행
    '  updateWrapperHeight();',
    '  setInterval(updateWrapperHeight, 1000);', // 1초마다 감시

    '  window.addEventListener("message",(e)=>{',
    '    if(e&&e.data&&e.data.type==="highlight_element"){',
    '      document.querySelectorAll(".wc-highlight").forEach(el=>el.classList.remove("wc-highlight"));',
    '      const t=document.querySelector("[data-block-id=\'"+e.data.blockId+"\']");',
    '      t&&t.classList.add("wc-highlight");',
    '    }',
    '    // 드래그 후 위치 업데이트 시 높이 재계산',
    '    if(e.data.type === "update_free_position") { setTimeout(updateWrapperHeight, 100); }',
    '  });',

    '  if(isRunning) return;',
    '  const wrap=document.getElementById("wrapper");',
    '  if(!wrap) return;',
    '  let dragging=null;',
    '  wrap.addEventListener("pointerdown",(ev)=>{',
    '    const t=ev.target.closest("#wrapper > [data-draggable=\'true\'][data-block-id]");',
    '    if(!t)return;',
    '    const r=t.getBoundingClientRect(),wr=wrap.getBoundingClientRect();',
    '    dragging={el:t,baseLeft:r.left-wr.left,baseTop:r.top-wr.top,startX:ev.clientX,startY:ev.clientY,guides:collectGuides(t),pointerId:ev.pointerId};',
    '    t.classList.add("wc-dragging");',
    '    t.setPointerCapture(ev.pointerId);',
    '    window.parent.postMessage({type:"select_block",blockId:t.getAttribute("data-block-id")},"*");',
    '  });',
    '  wrap.addEventListener("pointermove",(ev)=>{',
    '    if(!dragging)return;',
    '    const dx=ev.clientX-dragging.startX,dy=ev.clientY-dragging.startY;',
    '    let nextL=dragging.baseLeft+dx,nextT=dragging.baseTop+dy;',
    '    const r=dragging.el.getBoundingClientRect(),wr=wrap.getBoundingClientRect();',
    '    if(nextL<0)nextL=0;if(nextT<0)nextT=0;',
    '    // 높이 제한 제거 (아래로 무한정 갈 수 있게)',
    '    // if(nextT+r.height>wr.height)nextT=wr.height-r.height; (제거됨)', 
    '    const snap=computeSmartSnap({nextLeft:nextL,nextTop:nextT,width:r.width,height:r.height,guides:dragging.guides});',
    '    hideGuides();',
    '    snap.vLine&&showVSeg(snap.vLine,snap.vSeg.y1,snap.vSeg.y2);',
    '    snap.hLine&&showHSeg(snap.hLine,snap.hSeg.x1,snap.hSeg.x2);',
    '    dragging.el.style.left=nextL+snap.dx+"px";',
    '    dragging.el.style.top=nextT+snap.dy+"px";',
    '  });',
    '  wrap.addEventListener("pointerup",(ev)=>{',
    '    if(!dragging)return;',
    '    const t=dragging.el;',
    '    hideGuides();',
    '    t.classList.remove("wc-dragging");',
    '    window.parent.postMessage({type:"update_free_position",blockId:t.getAttribute("data-block-id"),x:parseInt(t.style.left),y:parseInt(t.style.top)},"*");',
    '    setTimeout(updateWrapperHeight, 100);', // 드래그 끝난 후 높이 재계산
    '    dragging=null;',
    '  });',
    '}',
    'window.onload = init;',
    '<\/script>',
    '</body></html>',
  ];
  const newHtml = htmlParts.join('\n');
  
  // 기존 코드와 비교해서 다를 때만 업데이트!
  if (previewSrc.value !== newHtml) {
    previewSrc.value = newHtml;
  }
};

/* ============================================================

 * 커스텀 블록 등록 및 툴박스

 * ============================================================ */

const defineCustomBlocks = () => {
  Layout.defineBlocks();
  Content.defineBlocks();
  ContentAttr.defineBlocks();
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

const toolboxXMLs = {
  layout: Layout.toolbox,
  content: Content.toolbox,
  contentAttr: ContentAttr.toolbox,
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
  // [핵심] 함수 시작하자마자 확실한 변수 하나를 만듭니다.
  // 이제 이 함수 안에서는 'workspace' 대신 'currentWorkspace'만 믿고 씁니다.
  const currentWorkspace = Blockly.getMainWorkspace();

  // 만약 아직 워크스페이스가 안 만들어졌다면, 에러 내지 말고 조용히 종료 (안전장치)
  if (!currentWorkspace) return; 

  let text = (xmlText || '<xml></xml>').trim();
  if (!text.startsWith('<xml')) text = `<xml>${text}</xml>`;

  try {
    const dom = Blockly.utils.xml.textToDom(text);

    // '요소' 카테고리가 없으면 추가하는 로직
    if (dom.getElementsByTagName('category').length === 0) {
      const category = Blockly.utils.xml.createElement('category');
      category.setAttribute('name', '요소');
      category.setAttribute('colour', '#5c6bc0');
      while (dom.firstChild) category.appendChild(dom.firstChild);
      dom.appendChild(category);
    }

    // 1. 툴박스 업데이트 (우리가 만든 변수 사용)
    currentWorkspace.updateToolbox(dom);

    // 2. 기존 CSS 조정 (우리가 만든 변수 사용)
    const workspaceDom = currentWorkspace.getParentSvg().parentNode;
    const toolboxDiv = workspaceDom.querySelector('.blocklyToolboxDiv');
    if (toolboxDiv) toolboxDiv.style.display = 'none';

    const toolbox = currentWorkspace.getToolbox();

    if (toolbox && toolbox.getToolboxItems && toolbox.getToolboxItems().length > 0) {
      toolbox.selectItemByPosition(0);
      currentWorkspace.getFlyout().autoClose = false;

      // 3. Flyout(메뉴) 일단 숨기기
      const immediateFlyouts = document.querySelectorAll('.blocklyFlyout');
      immediateFlyouts.forEach((flyout) => {
        flyout.style.opacity = '0';
      });

      // 4. 애니메이션 시작 (약간의 딜레이 후)
      setTimeout(() => {
        // 리사이즈도 우리 변수로 실행
        Blockly.svgResize(currentWorkspace);

        const allFlyouts = document.querySelectorAll('.blocklyFlyout');
        allFlyouts.forEach((flyoutSvg) => {
          flyoutSvg.style.opacity = '1';

          const blocks = flyoutSvg.querySelector('.blocklyBlockCanvas');

          if (blocks) {
            blocks.animate(
              [
                { transform: 'translate(-300px, 0)', opacity: 0 },
                { transform: 'translate(0, 0)', opacity: 1 }
              ],
              { duration: 300, easing: 'ease', fill: 'forwards', composite: 'add' }
            );
          }
        });
      }, 100);
    }
  } catch (e) {
    console.error("setToolbox 오류:", e);
    // 에러가 나도 우리 변수가 살아있으면 초기화 시도
    if (currentWorkspace) {
      currentWorkspace.updateToolbox('<xml></xml>');
    }
  }
};

const saveCurrentWorkspaceToPage = () => {
  if (!workspace) return;

  const page = pages.value.find((p) => p.id === selectedPageId.value);

  if (!page) return;

  // ✨ [추가] 현재 워크스페이스의 모든 블록을 돌며 좌표 데이터를 최신화합니다.

  workspace.getAllBlocks(false).forEach((block) => {
    if (block.type === 'style_tag' && block.data) {
      // 이미 block.data에 좌표가 있으므로, 이 데이터가 XML에 포함되도록 강제합니다.

      block.setMutationValue && block.setMutationValue('data', block.data);
    }
  });

  const dom = Blockly.Xml.workspaceToDom(workspace);

  const xmlText = Blockly.Xml.domToText(dom);

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
  saveCurrentWorkspaceToPage();

  codeCache.value = { structure: '', style: '', logic: '' };
  selectParent('structure');

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

  if (xml) {
    try {
      Blockly.Xml.domToWorkspace(Blockly.utils.xml.textToDom(xml), workspace);
    } catch (e) {}
  }

  setToolbox(toolboxXMLs.empty);

  const group = categoryGroups.find((g) => g.id === modeId);

  if (group && group.items && group.items.length > 0) {
    // 첫 번째 아이템(예: layout)을 선택하도록 호출

    selectCategory(group.items[0]);
  } else {
    // 하위 메뉴가 없으면 빈 툴박스

    setToolbox(toolboxXMLs.empty);
  }

  refreshCodeAndPreview();
};
// [상수 추가] 스크립트 맨 위에 추가해두세요
const FLYOUT_WIDTH = 300; 

const selectCategory = (key) => {
  if (!workspace) return;

  // 1. [닫기] 이미 열린 탭 클릭 시
  if (activeTab.value === key) {
    activeTab.value = null; // Vue 상태 해제 -> CSS가 워크스페이스 원상복구
    workspace.getFlyout().hide();
    
    // 워크스페이스가 줄어들었으니 리사이즈 한 번 해줌
    setTimeout(() => Blockly.svgResize(workspace), 300);
    return;
  }

  // 2. [열기/교체]
  activeTab.value = key; // Vue 상태 변경 -> CSS가 워크스페이스 밈(300px)

  // XML 파싱 및 블록 가져오기
  const xmlText = toolboxXMLs[key] || '<xml></xml>';
  const dom = Blockly.utils.xml.textToDom(xmlText);
  let blockList = [];

  if (dom.nodeName.toLowerCase() === 'xml') {
     const category = dom.querySelector('category');
     if (category) {
        const customType = category.getAttribute('custom');
        if (customType) {
            blockList = workspace.getToolboxCategoryCallback(customType)(workspace);
        } else {
            blockList = Array.from(category.children);
        }
     } else {
        blockList = Array.from(dom.children);
     }
  }

  // 3. Flyout 내용 교체 (단순하게 show만 호출)
  const flyout = workspace.getFlyout();
  flyout.autoClose = false; // 이거 하나만 필수
  flyout.show(blockList);
  flyout.scrollToStart();

  // 4. 리사이즈 (CSS 애니메이션과 싱크 맞추기 위해 약간의 딜레이만 줌)
  // 애니메이션 로직은 없습니다. 단지 "화면이 밀렸으니 좌표 다시 잡아라" 명령입니다.
  setTimeout(() => {
    Blockly.svgResize(workspace);
  }, 0);
};
const toggleRun = async () => {
  isRunning.value = !isRunning.value;
  await nextTick();
  updatePreview();
};

const changeModel = () => {
  isPhone.value = !isPhone.value;
  if (!isPhone.value) isLandscape.value = false;
  updatePreview();
};

const toggleOrientation = () => {
  isLandscape.value = !isLandscape.value;
  updatePreview(); // 화면 비율 변경 시 프리뷰 갱신
};
const handleThemeApply = (payload) => {
  // payload 구조: { theme: {...}, settings: {...} }
  
  // 1. 테마 적용 (payload.theme 사용)
  const selectedTheme = payload.theme;
  currentTheme.id = selectedTheme.id;
  currentTheme.toolboxColor = selectedTheme.toolboxColor;
  currentTheme.workspaceColor = selectedTheme.workspaceColor;
  
  // DOM 색상 변경 로직 (기존과 동일)
  const flyoutBg = document.querySelector('.flyout-bg-panel');
  if (flyoutBg) flyoutBg.style.backgroundColor = selectedTheme.toolboxColor;
  
  const workspaceBg = document.querySelector('.blocklyMainBackground');
  if (workspaceBg) workspaceBg.style.fill = selectedTheme.workspaceColor;

  const blocklyDiv = document.getElementById('blocklyDiv');
  if (blocklyDiv) blocklyDiv.style.backgroundColor = selectedTheme.workspaceColor;

  // 2. 다른 설정 적용 (payload.settings 사용)
  // 예: 그리드 설정, 프로젝트 이름 변경 등
  console.log("다른 설정들:", payload.settings);
  // 예: if (payload.settings.showGrid !== workspace.getGrid().isVisible()) ...
  
  // 3. 저장 및 닫기
  localStorage.setItem('wc_theme_settings', JSON.stringify(currentTheme));
  isThemeModalOpen.value = false;
}
onMounted(async () => {
  // 0. 한국어 설정
  if (Ko) Blockly.setLocale(Ko);

  // 1. 블록 정의
  defineCustomBlocks();
  await nextTick();

  // ============================================================
  // ✨ [설정] Blockly 주입 (기본 'zelos' 사용 - 뚱뚱한 블록)
  // ============================================================
  workspace = Blockly.inject('blocklyDiv', {
    renderer: 'zelos',  // 👈 형이 원한 뚱뚱한 스타일!
    toolbox: toolboxXMLs.empty,
    move: { scrollbars: true, drag: true, wheel: true },
    zoom: { 
      controls: true, 
      wheel: false, // Ctrl+휠 줌을 위해 기본 휠 줌은 끔
      startScale: 0.8 
    },
    grid: { spacing: 20, length: 3, colour: '#ccc', snap: true },
    trashcan: true,
  });

  // 2. 테마 적용 (저장된 설정 불러오기)
  let savedTheme = currentTheme;
  try {
    const loaded = localStorage.getItem('wc_theme_settings');
    if (loaded) {
      savedTheme = JSON.parse(loaded);
      Object.assign(currentTheme, savedTheme); 
    }
  } catch (e) {}

  // 색상 적용
  const flyoutBg = document.querySelector('.flyout-bg-panel');
  if (flyoutBg) flyoutBg.style.backgroundColor = savedTheme.toolboxColor;
  const wsBg = document.querySelector('.blocklyMainBackground');
  if (wsBg) wsBg.style.fill = savedTheme.workspaceColor;
  const blocklyDiv = document.getElementById('blocklyDiv');
  if (blocklyDiv) blocklyDiv.style.backgroundColor = savedTheme.workspaceColor;

  // ============================================================
  // ✨ [설정] UI 밀림 방지 (회색바 제거)
  // ============================================================
  const metricsManager = workspace.getMetricsManager();
  metricsManager.getToolboxMetrics = () => ({ width: 0, height: 0, position: Blockly.TOOLBOX_AT_LEFT });
  metricsManager.getFlyoutMetrics = () => ({ width: 0, height: 0, position: Blockly.TOOLBOX_AT_LEFT });
  
  const flyout = workspace.getFlyout();
  if (flyout) flyout.autoClose = false;
  workspace.resize();

  // ============================================================
  // ✨ [추가] VS Code 스타일 줌 (Ctrl + Wheel)
  // ============================================================
  blocklyDiv.addEventListener('wheel', (e) => {
    if (e.ctrlKey) {
      e.preventDefault();
      const direction = e.deltaY > 0 ? -1 : 1;
      workspace.zoom(e.offsetX, e.offsetY, direction);
    }
  }, { passive: false });

  // 3. Blockly 이벤트 리스너
  let debounceTimer = null;
  workspace.addChangeListener((e) => {
    if (e.type === Blockly.Events.SELECTED) {
      if (!isSelectingProgrammatically) handleSelection(e.newElementId, 'blockly');
      return;
    }
    if (e.type === Blockly.Events.UI || e.type === Blockly.Events.CLICK) return;
    
    // 블록 변경 시 업데이트
    if ([Blockly.Events.BLOCK_CHANGE, Blockly.Events.BLOCK_CREATE, Blockly.Events.BLOCK_DELETE, Blockly.Events.BLOCK_MOVE].includes(e.type)) {
      updateObjectListFromWorkspace();
    }
    
    if (debounceTimer) clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      refreshCodeAndPreview();
      if (selectedBlockId.value) handleSelection(selectedBlockId.value, 'blockly');
    }, 500);
  });

  // 4. Iframe 통신 (드래그, 선택 등)
  window.addEventListener('message', (event) => {
    const data = event.data;
    if (!data) return;
    
    if (data.type === 'update_free_position') {
      const { blockId, x, y } = data;
      const block = workspace.getBlockById(blockId);
      if (block) {
        block.data = JSON.stringify({ x: Number(x || 0), y: Number(y || 0) });
        saveCurrentWorkspaceToPage();
        refreshCodeAndPreview();
      }
    }
    // 페이지 이동 등 나머지 메시지 처리
    if (data.type === 'NAVIGATE' || data.type === 'REDIRECT' || data.type === 'change_page_request') {
      const targetId = data.pageId;
      const targetPage = pages.value.find((p) => p.id === targetId || p.route === targetId || p.name === targetId);
      if (targetPage) {
        lockPage(targetPage.id);
        selectPage(targetPage.id);
      } else {
        alert('이동할 페이지를 찾을 수 없습니다: ' + targetId);
      }
    }
    if (data.type === 'select_block') handleSelection(data.blockId, 'iframe');
    if (data.type === 'deselect_block') handleSelection(null, 'iframe');
  });

  // 5. 전역 함수 및 데이터 로드
  window.WC_GET_PAGES = () => {
    if (!pages.value || pages.value.length === 0) return [['페이지 없음', '']];
    return pages.value.map((p) => [p.name, p.id]);
  };

  const stored = loadPagesFromStorage();
  if (stored && stored.length > 0) {
    pages.value = stored;
    loadPageById(pages.value[0].id);
  } else {
    savePagesToStorage();
    loadPageById(pages.value[0].id);
  }

  // 6. 리사이즈 감지 (Workspace & Iframe)
  new ResizeObserver(() => {
    if (workspace) Blockly.svgResize(workspace);
  }).observe(document.getElementById('workspace-area'));

  // 🔥 반응형 PC 뷰를 위한 Iframe 크기 감지
// onMounted 맨 마지막 부분의 iframeResizeObserver 수정
  const iframeResizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      wrapperWidth.value = entry.contentRect.width;
      // 👇 [추가] 높이도 실시간으로 잽니다!
      wrapperHeight.value = entry.contentRect.height; 
    }
  });
  const iframeWrapper = document.querySelector('.iframe-wrapper');
  if (iframeWrapper) iframeResizeObserver.observe(iframeWrapper);

  // 7. ESC 키 종료
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isRunning.value) toggleRun();
  });
});
// PC 모드일 때는 강제로 넓게 잡고 축소해서 보여줌
const iframeStyle = computed(() => {
  if (isPhone.value) {
    return {
      width: '100%',
      height: '100%',
      transform: 'none',
      border: 'none'
    };
  } else {
    const baseWidth = 1920; 
    const baseHeight = 1080; // 기본 FHD 높이
    
    // 1. 박스 크기 가져오기
    const currentWidth = wrapperWidth.value || 600; 
    const currentHeight = wrapperHeight.value || 800;

    // 2. 배율 계산
    const scaleRatio = currentWidth / baseWidth; 

    // 🔥 [핵심 로직] 
    // "미리보기 박스 높이"를 "배율"로 나누면, iframe이 가져야 할 실제 높이가 나옵니다.
    // 예: 박스 800px / 배율 0.5 = iframe은 1600px이 되어야 꽉 참.
    // 단, 최소 1080px은 보장해야 함 (Math.max 사용)
    const finalHeight = Math.max(baseHeight, currentHeight / scaleRatio);

    return {
      position: 'absolute',
      transformOrigin: 'top left',
      
      width: `${baseWidth}px`,      
      height: `${finalHeight}px`, // 👈 계산된 높이 적용 (빈 공간 제거됨!)
      
      transform: `scale(${scaleRatio})`, 
      border: 'none',
      backgroundColor: '#fff',
      boxShadow: '0 0 30px rgba(0,0,0,0.1)' // (선택) 그림자 좀 더 진하게
    };
  }
});
// 📚 애니메이션 도서관 (이름: CSS코드)
const ANIMATION_LIBRARY = {
  // [등장]
  fadeIn: `@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }`,
  zoomIn: `@keyframes zoomIn { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }`,
  flipInY: `@keyframes flipInY { from { transform: perspective(400px) rotateY(90deg); opacity: 0; } to { transform: perspective(400px) rotateY(0deg); opacity: 1; } }`,
  backInDown: `@keyframes backInDown { 0% { transform: translateY(-1200px) scale(0.7); opacity: 0.7; } 80% { transform: translateY(0px) scale(0.7); opacity: 0.7; } 100% { transform: scale(1); opacity: 1; } }`,
  rollIn: `@keyframes rollIn { from { opacity: 0; transform: translateX(-100%) rotate(-120deg); } to { opacity: 1; transform: translateX(0px) rotate(0deg); } }`,
  slideInDown: `@keyframes slideInDown { from { transform: translateY(-100%); visibility: visible; } to { transform: translateY(0); } }`,
  bounceIn: `@keyframes bounceIn { 0%, 20%, 40%, 60%, 80%, 100% { transition-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1); } 0% { opacity: 0; transform: scale3d(0.3, 0.3, 0.3); } 20% { transform: scale3d(1.1, 1.1, 1.1); } 40% { transform: scale3d(0.9, 0.9, 0.9); } 60% { opacity: 1; transform: scale3d(1.03, 1.03, 1.03); } 80% { transform: scale3d(0.97, 0.97, 0.97); } 100% { opacity: 1; transform: scale3d(1, 1, 1); } }`,
  jackInTheBox: `@keyframes jackInTheBox { 0% { opacity: 0; transform: scale(0.1) rotate(30deg); transform-origin: center bottom; } 50% { transform: rotate(-10deg); } 70% { transform: rotate(3deg); } 100% { opacity: 1; transform: scale(1); } }`,
  blurIn: `@keyframes blurIn { from { filter: blur(20px); opacity: 0; } to { filter: blur(0); opacity: 1; } }`,
  swirlIn: `@keyframes swirlIn { from { transform: rotate(-540deg) scale(0); opacity: 0; } to { transform: rotate(0) scale(1); opacity: 1; } }`,
  
  // [강조]
  pulse: `@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }`,
  heartbeat: `@keyframes heartbeat { 0% { transform: scale(1); } 14% { transform: scale(1.1); } 28% { transform: scale(1); } 42% { transform: scale(1.1); } 70% { transform: scale(1); } }`,
  jello: `@keyframes jello { 11.1% { transform: translate3d(0, 0, 0); } 22.2% { transform: skewX(-12.5deg) skewY(-12.5deg); } 33.3% { transform: skewX(6.25deg) skewY(6.25deg); } 44.4% { transform: skewX(-3.125deg) skewY(-3.125deg); } 55.5% { transform: skewX(1.5625deg) skewY(1.5625deg); } 66.6% { transform: skewX(-0.78125deg) skewY(-0.78125deg); } 77.7% { transform: skewX(0.390625deg) skewY(0.390625deg); } 88.8% { transform: skewX(-0.1953125deg) skewY(-0.1953125deg); } 100% { transform: translate3d(0, 0, 0); } }`,
  floating: `@keyframes floating { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-15px); } }`,
  shake: `@keyframes shake { 0%, 100% { transform: translateX(0); } 10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); } 20%, 40%, 60%, 80% { transform: translateX(5px); } }`,
  tada: `@keyframes tada { 0% { transform: scale3d(1, 1, 1); } 10%, 20% { transform: scale3d(0.9, 0.9, 0.9) rotate3d(0, 0, 1, -3deg); } 30%, 50%, 70%, 90% { transform: scale3d(1.1, 1.1, 1.1) rotate3d(0, 0, 1, 3deg); } 40%, 60%, 80% { transform: scale3d(1.1, 1.1, 1.1) rotate3d(0, 0, 1, -3deg); } 100% { transform: scale3d(1, 1, 1); } }`,
  rubberBand: `@keyframes rubberBand { 0% { transform: scale3d(1, 1, 1); } 30% { transform: scale3d(1.25, 0.75, 1); } 40% { transform: scale3d(0.75, 1.25, 1); } 50% { transform: scale3d(1.15, 0.85, 1); } 65% { transform: scale3d(0.95, 1.05, 1); } 75% { transform: scale3d(1.05, 0.95, 1); } 100% { transform: scale3d(1, 1, 1); } }`,
  swing: `@keyframes swing { 20% { transform: rotate3d(0, 0, 1, 15deg); } 40% { transform: rotate3d(0, 0, 1, -10deg); } 60% { transform: rotate3d(0, 0, 1, 5deg); } 80% { transform: rotate3d(0, 0, 1, -5deg); } 100% { transform: rotate3d(0, 0, 1, 0deg); } }`,
  rainbow: `@keyframes rainbow { 0% { color: #ff0000; } 33% { color: #00ff00; } 66% { color: #0000ff; } 100% { color: #ff0000; } }`,
  flip3D: `@keyframes flip3D { from { transform: perspective(400px) rotateY(0); } to { transform: perspective(400px) rotateY(360deg); } }`,
  swinging: `@keyframes swinging {0% { transform: rotate(0deg); transform-origin: top center; } 20% { transform: rotate(15deg); }40% { transform: rotate(-10deg); }60% { transform: rotate(5deg); }80% { transform: rotate(-5deg); }100% { transform: rotate(0deg); }}`
};
// 💾 [배포] 전체 프로젝트를 ZIP으로 다운로드 (화면 깨짐 방지 + 멀티 페이지)
const downloadProject = async () => {
  const zip = new JSZip();
  
  // 1. 페이지 ID와 파일명 매핑 정보 생성 (링크 이동용)
  // 예: { "page_123": "index.html", "page_456": "login.html" }
  const pageMap = {};
  pages.value.forEach((p, index) => {
    // 첫 페이지는 무조건 index.html, 나머지는 페이지이름.html
    const filename = index === 0 ? 'index.html' : `${p.name.trim()}.html`;
    pageMap[p.id] = filename;
  });

  // 2. 모든 페이지 순회하며 파일 생성
  for (const page of pages.value) {
    const filename = pageMap[page.id];
    
    // (1) 해당 페이지의 코드 생성
    // 주의: 현재 워크스페이스가 아니라, 저장된 데이터(page.workspaces)를 써야 함
    const structCode = generateCodeFromXML(page.workspaces.structure);
    const styleCode = generateCodeFromXML(page.workspaces.style);
    const logicCode = generateCodeFromXML(page.workspaces.logic);

    // (2) 애니메이션 Tree Shaking (쓰인 것만 추출)
    const fullSourceCode = structCode + styleCode + logicCode;
    let usedKeyframes = '';
    Object.keys(ANIMATION_LIBRARY).forEach(name => {
      if (fullSourceCode.includes(name)) {
        usedKeyframes += ANIMATION_LIBRARY[name] + '\n';
      }
    });

    // (3) HTML 세탁 (편집용 속성 제거)
    const cleanContainer = document.createElement('div');
    cleanContainer.innerHTML = structCode;

    const dirtyAttributes = [
      'data-block-id', 'data-draggable', 'data-wc-block', 'data-wc-style', 
      'contenteditable', 'spellcheck'
    ];

    cleanContainer.querySelectorAll('*').forEach(el => {
      dirtyAttributes.forEach(attr => el.removeAttribute(attr));
      el.classList.remove('wc-highlight', 'wc-dragging', 'selected');
      if (el.classList.length === 0) el.removeAttribute('class');
      
      // ⚠️ 중요: style 속성은 절대 지우면 안 됨 (좌표값 들어있음)
      // data-x, data-y는 지워도 됨
      el.removeAttribute('data-x');
      el.removeAttribute('data-y');
    });

    const cleanHtmlBody = cleanContainer.innerHTML;

    // (4) 최종 HTML 조립 (깨짐 방지 CSS 포함)
    const htmlContent = `
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${page.name}</title>
  <style>
    /* 🔥 [필수] 화면 깨짐 방지용 리셋 CSS */
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
    body { 
      background-color: #fff; 
      overflow-x: hidden; 
      position: relative; /* 중요: 절대 좌표의 기준점 */
    }
    * { box-sizing: border-box; }
    
    /* 콘텐츠 래퍼 (이 안에서 absolute가 작동함) */
    #root {
      position: relative;
      width: 100%;
      min-height: 100vh;
      overflow: hidden;
    }

    /* 사용자 정의 CSS */
    ${styleCode}

    ${usedKeyframes}
  </style>
</head>
<body>
  <div id="root">
    ${cleanHtmlBody}
  </div>

  <script>
    // 🚀 페이지 이동 로직 (배포용)
    const PAGE_MAP = ${JSON.stringify(pageMap)};
    
    function navigateToPage(targetId) {
      if (PAGE_MAP[targetId]) {
        window.location.href = PAGE_MAP[targetId];
      } else {
        console.error('이동할 페이지를 찾을 수 없습니다:', targetId);
      }
    }
    
    // 블록리 사용 함수들 연결
    function redirectToPage(targetId) { navigateToPage(targetId); }
    function goToPage(targetId) { navigateToPage(targetId); }

    // 사용자 로직 실행
    ${logicCode}
  <\/script>
</body>
</html>`.trim();

    // ZIP에 파일 추가
    zip.file(filename, htmlContent);
  }

  // 3. ZIP 파일 생성 및 다운로드
  const content = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(content);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'WebCrafter_Project.zip'; // 폴더명
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
</script>

<template>
  <div class="ide-container">
      <aside
        :class="[
          isPhone ? 'phone-size' : 'pc-size', 
          { 'is-landscape': isPhone && isLandscape }
        ]"
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
              class="btn-toggle"
              :class="{ running: isRunning }"
              @click="toggleRun"
            >
              {{ isRunning ? '⏹ 정지' : '▶ 시작' }}
            </button>

            <button 
              v-if="isPhone" 
              class="btn-rotate" 
              @click="toggleOrientation"
              title="화면 회전"
            >
              <span :style="{ 
                display: 'inline-block', 
                transition: '0.3s', 
                transform: isLandscape ? 'rotate(90deg)' : 'rotate(0deg)' 
              }">🔄</span>
            </button>

            <button
              class="btn-deploy"
              :class="isPhone ? 'phone-hide' : ''"
              @click="downloadProject"  >
              🚀 저장 (ZIP)
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

          <div class="iframe-wrapper">
            <iframe
              :key="`${isRunning}-${selectedPageId}-${isPhone}`"
              id="previewFrame"
              :srcdoc="previewSrc"
              :style="iframeStyle" 
              frameborder="0"
              :sandbox="'allow-same-origin allow-forms allow-popups allow-modals allow-popups-to-escape-sandbox allow-scripts'"
            >
            </iframe>
          </div>
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
        <div class="item"></div>
        <button class="mr-[42px]" @click="isThemeModalOpen = true"><Settings :size="23" /></button>
        <Teleport to="body">
        <ThemeSettingsModal 
          :open="isThemeModalOpen"
          :current-theme-id="currentTheme.id"
          @close="isThemeModalOpen = false"
          @apply="handleThemeApply"
        />
        </Teleport>
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
          <div class="flyout-bg-panel" :class="{ open: activeTab }"></div>

          <div id="blocklyDiv"></div>
        </div>
      </div>
    </div>
    <!-- AI 생성 모달-->
    <ConfirmModal
      :open="confirmModal.open"
      type="warning"
      :message="confirmModal.message"
      confirm-text="삭제"
      cancel-text="취소"
      @confirm="confirmDeletePage"
      @cancel="closeDeleteConfirm"
    />

    <GlobalModal
      :open="modal.open"
      :message="modal.message"
      :type="modal.type"
      @confirm="closeModal"
    />
  </div>

  <Teleport to="body">
    <AiChatBot @generate="handleAiBlockGeneration" />
  </Teleport>

  <Teleport to="body">
    <div v-if="isRunning" class="fullscreen-modal">
      <div class="modal-header">
        <div class="header-left">
          <span class="preview-badge">LIVE PREVIEW</span>
          <span class="page-info">{{ currentPageUrl }}</span>
        </div>
        
        <button class="btn-close" @click="toggleRun">
          ✕ 종료 (Esc)
        </button>
      </div>

      <div class="modal-body">
        <iframe
          id="fullscreenFrame"
          :srcdoc="previewSrc"
          frameborder="0"
          class="full-iframe"
        ></iframe>
      </div>
    </div>
  </Teleport>
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

/* ✅ 기존 .btn-ai, .btn-toggle, .btn-deploy 정의를 이렇게 업데이트해줘 */
.btn-ai,
.btn-toggle,
.btn-rotate, /* 🔄 회전 버튼도 같이 적용 */
.btn-deploy {
  border: none;
  padding: 0 12px; /* 좌우 여백을 조금 더 줘서 안정감 있게 */
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
  color: white;
flex-grow: 1;
  /* 🔥 세로 깨짐 방지 핵심 코드 */
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  white-space: nowrap !important; /* 👈 글자가 아래로 떨어지는 걸 막아줌 */
  flex-shrink: 0 !important;      /* 👈 부모가 좁아도 버튼이 안 찌그러짐 */
  height: 32px !important;        /* 높이를 통일해서 예쁘게 정렬 */
  line-height: 1 !important;      /* 글자 수직 중앙 정렬 보정 */
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
.item{
  flex-grow: 1;
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

/* 기존 코드 수정 */
.workspace-wrapper { 
  position: relative; 
  width: 100%; 
  height: 100%; 
  overflow: hidden; 
  /* transition 제거 또는 width만 적용 */
  transition: width 0.3s ease; 
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

/* Flyout(블록 목록)을 독립적인 레이어로 설정 */

:deep(.blocklyFlyout) {
  z-index: 100 !important;

  /* 작업공간 위로 띄우기 위해 위치 고정 */

  position: absolute !important;
}

/* 배경 투명도 및 클릭 관통 방지 */

/* 기존 코드 수정: Blockly의 SVG 배경을 투명하게 만듦 */
:deep(.blocklyFlyoutBackground) {
  fill: transparent !important;       /* 색상 투명 */
  fill-opacity: 0 !important;         /* 불투명도 0 */
  stroke: none !important;            /* 테두리 없음 */
}


/* 메인 작업공간(SVG)이 전체 너비를 차지하도록 강제 */
:deep(.blocklySvg) {
  width: 100% !important;
}

:deep(.blocklyToolbox) {
  display: none !important; /* ⭕ 회색 사이드바 영구 숨김 */
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
/* 1. 회색 툴박스 영역(Gap의 원인)을 아예 화면에서 지워버림 */
:deep(.blocklyToolboxDiv) {
  display: none !important;
  visibility: hidden !important;
  width: 0 !important;
  border: none !important;
}

/* 2. Flyout(메뉴판)을 왼쪽 끝(0px)에 강제로 딱 붙임 */
:deep(.blocklyFlyout) {
  left: 0 !important; 
  /* (참고) transform 속성은 스크립트의 애니메이션 로직이 제어하므로 여기선 건드리지 않음 */
}

/* 3. 혹시 모를 Flyout 내부 여백 제거 */
:deep(.blocklyFlyoutBackground) {
  x: 0 !important;
  y: 0 !important;
  /* stroke(테두리선) 때문에 1px 이격이 보일 수 있으므로 제거 */
  stroke: none !important; 
}
/* 새로 만든 300px 배경 패널 */
.flyout-bg-panel {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 300px;
  background-color: #dcdcdcba;/* 원하는 배경색 (예: 흰색) */
  /* 🔥 중요: 레이어 순서 */
  z-index: 90; /* 워크스페이스(0) 위, Blockly Flyout(100) 아래 */
  /* 애니메이션: 왼쪽에서 스윽 나오게 */
  transform: translateX(-100%);
}

/* 메뉴가 열렸을 때 (activeTab이 있을 때) */
.flyout-bg-panel.open {
  transform: translateX(0); /* 제자리로 이동 */
}
/* ============================================================
   🔥 [필수 수정] 드래그 중인 블록을 최상단으로 올리기
   ============================================================ */

/* 1. 블록 드래그 레이어 (이게 낮으면 툴박스 뒤로 숨음) */
:deep(.blocklyBlockDragSurface) {
  z-index: 99999 !important; /* 툴박스(100)보다 무조건 높게 */
  overflow: visible !important;
}

/* 2. 워크스페이스 드래그 레이어 (혹시 모를 상황 대비) */
:deep(.blocklyWsDragSurface) {
  z-index: 99999 !important;
  overflow: visible !important;
}

/* 3. 입력창(드롭다운, 텍스트입력) 및 툴팁도 가려지지 않게 최상단 고정 */
:deep(.blocklyWidgetDiv), 
:deep(.blocklyTooltipDiv) {
  z-index: 99999 !important; 
}

/* ✅ 가로 모드일 때 왼쪽 패널 너비 확장 */
.entry-panel.is-landscape {
  width: 650px !important; 
}

/* ✅ 핵심: 세로 비율(9:19.5)을 완벽히 뒤집은 진짜 가로 비율 */
.entry-panel.is-landscape .browser-mockup {
  width: 95% !important;        
  max-width: 600px !important; 
  aspect-ratio: 19.5 / 9 !important; /* 👈 형이 말한 완벽한 반전 비율 */
  height: auto !important;      
  margin: 50px auto !important;  
  transition: all 0.3s ease-in-out;
  
  /* 기기 디테일 */
  border: 10px solid #222;
  border-radius: 24px;
  box-shadow: 0 15px 45px rgba(0,0,0,0.4);
}

.entry-panel.is-landscape .browser-mockup iframe {
  width: 100% !important;
  height: 100% !important;
}

/* 회전 버튼 스타일 */
.btn-rotate {
  background: #4c97ff;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  color: white;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-rotate:hover {
  background: #3676d1;
}

/* ✅ 가로 모드일 때 다른 요소들 최적화 */
.entry-panel.is-landscape .control-buttons {
  gap: 4px !important; /* 가로일 땐 버튼 간격 좁게 */
}

/* 가로 모드일 때 주소창 너비 조절 */
.entry-panel.is-landscape .url-bar {
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  max-width: 100%;
}

/* ✨ [수정] Flex 제거하고 일반 박스로 변경 */
.iframe-wrapper {
  width: 100%;
  height: 100%;
  overflow: hidden;        /* 넘치는 것 자르기 */
  background-color: #fff;
  position: relative;      /* 자식(iframe)의 기준점 */
  display: block;          /* 🔥 Flex 삭제! 그냥 블록으로! */
}
</style>
<style>
/* 🚀 [중요] 모달 스타일은 scoped 밖으로 빼야 body로 이동해도 깨지지 않습니다 */
.fullscreen-modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: white;
  z-index: 99999 !important; /* 무조건 최상단 */
  display: flex;
  flex-direction: column;
}

.modal-header {
  height: 50px;
  background: #1a1a2e;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid #333;
  flex-shrink: 0; /* 헤더 크기 고정 */
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
  color: white;
}

.preview-badge {
  background: #ff4081;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 4px;
  font-weight: bold;
  animation: pulse 1.5s infinite;
}

.page-info {
  font-size: 0.9rem;
  color: #ccc;
  font-family: monospace;
}

.btn-close {
  background: #333;
  color: white;
  border: 1px solid #555;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: 0.2s;
}

.btn-close:hover {
  background: #d32f2f;
}

.modal-body {
  flex: 1;
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #fff;
}

.full-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}
</style>