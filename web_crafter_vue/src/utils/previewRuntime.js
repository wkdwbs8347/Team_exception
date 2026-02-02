// src/utils/previewRuntime.js
export function buildWcPreviewSrcdoc({
  structureHtml = '',
  styleRaw = '',
  positionsMap = {},
  isRunning = false,

  webId = '',
  pageId = '',
  pageRoute = '',
  scaleRatio = 1,

  animationKeyframes = '',
  authRuntimeJs = '',
  valueRuntimeJs = '',
  logicJs = '',

  enableDrag = true,
  dragRuntimeJs = '',
  enableSnap = true,
}) {
  const extractStyleTagsOnly = (raw) => {
    if (!raw) return '';
    const matches = raw.match(/<style[^>]*>[\s\S]*?<\/style>/gi);
    return matches ? matches.join('\n') : '';
  };

  // positionsMap -> CSS (혹시 data-block-id selector가 직접 먹는 케이스 대비)
  let fixedPositionCss = '';
  if (positionsMap) {
    Object.keys(positionsMap).forEach((blockId) => {
      const pos = positionsMap[blockId];
      if (pos && typeof pos.x === 'number' && typeof pos.y === 'number') {
        fixedPositionCss += `
          [data-block-id="${blockId}"] { 
            position: absolute !important; 
            left: ${pos.x}px !important; 
            top: ${pos.y}px !important; 
            transform: none !important;
            z-index: 100;
          }
        `;
      }
    });
  }

  const styleCodeForPreview = extractStyleTagsOnly(styleRaw);

  const safeScript =
    logicJs?.trim() && !logicJs.includes('<script')
      ? `<script>${logicJs}<\/script>`
      : logicJs || '';

  const authRuntimeScript =
    isRunning && authRuntimeJs ? `<script>${authRuntimeJs}<\/script>` : '';

  const navRuntimeScript = isRunning
    ? `<script>
(function () {
  if (window.__WC_NAV_RUNTIME__) return;
  window.__WC_NAV_RUNTIME__ = true;

  window.goToPage = function (pageId) {
    try {
      var pid = String(pageId || '');
      if (!pid) return;

      // IDE 프리뷰(iframe)
      if (window.parent && window.parent !== window) {
        window.parent.postMessage({ type: 'NAVIGATE', pageId: pid }, '*');
        return;
      }

      // 단독 런타임 fallback
      if (pid.startsWith('/')) {
        location.href = pid;
        return;
      }
      location.href = '/' + pid;
    } catch (e) {
      console.error('[goToPage] failed:', e);
    }
  };
})();
<\/script>`
    : '';

  const valueRuntimeScript =
    isRunning && valueRuntimeJs ? `<script>${valueRuntimeJs}<\/script>` : '';

  const finalLogicScript = isRunning ? safeScript : '';

  const webIdBootstrapScript = `<script>
    window.WC_WEB_ID = ${JSON.stringify(String(webId ?? ''))};
  <\/script>`;

  const positionsJSON = JSON.stringify(positionsMap || {});
  const finalBodyClass = `${isRunning ? 'is-running' : 'is-design'} is-pc-mode`;

  // ✅ 드래그 런타임은 "디자인 모드"에서만 주입
  const dragScript =
    !isRunning && enableDrag && dragRuntimeJs?.trim()
      ? `<script>${dragRuntimeJs}<\/script>`
      : '';

  const srcdoc = [
    '<!DOCTYPE html><html><head><meta charset="utf-8">',

    '<style>',
    'html, body { margin:0; padding:0; width:100%; height:100%; overflow-y:auto; overflow-x:hidden; background:#fff; }',
    '* { box-sizing: border-box; }',
    '#wrapper { width:100%; min-height:100vh; position:relative; background:#fff; }',

    // ✅ 최상위 draggable은 항상 absolute + (0,0) 기본값
    '#wrapper > [data-draggable="true"][data-block-id] { position:absolute; left:0; top:0; transform:none; touch-action:none; user-select:none; -webkit-user-select:none; cursor:grab; }',

    // ✅ 하이라이트: "position을 절대 건드리지 않는다" (드래그 시 width 100% 되는 원인 제거)
    '.wc-highlight { outline:2px solid #ff4081 !important; box-shadow:0 0 0 2px #ff4081 !important; z-index: 2147483647 !important; }',

    // ✅ 그래도 다른 CSS가 position을 바꾸면 방어: 드래그 대상은 하이라이트 붙어도 absolute 유지
    '#wrapper > [data-draggable="true"][data-block-id].wc-highlight { position:absolute !important; }',

    '[data-wc-block] { position: relative; min-width: 50px; min-height: 50px; }',

    // ✅ 스냅 가이드 라인
    '.wc-guide-line { position:absolute; z-index: 10000; pointer-events:none; display:none; border-color: rgba(255, 0, 0, 0.75); border-style: dashed; }',
    '.wc-guide-v { width:0; border-left-width:1px; }',
    '.wc-guide-h { height:0; border-top-width:1px; }',

    fixedPositionCss,
    '</style>',

    animationKeyframes
      ? `<style id="anim-defs">${animationKeyframes}</style>`
      : '',
    '<style>body.is-design * { animation: none !important; transition: none !important; }</style>',
    styleCodeForPreview,

    '</head>',
    `<body class="${finalBodyClass}">`,
    '<div id="wrapper">',
    structureHtml,
    '<div id="wcGuideV" class="wc-guide-line wc-guide-v"></div>',
    '<div id="wcGuideH" class="wc-guide-line wc-guide-h"></div>',
    '</div>',

    webIdBootstrapScript,
    authRuntimeScript,
    valueRuntimeScript,
    navRuntimeScript,
    finalLogicScript,

    // ✅ builder runtime
    '<script>',
    `const WC_POSITIONS = ${positionsJSON};`,
    `const isRunning = ${!!isRunning};`,
    `window.__WC_IS_RUNNING__ = ${!!isRunning};`,
    `const WC_SCALE = ${Number(scaleRatio) || 1};`,
    `const PAGE_ID = ${JSON.stringify(pageId || '')};`,
    `const PAGE_ROUTE = ${JSON.stringify(pageRoute || '')};`,

    // ✅ 스냅 on/off 전역 노출 (dragRuntimeJs가 참고 가능)
    `window.WC_ENABLE_SNAP = ${!!enableSnap};`,
    `window.WC_SCALE = WC_SCALE;`,

    // ✅ builder style 적용
    'function applyBuilderStyles(){ const nodes = document.querySelectorAll("[data-wc-style]"); nodes.forEach(el => { el.style.cssText += ";" + (el.getAttribute("data-wc-style")||""); }); }',

    // ✅ 저장된 positionsMap 반영 (최상위 draggable만)
    'function applyPositions(){ const wrap = document.getElementById("wrapper"); if(!wrap) return; const targets = wrap.querySelectorAll(":scope > [data-draggable=\\\'true\\\']"); targets.forEach(el => { const id = el.getAttribute("data-block-id"); const p = WC_POSITIONS[id]; if(p && typeof p.x === "number" && typeof p.y === "number"){ el.style.setProperty("position","absolute","important"); el.style.setProperty("left", p.x+"px","important"); el.style.setProperty("top", p.y+"px","important"); el.style.setProperty("transform","none","important"); } }); }',

    // ✅ 가이드 라인 헬퍼
    'function hideGuides(){ const v = document.getElementById("wcGuideV"); const h = document.getElementById("wcGuideH"); if(v) v.style.display="none"; if(h) h.style.display="none"; }',
    'function showVSeg(x, y1, y2){ const v=document.getElementById("wcGuideV"); if(!v) return; v.style.left=x+"px"; v.style.top=Math.min(y1,y2)+"px"; v.style.height=Math.abs(y2-y1)+"px"; v.style.display="block"; }',
    'function showHSeg(y, x1, x2){ const h=document.getElementById("wcGuideH"); if(!h) return; h.style.top=y+"px"; h.style.left=Math.min(x1,x2)+"px"; h.style.width=Math.abs(x2-x1)+"px"; h.style.display="block"; }',

    // ✅ wrapper 기준 가이드 수집
    'function collectGuides(exceptEl){',
    '  const wrap = document.getElementById("wrapper");',
    '  if(!wrap) return { wrapRect:null, items:[] };',
    '  const wrapRect = wrap.getBoundingClientRect();',
    '  const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\\\'true\\\'][data-block-id]"))',
    '    .filter(el => el !== exceptEl);',
    '  return { wrapRect, items: els.map(el => {',
    '    const r = el.getBoundingClientRect();',
    '    const left = (r.left - wrapRect.left);',
    '    const right = (r.right - wrapRect.left);',
    '    const top = (r.top - wrapRect.top);',
    '    const bottom = (r.bottom - wrapRect.top);',
    '    return {',
    '      rect: { left, right, top, bottom, width:r.width, height:r.height },',
    '      v: [left, (left+right)/2, right],',
    '      h: [top, (top+bottom)/2, bottom]',
    '    };',
    '  })};',
    '}',

    // ✅ 스냅 계산
    'function computeSmartSnap({ nextLeft, nextTop, width, height, guides }){',
    '  guides = guides || { items:[] };',
    '  const curLeft = nextLeft, curRight = nextLeft + width;',
    '  const curTop = nextTop, curBottom = nextTop + height;',
    '  const curCX = (curLeft + curRight) / 2;',
    '  const curCY = (curTop + curBottom) / 2;',
    '  const selfV = [{x:curLeft},{x:curCX},{x:curRight}];',
    '  const selfH = [{y:curTop},{y:curCY},{y:curBottom}];',
    '  let best = { dx:0, dy:0, vLine:null, hLine:null, vSeg:null, hSeg:null, vDist:6, hDist:6 };',
    '  (guides.items||[]).forEach(it => {',
    '    (it.v||[]).forEach(gx => selfV.forEach(sv => {',
    '      const d = Math.abs(gx - sv.x);',
    '      if(d < best.vDist){',
    '        best.vDist = d; best.dx = gx - sv.x;',
    '        best.vLine = gx;',
    '        best.vSeg = { y1: Math.min(curTop, it.rect.top), y2: Math.max(curBottom, it.rect.bottom) };',
    '      }',
    '    }));',
    '    (it.h||[]).forEach(gy => selfH.forEach(sh => {',
    '      const d = Math.abs(gy - sh.y);',
    '      if(d < best.hDist){',
    '        best.hDist = d; best.dy = gy - sh.y;',
    '        best.hLine = gy;',
    '        best.hSeg = { x1: Math.min(curLeft, it.rect.left), x2: Math.max(curRight, it.rect.right) };',
    '      }',
    '    }));',
    '  });',
    '  return best;',
    '}',

    // ✅ dragRuntimeJs가 바로 쓰도록 window에 API 제공
    'window.WC_GUIDE_API = { hideGuides, showVSeg, showHSeg, collectGuides, computeSmartSnap };',

    // ✅ 선택 하이라이트 런타임: pointerdown(캡처)에서 무조건 wc-highlight 부여
    'function clearHighlight(){ document.querySelectorAll(".wc-highlight").forEach(n => n.classList.remove("wc-highlight")); }',
    'function pickBlockElFromEvent(e){',
    '  if(!e) return null;',
    '  const t = e.target;',
    '  if(!t || !t.closest) return null;',
    '  return t.closest("[data-block-id]");',
    '}',
    'function setHighlight(el){',
    '  if(!el) return;',
    '  clearHighlight();',
    '  el.classList.add("wc-highlight");',
    '  window.__WC_SELECTED_BLOCK_ID__ = el.getAttribute("data-block-id") || "";',
    '}',
    'function installSelectionRuntime(){',
    '  document.addEventListener("pointerdown", function(e){',
    '    const el = pickBlockElFromEvent(e);',
    '    if(!el) return;',
    '    setHighlight(el);',
    '  }, true);',
    '}',

    // ✅ init
    'function init(){ applyBuilderStyles(); applyPositions(); if(!window.WC_ENABLE_SNAP) hideGuides(); }',
    'window.addEventListener("DOMContentLoaded", function(){ init(); installSelectionRuntime(); });',
    '<\/script>',

    // ✅ 드래그 런타임 삽입
    dragScript,

    '</body></html>',
  ]
    .filter(Boolean)
    .join('\n');

  return srcdoc;
}
