// ===============================
// Page Engine (Single File)
// ===============================

/* ---------- 타입 ---------- */
export type PageStatus = "DRAFT" | "LOCKED";

export type Page = {
  id: string;
  name: string;
  route: string;
  aliases: string[];
  status: PageStatus;
};

/* ---------- 내부 상태 ---------- */
const pages: Page[] = [];
const urlToPageId = new Map<string, string>();
const pageElements: Record<string, Set<string>> = {};

let iframeEl: HTMLIFrameElement | null = null;

/* ---------- 유틸 ---------- */
function slugify(name: string) {
  return (
    "/" +
    name
      .trim()
      .toLowerCase()
      .replace(/\s+/g, "-")
      .replace(/[^a-z0-9\-]/g, "")
  );
}

function rebuildRouteIndex() {
  urlToPageId.clear();
  pages.forEach(p => {
    urlToPageId.set(p.route, p.id);
    p.aliases.forEach(a => urlToPageId.set(a, p.id));
  });
}

function generateElementId(type: "btn" | "img" | "text") {
  return `${type}_${crypto.randomUUID().slice(0, 6)}`;
}

/* ---------- 페이지 관리 ---------- */
export function createPage(name: string): Page {
  const page: Page = {
    id: "page_" + crypto.randomUUID().slice(0, 6),
    name,
    route: slugify(name),
    aliases: [],
    status: "DRAFT"
  };

  pages.push(page);
  pageElements[page.id] = new Set();
  rebuildRouteIndex();
  return page;
}

export function renamePage(pageId: string, newName: string) {
  const page = pages.find(p => p.id === pageId);
  if (!page) return;

  page.name = newName;

  // Draft 단계에서만 자동 연동
  if (page.status === "DRAFT") {
    page.route = slugify(newName);
    rebuildRouteIndex();
  }
}

export function lockPage(pageId: string) {
  const page = pages.find(p => p.id === pageId);
  if (!page || page.status === "LOCKED") return;
  page.status = "LOCKED";
}

export function getPageById(pageId: string) {
  return pages.find(p => p.id === pageId);
}

export function getPageIdByUrl(path: string) {
  return urlToPageId.get(path);
}

/* ---------- 요소(ID) 관리 ---------- */
export function registerElement(pageId: string, type: "btn" | "img" | "text") {
  const id = generateElementId(type);
  pageElements[pageId]?.add(id);
  return id;
}

/* ---------- iframe ---------- */
export function mountIframe(el: HTMLIFrameElement) {
  iframeEl = el;
}

function buildPreviewHtml(pageId: string) {
  return `
<!DOCTYPE html>
<html>
<body>
  <div id="app"></div>

  <script>
    const PAGE_ID = "${pageId}";

    function navigateToPage(pageId) {
      parent.postMessage({ type: "NAVIGATE", pageId }, "*");
    }

    function redirectToPage(pageId) {
      parent.postMessage({ type: "REDIRECT", pageId }, "*");
    }

    window.onerror = (msg, src, line, col, err) => {
      parent.postMessage({
        type: "IFRAME_ERROR",
        error: { message: err?.message || msg }
      }, "*");
    };

    window.onload = () => {
      parent.__runPageEnter(PAGE_ID);
    };
  </script>
</body>
</html>
`;
}

function showPage(pageId: string) {
  if (!iframeEl) return;
  iframeEl.srcdoc = buildPreviewHtml(pageId);
}

/* ---------- 이동 ---------- */
export function navigateToPage(pageId: string) {
  const page = getPageById(pageId);
  if (!page) return;

  lockPage(pageId);
  history.pushState({}, "", page.route);
  showPage(pageId);
}

export function redirectToPage(pageId: string) {
  const page = getPageById(pageId);
  if (!page) return;

  lockPage(pageId);
  history.replaceState({}, "", page.route);
  showPage(pageId);
}

/* ---------- URL 진입 ---------- */
export function handleUrlEntry(path: string) {
  const pageId = getPageIdByUrl(path);
  if (pageId) showPage(pageId);
}

/* ---------- iframe 메시지 ---------- */
export function initIframeBridge() {
  window.addEventListener("message", e => {
    const data = e.data;
    if (!data) return;

    if (data.type === "NAVIGATE") navigateToPage(data.pageId);
    if (data.type === "REDIRECT") redirectToPage(data.pageId);
    if (data.type === "IFRAME_ERROR") {
      alert("미리보기 오류:\n" + data.error.message);
    }
  });
}

/* ---------- 라이프사이클 ---------- */
let onEnterHandler: ((pageId: string) => void) | null = null;

export function onPageEnter(fn: (pageId: string) => void) {
  onEnterHandler = fn;
}

// iframe에서 호출
// @ts-ignore
window.__runPageEnter = function (pageId: string) {
  try {
    onEnterHandler?.(pageId);
  } catch (e) {
    console.error(e);
  }
};
