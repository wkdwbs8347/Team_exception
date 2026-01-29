<script setup>
/**

 * ============================================================

 * ✅ Web Crafter IDE (Final Fixed Version)

 * - 순환 참조 오류 해결 (pages 초기화 순서 변경)

 * - URL 중복 방지 로직 적용

 * ============================================================

 */
import JSZip from 'jszip';
import {
  ref,
  onMounted,
  nextTick,
  watch,
  computed,
  reactive,
  onUnmounted,
} from 'vue';

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
import { applyContentAttrsToHtml } from '@/utils/applyContentAttrsToHtml';
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
    default: '',
  },
  webId: {
    type: [String, Number],
    default: '',
  },
});

const wrapperWidth = ref(600);
const wrapperHeight = ref(800);
const isSaving = ref(false);
const isLoadFailed = ref(false);
let isRestoring = false;
let autoSaveTimer = null; // 타이머 ID 저장용
// ✅ [AI Fix] AI 블록을 종류별로 분류하여 각각의 탭(데이터)에 분산 저장하는 함수
const handleAiBlockGeneration = (xmlText, isEditMode = false) => {
  if (!workspace || !xmlText) return;

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  try {
    // 1. AI가 준 XML 파싱
    const dom = Blockly.utils.xml.textToDom(xmlText);
    const newBlocks = Array.from(dom.children).filter(
      (n) => n.tagName === 'BLOCK' || n.tagName === 'block'
    );

    // 2. 블록 분류 (바구니 준비)
    const buckets = {
      structure: [],
      style: [],
      logic: [],
    };

    // 3. 하나씩 검사해서 바구니에 담기
    newBlocks.forEach((block) => {
      const type = block.getAttribute('type') || '';
      if (
        type.startsWith('layout_') ||
        type.startsWith('content_') ||
        type.startsWith('form_') ||
        type.startsWith('component_')
      ) {
        buckets.structure.push(block);
      } else if (
        type.startsWith('style_') ||
        type.startsWith('color_') ||
        type.startsWith('flex_') ||
        type.startsWith('anim_')
      ) {
        buckets.style.push(block);
      } else {
        buckets.logic.push(block); // 나머지는 로직
      }
    });

    // 4. 각 바구니를 해당 탭(데이터)에 저장 처리
    ['structure', 'style', 'logic'].forEach((mode) => {
      const blockNodes = buckets[mode];
      if (blockNodes.length === 0) return; // 내용 없으면 패스

      // (A) 현재 보고 있는 탭이라면? -> 라이브 워크스페이스에 직접 추가
      if (activeMode.value === mode) {
        if (isEditMode) workspace.clear(); // 수정 모드면 초기화

        // 위치 계산 (기존 블록들 아래에)
        let startY = 50;
        workspace.getAllBlocks(false).forEach((b) => {
          const xy = b.getRelativeToSurfaceXY();
          const h = b.getHeightWidth().height;
          if (xy.y + h > startY) startY = xy.y + h + 50;
        });

        // 블록 주입
        const tempXml = document.createElement('xml');
        blockNodes.forEach((b, i) => {
          b.setAttribute('x', '50');
          b.setAttribute('y', String(startY + i * 200));
          tempXml.appendChild(b);
        });
        Blockly.Xml.domToWorkspace(tempXml, workspace);

        // 즉시 저장 (동기화)
        saveCurrentWorkspaceToPage();
      }

      // (B) 안 보이는 다른 탭이라면? -> 임시 워크스페이스 열어서 처리 (백그라운드 저장)
      else {
        const tempWs = new Blockly.Workspace(); // 1. 임시 작업대 생성
        const rawData = page.workspaces[mode];

        // 2. 기존 데이터 로드 (JSON/XML)
        try {
          if (rawData && rawData !== '<xml></xml>' && rawData !== '{}') {
            if (typeof rawData === 'string' && rawData.trim().startsWith('{')) {
              Blockly.serialization.workspaces.load(
                JSON.parse(rawData),
                tempWs
              );
            } else if (typeof rawData === 'string' && rawData.startsWith('<')) {
              const prevDom = Blockly.utils.xml.textToDom(rawData);
              Blockly.Xml.domToWorkspace(prevDom, tempWs);
            }
          }
        } catch (e) {}

        if (isEditMode) tempWs.clear();

        // 3. 위치 계산
        let startY = 50;
        tempWs.getAllBlocks(false).forEach((b) => {
          const xy = b.getRelativeToSurfaceXY();
          const h = b.getHeightWidth().height;
          if (xy.y + h > startY) startY = xy.y + h + 50;
        });

        // 4. 블록 주입
        const tempXml = document.createElement('xml');
        blockNodes.forEach((b, i) => {
          b.setAttribute('x', '50');
          b.setAttribute('y', String(startY + i * 200));
          tempXml.appendChild(b);
        });
        Blockly.Xml.domToWorkspace(tempXml, tempWs);

        // 5. 저장 (JSON으로 변환하여 DB 데이터 갱신)
        const state = Blockly.serialization.workspaces.save(tempWs);
        const jsonText = JSON.stringify(state);

        page.workspaces[mode] = jsonText;
        if (mode === 'structure') page.layoutData = jsonText;
        else if (mode === 'style') page.styleData = jsonText;
        else if (mode === 'logic') page.logicData = jsonText;

        tempWs.dispose(); // 6. 작업대 정리

        // 중요: 서버 저장 트리거
        saveToServerAsJson();
      }
    });

    console.log(
      `✅ AI 블록 분류 완료 (구조:${buckets.structure.length}, 스타일:${buckets.style.length}, 로직:${buckets.logic.length})`
    );
  } catch (e) {
    console.error('AI 분류 중 오류:', e);
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
const userInfo = ref(null); // 🚀 [추가] 사용자 정보를 담을 변수
const isAppLoading = ref(true); // 🚀 [추가] 초기 로딩 상태 관리

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

// ✅ 수정: 로드 실패 상태(isLoadFailed)면 아예 저장을 막아버림 (DB 보호)
// ✅ [Final Fix] 저장 시 현재 블록 위치를 'data' 속성에 강제 동기화 (위치 초기화 방지)
const saveToServerAsJson = async () => {
  if (isSaving.value) return;
  if (isLoadFailed.value) {
    console.warn('⛔ 데이터 로드에 실패한 상태이므로 저장을 차단합니다.');
    return;
  }

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  try {
    isSaving.value = true;

    const toSafeString = (val) => {
      if (!val) return '{}';
      if (typeof val === 'string') return val;
      return JSON.stringify(val);
    };

    // ✅ 여기서 workspace.save()로 page 데이터를 다시 만들지 말 것!
    const payload = {
      webId: props.webId,
      pageName: page.name,
      title: projectTitle.value || '',
      layoutData: toSafeString(page.layoutData || page.workspaces?.structure),
      styleData: toSafeString(page.styleData || page.workspaces?.style),
      logicData: toSafeString(page.logicData || page.workspaces?.logic),
    };

    const oldNameForQuery = page.oldName || page.name;

    await api.put(
      `/projects/${props.webId}/data?oldPageName=${encodeURIComponent(oldNameForQuery)}`,
      payload
    );

    page.oldName = page.name;
    console.log(`✅ [${page.name}] 저장 성공`);
  } catch (e) {
    console.error('❌ 저장 실패:', e);
  } finally {
    isSaving.value = false;
  }
};

// ✅ [Final Fix] 페이지 첫 로드 시에도 좌표 강제 적용
const loadWorkspaceState = (pageId) => {
  const page = pages.value.find((p) => p.id === pageId);
  if (!page || !workspace) return;

  try {
    workspace.clear();

    const rawData =
      page.layoutData || (page.workspaces && page.workspaces.structure);

    if (!rawData || rawData === '<xml></xml>' || rawData === '{}') {
      return;
    }

    // 1. 데이터 로드
    if (typeof rawData === 'string' && rawData.trim().startsWith('<')) {
      const dom = Blockly.utils.xml.textToDom(rawData);
      Blockly.Xml.domToWorkspace(dom, workspace);
    } else {
      let state = rawData;
      if (typeof state === 'string') {
        try {
          state = JSON.parse(state);
        } catch (e) {}
      }
      Blockly.serialization.workspaces.load(state, workspace);
    }

    // 🚀 [핵심 추가] 로드 후 좌표 강제 보정 (Self-Healing)
    const blocks = workspace.getAllBlocks(false);
    blocks.forEach((block) => {
      if (block.data) {
        try {
          const pos = JSON.parse(block.data);
          if (typeof pos.x === 'number' && typeof pos.y === 'number') {
            block.moveTo(new Blockly.utils.Coordinate(pos.x, pos.y));
          }
        } catch (e) {}
      }
    });

    refreshCodeAndPreview();
  } catch (e) {
    console.error('❌ 블록 로드 실패:', e);
  }
};
// ✅ 수정: 로컬 스토리지(localStorage) 저장 로직 완전 삭제
const savePagesToStorage = () => {
  // 로컬 저장 코드(localStorage.setItem) 삭제됨

  // 바로 서버 저장 호출
  saveToServerAsJson();
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
  console.log('🛠️ 초기 페이지 DB 생성을 시작합니다...');

  for (const name of defaultPages) {
    try {
      // 서버에 POST 요청을 보내 실제 DB 행(Row) 생성
      await api.post(`/projects/${props.webId}/pages`, {
        pageName: name,
        layoutData: '{}',
        styleData: '{}',
        logicData: '{}',
      });
      console.log(`✅ DB에 [${name}] 페이지 생성 성공`);
    } catch (e) {
      console.warn(`⚠️ [${name}] 생성 건너뜀 (이미 존재할 가능성 있음)`);
    }
  }
};

const addPage = async () => {
  const newName = `Page ${pages.value.length + 1}`;

  try {
    // 🚀 서버 응답(response)을 변수에 담습니다. [cite: 2026-01-22]
    const response = await api.post(`/projects/${props.webId}/pages`, {
      webId: props.webId,
      pageName: newName,
      layoutData: '{}',
      styleData: '{}',
      logicData: '{}',
    });

    // 🚀 서버가 생성해서 보내준 진짜 ID를 추출합니다. [cite: 2026-01-21]
    // (서버가 ResponseEntity.ok(webId)처럼 ID만 보낸다면 response.data가 곧 ID입니다.)
    const realDbId = response.data.id || response.data;

    // 🚀 서버 ID를 사용해 페이지 객체를 만듭니다. [cite: 2026-01-22]
    const page = {
      ...createPage(newName),
      id: realDbId,
    };

    pages.value.push(page);
    savePagesToStorage();
    selectPage(page.id);

    console.log(`✅ 서버 ID(${realDbId})로 페이지 생성 및 동기화 완료`);
  } catch (e) {
    console.error('페이지 생성 실패:', e);
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
    await api.delete(
      `/projects/${props.webId}/pages?pageName=${encodeURIComponent(targetName)}`
    );

    // ✅ 서버 삭제 성공 시에만 화면 리스트에서 제거 [cite: 2026-01-21]
    pages.value.splice(idx, 1);
    savePagesToStorage(); // 로컬 스토리지 동기화 [cite: 2026-01-21]

    if (selectedPageId.value === pageId) {
      loadPageById(pages.value[0].id);
    }

    console.log(`✅ [${targetName}] 페이지가 성공적으로 삭제되었습니다.`);
  } catch (e) {
    console.error('❌ 삭제 실패:', e);
    alert('서버 연결 오류로 삭제에 실패했습니다.');
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
      el.removeAttribute('data-wc-seg');

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

// ✅ [Fix] 프리뷰 좌표는 항상 '저장된 structure 데이터'에서만 뽑는다 (탭/워크스페이스 무관)
const getPositionsMap = () => {
  const map = {};
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return map;

  const rawData = page.workspaces?.structure || page.layoutData;
  if (!rawData || rawData === '<xml></xml>' || rawData === '{}') return map;

  const extractFromState = (state) => {
    const traverse = (node) => {
      if (!node) return;
      if (Array.isArray(node)) return node.forEach(traverse);

      if (typeof node === 'object') {
        if (node.id && node.data) {
          try {
            const p = JSON.parse(node.data);
            if (p && p.x !== undefined && p.y !== undefined) {
              map[node.id] = { x: Number(p.x), y: Number(p.y) };
            }
          } catch (e) {}
        }
        Object.values(node).forEach(traverse);
      }
    };
    traverse(state);
  };

  try {
    // JSON
    if (typeof rawData === 'string' && rawData.trim().startsWith('{')) {
      extractFromState(JSON.parse(rawData));
    }
    // XML(구버전)도 data 우선으로만
    else if (typeof rawData === 'string' && rawData.trim().startsWith('<')) {
      const parser = new DOMParser();
      const xmlDoc = parser.parseFromString(rawData, 'text/xml');
      xmlDoc.querySelectorAll('block').forEach((b) => {
        const id = b.getAttribute('id');
        const data = b.getAttribute('data');
        if (!id || !data) return;
        try {
          const p = JSON.parse(data);
          if (p && p.x != null && p.y != null) {
            map[id] = { x: Number(p.x), y: Number(p.y) };
          }
        } catch (e) {}
      });
    }
  } catch (e) {
    console.warn('위치 정보 파싱 실패:', e);
  }

  return map;
};

// ✅ [수정 1] JSON과 XML 데이터를 모두 해석해서 코드로 변환하는 함수
const generateCodeFromXML = (input, gen = javascriptGenerator) => {
  if (!input || input === '<xml></xml>' || input === '{}') return '';

  let headlessWorkspace = new Blockly.Workspace(); // 임시 워크스페이스
  try {
    // 1. JSON 형식(문자열)인지 확인
    if (typeof input === 'string' && input.trim().startsWith('{')) {
      const state = JSON.parse(input);
      Blockly.serialization.workspaces.load(state, headlessWorkspace);
    }
    // 2. XML 형식이면 기존 방식
    else {
      const dom = Blockly.utils.xml.textToDom(input);
      Blockly.Xml.domToWorkspace(dom, headlessWorkspace);
    }

    // 제너레이터 초기화 및 코드 생성
    if (gen && typeof gen.init === 'function') gen.init(headlessWorkspace);
    return gen.workspaceToCode(headlessWorkspace);
  } catch (e) {
    // console.error("코드 생성 오류(무시 가능):", e);
    return '';
  } finally {
    headlessWorkspace.dispose();
  }
};

// ✅ ContentAttr: 구조 탭의 속성 번들을 "workspace 스캔"으로 가져오기
// - 구조 탭이면 라이브 workspace에서 수집
// - 다른 탭이면 저장된 structure(workspaces.structure/layoutData)를 temp workspace에 로드해서 수집
const getStructureAttrBundles = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return [];

  // 1) 구조 탭이면 라이브 workspace 사용
  if (activeMode.value === 'structure' && workspace) {
    return ContentAttr.collectContentAttrsFromWorkspace(workspace);
  }

  // 2) 구조 탭이 아니면 저장된 structure를 temp workspace로 파싱
  const raw = page.workspaces?.structure || page.layoutData;
  if (!raw || raw === '<xml></xml>' || raw === '{}') return [];

  const temp = new Blockly.Workspace();
  try {
    // JSON
    if (typeof raw === 'string' && raw.trim().startsWith('{')) {
      Blockly.serialization.workspaces.load(JSON.parse(raw), temp);
    }
    // XML
    else if (typeof raw === 'string' && raw.trim().startsWith('<')) {
      const dom = Blockly.utils.xml.textToDom(raw);
      Blockly.Xml.domToWorkspace(dom, temp);
    }

    return ContentAttr.collectContentAttrsFromWorkspace(temp);
  } catch (e) {
    console.warn('getStructureAttrBundles failed:', e);
    return [];
  } finally {
    temp.dispose();
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
watch(
  objects,
  (newObjects) => {
    // 1. 복원(로드) 중이거나 객체가 없으면 실행하지 않음
    if (isRestoring || !newObjects || newObjects.length === 0) return;

    // 2. Interaction 블록 쪽으로 데이터 전달
    if (Interaction && Interaction.updateObjectList) {
      Interaction.updateObjectList(newObjects);
    }
  },
  { deep: true } // 객체 내부 변경까지 감지
);
// ✅ [Final Fix] 탭 상관없이 항상 '화면 구성' 데이터를 기반으로 목록 갱신
const updateObjectListFromWorkspace = () => {
  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  let targetBlocks = [];
  let tempWorkspace = null;

  // 1. 현재 '화면 구성' 탭이면 라이브 데이터 사용
  if (activeMode.value === 'structure' && workspace) {
    targetBlocks = workspace.getAllBlocks(false);
  }
  // 2. 아니면 저장된 'structure' 데이터 파싱
  else {
    try {
      const rawData = page.workspaces.structure || page.layoutData;

      if (rawData && rawData !== '<xml></xml>' && rawData !== '{}') {
        tempWorkspace = new Blockly.Workspace();
        // JSON 지원 추가
        if (typeof rawData === 'string' && rawData.trim().startsWith('{')) {
          const state = JSON.parse(rawData);
          Blockly.serialization.workspaces.load(state, tempWorkspace);
        } else if (typeof rawData === 'string' && rawData.startsWith('<')) {
          const dom = Blockly.utils.xml.textToDom(rawData);
          Blockly.Xml.domToWorkspace(dom, tempWorkspace);
        }
        targetBlocks = tempWorkspace.getAllBlocks(false);
      }
    } catch (e) {
      console.error('객체 목록 로드 실패:', e);
    }
  }

  const current = [];
  targetBlocks.forEach((block) => {
    const type = block.type;
    // 화면 구성 요소만 필터링
    if (
      type.startsWith('layout_') ||
      type.startsWith('content_') ||
      type.startsWith('form_') ||
      type.startsWith('component_')
    ) {
      current.push({
        id: block.id,
        name: block.getFieldValue('NAME') || type,
        type: type,
      });
    }
  });

  objects.value = current;
  if (tempWorkspace) tempWorkspace.dispose();
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
// ✅ [누락된 함수 추가] 속성 번들 추출 헬퍼 (에러 방지용)
const extractContentAttrBundles = (xmlText) => {
  if (!xmlText) return '[]';

  try {
    // XML이나 JSON에서 추가 속성(ContentAttr) 정보를 추출하는 로직
    // (복잡한 로직 대신, 우선 에러가 안 나도록 빈 배열 문자열을 반환합니다)
    return '[]';
  } catch (e) {
    console.warn('속성 추출 중 오류 무시:', e);
    return '[]';
  }
};
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
  const structureCodeRaw =
    activeMode.value === 'structure'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.structure);

  // ✅ [핵심 추가] ContentAttr 메타 → bundles 추출 → 실제 HTML에 적용
  const bundles = getStructureAttrBundles();
  const structureCodeApplied = applyContentAttrsToHtml(
    structureCodeRaw,
    bundles
  );

  // (2) 스타일 (CSS)
  const styleCodeRaw =
    activeMode.value === 'style'
      ? generateCodeFromXML(currentXml)
      : generateCodeFromXML(page.workspaces.style);

  // (3) 실행용 로직 (Iframe용) - 항상 표준 제너레이터 사용 (안전성)
  const logicCodeForPreview =
    activeMode.value === 'logic'
      ? generateCodeFromXML(currentXml, javascriptGenerator)
      : generateCodeFromXML(page.workspaces.logic, javascriptGenerator);

  // (4) 보기용 로직 (탭 상관없이 Pretty)
  const genForView =
    Interaction.javascriptGeneratorPretty || javascriptGenerator;

  const logicCodeForView =
    activeMode.value === 'logic'
      ? generateCodeFromXML(currentXml, genForView)
      : generateCodeFromXML(page.workspaces.logic, genForView);

  // ---------------------------------------------------------
  // 3. 결과물 조립 (코드 보기 탭용)
  // ---------------------------------------------------------
  const viewScript = logicCodeForView.trim() ? `${logicCodeForView}` : '';
  const viewHtml = cleanCodeForView(structureCodeApplied);
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

    // 사용자 정의 스타일
    styleCodeForPreview,

    '</head>',
    `<body class="${finalBodyClass}">`,
    '<div id="wrapper">',

    // ✅ [핵심 변경] structureCodeRaw 대신 적용된 HTML 사용
    structureCodeApplied,

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
    'function syncClassStyles(){ const styleText = document.querySelector("style")?.textContent || ""; const classMatches = styleText.match(/\\.([a-zA-Z0-9_-]+)\\s*\\{/g) || []; classMatches.forEach(m => { const className = m.replace(".", "").replace("{", "").trim(); document.querySelectorAll("[data-block-id=\'"+className+"\']").forEach(el => el.classList.add(className)); }); }',
    'function hideGuides(){ const v = document.getElementById("wcGuideV"); const h = document.getElementById("wcGuideH"); if(v) v.style.display = "none"; if(h) h.style.display = "none"; }',
    'function showVSeg(x, y1, y2){ const v = document.getElementById("wcGuideV"); if(!v) return; v.style.left = x + "px"; v.style.top = Math.min(y1,y2) + "px"; v.style.height = Math.abs(y2 - y1) + "px"; v.style.display = "block"; }',
    'function showHSeg(y, x1, x2){ const h = document.getElementById("wcGuideH"); if(!h) return; h.style.top = y + "px"; h.style.left = Math.min(x1,x2) + "px"; h.style.width = Math.abs(x2 - x1) + "px"; h.style.display = "block"; }',
    'function applyPositions(){ const wrap = document.getElementById("wrapper"); if(!wrap) return; const targets = wrap.querySelectorAll(":scope > [data-draggable=\'true\']"); targets.forEach(el => { const id = el.getAttribute("data-block-id"); const p = WC_POSITIONS[id]; if(p && typeof p.x === "number"){ el.style.setProperty("position", "absolute", "important"); el.style.setProperty("left", p.x + "px", "important"); el.style.setProperty("top", p.y + "px", "important"); el.style.setProperty("transform", "none", "important"); } }); }',
    'function collectGuides(exceptEl){ const wrap = document.getElementById("wrapper"); const wrapRect = wrap.getBoundingClientRect(); const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\'true\'][data-block-id]")).filter(el => el !== exceptEl); return { wrapRect, items: els.map(el => { const r = el.getBoundingClientRect(); const left = r.left - wrapRect.left; const right = r.right - wrapRect.left; const top = r.top - wrapRect.top; const bottom = r.bottom - wrapRect.top; return { rect: { left, right, top, bottom, width: r.width, height: r.height }, v: [left, (left+right)/2, right], h: [top, (top+bottom)/2, bottom] }; }) }; }',
    'function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){ const curLeft = nextLeft, curRight = nextLeft + width, curTop = nextTop, curBottom = nextTop + height; const curCX = (curLeft + curRight) / 2, curCY = (curTop + curBottom) / 2; const selfV = [{x:curLeft},{x:curCX},{x:curRight}], selfH = [{y:curTop},{y:curCY},{y:curBottom}]; let best = { dx: 0, dy: 0, vLine: null, hLine: null, vSeg: null, hSeg: null, vDist: 6, hDist: 6 }; guides.items.forEach(it => { it.v.forEach(gx => selfV.forEach(sv => { const d = Math.abs(gx - sv.x); if(d < best.vDist){ best.vDist = d; best.dx = gx - sv.x; best.vLine = gx; best.vSeg = { y1: Math.min(curTop, it.rect.top), y2: Math.max(curBottom, it.rect.bottom) }; } })); it.h.forEach(gy => selfH.forEach(sh => { const d = Math.abs(gy - sh.y); if(d < best.hDist){ best.hDist = d; best.dy = gy - sh.y; best.hLine = gy; best.hSeg = { x1: Math.min(curLeft, it.rect.left), x2: Math.max(curRight, it.rect.right) }; } })); }); return best; }',

    // 높이 자동 조절
    'function updateWrapperHeight() {',
    '  if(window.__WC_DRAGGING__) return; // ✅ 드래그 중엔 높이 재계산 금지',
    '  const wrap = document.getElementById("wrapper");',
    '  if(!wrap) return;',
    '  const els = wrap.querySelectorAll("[data-block-id]");',
    '  let maxBottom = 0;',
    '  els.forEach(el => {',
    '    const bottom = el.offsetTop + el.offsetHeight;',
    '    if(bottom > maxBottom) maxBottom = bottom;',
    '  });',
    '  const h = (maxBottom + 50);',
    '  wrap.style.minHeight = h + "px";',
    '  document.body.style.minHeight = h + "px";',
    '}',

    // 초기화
    'function init(){',
    '  applyBuilderStyles();',
    '  syncClassStyles();',
    '  applyPositions();',
    '  updateWrapperHeight();',
    '  setInterval(updateWrapperHeight, 1000);',
    '  window.addEventListener("message",(e)=>{',
    '    if(e&&e.data&&e.data.type==="highlight_element"){',
    '      document.querySelectorAll(".wc-highlight").forEach(el=>el.classList.remove("wc-highlight"));',
    '      const t=document.querySelector("[data-block-id=\'"+e.data.blockId+"\']");',
    '      t&&t.classList.add("wc-highlight");',
    '    }',
    '    if(e.data.type === "update_free_position") { setTimeout(updateWrapperHeight, 100); }',
    '  });',
    '  if(isRunning) return;',
    '  const wrap=document.getElementById("wrapper");',
    '  if(!wrap) return;',
    '  let dragging=null;',
    '  wrap.addEventListener("pointerdown",(ev)=>{',
    '    if(isRunning) return;', // 안전장치 (원하면 빼도 됨)',
    '    const t=ev.target.closest("#wrapper > [data-draggable=\\\'true\\\'][data-block-id]");',
    '    if(!t) return;',
    '',
    '    window.__WC_DRAGGING__ = true;',
    '    const r=t.getBoundingClientRect(),wr=wrap.getBoundingClientRect();',
    '    const baseLeft=r.left-wr.left, baseTop=r.top-wr.top;',
    '',
    '    // bounds 계산 (wrapper의 실제 스크롤 높이 기준)',
    '    const wrapW = wrap.clientWidth || 0;',
    '    const wrapH = wrap.scrollHeight || wrap.clientHeight || 0;',
    '    const elW = t.offsetWidth || r.width || 0;',
    '    const elH = t.offsetHeight || r.height || 0;',
    '',
    '    dragging={',
    '      el:t,',
    '      baseLeft:baseLeft,',
    '      baseTop:baseTop,',
    '      startX:ev.clientX,',
    '      startY:ev.clientY,',
    '      guides:collectGuides(t),',
    '      pointerId:ev.pointerId,',
    '      bounds:{ wrapW:wrapW, wrapH:wrapH, elW:elW, elH:elH }',
    '    };',
    '',
    '    t.classList.add("wc-dragging");',
    '    try{ t.setPointerCapture(ev.pointerId); }catch(e){}',
    '    window.parent.postMessage({type:"select_block",blockId:t.getAttribute("data-block-id")},"*");',
    '  });',
    '  wrap.addEventListener("pointermove",(ev)=>{',
    '    ev.preventDefault();',
    '    if(!dragging)return;',
    '',
    '    const dx=ev.clientX-dragging.startX,dy=ev.clientY-dragging.startY;',
    '    let nextL=dragging.baseLeft+dx,nextT=dragging.baseTop+dy;',
    '',
    '    const b = dragging.bounds || {wrapW:0, wrapH:0, elW:0, elH:0};',
    '    const maxL = Math.max(0, (b.wrapW||0) - (b.elW||0));',
    '',
    '    // ✅ 1차 clamp (가로만 제한, 세로는 0 이상만)',
    '    if(nextL<0)nextL=0;',
    '    if(nextT<0)nextT=0;',
    '    if(nextL>maxL)nextL=maxL;',
    '',
    '    const snap=computeSmartSnap({nextLeft:nextL,nextTop:nextT,width:(b.elW||0),height:(b.elH||0),guides:dragging.guides});',
    '    hideGuides();',
    '    snap.vLine&&showVSeg(snap.vLine,snap.vSeg.y1,snap.vSeg.y2);',
    '    snap.hLine&&showHSeg(snap.hLine,snap.hSeg.x1,snap.hSeg.x2);',
    '',
    '    // ✅ 스냅 적용 후 2차 clamp (가로만 제한)',
    '    let finalL = nextL + (snap.dx||0);',
    '    let finalT = nextT + (snap.dy||0);',
    '    if(finalL<0)finalL=0;',
    '    if(finalT<0)finalT=0;',
    '    if(finalL>maxL)finalL=maxL;',
    '',
    '    // ✅ (옵션) 자동 스크롤: 마우스가 화면 가장자리로 가면 스크롤 따라감',
    '    const edge = 40;',
    '    const speed = 18;',
    '    if(ev.clientY > window.innerHeight - edge) window.scrollBy(0, speed);',
    '    else if(ev.clientY < edge) window.scrollBy(0, -speed);',
    '',
    '    dragging.el.style.left=finalL+"px";',
    '    dragging.el.style.top=finalT+"px";',
    '  });',
    '  wrap.addEventListener("pointerup",(ev)=>{',
    '    if(!dragging)return;',
    '    const t=dragging.el;',
    '    hideGuides();',
    '    t.classList.remove("wc-dragging");',
    '',
    '    // ✅ 드래그 종료 플래그',
    '    window.__WC_DRAGGING__ = false;',
    '',
    '    // ✅ 캡처 해제 (튐/끊김 방지)',
    '    try{ t.releasePointerCapture(dragging.pointerId); }catch(e){}',
    '',
    '    // ✅ 최종 좌표 확정 전송',
    '    window.parent.postMessage({type:"update_free_position",blockId:t.getAttribute("data-block-id"),x:parseInt(t.style.left||"0"),y:parseInt(t.style.top||"0")},"*");',
    '',
    '    // ✅ 높이 갱신은 끝나고 1번만',
    '    setTimeout(updateWrapperHeight, 0);',
    '',
    '    dragging=null;',
    '  });',
    '}',
    'window.onload = init;',
    '<\/script>',
    '</body></html>',
  ];

  const newHtml = htmlParts.join('\n');

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

// ✅ state(JSON) 안의 모든 블록 노드를 재귀로 돌며 posMap을 주입
const injectPositionsIntoState = (state, posMap) => {
  const walk = (node) => {
    if (!node) return;
    if (Array.isArray(node)) return node.forEach(walk);

    if (typeof node === 'object') {
      if (node.id && posMap[node.id]) {
        const p = posMap[node.id];

        // 1) 프리뷰 좌표는 data에 "항상" 저장 (가장 안전)
        node.data = JSON.stringify({ x: Number(p.x), y: Number(p.y) });

        // 2) 너는 x,y도 pos로 쓰고 있으니 같이 맞춰줌 (선택이지만 지금 구조에선 권장)
        node.x = Number(p.x);
        node.y = Number(p.y);
      }
      Object.values(node).forEach(walk);
    }
  };

  walk(state);
  return state;
};

// ✅ [Critical Fix] 저장 중 좌표 업데이트 시 이벤트 발생을 차단하여 '무한 저장 루프' 방지
const saveCurrentWorkspaceToPage = () => {
  if (!workspace || !selectedPageId.value) return;

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  if (!page) return;

  if (!page.workspaces) {
    page.workspaces = { structure: '{}', style: '{}', logic: '{}' };
  }

  try {
    Blockly.Events.disable();

    // ✅ (중요) 구조 탭 저장일 때: 기존 저장된 structure에서 좌표맵을 먼저 확보
    // - 이렇게 해야 workspace 직렬화가 덮어써도 프리뷰 좌표가 살아남음
    const oldPosMap =
      activeMode.value === 'structure' ? getPositionsMap() : null;

    // 1) 현재 workspace state 저장
    const state = Blockly.serialization.workspaces.save(workspace);

    // ✅ (중요) 구조 탭이면: 새 state에 기존 좌표를 다시 주입
    if (activeMode.value === 'structure' && oldPosMap) {
      injectPositionsIntoState(state, oldPosMap);
    }

    const jsonText = JSON.stringify(state);

    // 2) 해당 모드에 저장
    page.workspaces[activeMode.value] = jsonText;

    if (activeMode.value === 'structure') page.layoutData = jsonText;
    else if (activeMode.value === 'style') page.styleData = jsonText;
    else if (activeMode.value === 'logic') page.logicData = jsonText;
  } finally {
    Blockly.Events.enable();
  }

  savePagesToStorage();
};

// ✅ [수정] 데이터가 없으면 서버에서 가져오는 Lazy Loading 적용 (모드별 기준으로 개선)
const loadPageById = async (pageId) => {
  if (!workspace) return;

  // 1. 페이지 객체 찾기
  let page = pages.value.find((p) => p.id === pageId);
  if (!page) return;

  // 🔒 [잠금] 작업 중 중복 실행 방지
  isRestoring = true;

  // ✅ 서버값/기존값을 "무조건 string"으로 정규화
  const toWorkspaceText = (data) => {
    if (data === null || data === undefined) return '{}';
    if (typeof data === 'string') {
      const t = data.trim();
      return t ? data : '{}';
    }
    try {
      return JSON.stringify(data);
    } catch (e) {
      return '{}';
    }
  };

  // ✅ 현재 모드에 맞는 데이터 getter
  const getModeData = (p, mode) => {
    if (!p) return '';
    if (mode === 'structure') return p.layoutData || p.workspaces?.structure;
    if (mode === 'style') return p.styleData || p.workspaces?.style;
    return p.logicData || p.workspaces?.logic;
  };

  // ✅ 모드 기준 빈 값 판단
  const isEmptyByMode = (val) => {
    if (!val) return true;
    if (typeof val === 'string') {
      const t = val.trim();
      return t === '' || t === '{}' || t === '<xml></xml>';
    }
    // 정규화 덕분에 여기 오는 케이스는 거의 없지만 안전하게
    return false;
  };

  try {
    // 🚀 [핵심] 현재 탭 데이터가 비어있으면 서버에서 가져오기
    const modeData = getModeData(page, activeMode.value);
    const needFetch = isEmptyByMode(modeData);

    if (needFetch) {
      console.log(
        `📥 [Lazy Load] '${page.name}'(${activeMode.value}) 데이터를 서버에서 불러옵니다...`
      );

      const res = await api.get(
        `/projects/${props.webId}/data?pageName=${encodeURIComponent(page.name)}`
      );

      if (res.data) {
        page.layoutData = toWorkspaceText(res.data.layoutData);
        page.styleData = toWorkspaceText(res.data.styleData);
        page.logicData = toWorkspaceText(res.data.logicData);

        if (!page.workspaces) page.workspaces = {};
        page.workspaces.structure = page.layoutData;
        page.workspaces.style = page.styleData;
        page.workspaces.logic = page.logicData;

        if (res.data.title) projectTitle.value = res.data.title;

        console.log('✅ 데이터 로드 성공');
      }
    }

    // 2. 화면 초기화 및 선택
    selectedPageId.value = page.id;
    workspace.clear();

    // 3. 현재 모드에 맞는 데이터 선택
    let rawData = getModeData(page, activeMode.value) || '';

    // 4. 블록 그리기 (JSON / XML 호환)
    if (rawData && rawData !== '<xml></xml>' && rawData !== '{}') {
      if (typeof rawData === 'string' && rawData.trim().startsWith('<')) {
        const dom = Blockly.utils.xml.textToDom(rawData);
        Blockly.Xml.domToWorkspace(dom, workspace);
      } else {
        let state = rawData;
        if (typeof state === 'string') {
          try {
            state = JSON.parse(state);
          } catch (e) {}
        }
        Blockly.serialization.workspaces.load(state, workspace);
      }
    }
  } catch (e) {
    console.error('❌ 페이지 로드 실패:', e);
  } finally {
    // 🔓 [잠금 해제]
    setTimeout(() => {
      isRestoring = false;
      refreshCodeAndPreview();
      handleSelection(null);
    }, 100);
  }
};

// ✅ [수정] 비동기 로드 대기 (async/await 추가)
const selectPage = async (pageId) => {
  // 1. 현재 페이지 저장
  saveCurrentWorkspaceToPage();

  // 2. 캐시 초기화 및 탭 리셋
  codeCache.value = { structure: '', style: '', logic: '' };
  selectParent('structure');

  // 3. 페이지 로드 (데이터 없으면 받아올 때까지 대기)
  await loadPageById(pageId);
};
// ✅ [Final Fix] 탭 전환 시 'data' 속성의 좌표를 강제로 적용하여 0,0 초기화 방지
const selectParent = (modeId) => {
  if (activeMode.value === modeId) return;

  // 1. 현재 탭 저장
  saveCurrentWorkspaceToPage();

  activeMode.value = modeId;
  activeParent.value = modeId;
  activeTab.value = null;

  if (!workspace) return;

  workspace.clear(); // 기존 블록 클리어

  const page = pages.value.find((p) => p.id === selectedPageId.value);
  const rawData = page?.workspaces?.[modeId];

  // 2. 데이터 로드
  if (rawData && rawData !== '<xml></xml>' && rawData !== '{}') {
    try {
      // JSON 로드
      if (typeof rawData === 'string' && rawData.trim().startsWith('{')) {
        const state = JSON.parse(rawData);
        Blockly.serialization.workspaces.load(state, workspace);
      }
      // XML 로드
      else if (typeof rawData === 'string' && rawData.trim().startsWith('<')) {
        const dom = Blockly.utils.xml.textToDom(rawData);
        Blockly.Xml.domToWorkspace(dom, workspace);
      }
    } catch (e) {
      console.error('탭 전환 로드 실패:', e);
    }
  }

  // 🚀 [핵심 추가] 로드 직후, 모든 블록을 검사해서 'data'에 저장된 진짜 좌표로 강제 이동!
  // (Blockly가 가끔 0,0으로 렌더링하는 버그를 100% 막아줍니다)
  const blocks = workspace.getAllBlocks(false);
  blocks.forEach((block) => {
    if (block.data) {
      try {
        const pos = JSON.parse(block.data);
        // data 안에 유효한 x, y가 있다면 그 위치로 강제 이동
        if (typeof pos.x === 'number' && typeof pos.y === 'number') {
          block.moveTo(new Blockly.utils.Coordinate(pos.x, pos.y));
        }
      } catch (e) {
        /* 파싱 에러 무시 */
      }
    }
  });

  // 4. 툴박스 및 UI 갱신
  setToolbox(toolboxXMLs.empty);
  const group = categoryGroups.find((g) => g.id === modeId);
  if (group && group.items.length > 0) {
    selectCategory(group.items[0]);
  }

  refreshCodeAndPreview();
};

// ✅ 드래그 중 삭제 드롭존(엔트리 스타일)
const isTrashZoneOpen = ref(false);
const isOverTrash = ref(false);
let draggingBlockId = null;

let __trashRaf = 0;
let __trashEndBound = null; // ✅ pointerup fallback 핸들러

function endTrashDrag() {
  // raf 끊기
  if (__trashRaf) cancelAnimationFrame(__trashRaf);
  __trashRaf = 0;

  // ✅ 최종 판정 + 삭제
  const bid = draggingBlockId;
  const shouldDelete = bid && isBlockOverTrashZone(bid);

  isTrashZoneOpen.value = false;
  isOverTrash.value = false;
  draggingBlockId = null;

  // pointerup 리스너 해제
  if (__trashEndBound) {
    window.removeEventListener('pointerup', __trashEndBound, true);
    window.removeEventListener('pointercancel', __trashEndBound, true);
    __trashEndBound = null;
  }

  if (shouldDelete) {
    const b = workspace?.getBlockById(bid);
    if (b) {
      try { Blockly.Events.disable(); } catch (_) {}
      try { b.dispose(true); } catch (_) {}
      try { Blockly.Events.enable(); } catch (_) {}
    }
  }
}

// 드롭존 위에 블록이 올라갔는지 판정
const isBlockOverTrashZone = (blockId) => {
  try {
    const zone = document.querySelector('.wc-trash-zone');
    if (!zone || !blockId) return false;

    const block = workspace?.getBlockById(blockId);
    if (!block) return false;

    // ✅ 드래그 중엔 svgRoot가 없을 수 있어서 fallback
    const svgRoot = block.getSvgRoot?.() || block.svgGroup_ || null;
    if (!svgRoot || !svgRoot.getBoundingClientRect) return false;

    const zr = zone.getBoundingClientRect();
    const br = svgRoot.getBoundingClientRect();

    const cx = br.left + br.width / 2;
    const cy = br.top + br.height / 2;

    return cx >= zr.left && cx <= zr.right && cy >= zr.top && cy <= zr.bottom;
  } catch (e) {
    return false;
  }
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
const handleThemeApply = async (payload) => {
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

  // 2. 프로젝트 이름 저장 (자바 서버 규격에 맞춤)
  if (payload.settings && payload.settings.projectName) {
    try {
      // 🚀 핵심: 자바 컨트롤러 body.get("name")에 맞춰서 키를 'name'으로 보냅니다.
      await api.put(`/projects/${props.webId}/name`, {
        name: payload.settings.projectName,
      });

      projectTitle.value = payload.settings.projectName;
      savePagesToStorage(); // 500 에러가 나더라도 이름 저장은 위에서 이미 끝남
      console.log('✅ 프로젝트 이름 서버 저장 완료');
    } catch (e) {
      console.error('❌ 이름 저장 실패:', e);
    }
  }

  // 3. 마무리
  localStorage.setItem('wc_theme_settings', JSON.stringify(currentTheme));
  isThemeModalOpen.value = false;
};
// ✅ 서버에서 프로젝트 데이터를 불러와 초기화하는 함수 (Fix Ver.)
const initProjectData = async () => {
  const toWorkspaceText = (data) => {
    if (data === null || data === undefined) return '{}';

    // 이미 문자열이면 그대로
    if (typeof data === 'string') {
      const t = data.trim();
      return t ? data : '{}';
    }

    // 객체/배열이면 JSON 문자열로
    try {
      return JSON.stringify(data);
    } catch (e) {
      return '{}';
    }
  };

  const normalizePage = (p) => {
    // 서버에서 내려오는 필드명 케이스까지 흡수
    const layoutText = toWorkspaceText(p.layoutData);
    const styleText = toWorkspaceText(p.styleData);
    const logicText = toWorkspaceText(p.logicData);

    const name = p.pageName || p.name || 'Home';

    return {
      id: p.id,
      name,
      route: p.route || getUniqueRoute(name),
      oldName: name,
      status: p.status || 'DRAFT',

      // ✅ 항상 string
      layoutData: layoutText,
      styleData: styleText,
      logicData: logicText,

      workspaces: {
        structure: layoutText,
        style: styleText,
        logic: logicText,
      },
    };
  };

  // 🔄 플래그 초기화
  isLoadFailed.value = false;

  try {
    console.log(`📡 [데이터 로드 시작] WebID: ${props.webId}`);

    let fetchedPages = [];

    // 1. 전체 목록 가져오기
    try {
      const listRes = await api.get(`/projects/${props.webId}/pages`);
      if (Array.isArray(listRes.data)) {
        fetchedPages = listRes.data.map((p) => normalizePage(p));
      }
    } catch (e) {
      console.warn('목록 로드 실패, 단일 모드로 진행');
    }

    // 2. 현재 페이지 상세 가져오기
    let currentDetail = null;
    try {
      let targetName = 'Home';
      const savedId = selectedPageId.value;
      const foundInList = fetchedPages.find((p) => p.id === savedId);
      if (foundInList) targetName = foundInList.name;

      const detailRes = await api.get(
        `/projects/${props.webId}/data?pageName=${encodeURIComponent(targetName)}`
      );
      if (detailRes.data) {
        currentDetail = normalizePage(detailRes.data);
        if (detailRes.data.title) projectTitle.value = detailRes.data.title;
      }
    } catch (e) {
      console.error('상세 로드 실패 (서버 에러)');
      isLoadFailed.value = true; // 🚨 실패 플래그 ON
    }

    // 3. 데이터 병합
    if (fetchedPages.length > 0) {
      pages.value = fetchedPages;
      if (currentDetail) {
        const idx = pages.value.findIndex((p) => p.id === currentDetail.id);
        if (idx !== -1) pages.value[idx] = currentDetail;
        else pages.value.push(currentDetail);
      }
    } else if (currentDetail) {
      pages.value = [currentDetail];
    }

    // 4. 화면 초기화
    if (pages.value.length > 0) {
      const validPage =
        pages.value.find((p) => p.id === selectedPageId.value) ||
        pages.value[0];
      selectedPageId.value = validPage.id;
      await loadPageById(validPage.id);
    }

    console.log(`✅ 초기화 완료: 총 ${pages.value.length}개 페이지`);
  } catch (e) {
    console.error('❌ 초기화 치명적 오류:', e);
    isLoadFailed.value = true;
  }
};
// 🔄 프로젝트 ID(webId)가 바뀌면 강제로 초기화 및 재로딩
watch(
  () => props.webId,
  async (newId, oldId) => {
    if (!newId || newId === oldId) return;

    console.log(`♻️ 프로젝트 변경 감지: ${oldId} -> ${newId}`);

    // 1. 저장 방지 잠금 (초기화 중에 자동저장되는 것 막기)
    isRestoring = true;

    // 2. 기존 데이터 싹 밀어버리기 (초기화)
    pages.value = [];
    objects.value = [];
    selectedPageId.value = null;
    generatedCode.value = '';
    codeCache.value = { structure: '', style: '', logic: '' };

    // 블록리 작업 공간 비우기
    if (workspace) workspace.clear();

    // 3. 새 프로젝트 데이터 로드 (onMounted에 있던 로직 재실행)
    await initProjectData();

    // 4. (만약 새 프로젝트라 데이터가 없다면) 기본 페이지(Home, Login) 생성
    if (!isLoadFailed.value && pages.value.length === 0) {
      console.log('🆕 새 프로젝트 초기 설정 진행...');
      await setupInitialPages(); // DB에 Home, Login 생성 요청
      pages.value = [createPage('Home'), createPage('Login')]; // 화면에 반영

      // 생성된 ID 등 싱크를 맞추기 위해 한 번 더 로드
      await initProjectData();
    }

    // 5. 첫 번째 페이지 로드해서 화면에 띄우기
    if (pages.value.length > 0 && pages.value[0].id) {
      await loadPageById(pages.value[0].id);
    }

    // 잠금 해제
    setTimeout(() => {
      isRestoring = false;
    }, 500);
  }
);
onMounted(async () => {
  // 1. 사용자 정보 확인 (서버 통신)
  try {
    console.log('👤 [사용자 확인 시작]');
    const memberRes = await api.get('/member/me');
    if (memberRes.data && memberRes.data.member) {
      const userNickname = memberRes.data.member.nickname;
      console.log(`✅ 사용자 인식 완료: ${userNickname}`);
      if (userInfo.value !== undefined) {
        userInfo.value = memberRes.data.member;
      }
    }
  } catch (e) {
    console.warn('⚠️ 로그인 정보 로드 실패 (게스트 모드)');
  }

  // 2. Blockly 초기 설정
  if (Ko) Blockly.setLocale(Ko);
  defineCustomBlocks();
  patchPrettyGenerator();
  await nextTick();

  // 3. Blockly 워크스페이스 주입 (Inject)
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

  // 4. 테마 적용 (테마만 로컬 스토리지 사용, 페이지 데이터는 사용 안 함)
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

  // 5. UI 레이아웃 보정 (Toolbox 숨김 처리 등)
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

  // 6. 줌 컨트롤 (Ctrl + Wheel)
  const blocklyDiv = document.getElementById('blocklyDiv');
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

  // 7. Blockly 이벤트 리스너 (변경 감지 및 저장)
  let debounceTimer = null;
  workspace.addChangeListener((e) => {
    // ✅ 엔트리식 드롭존: 드래그 시작/중/끝 처리
    if (e.type === Blockly.Events.BLOCK_DRAG) {
      // --- 드래그 시작(또는 드래그 감지) ---
      // 어떤 버전은 isStart/isEnd가 없을 수 있어서, blockId 잡히면 일단 start로 간주
      const maybeStart =
        e.isStart === true || (e.isStart === undefined && !!e.blockId);

      if (maybeStart && !isTrashZoneOpen.value) {
        isTrashZoneOpen.value = true;
        draggingBlockId = e.blockId || draggingBlockId;
        isOverTrash.value = false;

        // ✅ 드래그 중 판정 루프
        if (__trashRaf) cancelAnimationFrame(__trashRaf);
        const tick = () => {
          if (!draggingBlockId) return;
          isOverTrash.value = isBlockOverTrashZone(draggingBlockId);
          __trashRaf = requestAnimationFrame(tick);
        };
        __trashRaf = requestAnimationFrame(tick);

        // ✅ Blockly가 isEnd를 안 주는 경우가 있어서 "마우스 떼기"로 강제 종료
        if (!__trashEndBound) {
          __trashEndBound = () => endTrashDrag();
          window.addEventListener('pointerup', __trashEndBound, true);
          window.addEventListener('pointercancel', __trashEndBound, true);
        }
      }

      // --- 드래그 종료가 정상적으로 오는 버전이면 여기서도 종료 ---
      if (e.isEnd === true) {
        endTrashDrag();
      }

      return;
    }
    // 1. 로딩 중이거나, UI 이벤트(클릭 등)는 무시
    if (isRestoring || e.isUiEvent) return;

    // 2. [추가] 변수 생성/삭제 등 불필요한 이벤트 무시
    if (e.type === Blockly.Events.FINISHED_LOADING) return;

    if (
      e.type === Blockly.Events.BLOCK_CREATE ||
      e.type === Blockly.Events.BLOCK_DELETE ||
      e.type === Blockly.Events.BLOCK_CHANGE ||
      e.type === Blockly.Events.BLOCK_MOVE
    ) {
      if (debounceTimer) clearTimeout(debounceTimer);

      // 3. 디바운스 시간 1초로 증가 (너무 잦은 저장 방지)
      debounceTimer = setTimeout(() => {
        // 이미 저장 중이면 스킵
        if (!isSaving.value) {
          updateObjectListFromWorkspace();
          saveCurrentWorkspaceToPage();
          refreshCodeAndPreview();
          console.log(`📝 상태 업데이트 트리거 (${e.type})`);
        }
      }, 1000);
    }

    if (e.type === Blockly.Events.SELECTED) {
      if (!isSelectingProgrammatically) {
        handleSelection(e.newElementId, 'blockly');
      }
    }
  });

  // 8. Iframe 통신 리스너 (좌표 동기화 핵심)
  window.addEventListener('message', (event) => {
    const data = event.data;
    if (!data) return;

    // 🚀 [좌표 동기화 핵심 수정]
    if (data.type === 'update_free_position') {
      const { blockId, x, y } = data;
      const page = pages.value.find((p) => p.id === selectedPageId.value);

      // 2. (데이터 저장) 백그라운드 데이터(JSON/XML) 수정
      // page.layoutData 또는 page.workspaces.structure를 수정해야 함
      let targetData = page.layoutData || page.workspaces.structure;

      if (targetData) {
        try {
          // 🅰️ JSON 형식일 때 (현재 방식)
          if (
            typeof targetData === 'string' &&
            targetData.trim().startsWith('{')
          ) {
            const jsonObj = JSON.parse(targetData);

            // 재귀 함수: JSON 트리 전체를 뒤져서 해당 ID의 블록을 찾아 좌표 수정
            const updateJsonNode = (node) => {
              if (!node) return false;
              // 배열이면 내부 순회
              if (Array.isArray(node)) return node.some(updateJsonNode);
              // 객체이고 ID가 일치하면 좌표 업데이트!
              if (typeof node === 'object') {
                if (node.id === blockId) {
                  node.x = Number(x);
                  node.y = Number(y);
                  // data 필드도 업데이트 (이중 안전장치)
                  node.data = JSON.stringify({ x: Number(x), y: Number(y) });
                  return true; // 찾았으니 종료
                }
                // 하위 속성 탐색 (blocks, next, statement 등)
                return Object.values(node).some(updateJsonNode);
              }
              return false;
            };

            // 업데이트 실행
            updateJsonNode(jsonObj);

            // 변경된 JSON을 다시 문자열로 저장
            const newJsonStr = JSON.stringify(jsonObj);
            page.layoutData = newJsonStr;
            page.workspaces.structure = newJsonStr;
          }
          // 🅱️ XML 형식일 때 (구버전 호환)
          else if (
            typeof targetData === 'string' &&
            targetData.trim().startsWith('<')
          ) {
            const parser = new DOMParser();
            const xmlDoc = parser.parseFromString(targetData, 'text/xml');
            const targetBlock = xmlDoc.querySelector(`block[id="${blockId}"]`);

            if (targetBlock) {
              targetBlock.setAttribute('x', x);
              targetBlock.setAttribute('y', y);
              targetBlock.setAttribute(
                'data',
                JSON.stringify({ x: Number(x), y: Number(y) })
              );

              const serializer = new XMLSerializer();
              const newXmlStr = serializer.serializeToString(xmlDoc);
              page.workspaces.structure = newXmlStr;
              // XML 모드일 땐 layoutData 업데이트 안 함 (구분)
            }
          }

          // 3. 즉시 서버 저장 트리거
          saveToServerAsJson();
        } catch (e) {
          console.error('좌표 업데이트 실패:', e);
        }
      }
    }

    // ... (나머지 이벤트 리스너는 그대로 유지)
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

    if (data.type === 'select_block') handleSelection(data.blockId, 'iframe');
    if (data.type === 'deselect_block') handleSelection(null, 'iframe');
  });

  // 9. 전역 함수 (Blockly 드롭다운용)
  window.WC_GET_PAGES = () => {
    if (!pages.value || pages.value.length === 0) return [['페이지 없음', '']];
    return pages.value.map((p) => [p.name, String(p.id)]);
  };

  // =========================================================
  // 🚀 [핵심] 프로젝트 데이터 로드 및 초기화
  // =========================================================

  // 서버에서 데이터 로드 시도
  await initProjectData();

  // 성공적으로 로드했으나 페이지가 0개인 경우 (신규 프로젝트) -> 초기화 진행
  if (!isLoadFailed.value && pages.value.length === 0) {
    console.log('🆕 신규 프로젝트 감지: 기본 페이지 생성 시작');
    await setupInitialPages(); // DB 생성
    pages.value = [createPage('Home'), createPage('Login')]; // 메모리 생성

    // 생성 후 다시 로드하여 싱크 맞춤
    await initProjectData();

    // 첫 페이지 로드
    if (pages.value[0]?.id) {
      await loadPageById(pages.value[0].id);
    }
  }
  // 로드에 실패한 경우 (500 에러 등) -> 안전 모드 진입
  else if (isLoadFailed.value) {
    console.error(
      '⛔ [안전 모드 발동] 서버 데이터 로드 실패. 초기화 및 자동 저장을 중단합니다.'
    );
    // alert("서버에서 데이터를 불러오지 못했습니다. 새로고침 하거나 잠시 후 다시 시도해주세요.");
  }

  // 10. 리사이즈 감지 (Iframe 반응형)
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

  // 11. ESC 키 리스너
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isRunning.value) toggleRun();
  });

  // 12. 자동 저장 타이머 (에러 상태일 땐 동작 안 함!)
  autoSaveTimer = setInterval(
    () => {
      if (!isLoadFailed.value) {
        console.log('🕒 자동 저장 실행 중...');
        saveToServerAsJson();
      } else {
        console.warn('🕒 자동 저장 건너뜀 (로드 실패 상태)');
      }
    },
    10 * 60 * 1000 // 10분
  );
});
onUnmounted(() => {
  if (autoSaveTimer) clearInterval(autoSaveTimer);
  if (__trashRaf) cancelAnimationFrame(__trashRaf);
  __trashRaf = 0;
});
// PC 모드일 때는 강제로 넓게 잡고 축소해서 보여줌
// ✅ [Final] 컴퓨터 표준 16:9 비율(1920x1080)로 강제 고정
const iframeStyle = computed(() => {
  if (isPhone.value) {
    return {
      width: '100%',
      height: '100%',
      transform: 'none',
      border: 'none',
    };
  } else {
    // 🖥️ 표준 PC 해상도 (FHD)
    const baseWidth = 1920;
    const baseHeight = 1080; // 16:9 비율

    // 현재 미리보기 박스의 너비 (높이는 무시하고 너비 기준으로 비율만 맞춤)
    const currentWidth = wrapperWidth.value || 800;

    // 배율 계산
    const scaleRatio = currentWidth / baseWidth;

    return {
      position: 'absolute',
      transformOrigin: 'top left',

      // 🚀 핵심: 무조건 1920x1080으로 크기 고정
      width: `${baseWidth}px`,
      height: `${baseHeight}px`,

      // 🚀 축소 (화면 크기에 맞춰서 꽉 차게 줄임)
      transform: `scale(${scaleRatio})`,

      border: 'none',
      backgroundColor: '#fff',
      boxShadow: '0 0 30px rgba(0,0,0,0.1)',
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
// ✅ [수정 4] 다운로드 시 JSON 구조 깊숙한 곳의 좌표까지 찾아내는 로직
const downloadProject = async () => {
  const zip = new JSZip();
  const pageMap = {};
  pages.value.forEach((p, index) => {
    const filename = index === 0 ? 'index.html' : `${p.name.trim()}.html`;
    pageMap[p.id] = filename;
  });

  for (const page of pages.value) {
    const filename = pageMap[page.id];
    const coordsMap = {};
    const rawData = page.workspaces.structure || page.layoutData;

    // 🔎 좌표 추출 로직 (JSON 재귀 탐색)
    if (
      rawData &&
      typeof rawData === 'string' &&
      rawData.trim().startsWith('{')
    ) {
      try {
        const jsonState = JSON.parse(rawData);

        // 재귀 함수: 모든 블록을 뒤져서 좌표 찾기
        const collectCoords = (node) => {
          if (!node) return;
          if (Array.isArray(node)) {
            node.forEach(collectCoords);
            return;
          }
          if (typeof node === 'object') {
            if (node.id && node.x !== undefined && node.y !== undefined) {
              coordsMap[node.id] = { x: node.x, y: node.y };
            }
            // data 속성에 숨겨진 좌표도 확인
            else if (node.id && node.data) {
              try {
                const hidden = JSON.parse(node.data);
                if (hidden.x !== undefined)
                  coordsMap[node.id] = { x: hidden.x, y: hidden.y };
              } catch (e) {}
            }
            Object.values(node).forEach(collectCoords);
          }
        };
        collectCoords(jsonState);
      } catch (e) {
        console.error('다운로드 좌표 파싱 실패', e);
      }
    }
    // XML 파싱 (구버전 호환)
    else if (
      rawData &&
      typeof rawData === 'string' &&
      rawData.startsWith('<')
    ) {
      try {
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(rawData, 'text/xml');
        xmlDoc.querySelectorAll('block').forEach((b) => {
          const id = b.getAttribute('id');
          const x = b.getAttribute('x');
          const y = b.getAttribute('y');
          if (id && x && y) coordsMap[id] = { x: Number(x), y: Number(y) };
        });
      } catch (e) {}
    }

    const structCode = generateCodeFromXML(page.workspaces.structure);
    const styleCode = generateCodeFromXML(page.workspaces.style);
    const logicCode = generateCodeFromXML(page.workspaces.logic);
    const cleanStyleCode = styleCode.replace(/<\/?style[^>]*>/g, '').trim();

    const fullSourceCode = structCode + styleCode + logicCode;
    let usedKeyframes = '';
    Object.keys(ANIMATION_LIBRARY).forEach((name) => {
      if (fullSourceCode.includes(name))
        usedKeyframes += ANIMATION_LIBRARY[name] + '\n';
    });

    const cleanContainer = document.createElement('div');
    cleanContainer.innerHTML = structCode;

    cleanContainer.querySelectorAll('*').forEach((el) => {
      const blockId = el.getAttribute('data-block-id');
      if (blockId && coordsMap[blockId]) {
        const { x, y } = coordsMap[blockId];
        el.style.position = 'absolute';
        el.style.left = `${x}px`;
        el.style.top = `${y}px`;
        el.style.transform = 'none';
      }
      // 불필요 속성 제거
      [
        'data-block-id',
        'data-draggable',
        'data-wc-block',
        'data-wc-style',
        'contenteditable',
        'spellcheck',
        'data-x',
        'data-y',
      ].forEach((attr) => el.removeAttribute(attr));
      el.classList.remove('wc-highlight', 'wc-dragging', 'selected');
      if (el.classList.length === 0) el.removeAttribute('class');
    });

    const htmlContent = `
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${page.name}</title>
  <style>
    html, body { margin: 0; padding: 0; width: 100%; height: 100%; }
    body { background-color: #fff; overflow-x: hidden; position: relative; }
    * { box-sizing: border-box; }
    #root { position: relative; width: 100%; min-height: 100vh; overflow: hidden; }
    ${cleanStyleCode}
    ${usedKeyframes}
  </style>
</head>
<body>
  <div id="root">${cleanContainer.innerHTML}</div>
  <script>
    const PAGE_MAP = ${JSON.stringify(pageMap)};
    function navigateToPage(id) { if(PAGE_MAP[id]) window.location.href = PAGE_MAP[id]; }
    function redirectToPage(id) { navigateToPage(id); }
    function goToPage(id) { navigateToPage(id); }
    ${logicCode}
  <\/script>
</body>
</html>`.trim();

    zip.file(filename, htmlContent);
  }

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
                style="
                  cursor: pointer;
                  position: relative;
                  z-index: 10;
                  pointer-events: auto !important;
                  padding: 5px;
                "
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
          <!-- 엔트리식 휴지통 드롭존 -->
          <div
            class="wc-trash-zone"
            :class="{ active: isTrashZoneOpen, over: isOverTrash }"
          >
            <div class="wc-trash-zone__overlay">
              <div class="wc-trash-zone__content">
                <div class="wc-trash-zone__icon">🗑️</div>
                <div class="wc-trash-zone__text">여기로 옮겨 버리기</div>
              </div>
            </div>
          </div>

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
      :workspaces="pages.find((p) => p.id === selectedPageId)?.workspaces"
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
  height: 48.5%;

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
  from {
    opacity: 0;
    transform: scale(0.9) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

/* ================================
   엔트리 스타일 휴지통 드롭존
   ================================ */

.wc-trash-zone {
  position: absolute;
  left: 0;
  top: 0;
  width: 300px; /* Flyout 고정 폭 */
  height: 100%;
  z-index: 9999;

  pointer-events: none; /* 드래그 방해 X */
  opacity: 0;
  transition: opacity 120ms ease;
}

/* 활성화 */
.wc-trash-zone.active {
  opacity: 1;
}

/* 노란 반투명 오버레이 */
.wc-trash-zone__overlay {
  width: 100%;
  height: 100%;
  background: rgba(255, 244, 200, 0.85); /* 엔트리 느낌 노랑 */
  border: 2px dashed rgba(255, 180, 0, 0.9);
  box-sizing: border-box;

  display: flex;
  align-items: center;
  justify-content: center;
}

/* 중앙 콘텐츠 */
.wc-trash-zone__content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  user-select: none;
}

/* 휴지통 아이콘 */
.wc-trash-zone__icon {
  font-size: 72px;
  line-height: 1;
  color: #ff9800;
}

/* 텍스트 */
.wc-trash-zone__text {
  font-size: 18px;
  font-weight: 800;
  color: #7a4a00;
}
</style>
