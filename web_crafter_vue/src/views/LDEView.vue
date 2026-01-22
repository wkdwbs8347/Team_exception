<script setup>
/**

 * ============================================================

 * ✅ Web Crafter IDE (Final Fixed Version)

 * - 순환 참조 오류 해결 (pages 초기화 순서 변경)

 * - URL 중복 방지 로직 적용

 * ============================================================

 */
import JSZip from 'jszip';
import { ref, onMounted, nextTick, watch, computed, reactive, onUnmounted } from 'vue';

import * as Blockly from 'blockly';

import { javascriptGenerator } from 'blockly/javascript';

import * as Ko from 'blockly/msg/ko';

import 'blockly/blocks';

import ConfirmModal from '@/modal/ConfirmModal.vue';

import GlobalModal from '@/modal/GlobalModal.vue';

import api from '@/api/axios';

// ===== 카테고리 블록 import =====
//blockly 블록 정의 및 툴박스 XML을 각각의 모듈에서 가져옵니다.
import * as Layout from '@/components/block/Layout.vue';
import * as Content from '@/components/block/Content.vue';
import * as Form from '@/components/block/Form.vue';
import * as ContentAttr from '@/components/block/ContentAttr.vue';
import * as Component from '@/components/block/Component.vue';
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
import { Settings } from 'lucide-vue-next';
// 1. 컴포넌트 임포트
import AiChatBot from '@/modal/AiChatBot.vue';
import ThemeSettingsModal from '@/modal/ThemeSettingsModal.vue';
//기본 테마 설정
const isThemeModalOpen = ref(false);
const currentTheme = reactive({
  id: 'default',
  toolboxColor: '#dcdcdcba',
  workspaceColor: '#ffffff',
});

const props = defineProps({
  nickname: {
    type: String,
    default: 'lde_tester',
  },
  webId: {
    type: [String, Number],
    default: '9999', // 👈 빈 값('') 대신 본인만의 테스트 ID를 넣으세요 [cite: 2026-01-21]
  },
});
// ✅ [Fix] 블록 위치와 데이터 좌표 분리
const mergeBlockXmlByCategory = (existingXml, newDom, category) => {
  if (!existingXml || existingXml === '<xml></xml>') {
    return Blockly.Xml.domToText(newDom);
  }

  const existingDom = Blockly.utils.xml.textToDom(existingXml);
  const blocks = Array.from(existingDom.childNodes).filter(
    (n) => n.nodeName === 'block'
  );

  let startX = 50;
  let startY = 50;

  blocks.forEach((b) => {
    const y = parseInt(b.getAttribute('y') || '0');
    if (y > startY) startY = y;
  });

  const newBlocks = Array.from(newDom.children).filter(
    (n) => n.nodeName === 'block'
  );

  newBlocks.forEach((block, idx) => {
    let newX, newY;

    if (category === 'style') {
      const col = idx % 2;
      const row = Math.floor(idx / 2);
      newX = startX + col * 450;
      newY = startY + 200 + row * 300;
    } else {
      newX = startX;
      newY = startY + 150 + idx * 200;
    }

    // 1. 블록의 '에디터 상 위치'는 자동 정렬해 줍니다.
    block.setAttribute('x', newX.toString());
    block.setAttribute('y', newY.toString());

    // 🔥 [핵심 수정] 여기서 data 속성을 덮어쓰지 않습니다!
    // 기존에 iframe에서 저장한 좌표(data)가 있다면 유지하고, 없을 때만 초기화하거나 놔둡니다.
    // block.setAttribute('data', ...);  <-- 이 줄을 삭제함

    existingDom.appendChild(block);
  });

  return Blockly.Xml.domToText(existingDom);
};
const wrapperWidth = ref(600);
const wrapperHeight = ref(800);
const isSaving = ref(false);
let autoSaveTimer = null; // 타이머 ID 저장용
const handleAiBlockGeneration = (xmlText, isEditMode = false) => {
  if (!workspace || !xmlText) return;

  try {
    const xmlDom = Blockly.utils.xml.textToDom(xmlText);
    const categoryBuckets = {
      structure: document.createElement('xml'),
      style: document.createElement('xml'),
      logic: document.createElement('xml'),
    };

    // 1. 블록 분류
    Array.from(xmlDom.children).forEach((blockNode) => {
      if (blockNode.nodeName.toLowerCase() !== 'block') return;
      const type = blockNode.getAttribute('type') || '';
      if (
        type.startsWith('layout_') ||
        type.startsWith('content_') ||
        type.startsWith('form_') ||
        type.startsWith('component_')
      ) {
        categoryBuckets.structure.appendChild(blockNode);
      } else if (
        type.startsWith('style_') ||
        type.startsWith('effect_') ||
        type.startsWith('anim_')
      ) {
        categoryBuckets.style.appendChild(blockNode);
      } else {
        categoryBuckets.logic.appendChild(blockNode);
      }
    });

    const page = pages.value.find((p) => p.id === selectedPageId.value);
    if (!page) return;

    // 🔥 [에러 해결 핵심] page.workspaces가 없으면 초기화해줍니다.
    if (!page.workspaces) {
      page.workspaces = {
        structure: '<xml></xml>',
        style: '<xml></xml>',
        logic: '<xml></xml>',
      };
    }

    // 2. 각 카테고리별로 독립적으로 병합 및 저장
    Object.keys(categoryBuckets).forEach((key) => {
      const bucket = categoryBuckets[key];
      if (bucket.children.length > 0) {
        if (isEditMode) {
          page.workspaces[key] = Blockly.Xml.domToText(bucket);
        } else {
          // 기존 XML 데이터가 유효한지 한 번 더 확인 후 병합
          const existingXml = page.workspaces[key] || '<xml></xml>';
          page.workspaces[key] = mergeBlockXmlByCategory(
            existingXml,
            bucket,
            key
          );
        }
      }
    });

    savePagesToStorage();

    // 3. UI 갱신 (지우지 않고 필요한 데이터만 다시 로드)
    nextTick(() => {
      loadPageById(page.id);
    });
  } catch (e) {
    console.error('분류 중 오류:', e);
  }
};
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
let workspace = null;

// ✨ [핵심 수정] pages 선언과 초기값 주입 분리 (순환 참조 방지)

// 1. 빈 배열로 먼저 선언 (이제 createPage 안에서 pages.value 접근 가능)

const pages = ref([]);
const projectTitle = ref(''); // 프로젝트 전체 제목 전용 변수

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
  component: Component.category,

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

// IDEView.vue 내 수정



// IDEView.vue -> saveToServerAsJson 함수

const saveToServerAsJson = async () => {
  if (isSaving.value || !workspace) return;
  const page = pages.value.find(p => p.id === selectedPageId.value);
  if (!page) return;

  try {
    isSaving.value = true;
    
    // 🚀 수정: JSON 추출 후, 만약 비어있다면(기본값) XML로 강제 전환
    const state = Blockly.serialization.workspaces.save(workspace);
    let jsonState = JSON.stringify(state);
    
    // 블록이 있는데 JSON이 비어있다고 판단되면 XML 사용 (안전장치) [cite: 2026-01-21]
    if (jsonState.length < 50) { 
      const xmlDom = Blockly.Xml.workspaceToDom(workspace);
      jsonState = Blockly.Xml.domToText(xmlDom);
    }

    if (activeMode.value === 'structure') page.layoutData = jsonState;
    else if (activeMode.value === 'style') page.styleData = jsonState;
    else if (activeMode.value === 'logic') page.logicData = jsonState;

    const payload = {
      webId: props.webId,
      pageName: page.name,
      title: projectTitle.value,
      layoutData: page.layoutData, // 👈 실제 DB로 날아가는 알맹이
      styleData: page.styleData || '{}',
      logicData: page.logicData || '{}'
    };

    // 로그를 찍어 서버로 진짜 데이터가 가는지 직접 눈으로 확인하세요! [cite: 2026-01-21]
    console.log("📤 서버로 보낼 데이터:", payload.layoutData);

    const oldNameForQuery = page.oldName || page.name;
    await api.put(`/projects/${props.webId}/data?oldPageName=${encodeURIComponent(oldNameForQuery)}`, payload);
    
    page.oldName = page.name;
    console.log(`✅ [${page.name}] 서버 저장 성공!`);

  } catch (e) {
    console.error('❌ 저장 실패:', e);
  } finally {
    isSaving.value = false;
  }
};


// ✅ 블록 데이터를 화면에 그리는 함수
const loadWorkspaceState = (pageId) => {
  const page = pages.value.find(p => p.id === pageId);
  if (!page || !workspace) return;

  try {
    // 1. 렌더링 일시 중지 (성능 및 꼬임 방지) [cite: 2026-01-21]
    Blockly.Events.disable(); 
    workspace.clear();

    const rawData = page.layoutData || (page.workspaces && page.workspaces.structure);
    if (!rawData || rawData === '<xml></xml>' || rawData === '{}') {
      Blockly.Events.enable();
      return;
    }

    // 2. 데이터 주입
    if (typeof rawData === 'string' && rawData.trim().startsWith('<xml')) {
        const dom = Blockly.utils.xml.textToDom(rawData);
        Blockly.Xml.domToWorkspace(dom, workspace);
    } else {
        let state = typeof rawData === 'string' ? JSON.parse(rawData) : rawData;
        Blockly.serialization.workspaces.load(state, workspace);
    }

    // 🚀 [핵심 추가] 주입 직후 강제로 렌더링을 다시 계산하게 시킵니다.
    workspace.render(); // 블록들을 다시 그림 [cite: 2026-01-21]
    workspace.cleanUp(); // 블록 배치 정돈
    
    // 3. 렌더링 재개 및 화면 갱신 [cite: 2026-01-21]
    Blockly.Events.enable();
    Blockly.svgResize(workspace); 

    refreshCodeAndPreview();
  } catch (e) {
    console.error("❌ 블록 불러오기 최종 실패:", e);
    Blockly.Events.enable();
  }
};

const loadPagesFromStorage = () => {
  try {
    const rawData = localStorage.getItem(`wc_pages_${props.webId}`);
    if (!rawData) return []; // null 대신 빈 배열 추천

    const parsed = JSON.parse(rawData);
    
    // 프로젝트 제목 복구
    if (parsed.settings?.projectName) {
      projectTitle.value = parsed.settings.projectName;
    }

    return parsed.pages ?? parsed;
  } catch (e) {
    console.error('로컬 로드 실패:', e);
    return [];
  }
};
const savePagesToStorage = () => {
  try {
    const dataToSave = {
      settings: {
        projectName: projectTitle.value || '',
      },
      pages: pages.value,
    };

    localStorage.setItem(`wc_pages_${props.webId}`, JSON.stringify(dataToSave));

    // 🔥 [신규] 서버에 JSON 형식으로도 저장
    saveToServerAsJson();
  } catch (e) {
    console.error('로컬 저장 실패:', e);
  }
};
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

const setupInitialPages = async () => {
  const defaultPages = ['Home', 'Login'];
  console.log("🛠️ 초기 페이지 DB 생성을 시작합니다...");

  for (const name of defaultPages) {
    try {
      // 서버에 POST 요청을 보내 실제 DB 행(Row) 생성
      await api.post(`/projects/${props.webId}/pages`, {
        pageName: name,
        layoutData: '<xml></xml>',
        styleData: '{}',
        logicData: '{}'
      });
      console.log(`✅ DB에 [${name}] 페이지 생성 성공`);
    } catch (e) {
      console.warn(`⚠️ [${name}] 생성 건너뜀 (이미 존재할 가능성 있음)`);
    }
  }
};

const addPage = async () => {
  // 1. 새 페이지 이름 생성
  const newName = `Page ${pages.value.length + 1}`;
  
  try {
    // 2. 서버 DB에 새 페이지 전용 행(Row) 생성 요청
    // 💡 이 요청이 성공해야 나중에 '저장' 버튼을 눌렀을 때 DB가 데이터를 받아줍니다.
    await api.post(`/projects/${props.webId}/pages`, {
      pageName: newName,
      layoutData: '{}', 
      styleData: '{}',
      logicData: '{}'
    });

    // 3. 서버 생성 성공 시에만 로컬 리스트에 추가
    const page = createPage(newName);
    pages.value.push(page);
    
    // 4. 로컬 스토리지 동기화 및 페이지 이동
    savePagesToStorage();
    selectPage(page.id); 
    
    console.log(`🚀 [${newName}] DB 행 생성 및 페이지 추가 완료`);
  } catch (e) {
    console.error("페이지 생성 실패:", e);
    alert("서버에 페이지를 생성하지 못했습니다. 다시 시도해 주세요.");
  }
};

// 1. 삭제 버튼 클릭 시 실행되는 함수
const deletePage = (pageId) => {
  // 💡 [첫 번째 체크] 모달을 띄우기 전에 미리 확인 [cite: 2026-01-21]
  if (pages.value.length <= 1) {
    openModal('최소 하나의 페이지는 있어야 합니다.', 'info');
    return;
  }

  openDeleteConfirm(pageId); // 삭제 확인창 띄움 [cite: 2026-01-21]
};

const openDeleteConfirm = (pageId) => {
  confirmModal.value = {
    open: true,
    message: '이 페이지를 삭제하시겠습니까?',
    payload: { pageId },
  };
};

// 2. 삭제 확인 모달에서 '확인'을 눌렀을 때 실행되는 실제 삭제 함수
const deletePageNow = async (pageId) => {
  // 💡 [두 번째 체크] 실제로 지우기 직전에 한 번 더 확인 (보안 및 오류 방지) [cite: 2026-01-21]
  if (pages.value.length <= 1) {
    openModal('최소 하나의 페이지는 있어야 합니다.', 'info');
    return;
  }

  const idx = pages.value.findIndex((p) => p.id === pageId);
  if (idx === -1) return;

  const targetPage = pages.value[idx];
  // 이름 변경 대응을 위해 oldName 사용 [cite: 2026-01-21]
  const targetName = targetPage.oldName || targetPage.name;

  try {
    // 🔥 [서버 DB 삭제] 백엔드에 삭제 요청 [cite: 2026-01-21]
    await api.delete(`/projects/${props.webId}/pages?pageName=${encodeURIComponent(targetName)}`);

    // ✅ 서버 삭제 성공 시에만 화면 리스트에서 제거 [cite: 2026-01-21]
    pages.value.splice(idx, 1);
    savePagesToStorage(); // 로컬 스토리지 동기화 [cite: 2026-01-21]

    if (selectedPageId.value === pageId) {
      loadPageById(pages.value[0].id);
    }
    
    console.log(`✅ [${targetName}] 페이지가 성공적으로 삭제되었습니다.`);
  } catch (e) {
    console.error("❌ 삭제 실패:", e);
    alert("서버 연결 오류로 삭제에 실패했습니다.");
  }
};

const closeDeleteConfirm = () => {
  confirmModal.value.open = false;
};

const confirmDeletePage = () => {
  const pageId = confirmModal.value.payload?.pageId;
  if (!pageId) return;

  deletePageNow(pageId);
  closeDeleteConfirm();
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
      el.removeAttribute('data-wc-field');

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

// ✅ [Fix] 위치 정보 로드 로직 수정
const getPositionsMap = () => {
  const map = {};
  const page = pages.value.find((p) => p.id === selectedPageId.value);

  if (!page) return map;

  // 헬퍼 함수: 블록 리스트에서 data(좌표) 추출
  const extractFromBlocks = (blocks) => {
    blocks.forEach((b) => {
      // 1순위: 블록의 data 속성에 저장된 JSON 좌표 ({x: 100, y: 200})
      if (b.data) {
        try {
          const p = JSON.parse(b.data);
          // 좌표가 유효한 숫자일 때만 맵에 등록
          if (p && typeof p.x === 'number' && typeof p.y === 'number') {
            map[b.id] = { x: p.x, y: p.y };
          }
        } catch (e) {
          /* JSON 파싱 에러 무시 */
        }
      }
    });
  };

  // Case 1: 현재 '화면 구성(structure)' 탭을 보고 있다면? -> 라이브 워크스페이스에서 가져옴
  if (activeMode.value === 'structure' && workspace) {
    extractFromBlocks(workspace.getAllBlocks(false));
  }

  // Case 2: 다른 탭(디자인/로직)에 있거나 실행(Start) 중이라면? -> 저장된 XML을 파싱해서 가져옴
  // (이 로직이 없으면 다른 탭 갔을 때 좌표가 다 날아갑니다)
  if (
    page.workspaces.structure &&
    page.workspaces.structure !== '<xml></xml>'
  ) {
    try {
      // 이미 맵에 있는 건 건너뛰고(라이브 우선), 없는 것만 채워넣기 위해 임시 워크스페이스 생성
      const tempWs = new Blockly.Workspace();
      const dom = Blockly.utils.xml.textToDom(page.workspaces.structure);
      Blockly.Xml.domToWorkspace(dom, tempWs);

      const savedBlocks = tempWs.getAllBlocks(false);
      savedBlocks.forEach((b) => {
        // 이미 맵에 최신 정보가 있다면 덮어쓰지 않음
        if (!map[b.id] && b.data) {
          try {
            const p = JSON.parse(b.data);
            if (p && typeof p.x === 'number' && typeof p.y === 'number') {
              map[b.id] = { x: p.x, y: p.y };
            }
          } catch (e) {}
        }
      });
      tempWs.dispose(); // 메모리 정리
    } catch (e) {
      console.error('위치 정보 로드 실패:', e);
    }
  }

  return map;
};

const generateCodeFromXML = (xmlText, gen = javascriptGenerator) => {
  if (!xmlText || xmlText === '<xml></xml>') return '';

  let headlessWorkspace = null;
  try {
    const dom = Blockly.utils.xml.textToDom(xmlText);
    headlessWorkspace = new Blockly.Workspace();
    Blockly.Xml.domToWorkspace(dom, headlessWorkspace);

    // ✅ 어떤 generator든 init 가능하면 무조건 init
    if (gen && typeof gen.init === 'function') gen.init(headlessWorkspace);

    return gen.workspaceToCode(headlessWorkspace);
  } catch (e) {
    return '';
  } finally {
    headlessWorkspace?.dispose();
  }
};

const patchPrettyGenerator = () => {
  const pretty = Interaction.javascriptGeneratorPretty;
  if (!pretty) return;

  // pretty가 forBlock을 갖고 있어야 함
  pretty.forBlock = pretty.forBlock || {};

  // ✅ pretty에 "없는 것만" 원본에서 가져오기
  Object.keys(javascriptGenerator.forBlock || {}).forEach((type) => {
    if (!pretty.forBlock[type]) {
      pretty.forBlock[type] = javascriptGenerator.forBlock[type];
    }
  });

  // (선택) pretty에 없는 scrub_ 같은 것도 원본으로 맞추고 싶으면:
  if (!pretty.scrub_) pretty.scrub_ = javascriptGenerator.scrub_;
  if (!pretty.finish) pretty.finish = javascriptGenerator.finish?.bind(pretty);
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

watch(objects,(newObjects) => {
    if (isRestoring || !newObjects || newObjects.length === 0) return;

    if (Interaction.updateObjectList) Interaction.updateObjectList(newObjects);
  },
  { deep: true }
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
      console.error('객체 목록 로드 실패:', e);
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

// 동작 카테고리 로그인/회원가입 기능 블록 관련 RUNTIME
const AUTH_RUNTIME_JS = `(function(){
  if(window.__WC_AUTH_RUNTIME__) return;
  window.__WC_AUTH_RUNTIME__ = true;

  function wcGetApiBase(){
    return (window.WC_API_BASE || "http://localhost:8080/api").toString().trim();
  }
  function wcGetAuthMode(){
    return (window.WC_AUTH_MODE || "cookie").toString().trim(); // "cookie" | "jwt"
  }
  function wcGetTokenKey(){
    return (window.WC_AUTH_TOKEN_KEY || "wc_token").toString().trim();
  }

  window.wcAuthFindForm = function(fromEl){
    if(fromEl && fromEl.closest){
      var f = fromEl.closest("form");
      if(f) return f;
    }
    return document.querySelector("form");
  };

  window.wcAuthCollect = function(form){
    var out = {};
    if(!form) return out;
    var els = form.querySelectorAll("[name]");
    els.forEach(function(el){
      var name = el.getAttribute("name");
      if(!name) return;

      if(el.type === "checkbox") out[name] = !!el.checked;
      else if(el.type === "radio"){ if(el.checked) out[name] = (el.value ?? "").toString(); }
      else out[name] = (el.value ?? "").toString();
    });
    return out;
  };

  window.wcAuthGetToken = function(){
    var key = wcGetTokenKey();
    return sessionStorage.getItem(key) || localStorage.getItem(key) || "";
  };

  window.wcAuthSetToken = function(token){
    var key = wcGetTokenKey();
    sessionStorage.setItem(key, token ?? "");
  };

  window.wcAuthRequest = async function(path, opt){
    opt = opt || {};
    var method = opt.method || "GET";
    var body = opt.body || null;

    var base = wcGetApiBase();
    var mode = wcGetAuthMode();

    var headers = { "Content-Type": "application/json" };

    if(mode === "jwt"){
      var t = window.wcAuthGetToken();
      if(t) headers["Authorization"] = "Bearer " + t;
    }

    var res = await fetch(base + path, {
      method: method,
      headers: headers,
      body: body ? JSON.stringify(body) : null,
      credentials: "include"
    });

    var text = await res.text();
    var data = null;
    try { data = text ? JSON.parse(text) : null; } catch(e){ data = text; }

    if(!res.ok){
      throw new Error((data && data.message) ? data.message : ("HTTP " + res.status));
    }
    return data;
  };
})();`;

// ✅ Logic(Value) 블록 런타임 유틸 
const VALUE_RUNTIME_JS = `(function(){
  if(window.__WC_VALUE_RUNTIME__) return;
  window.__WC_VALUE_RUNTIME__ = true;

  window.wcFormValue = function(field){
    try{
      field = String(field || "").trim();
      if(!field) return "";

      var btn = window.__WC_LAST_EVENT_TARGET__ || null;

      // 1) auth runtime 있으면 우선 사용
      var form = null;
      if(window.wcAuthFindForm) form = window.wcAuthFindForm(btn);

      // 2) fallback: closest('form')
      if(!form && btn && btn.closest) form = btn.closest("form");

      // 3) 그래도 없으면 document에서 첫 form
      if(!form) form = document.querySelector("form");
      if(!form) return "";

      var el = form.querySelector('[name="' + field.replace(/"/g, '\\"') + '"]');
      if(!el) return "";

      if(typeof el.value !== "undefined" && el.value !== null) return String(el.value);
      if(typeof el.checked !== "undefined") return el.checked ? "true" : "false";
      return "";
    }catch(e){
      return "";
    }
  };

  window.wcTextMatchesRegex = function(text, pattern, flags){
    try{
      var _t = String(text);
      var _p = String(pattern);
      var _f = String(flags || "");
      var re = new RegExp(_p, _f);
      return re.test(_t);
    }catch(e){
      return false;
    }
  };
})();`;

const updatePreview = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  // 헬퍼: CSS 태그만 추출
  const extractStyleTagsOnly = (raw) => {
    if (!raw) return '';
    const matches = raw.match(/<style[^>]*>[\s\S]*?<\/style>/gi);
    return matches ? matches.join('\n') : '';
  };

  // 1. 현재 워크스페이스의 XML 가져오기
  const currentXml = workspace
    ? Blockly.Xml.domToText(Blockly.Xml.workspaceToDom(workspace))
    : '';

  // ---------------------------------------------------------
  // 2. 코드 생성 (실행용 vs 보기용 분리)
  // ---------------------------------------------------------

  // (1) 구조 (HTML)
  const structureCode =
    activeMode.value === 'structure'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.structure);

  // (2) 스타일 (CSS)
  const styleCodeRaw =
    activeMode.value === 'style'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.style);

  // (3) 🔥 [수정] 실행용 로직 (Iframe용) - 항상 표준 제너레이터 사용 (안전성)
  const logicCodeForPreview =
    activeMode.value === 'logic'
      ? generateCodeFromXML(currentXml, javascriptGenerator)
      : generateCodeFromXML(page.workspaces.logic, javascriptGenerator);

  // (4) 🔥 [수정] 보기용 로직 (사용자 눈요기용) - 탭 상관없이 무조건 Pretty 사용!
  const genForView = Interaction.javascriptGeneratorPretty || javascriptGenerator;

  const logicCodeForView =
    activeMode.value === 'logic'
      ? generateCodeFromXML(currentXml, genForView) // 현재 작성 중이면 현재 것
      : generateCodeFromXML(page.workspaces.logic, genForView); // 아니면 저장된 것 (하지만 Pretty하게!)

  // ---------------------------------------------------------
  // 3. 결과물 조립 (코드 보기 탭용)
  // ---------------------------------------------------------
  const viewScript = logicCodeForView.trim() ? `${logicCodeForView}` : '';
  const viewHtml = cleanCodeForView(structureCode);
  const viewStyle = styleCodeRaw.trim() ? `${styleCodeRaw}` : '';

  generatedCode.value = [viewScript, viewHtml, viewStyle]
    .filter(Boolean)
    .join('\n\n');

  // ---------------------------------------------------------
  // 4. Iframe 프리뷰 생성 (실행용 코드 주입)
  // ---------------------------------------------------------
  const styleCodeForPreview = extractStyleTagsOnly(styleCodeRaw);

  const safeScript =
    logicCodeForPreview.trim() && !logicCodeForPreview.includes('<script')
      ? `<script>${logicCodeForPreview}<\/script>`
      : logicCodeForPreview;

  const authRuntimeScript = isRunning.value
    ? `<script>${AUTH_RUNTIME_JS}<\/script>`
    : '';

  const valueRuntimeScript = isRunning.value
    ? `<script>${VALUE_RUNTIME_JS}<\/script>`
    : '';

  const finalLogicScript = isRunning.value ? safeScript : '';

  const positionsJSON = JSON.stringify(getPositionsMap());
  const PAGE_ID = page.id;
  const PAGE_ROUTE = page.route;

  const deviceClass = isPhone.value ? 'is-mobile-mode' : 'is-pc-mode';
  const orientationClass =
    isPhone.value && isLandscape.value ? 'is-landscape' : '';
  const finalBodyClass = `${isRunning.value ? 'is-running' : 'is-design'} ${deviceClass} ${orientationClass}`;
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
    `<body class="${finalBodyClass}">`,
    '<div id="wrapper">',
    structureCode,
    '<div id="wcGuideV" class="wc-guide-line wc-guide-v"></div><div id="wcGuideH" class="wc-guide-line wc-guide-h"></div></div>',
    authRuntimeScript,
    valueRuntimeScript,
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
  Component.defineBlocks();
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
  component: Component.toolbox,
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

    if (
      toolbox &&
      toolbox.getToolboxItems &&
      toolbox.getToolboxItems().length > 0
    ) {
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
                { transform: 'translate(0, 0)', opacity: 1 },
              ],
              {
                duration: 300,
                easing: 'ease',
                fill: 'forwards',
                composite: 'add',
              }
            );
          }
        });
      }, 100);
    }
  } catch (e) {
    console.error('setToolbox 오류:', e);
    // 에러가 나도 우리 변수가 살아있으면 초기화 시도
    if (currentWorkspace) {
      currentWorkspace.updateToolbox('<xml></xml>');
    }
  }
};

// ✅ [수정] 데이터 오염 방지용 클린 저장 함수
const saveCurrentWorkspaceToPage = () => {
  if (!workspace || !selectedPageId.value) return;

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  // 방어 코드: workspaces 객체가 없으면 생성
  if (!page.workspaces) {
    page.workspaces = {
      structure: '<xml></xml>',
      style: '<xml></xml>',
      logic: '<xml></xml>',
    };
  }

  // 🔥 [중요] 여기서 block.data를 건드리는 코드가 절대 있으면 안 됩니다!
  // 오직 현재 워크스페이스 상태를 XML로 변환하여 저장만 합니다.

  const dom = Blockly.Xml.workspaceToDom(workspace);
  const xmlText = Blockly.Xml.domToText(dom);

  page.workspaces[activeMode.value] = xmlText;

  // 로컬 스토리지에 최종 반영
  savePagesToStorage();
};

const loadPageById = (pageId) => {
  if (!workspace) return;
  const page = pages.value.find((p) => p.id === pageId);
  if (!page) return;

  // 🔒 [잠금] 불러오는 동안 자동 저장 방지 [cite: 2026-01-21]
  isRestoring = true;

  selectedPageId.value = page.id;
  workspace.clear();

  // 🔥 [핵심 수정] DB에서 받아온 데이터와 로컬 메모리 데이터를 통합해서 선택 [cite: 2026-01-21]
  // 현재 모드(structure, style, logic)에 맞는 데이터를 가져옵니다.
  let rawData = "";
  if (activeMode.value === 'structure') {
    rawData = page.layoutData || (page.workspaces && page.workspaces.structure);
  } else if (activeMode.value === 'style') {
    rawData = page.styleData || (page.workspaces && page.workspaces.style);
  } else if (activeMode.value === 'logic') {
    rawData = page.logicData || (page.workspaces && page.workspaces.logic);
  }

  // 🔹 데이터 복원 (앞서 만든 JSON/XML 호환 로직 적용) [cite: 2026-01-21]
  if (rawData && rawData !== '<xml></xml>' && rawData !== '{}') {
    try {
      if (typeof rawData === 'string' && rawData.trim().startsWith('<xml')) {
        // XML 방식 복원 [cite: 2026-01-21]
        const dom = Blockly.utils.xml.textToDom(rawData);
        Blockly.Xml.domToWorkspace(dom, workspace);
      } else {
        // JSON 방식 복원 [cite: 2026-01-21]
        const state = typeof rawData === 'string' ? JSON.parse(rawData) : rawData;
        Blockly.serialization.workspaces.load(state, workspace);
      }
    } catch (e) {
      console.error("데이터 복원 중 오류:", e);
    }
  }

  // 🔓 [잠금 해제] 복구 완료 후 0.5초 뒤에 저장 기능 재활성화 [cite: 2026-01-21]
setTimeout(() => {
    isRestoring = false; // 👈 반드시 setTimeout '안쪽'에서 풀어야 함
    console.log("🔓 [시크릿 모드 전용] 렌더링 완료 - 잠금 해제");
  }, 800); // 시간을 0.8초로 넉넉하게 늘립니다 [cite: 2026-01-21]

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

  // 1. 현재 탭의 블록 위치와 내용을 저장
  saveCurrentWorkspaceToPage();

  activeMode.value = modeId;
  activeParent.value = modeId;
  activeTab.value = null;

  if (!workspace) return;

  // 2. 워크스페이스 비우기
  workspace.clear();

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  const xml = page?.workspaces?.[modeId];

  // 3. 저장된 XML에 좌표 정보가 있으므로, domToWorkspace가 그 자리에 그대로 그려줍니다.
  if (xml && xml !== '<xml></xml>') {
    try {
      const dom = Blockly.utils.xml.textToDom(xml);
      Blockly.Xml.domToWorkspace(dom, workspace);
    } catch (e) {
      console.error('탭 전환 중 로드 실패:', e);
    }
  }

  // 툴박스 갱신
  setToolbox(toolboxXMLs.empty);
  const group = categoryGroups.find((g) => g.id === modeId);
  if (group && group.items.length > 0) {
    selectCategory(group.items[0]);
  }

  refreshCodeAndPreview();
};
// [상수 추가] 스크립트 맨 위에 추가해두세요
const FLYOUT_WIDTH = 300;

/**
 * 카테고리 선택 및 블록 목록(Flyout) 표시 함수
 * @param {string} key - 선택된 카테고리 키 (layout, style, data 등)
 */
const selectCategory = (key) => {
  if (!workspace) return;

  // 이미 열린 탭을 다시 누르면 닫기만 수행
  if (activeTab.value === key) {
    activeTab.value = null;
    workspace.getFlyout().hide();
    setTimeout(() => Blockly.svgResize(workspace), 310);
    return;
  }

  // 1. 활성 탭 상태만 변경 (워크스페이스 클리어 금지!)
  activeTab.value = key;

  // 2. Flyout(메뉴판)에 보여줄 블록 목록만 갱신
  const xmlText = toolboxXMLs[key] || '<xml></xml>';
  const dom = Blockly.utils.xml.textToDom(xmlText);
  const flyout = workspace.getFlyout();

  if (flyout) {
    flyout.show(Array.from(dom.children));
    flyout.scrollToStart();
  }

  // 3. 사이드바가 열리는 애니메이션 시간에 맞춰 너비만 재계산
  setTimeout(() => {
    if (workspace) {
      Blockly.svgResize(workspace);
    }
  }, 350);
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
  if (blocklyDiv)
    blocklyDiv.style.backgroundColor = selectedTheme.workspaceColor;

  // 2. 다른 설정 적용 (payload.settings 사용)
  // 예: 그리드 설정, 프로젝트 이름 변경 등
  console.log('다른 설정들:', payload.settings);
  // 예: if (payload.settings.showGrid !== workspace.getGrid().isVisible()) ...

  if (payload.settings && payload.settings.projectName) {
    projectTitle.value = payload.settings.projectName; // 👈 이 코드가 있어야 상단 제목이 바뀝니다.
    savePagesToStorage();
  }

  // 3. 저장 및 닫기
  localStorage.setItem('wc_theme_settings', JSON.stringify(currentTheme));
  isThemeModalOpen.value = false;
};
let isRestoring = false;

  const initProjectData = async () => {
    // ✅ [핵심] 서버/로컬 어떤 데이터가 오든 page.workspaces 3종은 무조건 보장
    const normalizePage = (p = {}) => ({
      id: p.id,
      name: p.name || p.pageName || 'Home',
      route: p.route || '/home',
      aliases: Array.isArray(p.aliases) ? p.aliases : [],
      status: p.status || 'DRAFT',
      workspaces: p.workspaces || {
        structure: '<xml></xml>',
        style: '<xml></xml>',
        logic: '<xml></xml>',
      },
    });

    try {
  // 1. 현재 선택된 ID(selectedPageId)를 사용해 전체 페이지(pages)에서 해당 객체를 찾습니다. [cite: 2026-01-21]
  const currentPage = pages.value.find(p => p.id === selectedPageId.value);
  // 2. 찾은 페이지가 있으면 그 이름을 쓰고, 없으면 안전하게 'index'를 기본값으로 씁니다. [cite: 2026-01-21]
  const targetPageName = currentPage ? currentPage.name : 'index'; 
  // 3. 서버에 정확한 pageName 파라미터를 실어서 보냅니다. [cite: 2026-01-21]
  const response = await api.get(`/projects/${props.webId}/data?pageName=${encodeURIComponent(targetPageName)}`);

      // ✅ 1) 서버 데이터가 정상적으로 존재할 때
      if (response?.data && response.data.title) {
        const loaded = response.data;

        // 프로젝트명(상단 타이틀)
        projectTitle.value = loaded.title;

        // ✅ 서버가 pages 배열을 주는 경우 (멀티 페이지)
        if (Array.isArray(loaded.pages) && loaded.pages.length > 0) {
          pages.value = loaded.pages.map(normalizePage);
        }
        // ✅ 서버가 단일 페이지 구조만 주는 경우
        else {
          pages.value = [
            normalizePage({
              id: loaded.id || props.webId, // 서버 id 있으면 우선, 없으면 webId
              name: loaded.pageName || loaded.name || 'Home',
              route: loaded.route || '/home',
              aliases: loaded.aliases,
              status: loaded.status,
              workspaces: loaded.workspaces, // 이게 없을 수 있으니 normalize가 채워줌
            }),
          ];
        }

        // ✅ 서버 성공 시: 로컬도 덮어써서 예전 로컬 데이터를 밀어버림
        savePagesToStorage();

        // ✅ 첫 페이지 로드
        if (pages.value[0]?.id) {
          await loadPageById(pages.value[0].id);
        }
        return; // 🚀 중요: 서버 성공이면 여기서 끝 (아래 로컬 복구 로직 실행 금지)
      }
    } catch (e) {
      console.error('서버 데이터 로드 실패, 로컬 데이터를 시도합니다.', e);
    }

    // ✅ 2) 서버 로드 실패 시에만: 로컬스토리지 복구
    const stored = loadPagesFromStorage();
    if (stored) {
      // 저장 구조가 {settings, pages} 일 수도 있고 pages 배열일 수도 있어서 둘 다 처리
      const storedPages = Array.isArray(stored) ? stored : stored.pages;

      if (Array.isArray(storedPages) && storedPages.length > 0) {
        pages.value = storedPages.map(normalizePage);

        // 프로젝트명 복원 (settings 우선)
        if (!projectTitle.value) {
          // loadPagesFromStorage에서 projectTitle.value를 세팅해줬다면 그대로 두고,
          // 없다면 pages[0] 이름이라도 쓰기
          projectTitle.value = pages.value[0]?.name || 'Untitled Project';
        }

        if (pages.value[0]?.id) {
          await loadPageById(pages.value[0].id);
        }
      }
    }
  };

onMounted(async () => {
  
  // 0. 한국어 설정
  if (Ko) Blockly.setLocale(Ko);

  // 1. 블록 정의
  defineCustomBlocks();
  patchPrettyGenerator();
  await nextTick();

  // ============================================================
  // ✨ Blockly 주입
  // ============================================================
  workspace = Blockly.inject('blocklyDiv', {
    renderer: 'zelos',
    toolbox: toolboxXMLs.empty,
    move: { scrollbars: true, drag: true, wheel: true },
    zoom: {
      controls: true,
      wheel: false,
      startScale: 0.8,
    },
    grid: { spacing: 20, length: 3, colour: '#ccc', snap: true },
    trashcan: true,
  });

  // 2. 테마 및 배경색 적용
  let savedTheme = currentTheme;
  try {
    const loaded = localStorage.getItem('wc_theme_settings');
    if (loaded) {
      savedTheme = JSON.parse(loaded);
      Object.assign(currentTheme, savedTheme);
    }
  } catch (e) {}

  const flyoutBg = document.querySelector('.flyout-bg-panel');
  if (flyoutBg) flyoutBg.style.backgroundColor = savedTheme.toolboxColor;
  const wsBg = document.querySelector('.blocklyMainBackground');
  if (wsBg) wsBg.style.fill = savedTheme.workspaceColor;
  const blocklyDiv = document.getElementById('blocklyDiv');
  if (blocklyDiv) blocklyDiv.style.backgroundColor = savedTheme.workspaceColor;
  const loadedPages = loadPagesFromStorage(); 
  
  if (loadedPages && loadedPages.length > 0) {
    pages.value = loadedPages;
    selectedPageId.value = loadedPages[0].id; // 첫 페이지 선택
  } else {
    return;
  }

  // 2. 블록 화면 그리기 (중요!)
  // Blockly 주입(inject)이 완료된 후 실행해야 함
  setTimeout(() => {
     loadWorkspaceState(selectedPageId.value);
  }, 100);
  // UI 밀림 방지
  const metricsManager = workspace.getMetricsManager();
  metricsManager.getToolboxMetrics = () => ({
    width: 0,
    height: 0,
    position: Blockly.TOOLBOX_AT_LEFT,
  });
  metricsManager.getFlyoutMetrics = () => ({
    width: 0,
    height: 0,
    position: Blockly.TOOLBOX_AT_LEFT,
  });

  const flyout = workspace.getFlyout();
  if (flyout) flyout.autoClose = false;
  workspace.resize();

  // VS Code 스타일 줌 (Ctrl + Wheel)
  blocklyDiv.addEventListener(
    'wheel',
    (e) => {
      if (e.ctrlKey) {
        e.preventDefault();
        const direction = e.deltaY > 0 ? -1 : 1;
        workspace.zoom(e.offsetX, e.offsetY, direction);
      }
    },
    { passive: false }
  );

  // ============================================================
  // 3. Blockly 이벤트 리스너 (여기에 위치 로직 통합됨!)
  // ============================================================
  // 5. Blockly 이벤트 리스너 (통합 및 최적화 버전)
  let debounceTimer = null;
  workspace.addChangeListener((e) => {
    
    // 1. 로딩 중이거나, 단순 UI 이벤트(클릭 등)는 무시
    if (isRestoring || e.isUiEvent) return;

    // 2. 우리가 관심을 가질 이벤트들 (생성, 삭제, 변경, 이동)
    if (
      e.type === Blockly.Events.BLOCK_CREATE ||
      e.type === Blockly.Events.BLOCK_DELETE ||
      e.type === Blockly.Events.BLOCK_CHANGE ||
      e.type === Blockly.Events.BLOCK_MOVE
    ) {
      
      // 3. 디바운스 적용 (0.3초 대기)
      // 설명: 블록을 드래그하는 동안에는 저장/갱신을 하지 않고, 
      // 손을 놓거나 동작이 멈추면 그때 한 번만 실행합니다. (성능 최적화 + 블록 사라짐 방지)
      if (debounceTimer) clearTimeout(debounceTimer);
      
      debounceTimer = setTimeout(() => {
        // (1) 객체 목록 갱신
        updateObjectListFromWorkspace();
        
        // (2) 저장 실행
        saveCurrentWorkspaceToPage();
        
        // (3) 코드 생성 및 미리보기 갱신 (이제 이동(Move)할 때도 실행됨!)
        refreshCodeAndPreview();
        
        console.log(`📝 상태 업데이트 완료 (${e.type})`);
      }, 300); // 300ms 딜레이
    }

    // 4. 선택 이벤트 (기존 유지)
    if (e.type === Blockly.Events.SELECTED) {
      if (!isSelectingProgrammatically) {
        handleSelection(e.newElementId, 'blockly');
      }
    }
  });

  // 4. Iframe 통신 리스너
  window.addEventListener('message', (event) => {
    const data = event.data;
    if (!data) return;

    // 🚀 [핵심] iframe 위치 이동 시: XML 데이터를 직접 수정해서 저장
    if (data.type === 'update_free_position') {
      const { blockId, x, y } = data;
      const page = pages.value.find((p) => p.id === selectedPageId.value);

      if (page && page.workspaces && page.workspaces.structure) {
        try {
          // 1. 저장된 XML을 파싱 (DOM으로 변환)
          const parser = new DOMParser();
          const xmlDoc = parser.parseFromString(
            page.workspaces.structure,
            'text/xml'
          );

          // 2. 해당 블록 ID를 가진 태그 찾기
          const targetBlock = xmlDoc.querySelector(`block[id="${blockId}"]`);

          if (targetBlock) {
            // 3. data 속성에 좌표값 강제 주입
            const newPos = JSON.stringify({ x: Number(x), y: Number(y) });
            targetBlock.setAttribute('data', newPos);

            // 4. XML 문자열로 다시 변환하여 저장
            const serializer = new XMLSerializer();
            page.workspaces.structure = serializer.serializeToString(xmlDoc);

            // 5. 로컬 스토리지 저장
            savePagesToStorage();

            console.log(`📍 XML 직접 업데이트 완료: ${blockId} -> ${newPos}`);

            // 6. (옵션) 만약 현재 화면 구성 탭이라면, 라이브 블록에도 반영 (UI 싱크용)
            if (workspace && activeMode.value === 'structure') {
              const liveBlock = workspace.getBlockById(blockId);
              if (liveBlock) liveBlock.data = newPos;
            }
          }
        } catch (e) {
          console.error('XML 직접 수정 실패:', e);
        }
      }
    }

    // [기존 코드 유지] 페이지 이동
    if (
      data.type === 'NAVIGATE' ||
      data.type === 'REDIRECT' ||
      data.type === 'change_page_request'
    ) {
      const targetId = data.pageId;
      const targetPage = pages.value.find(
        (p) => p.id === targetId || p.route === targetId || p.name === targetId
      );
      if (targetPage) {
        lockPage(targetPage.id);
        selectPage(targetPage.id);
      } else {
        alert('이동할 페이지를 찾을 수 없습니다: ' + targetId);
      }
    }

    // [기존 코드 유지] 선택 하이라이트
    if (data.type === 'select_block') handleSelection(data.blockId, 'iframe');
    if (data.type === 'deselect_block') handleSelection(null, 'iframe');
  });

  // 5. 전역 함수 및 데이터 로드
  window.WC_GET_PAGES = () => {
    if (!pages.value || pages.value.length === 0) return [['페이지 없음', '']];
    // 🔥 [수정] 모든 값을 문자열로 변환 (Blockly 드롭다운 요구사항)
    return pages.value.map((p) => [p.name, String(p.id)]);
  };

  // IDEView.vue 내 수정

  // 1. 프로젝트 제목을 별도로 관리할 변수 선언 (이미 있다면 확인)
  // IDEView.vue 내 initProjectData 함수 수정


// 1️⃣ [잠금] 서버 데이터를 다 그릴 때까지 '자동 저장'을 원천 봉쇄합니다. [cite: 2026-01-21]
isRestoring = true; 

  // 2️⃣ 서버 데이터 로드 시도
  console.log("📡 서버 데이터 로딩 시작...");
  await initProjectData();
  
  // 🚀 [핵심] Vue가 pages 변수를 갱신할 때까지 대기 [cite: 2026-01-21]
  await nextTick(); 

  if (pages.value && pages.value.length > 0) {
    // 3️⃣ [지연 렌더링] 워크스페이스가 준비될 시간을 0.8초 줍니다.
    setTimeout(async () => {
      if (workspace) {
        // 📏 크기 재조정 후 블록 주입 [cite: 2026-01-21]
        workspace.resize();
        Blockly.svgResize(workspace);
        
        const firstPageId = pages.value[0].id;
        loadWorkspaceState(firstPageId);
        console.log("🎨 [성공] 화면에 블록 주입 완료");
      }

      // 4️⃣ [잠금 해제] 화면에 모든 블록이 뜬 '후'에만 저장을 허용 [cite: 2026-01-21]
      setTimeout(() => {
        isRestoring = false;
        console.log("🔓 이제부터 안전하게 저장이 가능합니다.");
      }, 1000); // 넉넉하게 1초 뒤 잠금 해제
    }, 800);
  } else {
    // 신규 프로젝트 로직 [cite: 2026-01-21]
    await setupInitialPages();
    pages.value = [createPage('Home'), createPage('Login')];
    savePagesToStorage();
    isRestoring = false;
  }

  // 6. 리사이즈 감지
  new ResizeObserver(() => {
    if (workspace) Blockly.svgResize(workspace);
  }).observe(document.getElementById('workspace-area'));

  const iframeResizeObserver = new ResizeObserver((entries) => {
    for (const entry of entries) {
      wrapperWidth.value = entry.contentRect.width;
      wrapperHeight.value = entry.contentRect.height;
    }
  });
  const iframeWrapper = document.querySelector('.iframe-wrapper');
  if (iframeWrapper) iframeResizeObserver.observe(iframeWrapper);

  // 7. ESC 키 종료
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isRunning.value) toggleRun();
  });
  autoSaveTimer = setInterval(
    () => {
      console.log('🕒 자동 저장 실행 중...');
      saveToServerAsJson();
    },
    10 * 60 * 1000
  );
});
onUnmounted(() => {
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer); // 페이지 나갈 때 타이머 해제
  }
});
// PC 모드일 때는 강제로 넓게 잡고 축소해서 보여줌
const iframeStyle = computed(() => {
  if (isPhone.value) {
    return {
      width: '100%',
      height: '100%',
      transform: 'none',
      border: 'none',
    };
  } else {
    const baseWidth = 1920;
    const baseHeight = 1130; // 기본 FHD 높이

    // 1. 박스 크기 가져오기
    const currentWidth = 651;
    const currentHeight = 800;

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
      boxShadow: '0 0 30px rgba(0,0,0,0.1)', // (선택) 그림자 좀 더 진하게
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
  swinging: `@keyframes swinging {0% { transform: rotate(0deg); transform-origin: top center; } 20% { transform: rotate(15deg); }40% { transform: rotate(-10deg); }60% { transform: rotate(5deg); }80% { transform: rotate(-5deg); }100% { transform: rotate(0deg); }}`,
};
// 💾 [배포] 전체 프로젝트를 ZIP으로 다운로드 (CSS 오류 수정 + 좌표 강제 적용)
const downloadProject = async () => {
  const zip = new JSZip();

  // 1. 페이지 ID와 파일명 매핑
  const pageMap = {};
  pages.value.forEach((p, index) => {
    const filename = index === 0 ? 'index.html' : `${p.name.trim()}.html`;
    pageMap[p.id] = filename;
  });

  // 2. 모든 페이지 순회
  for (const page of pages.value) {
    const filename = pageMap[page.id];

    // (1) XML에서 좌표 정보(x, y) 추출
    const coordsMap = {};
    if (page.workspaces.structure) {
      try {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(
          page.workspaces.structure,
          'text/xml'
        );
        const blocks = xmlDoc.querySelectorAll('block');
        blocks.forEach((block) => {
          const id = block.getAttribute('id');
          const dataStr = block.getAttribute('data');

          if (id && dataStr) {
            try {
              const pos = JSON.parse(dataStr);
              // 좌표가 유효한지 확인
              if (
                pos &&
                typeof pos.x === 'number' &&
                typeof pos.y === 'number'
              ) {
                coordsMap[id] = pos;
              }
            } catch (e) {}
          }
        });
      } catch (e) {
        console.error('좌표 파싱 오류:', e);
      }
    }

    // (2) 코드 생성
    const structCode = generateCodeFromXML(page.workspaces.structure);
    const styleCode = generateCodeFromXML(page.workspaces.style);
    const logicCode = generateCodeFromXML(page.workspaces.logic);

    // 🔥 [핵심 수정 1] CSS 태그 중복 제거 (styleCode 안에 <style>태그가 포함되어 있으면 제거)
    // 블록이 "<style>...</style>"을 반환하더라도, 여기서 태그를 벗겨내서 순수 CSS만 남깁니다.
    const cleanStyleCode = styleCode.replace(/<\/?style[^>]*>/g, '').trim();

    // (3) 애니메이션 Tree Shaking
    const fullSourceCode = structCode + styleCode + logicCode;
    let usedKeyframes = '';
    Object.keys(ANIMATION_LIBRARY).forEach((name) => {
      if (fullSourceCode.includes(name)) {
        usedKeyframes += ANIMATION_LIBRARY[name] + '\n';
      }
    });

    // (4) HTML 세탁 및 좌표 주입
    const cleanContainer = document.createElement('div');
    cleanContainer.innerHTML = structCode;

    cleanContainer.querySelectorAll('*').forEach((el) => {
      const blockId = el.getAttribute('data-block-id');

      // 🔥 [핵심 수정 2] 좌표 적용 로직
      if (blockId && coordsMap[blockId]) {
        const { x, y } = coordsMap[blockId];
        // 기존 스타일 유지하며 좌표 추가
        el.style.position = 'absolute';
        el.style.left = `${x}px`;
        el.style.top = `${y}px`;
        // 배포 버전에서는 transform 제거 (드래그 잔재 방지)
        el.style.transform = 'none';
      }

      // 불필요한 속성 제거
      const dirtyAttributes = [
        'data-block-id',
        'data-draggable',
        'data-wc-block',
        'data-wc-style',
        'contenteditable',
        'spellcheck',
        'data-x',
        'data-y',
      ];

      dirtyAttributes.forEach((attr) => el.removeAttribute(attr));
      el.classList.remove('wc-highlight', 'wc-dragging', 'selected');

      // 클래스가 비어있으면 속성 삭제
      if (el.classList.length === 0) el.removeAttribute('class');
    });

    const cleanHtmlBody = cleanContainer.innerHTML;

    // (5) 최종 HTML 조립
    const htmlContent = `
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${page.name}</title>
  <style>
    /* 기본 리셋 */
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
    body { 
      background-color: #fff; 
      overflow-x: hidden; 
      position: relative; 
    }
    * { box-sizing: border-box; }
    
    #root {
      position: relative;
      width: 100%;
      min-height: 100vh;
      overflow: hidden;
    }

    /* 🔥 사용자 정의 스타일 (태그 없이 내용만 삽입됨) */
    ${cleanStyleCode}

    /* 애니메이션 키프레임 */
    ${usedKeyframes}
  </style>
  </head>
  <body>
    <div id="root">
      ${cleanHtmlBody}
    </div>

    <script>
      const PAGE_MAP = ${JSON.stringify(pageMap)};
      
      function navigateToPage(targetId) {
        if (PAGE_MAP[targetId]) {
          window.location.href = PAGE_MAP[targetId];
        } else {
          console.error('이동할 페이지를 찾을 수 없습니다:', targetId);
        }
      }
      
      function redirectToPage(targetId) { navigateToPage(targetId); }
      function goToPage(targetId) { navigateToPage(targetId); }

      ${logicCode}
    <\/script>
  </body>
  </html>`.trim();

    zip.file(filename, htmlContent);
  }

  // 3. ZIP 생성 및 다운로드 실행
  const content = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(content);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'WebCrafter_Project.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
};
// 상태 관리
const isXmlModalOpen = ref(false);
const manualXmlInput = ref('');

// [핵심] 모달에서 입력한 XML을 블록으로 변환
const applyManualXml = () => {
  if (!manualXmlInput.value.trim()) {
    alert('XML 코드를 입력해주세요.');
    return;
  }

  // 기존에 만들어둔 AI 블록 생성 함수를 그대로 사용합니다.
  // 두 번째 인자가 true이면 교체, false이면 기존 블록 아래에 추가됩니다.
  handleAiBlockGeneration(manualXmlInput.value, false);

  // 완료 후 초기화 및 닫기
  manualXmlInput.value = '';
  isXmlModalOpen.value = false;
};
</script>

<template>
  <div class="ide-container">
    <aside
      :class="[
        isPhone ? 'phone-size' : 'pc-size',
        { 'is-landscape': isPhone && isLandscape },
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
              @click="saveToServerAsJson"
              :disabled="isSaving"
              :class="[
                'px-4 py-2 rounded font-bold text-white transition-colors flex items-center gap-2 btn-save',
                isSaving
                  ? 'bg-gray-400 cursor-not-allowed'
                  : 'bg-blue-600 hover:bg-blue-700',
                isPhone ? 'phone-hide' : '',
              ]"
            >
              <template v-if="isSaving">
                <svg
                  class="animate-spin h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    class="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    stroke-width="4"
                  ></circle>
                  <path
                    class="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                <span>저장 중...</span>
              </template>

              <template v-else>
                <span class="text-xl">💾</span> <span>저장</span>
              </template>
            </button>
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
              <span
                :style="{
                  display: 'inline-block',
                  transition: '0.3s',
                  transform: isLandscape ? 'rotate(90deg)' : 'rotate(0deg)',
                }"
                >🔄</span
              >
            </button>

            <button
              class="btn-deploy"
              :class="isPhone ? 'phone-hide' : ''"
              @click="downloadProject"
            >
              🚀 다운 (ZIP)
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

              <button 
                class="btn-del" 
                @click.stop.prevent="deletePage(page.id)" 
                style="cursor: pointer; position: relative; z-index: 10; pointer-events: auto !important; padding: 5px;"
              >
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
        <div class="header-actions">
          <button class="ghost-btn" @click="isXmlModalOpen = true">
            <i class="icon-code"></i> XML 직접 입력
          </button>

          <div
            v-if="isXmlModalOpen"
            class="xml-modal-overlay"
            @click.self="isXmlModalOpen = false"
          >
            <div class="xml-modal-content">
              <h3>Blockly XML 붙여넣기</h3>
              <p>생성된 XML 코드를 아래에 붙여넣으세요.</p>

              <textarea
                v-model="manualXmlInput"
                placeholder="<xml>...</xml> 코드를 입력하세요"
                class="xml-textarea"
              ></textarea>

              <div class="modal-actions">
                <button class="btn-secondary" @click="isXmlModalOpen = false">
                  취소
                </button>
                <button class="btn-primary" @click="applyManualXml">
                  블록 생성
                </button>
              </div>
            </div>
          </div>
        </div>
        <button class="mr-[42px]" @click="isThemeModalOpen = true">
          <Settings :size="23" />
        </button>
        <Teleport to="body">
          <ThemeSettingsModal
            v-if="isThemeModalOpen"
            :open="isThemeModalOpen"
            :project="{
              id: props.webId,
              title: projectTitle,
            }"
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

    <GlobalModal
      :open="modal.open"
      :message="modal.message"
      :type="modal.type"
      @confirm="closeModal"
    />
  </div>

  <Teleport to="body">
    <AiChatBot 
      :workspaces="pages.find(p => p.id === selectedPageId)?.workspaces" 
      @generate="handleAiBlockGeneration" 
    />
  </Teleport>

<!-- ✅ 삭제 확인 모달 -->
<ConfirmModal
  :open="confirmModal.open"
  :message="confirmModal.message"
  type="warning" 
  confirm-text="삭제"
  cancel-text="취소"
  @confirm="confirmDeletePage"
  @cancel="closeDeleteConfirm"
/>

  <Teleport to="body">
    <div v-if="isRunning" class="fullscreen-modal">
      <div class="modal-header">
        <div class="header-left">
          <span class="preview-badge">LIVE PREVIEW</span>
          <span class="page-info">{{ currentPageUrl }}</span>
        </div>

        <button class="btn-close" @click="toggleRun">✕ 종료 (Esc)</button>
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
.btn-deploy,
.btn-save {
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
  flex-shrink: 0 !important; /* 👈 부모가 좁아도 버튼이 안 찌그러짐 */
  height: 32px !important; /* 높이를 통일해서 예쁘게 정렬 */
  line-height: 1 !important; /* 글자 수직 중앙 정렬 보정 */
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
  fill: transparent !important; /* 색상 투명 */
  fill-opacity: 0 !important; /* 불투명도 0 */
  stroke: none !important; /* 테두리 없음 */
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
  background-color: #dcdcdcba; /* 원하는 배경색 (예: 흰색) */
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
  box-shadow: 0 15px 45px rgba(0, 0, 0, 0.4);
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
  overflow: hidden; /* 넘치는 것 자르기 */
  background-color: #fff;
  position: relative; /* 자식(iframe)의 기준점 */
  display: block; /* 🔥 Flex 삭제! 그냥 블록으로! */
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
/* 모달 배경 (어둡게) */
.xml-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

/* 모달 본체 */
.xml-modal-overlay .xml-modal-content {
  background: #252526; /* IDE 어두운 배경색 */
  padding: 24px;
  border-radius: 12px;
  width: 600px;
  max-width: 90%;
  color: #fff;
  opacity: 1 !important;
}
.header-actions {
  flex-grow: 1;
  text-align: right;
  z-index: 1001;
}
.ghost-btn {
  opacity: 0.01;
}
/* 텍스트 입력창 */
.xml-textarea {
  width: 100%;
  height: 300px;
  background: #1e1e1e;
  color: #d4d4d4;
  font-family: 'Consolas', monospace;
  padding: 12px;
  border: 1px solid #3c3c3c;
  border-radius: 4px;
  margin: 16px 0;
  resize: none;
}

/* 버튼들 */
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@keyframes popIn {
  from { opacity: 0; transform: scale(0.9) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>