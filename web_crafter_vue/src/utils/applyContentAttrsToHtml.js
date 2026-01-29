import { parseTargetToSelector } from '@/components/block/ContentAttr.vue';

export function applyContentAttrsToHtml(html, bundles) {
  if (!html || !bundles || bundles.length === 0) return html;

  const doc = new DOMParser().parseFromString(html, 'text/html');

  // ✅ helpers (text_segment용)
  const escapeHtml = (s) =>
    (s ?? '')
      .toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

  const escapeAttr = (s) =>
    (s ?? '').toString().replace(/&/g, '&amp;').replace(/"/g, '&quot;');

  const applyTextSegment = (el, op) => {
    const base = (op.base || '').trim(); // 기준 텍스트
    const add = (op.text || '').trim(); // ✅ ContentAttr에서 text로 저장함
    const cls = (op.cls || '').trim(); // ✅ ContentAttr에서 cls로 저장함
    const pos = (op.mode || 'after').toLowerCase(); // ✅ ContentAttr에서 mode로 저장함

    if (!base || !add) return;

    // ✅ "현재 요소의 텍스트" 기준으로 base 찾기
    const full = el.textContent || '';
    const idx = full.indexOf(base);
    if (idx < 0) return;

    const before = full.slice(0, idx);
    const after = full.slice(idx + base.length);
    const classAttr = cls ? ` class="${escapeAttr(cls)}"` : '';

    // ✅ 추가 텍스트는 span으로 감싸기 (코드보기에서 보여도 OK)
    const segSpan = `<span data-wc-seg="${escapeAttr(base)}"${classAttr}>${escapeHtml(add)}</span>`;

    let out = '';
    if (pos === 'before') {
      out = `${escapeHtml(before)}${segSpan}${escapeHtml(base)}${escapeHtml(after)}`;
    } else if (pos === 'replace') {
      out = `${escapeHtml(before)}${segSpan}${escapeHtml(after)}`;
    } else {
      out = `${escapeHtml(before)}${escapeHtml(base)}${segSpan}${escapeHtml(after)}`;
    }

    el.innerHTML = out;
  };

  for (const bundle of bundles) {
    const selector = parseTargetToSelector(bundle.target);
    if (!selector) continue;

    const elements = doc.querySelectorAll(selector);
    if (!elements || elements.length === 0) continue;

    elements.forEach((el) => {
      bundle.ops.forEach((op) => {
        switch (op.t) {
          case 'id':
            if (op.v) el.id = op.v;
            break;

          case 'class_add':
            if (op.v) el.classList.add(...op.v.split(/\s+/).filter(Boolean));
            break;

          case 'data':
            if (op.k) el.setAttribute(`data-${op.k}`, op.v ?? '');
            break;

          case 'aria':
            if (op.k) el.setAttribute(`aria-${op.k}`, op.v ?? '');
            break;

          case 'placeholder':
            el.setAttribute('placeholder', op.v ?? '');
            break;

          case 'value':
            el.setAttribute('value', op.v ?? '');
            break;

          case 'required':
            el.setAttribute('required', '');
            break;

          case 'disabled':
            el.setAttribute('disabled', '');
            break;

          case 'readonly':
            el.setAttribute('readonly', '');
            break;

          case 'target_blank':
            el.setAttribute('target', '_blank');
            break;

          case 'rel_noopener': {
            const prev = (el.getAttribute('rel') || '').split(/\s+/);
            const set = new Set(prev.filter(Boolean));
            set.add('noopener');
            set.add('noreferrer');
            el.setAttribute('rel', [...set].join(' '));
            break;
          }

          case 'for':
            if (op.v) el.setAttribute('for', op.v);
            break;

          case 'server_field':
            if (op.v) {
              el.setAttribute('name', op.v);
              el.setAttribute('data-wc-field', op.v);
            }
            break;

          case 'dup_target':
            if (op.v) {
              el.setAttribute('data-wc-action', 'duplicate-check');
              el.setAttribute('data-wc-target', op.v);
            }
            break;

          case 'style':
            if (op.v) {
              const prev = el.getAttribute('style') || '';
              const merged = prev
                ? (prev.trim().endsWith(';') ? prev : prev + ';') + op.v
                : op.v;
              el.setAttribute('style', merged);
            }
            break;

          // ✅ 추가: 텍스트 세그먼트(span 삽입)
          case 'text_segment':
            applyTextSegment(el, op);
            break;
        }
      });
    });
  }

  return doc.body.innerHTML.trim();
}
