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

  enableDrag = true, // ✅ 추가
  dragRuntimeJs = '', // ✅ 추가 (IDE에서 쓰던 드래그 코드 문자열)
  enableSnap = true,
}) {
  const extractStyleTagsOnly = (raw) => {
    if (!raw) return '';
    const matches = raw.match(/<style[^>]*>[\s\S]*?<\/style>/gi);
    return matches ? matches.join('\n') : '';
  };

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

  const valueRuntimeScript =
    isRunning && valueRuntimeJs ? `<script>${valueRuntimeJs}<\/script>` : '';

  const finalLogicScript = isRunning ? safeScript : '';

  const webIdBootstrapScript = `<script>
    window.WC_WEB_ID = ${JSON.stringify(String(webId ?? ''))};
  <\/script>`;

  const positionsJSON = JSON.stringify(positionsMap || {});
  const finalBodyClass = `${isRunning ? 'is-running' : 'is-design'} is-pc-mode`;

  // ✅ 드래그 런타임(IDE에서 쓰던 것) 주입
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
    '#wrapper > [data-draggable="true"][data-block-id] { position: absolute; left: 0; top: 0; transform:none; touch-action:none; user-select:none; -webkit-user-select:none; cursor: grab; }',
    '.wc-highlight { outline:2px solid #ff4081 !important; z-index: 9999; }',
    '[data-wc-block] { position: relative; min-width: 50px; min-height: 50px; }',
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
    finalLogicScript,

    // ✅ builder runtime
    '<script>',
    `const WC_POSITIONS = ${positionsJSON};`,
    `const isRunning = ${!!isRunning};`,
    `window.__WC_IS_RUNNING__ = ${!!isRunning};`,
    `const WC_SCALE = ${Number(scaleRatio) || 1};`,
    `const PAGE_ID = ${JSON.stringify(pageId || '')};`,
    `const PAGE_ROUTE = ${JSON.stringify(pageRoute || '')};`,

    'function applyBuilderStyles(){ const nodes = document.querySelectorAll("[data-wc-style]"); nodes.forEach(el => { el.style.cssText += ";" + (el.getAttribute("data-wc-style")||""); }); }',
    'function applyPositions(){ const wrap = document.getElementById("wrapper"); if(!wrap) return; const targets = wrap.querySelectorAll(":scope > [data-draggable=\'true\']"); targets.forEach(el => { const id = el.getAttribute("data-block-id"); const p = WC_POSITIONS[id]; if(p && typeof p.x === "number"){ el.style.setProperty("position","absolute","important"); el.style.setProperty("left", p.x+"px","important"); el.style.setProperty("top", p.y+"px","important"); el.style.setProperty("transform","none","important"); } }); }',
    // ✅ 스냅 가이드 헬퍼들 (DRAG_RUNTIME_JS가 이거 보고 스냅 켬)
    'function hideGuides(){ const v = document.getElementById("wcGuideV"); const h = document.getElementById("wcGuideH"); if(v) v.style.display="none"; if(h) h.style.display="none"; }',
    'function showVSeg(x, y1, y2){ const v=document.getElementById("wcGuideV"); if(!v) return; v.style.left=x+"px"; v.style.top=Math.min(y1,y2)+"px"; v.style.height=Math.abs(y2-y1)+"px"; v.style.display="block"; }',
    'function showHSeg(y, x1, x2){ const h=document.getElementById("wcGuideH"); if(!h) return; h.style.top=y+"px"; h.style.left=Math.min(x1,x2)+"px"; h.style.width=Math.abs(x2-x1)+"px"; h.style.display="block"; }',

    // ✅ wrapper 기준으로 "다른 블록들"의 left/center/right / top/center/bottom 가이드 수집
    'function collectGuides(exceptEl){',
    '  const wrap = document.getElementById("wrapper");',
    '  if(!wrap) return { wrapRect:null, items:[] };',
    '  const wrapRect = wrap.getBoundingClientRect();',
    '  const els = Array.from(document.querySelectorAll("#wrapper > [data-draggable=\\\'true\\\'][data-block-id]"))',
    '    .filter(el => el !== exceptEl);',
    '  return { wrapRect, items: els.map(el => {',
    '    const r = el.getBoundingClientRect();',
    '    const left = r.left - wrapRect.left;',
    '    const right = r.right - wrapRect.left;',
    '    const top = r.top - wrapRect.top;',
    '    const bottom = r.bottom - wrapRect.top;',
    '    return {',
    '      rect: { left, right, top, bottom, width:r.width, height:r.height },',
    '      v: [left, (left+right)/2, right],',
    '      h: [top, (top+bottom)/2, bottom]',
    '    };',
    '  })};',
    '}',

    // ✅ 스냅 계산 (dx/dy + 가이드 라인 위치)
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
    'function init(){ applyBuilderStyles(); applyPositions(); }',
    'window.addEventListener("DOMContentLoaded", init);',
    '<\/script>',

    // ✅ 여기서 드래그 런타임 삽입 (핵심)
    dragScript,

    '</body></html>',
  ]
    .filter(Boolean)
    .join('\n');

  return srcdoc;
}
