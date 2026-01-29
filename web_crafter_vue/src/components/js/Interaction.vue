<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '동작',
  color: '#ff7043',
  icon: '⚡',
};

export const toolbox = `
<xml>
  <block type="script_tag"></block>
  <block type="event_element"></block>
  <block type="event_page_load"></block>

  <sep gap="16"></sep>
  <block type="action_alert"></block>
  <block type="action_modal_alert"></block>

  <!-- ✅ 추가: 대상 선택(옵션) + 모달(오픈/클로즈/토글) -->
  <block type="target_select_class"></block>
  <block type="modal_state_class"></block>

  <block type="dom_change_text_class"></block>
  <block type="action_navigate_internal"></block>

  <sep gap="16"></sep>

  <block type="form_set_field"></block>

  <sep gap="16"></sep>
  <block type="auth_set_mode"></block>

  <sep gap="16"></sep>
  <block type="auth_duplicate_check_branch"></block>

  <sep gap="16"></sep>
  <block type="auth_register_call_fixed"></block>
  <block type="auth_login_call_fixed"></block>
</xml>
`;

// =========================================================
// ✅ Pretty Generator (코드보기 전용)
// =========================================================
export const javascriptGeneratorPretty = new Blockly.Generator(
  'JavaScriptPretty'
);

javascriptGeneratorPretty.ORDER_ATOMIC = javascriptGenerator.ORDER_ATOMIC;
javascriptGeneratorPretty.ORDER_NONE = javascriptGenerator.ORDER_NONE;
javascriptGeneratorPretty.ORDER_FUNCTION_CALL =
  javascriptGenerator.ORDER_FUNCTION_CALL;

javascriptGeneratorPretty.init = function (workspace) {
  this.nameDB_ = new Blockly.Names(this.RESERVED_WORDS_);
  this.nameDB_.setVariableMap(workspace.getVariableMap());
};

javascriptGeneratorPretty.finish = function (code) {
  return (code || '').trim() + '\n';
};

// Pretty util
const _q = (s) => JSON.stringify(String(s ?? ''));
const _indent = (code, pad = '  ') =>
  (code || '')
    .split('\n')
    .map((l) => (l.trim() ? pad + l : l))
    .join('\n');

const _stripTrailing = (s) => (s || '').replace(/\s+$/g, '');

export const defineBlocks = () => {
  /* =========
    1) 요소 이벤트 (클래스 기반)
  ========= */
  Blockly.Blocks['event_element'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('⚡ 클래스가')
        .appendField(new Blockly.FieldTextInput('btn'), 'TARGET_CLASS')
        .appendField('인 요소')
        .appendField(
          new Blockly.FieldDropdown([
            ['클릭했을 때', 'click'],
            ['마우스 올렸을 때', 'mouseover'],
            ['마우스 나갔을 때', 'mouseout'],
            ['더블클릭했을 때', 'dblclick'],
            ['우클릭했을 때', 'contextmenu'],
          ]),
          'WHEN'
        )
        .appendField('실행');

      this.appendStatementInput('DO').setCheck(null);

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip(
        '해당 클래스를 가진 요소에서 이벤트가 발생했을 때 실행됩니다.'
      );
    },
  };

  // ===== RUN
  javascriptGenerator.forBlock['event_element'] = function (block, generator) {
    const targetClass = (block.getFieldValue('TARGET_CLASS') || '').trim();
    const when = block.getFieldValue('WHEN') || 'click';
    const body = generator.statementToCode(block, 'DO');
    if (!targetClass) return '';

    const shouldPrevent =
      when === 'click' || when === 'dblclick' || when === 'contextmenu';

    return `(function() {
  document.addEventListener('${when}', function(e) {
    var target = e.target && e.target.closest ? e.target.closest('.${targetClass}') : null;
    if (target) {
      ${
        shouldPrevent
          ? `
      try { e.preventDefault?.(); } catch(_) {}
      try { e.stopPropagation?.(); } catch(_) {}
      `
          : ''
      }

      window.__WC_LAST_EVENT_TARGET__ = target;
      window.__WC_LAST_EVENT__ = e;

      // ✅ 기본 대상 = "자기 자신"
      window.__WC_TARGET__ = target;
      window.__WC_TARGET_KIND__ = 'self';
      window.__WC_TARGET_CLASS__ = '';

${body}
    }
  }, true);
})();\n`;
  };

  // ===== PRETTY
  javascriptGeneratorPretty.forBlock['event_element'] = function (
    block,
    generator
  ) {
    const cls = (block.getFieldValue('TARGET_CLASS') || '').trim();
    const when = block.getFieldValue('WHEN') || 'click';
    const body = generator.statementToCode(block, 'DO');
    if (!cls) return '';

    const shouldPrevent =
      when === 'click' || when === 'dblclick' || when === 'contextmenu';

    return (
      _stripTrailing(
        `document.addEventListener('${when}', (e) => {
  const el = e.target?.closest?.('.${cls}');
  if (!el) return;
  ${shouldPrevent ? `e.preventDefault?.(); e.stopPropagation?.();` : ``}

  window.__WC_LAST_EVENT_TARGET__ = el;
  window.__WC_LAST_EVENT__ = e;

  window.__WC_TARGET__ = el;
  window.__WC_TARGET_KIND__ = 'self';
  window.__WC_TARGET_CLASS__ = '';

${_indent(body, '  ')}
}, true);\n`
      ) + '\n'
    );
  };

  /* =========
    ✅ 추가 A) 대상 선택(클래스)
    - 기본 텍스트: "엘리먼트"
    - 안 쓰면: event_element의 기본 대상(self)이 그대로 유지됨
  ========= */
  Blockly.Blocks['target_select_class'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🎯 대상 선택')
        .appendField(new Blockly.FieldTextInput('엘리먼트'), 'TARGET_CLASS');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip(
        '이 아래 동작 블록들이 바꿀 "대상"을 클래스 기준으로 지정합니다. (비우면 자기 자신)'
      );
    },
  };

  // RUN
  javascriptGenerator.forBlock['target_select_class'] = function (block) {
    const cls = (block.getFieldValue('TARGET_CLASS') || '').trim();

    // "엘리먼트"는 안내용 기본값이니까, 실제로는 빈 값 취급(= 자기 자신)
    const real = cls === '엘리먼트' ? '' : cls;

    return `(function(){
  window.__WC_TARGET_KIND__ = ${real ? `'class'` : `'self'`};
  window.__WC_TARGET_CLASS__ = ${JSON.stringify(real)};
  if(${real ? 'false' : 'true'}) {
    window.__WC_TARGET__ = window.__WC_LAST_EVENT_TARGET__ || window.__WC_TARGET__ || null;
  } else {
    window.__WC_TARGET__ = null;
  }
})();\n`;
  };

  // PRETTY
  javascriptGeneratorPretty.forBlock['target_select_class'] = function (block) {
    const cls = (block.getFieldValue('TARGET_CLASS') || '').trim();
    const real = cls === '엘리먼트' ? '' : cls;

    return (
      _stripTrailing(
        `{
  const cls = ${_q(real)};
  window.__WC_TARGET_KIND__ = cls ? 'class' : 'self';
  window.__WC_TARGET_CLASS__ = cls;
  if (!cls) window.__WC_TARGET__ = window.__WC_LAST_EVENT_TARGET__ ?? window.__WC_TARGET__ ?? null;
  else window.__WC_TARGET__ = null;
}\n`
      ) + '\n'
    );
  };

  /* =========
    ✅ 추가 B) 모달 띄우기/닫기/토글
    - UI: "모달 / 클래스(비우면 대상) / 상태(open|close|toggle)"
    - 실제 동작:
      open  -> class 'open' 추가, 'close' 제거, display:flex
      close -> class 'close' 추가, 'open' 제거, display:none
      toggle-> open/close 반전
  ========= */
  Blockly.Blocks['modal_state_class'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('✅ 커스텀 모달')
        .appendField('이름')
        .appendField(new Blockly.FieldTextInput(''), 'MODAL_CLASS')
        .appendField('상태')
        .appendField(
          new Blockly.FieldDropdown([
            ['열기(open)', 'open'],
            ['닫기(close)', 'close'],
            ['토글(toggle)', 'toggle'],
          ]),
          'STATE'
        );
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip(
        '모달(오버레이) 요소에 open/close 클래스와 display를 적용합니다. 클래스 비우면 현재 대상(또는 자기 자신)에 적용됩니다.'
      );
    },
  };

  // RUN
  javascriptGenerator.forBlock['modal_state_class'] = function (block) {
    const modalClass = (block.getFieldValue('MODAL_CLASS') || '').trim();
    const state = block.getFieldValue('STATE') || 'open';
    const safeClass = JSON.stringify(modalClass);

    return `(function(){
  // ✅ 대상 해석: (1) 입력 클래스 우선 (2) target_select_class로 지정된 클래스 (3) 자기 자신
  function __wcResolveTargets(){
    var inputCls = (${safeClass} || "").trim();
    if(inputCls){
      return Array.from(document.querySelectorAll('.' + inputCls));
    }
    var kind = (window.__WC_TARGET_KIND__ || 'self');
    var cls = (window.__WC_TARGET_CLASS__ || '').trim();
    if(kind === 'class' && cls){
      return Array.from(document.querySelectorAll('.' + cls));
    }
    var self = window.__WC_TARGET__ || window.__WC_LAST_EVENT_TARGET__ || null;
    return self ? [self] : [];
  }

  function __wcOpen(el){
    try{
      el.classList.add('open');
      el.classList.remove('close');
      el.style.display = 'flex';
      el.setAttribute('aria-hidden','false');
    }catch(_){}
  }

  function __wcClose(el){
    try{
      el.classList.add('close');
      el.classList.remove('open');
      el.style.display = 'none';
      el.setAttribute('aria-hidden','true');
    }catch(_){}
  }

  var targets = __wcResolveTargets();
  if(!targets.length) return;

  targets.forEach(function(el){
    if(!el) return;

    if(${JSON.stringify(state)} === 'open'){
      __wcOpen(el);
      return;
    }
    if(${JSON.stringify(state)} === 'close'){
      __wcClose(el);
      return;
    }
    // toggle
    var isOpen = false;
    try{
      isOpen = el.classList.contains('open') && el.style.display !== 'none';
    }catch(_){}
    if(isOpen) __wcClose(el);
    else __wcOpen(el);
  });
})();\n`;
  };

  // PRETTY
  javascriptGeneratorPretty.forBlock['modal_state_class'] = function (block) {
    const modalClass = (block.getFieldValue('MODAL_CLASS') || '').trim();
    const state = block.getFieldValue('STATE') || 'open';

    return (
      _stripTrailing(
        `{
  const inputCls = ${_q(modalClass)}.trim();
  const kind = window.__WC_TARGET_KIND__ || 'self';
  const cls = (window.__WC_TARGET_CLASS__ || '').trim();

  const targets = inputCls
    ? Array.from(document.querySelectorAll('.' + inputCls))
    : (kind === 'class' && cls)
      ? Array.from(document.querySelectorAll('.' + cls))
      : ((window.__WC_TARGET__ || window.__WC_LAST_EVENT_TARGET__) ? [window.__WC_TARGET__ || window.__WC_LAST_EVENT_TARGET__] : []);

  const open = (el) => { el.classList.add('open'); el.classList.remove('close'); el.style.display = 'flex'; el.setAttribute('aria-hidden','false'); };
  const close = (el) => { el.classList.add('close'); el.classList.remove('open'); el.style.display = 'none'; el.setAttribute('aria-hidden','true'); };

  targets.forEach((el) => {
    if (!el) return;
    if (${_q(state)} === 'open') return open(el);
    if (${_q(state)} === 'close') return close(el);
    const isOpen = el.classList.contains('open') && el.style.display !== 'none';
    isOpen ? close(el) : open(el);
  });
}\n`
      ) + '\n'
    );
  };

  /* =========
    2) 페이지 로드 이벤트
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init: function () {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때');
      this.appendStatementInput('DO').setCheck(null);
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    },
  };

  javascriptGenerator.forBlock['event_page_load'] = function (
    block,
    generator
  ) {
    const body = generator.statementToCode(block, 'DO');
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`;
  };

  javascriptGeneratorPretty.forBlock['event_page_load'] = function (
    block,
    generator
  ) {
    const body = generator.statementToCode(block, 'DO');
    return (
      _stripTrailing(
        `window.addEventListener('DOMContentLoaded', () => {
${_indent(body, '  ')}
});\n`
      ) + '\n'
    );
  };

  /* =========
    3) 알림 액션
  ========= */
  Blockly.Blocks['action_alert'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🔔 알림')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE');

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    },
  };

  javascriptGenerator.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    return `alert(${JSON.stringify(msg)});\n`;
  };

  javascriptGeneratorPretty.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    return `alert(${_q(msg)});\n`;
  };

  /* =========
  ✅ 모달 알림(확인/취소 + 분기) - alert 확장판
========= */
  Blockly.Blocks['action_modal_alert'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🔔 모달알림')
        .appendField('메시지')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE');

      this.appendDummyInput()
        .appendField('확인 버튼')
        .appendField(
          new Blockly.FieldDropdown([
            ['ON', 'ON'],
            ['OFF', 'OFF'],
          ]),
          'OK_ON'
        )
        .appendField('텍스트')
        .appendField(new Blockly.FieldTextInput('확인'), 'OK_TEXT');

      this.appendDummyInput()
        .appendField('취소 버튼')
        .appendField(
          new Blockly.FieldDropdown([
            ['ON', 'ON'],
            ['OFF', 'OFF'],
          ]),
          'CANCEL_ON'
        )
        .appendField('텍스트')
        .appendField(new Blockly.FieldTextInput('취소'), 'CANCEL_TEXT');

      this.appendStatementInput('ON_OK')
        .setCheck(null)
        .appendField('✅ 확인 눌렀을 때');
      this.appendStatementInput('ON_CANCEL')
        .setCheck(null)
        .appendField('❌ 취소/닫힘(ESC/배경/X)');

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip(
        '텍스트만으로 기본 모달을 띄우고, 확인/취소 클릭에 따른 분기 슬롯을 실행합니다. (자동 생성)'
      );
    },
  };

  // RUN
  javascriptGenerator.forBlock['action_modal_alert'] = function (
    block,
    generator
  ) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    const okOn = block.getFieldValue('OK_ON') || 'ON';
    const cancelOn = block.getFieldValue('CANCEL_ON') || 'ON';
    const okText = block.getFieldValue('OK_TEXT') ?? '확인';
    const cancelText = block.getFieldValue('CANCEL_TEXT') ?? '취소';

    const onOk = generator.statementToCode(block, 'ON_OK') || '';
    const onCancel = generator.statementToCode(block, 'ON_CANCEL') || '';

    return `(function(){
  var MESSAGE = ${JSON.stringify(String(msg ?? ''))};
  var OK_ON = ${JSON.stringify(okOn)};
  var CANCEL_ON = ${JSON.stringify(cancelOn)};
  var OK_TEXT = ${JSON.stringify(String(okText ?? '확인'))};
  var CANCEL_TEXT = ${JSON.stringify(String(cancelText ?? '취소'))};

  // 1) 기본 CSS 1회 주입 (좀 더 깔끔한 스타일)
  var STYLE_ID = '__wc_auto_modal_style_v2__';
  if(!document.getElementById(STYLE_ID)){
    var st = document.createElement('style');
    st.id = STYLE_ID;
    st.textContent = \`
/* ===== WebCrafter Auto Modal v2 (깔끔) ===== */
.__wc_auto_modal__{
  position: fixed; inset: 0;
  display: none;
  align-items: center; justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, .55); /* slate-900 느낌 */
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  z-index: 999999;
}
.__wc_auto_modal__.open{ display:flex; }

.__wc_auto_modal__ .__wc_auto_panel__{
  position: relative;
  width: min(520px, 92vw);
  background: rgba(255,255,255,.95);
  border: 1px solid rgba(0,0,0,.06);
  border-radius: 18px;
  padding: 18px 18px 14px;
  box-shadow: 0 18px 60px rgba(0,0,0,.35);
  transform: translateY(6px);
  opacity: .98;
}

.__wc_auto_modal__ .__wc_auto_header__{
  display:flex; align-items:center; justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}
.__wc_auto_modal__ .__wc_auto_title__{
  font-weight: 800;
  font-size: 16px;
  letter-spacing: -0.2px;
  color: rgba(2,6,23,.92);
}
.__wc_auto_modal__ .__wc_auto_close__{
  width: 36px; height: 36px;
  border-radius: 12px;
  border: 1px solid rgba(0,0,0,.06);
  background: rgba(0,0,0,.03);
  cursor:pointer;
  font-size: 14px;
}
.__wc_auto_modal__ .__wc_auto_close__:hover{
  background: rgba(0,0,0,.07);
}

.__wc_auto_modal__ .__wc_auto_body__{
  font-size: 14px;
  line-height: 1.55;
  color: rgba(2,6,23,.82);
  white-space: pre-wrap;
  margin: 6px 0 14px;
}

.__wc_auto_modal__ .__wc_auto_footer__{
  display:flex;
  justify-content:flex-end;
  gap: 10px;
  padding-top: 12px;
  border-top: 1px solid rgba(0,0,0,.06);
}

.__wc_auto_modal__ .__wc_btn__{
  border: 1px solid rgba(0,0,0,.08);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 14px;
  cursor: pointer;
  transition: transform .06s ease, background .12s ease;
}
.__wc_auto_modal__ .__wc_btn__:active{ transform: translateY(1px); }

.__wc_auto_modal__ .__wc_btn_ok__{
  background: rgba(79,70,229,.95); /* indigo */
  border-color: rgba(79,70,229,.95);
  color: #fff;
}
.__wc_auto_modal__ .__wc_btn_ok__:hover{ background: rgba(79,70,229,1); }

.__wc_auto_modal__ .__wc_btn_cancel__{
  background: rgba(0,0,0,.04);
  color: rgba(2,6,23,.84);
}
.__wc_auto_modal__ .__wc_btn_cancel__:hover{ background: rgba(0,0,0,.08); }
\`;
    document.head.appendChild(st);
  }

  // 2) 모달 DOM 1회 생성
  var MODAL_ID = '__wc_auto_modal__';
  var modal = document.getElementById(MODAL_ID);

  if(!modal){
    modal = document.createElement('div');
    modal.id = MODAL_ID;
    modal.className = '__wc_auto_modal__';
    modal.setAttribute('aria-hidden','true');

    modal.innerHTML = \`
      <div class="__wc_auto_panel__" role="dialog" aria-modal="true">
        <div class="__wc_auto_header__">
          <div class="__wc_auto_title__">알림</div>
          <button type="button" class="__wc_auto_close__" aria-label="close">✕</button>
        </div>
        <div class="__wc_auto_body__"></div>
        <div class="__wc_auto_footer__"></div>
      </div>
    \`;
    document.body.appendChild(modal);
  }

  // 3) 공용 함수
  function __wcClose(){
    try{
      modal.classList.remove('open');
      modal.style.display = 'none';
      modal.setAttribute('aria-hidden','true');
      document.body.style.overflow = '';
    }catch(_){}
  }

  // 4) 동적 바인딩(매번 새 메시지/버튼/콜백 반영)
  //    - 기존 핸들러 중복 방지: dataset 키로 1회만 바인딩 + 현재 콜백은 window에 저장
  window.__WC_AUTO_MODAL_OK__ = function(){
    try{ __wcClose(); }catch(_){}
    try{ ${onOk} }catch(_){}
  };
  window.__WC_AUTO_MODAL_CANCEL__ = function(){
    try{ __wcClose(); }catch(_){}
    try{ ${onCancel} }catch(_){}
  };

  // 메시지
  var body = modal.querySelector('.__wc_auto_body__');
  if(body) body.textContent = String(MESSAGE ?? '');

  // footer 버튼 렌더
  var footer = modal.querySelector('.__wc_auto_footer__');
  if(footer){
    footer.innerHTML = '';
    if(CANCEL_ON === 'ON'){
      var bCancel = document.createElement('button');
      bCancel.type = 'button';
      bCancel.className = '__wc_btn__ __wc_btn_cancel__';
      bCancel.textContent = String(CANCEL_TEXT || '취소');
      bCancel.addEventListener('click', function(){ window.__WC_AUTO_MODAL_CANCEL__ && window.__WC_AUTO_MODAL_CANCEL__(); });
      footer.appendChild(bCancel);
    }
    if(OK_ON === 'ON'){
      var bOk = document.createElement('button');
      bOk.type = 'button';
      bOk.className = '__wc_btn__ __wc_btn_ok__';
      bOk.textContent = String(OK_TEXT || '확인');
      bOk.addEventListener('click', function(){ window.__WC_AUTO_MODAL_OK__ && window.__WC_AUTO_MODAL_OK__(); });
      footer.appendChild(bOk);
    }
    // 버튼이 모두 OFF면 footer 숨김 느낌
    if(OK_ON !== 'ON' && CANCEL_ON !== 'ON'){
      footer.style.display = 'none';
    } else {
      footer.style.display = 'flex';
    }
  }

  // close 버튼/overlay/ESC 바인딩(중복 방지)
  if(!modal.dataset.wcBound){
    modal.dataset.wcBound = '1';

    // X 클릭 = cancel
    modal.querySelector('.__wc_auto_close__')?.addEventListener('click', function(){
      window.__WC_AUTO_MODAL_CANCEL__ && window.__WC_AUTO_MODAL_CANCEL__();
    });

    // overlay 클릭(패널 밖) = cancel
    modal.addEventListener('click', function(e){
      if(e.target === modal){
        window.__WC_AUTO_MODAL_CANCEL__ && window.__WC_AUTO_MODAL_CANCEL__();
      }
    });

    // ESC = cancel
    document.addEventListener('keydown', function(e){
      if(e.key === 'Escape'){
        var m = document.getElementById('__wc_auto_modal__');
        if(m && m.classList.contains('open')){
          window.__WC_AUTO_MODAL_CANCEL__ && window.__WC_AUTO_MODAL_CANCEL__();
        }
      }
    });
  }

  // 5) open
  try{
    modal.classList.add('open');
    modal.style.display = 'flex';
    modal.setAttribute('aria-hidden','false');
    document.body.style.overflow = 'hidden';
  }catch(_){}
})();\n`;
  };

  // PRETTY (코드보기용: 보기 좋게만)
  javascriptGeneratorPretty.forBlock['action_modal_alert'] = function (
    block,
    generator
  ) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    const okOn = block.getFieldValue('OK_ON') || 'ON';
    const cancelOn = block.getFieldValue('CANCEL_ON') || 'ON';
    const okText = block.getFieldValue('OK_TEXT') ?? '확인';
    const cancelText = block.getFieldValue('CANCEL_TEXT') ?? '취소';

    const onOk = generator.statementToCode(block, 'ON_OK') || '';
    const onCancel = generator.statementToCode(block, 'ON_CANCEL') || '';

    return (
      _stripTrailing(
        `showModal({
  message: ${_q(msg)},
  ok: ${okOn === 'ON' ? _q(okText) : 'false'},
  cancel: ${cancelOn === 'ON' ? _q(cancelText) : 'false'},
  onOk: () => {
${_indent(onOk, '    ')}
  },
  onCancel: () => {
${_indent(onCancel, '    ')}
  }
});\n`
      ) + '\n'
    );
  };

  /* =========
    4) 페이지 이동 액션
  ========= */
  Blockly.Blocks['action_navigate_internal'] = {
    init: function () {
      this.appendDummyInput('DUMMY')
        .appendField('페이지 이동하기 📄')
        .appendField(
          new Blockly.FieldDropdown(function () {
            return window.WC_GET_PAGES
              ? window.WC_GET_PAGES()
              : [['로딩중...', '']];
          }),
          'PAGE_ID'
        );

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('이동할 페이지를 선택하세요.');
    },
  };

  javascriptGenerator.forBlock['action_navigate_internal'] = function (block) {
    const pageId = block.getFieldValue('PAGE_ID');
    if (!pageId) return '';
    return `goToPage('${pageId}');\n`;
  };

  javascriptGeneratorPretty.forBlock['action_navigate_internal'] = function (
    block
  ) {
    const pageId = block.getFieldValue('PAGE_ID');
    if (!pageId) return '';
    return `goToPage(${_q(pageId)});\n`;
  };

  /* =========
    0) 스크립트 태그 래퍼
  ========= */
  Blockly.Blocks['script_tag'] = {
    init: function () {
      this.appendDummyInput().appendField('📜 스크립트');
      this.appendStatementInput('BODY').setCheck(null).appendField('내용');
      this.setColour('#ff7043');
      this.setTooltip('여기에 이벤트와 동작을 넣으세요.');
      this.setStyle('hat_blocks');
    },
  };

  javascriptGenerator.forBlock['script_tag'] = function (block, generator) {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };

  javascriptGeneratorPretty.forBlock['script_tag'] = function (
    block,
    generator
  ) {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };

  /* =========
    5) 요소 내용 바꾸기 (Class)  // ✅ 기존 그대로
  ========= */
  Blockly.Blocks['dom_change_text_class'] = {
    init: function () {
      this.jsonInit({
        type: 'dom_change_text_class',
        message0: '요소 내용 바꾸기 (Class: %1) ➡️ %2',
        args0: [
          { type: 'input_value', name: 'CLASS', check: 'String' },
          { type: 'input_value', name: 'TEXT', check: 'String' },
        ],
        previousStatement: null,
        nextStatement: null,
        colour: '#ff7043',
      });
    },
  };

  javascriptGenerator.forBlock['dom_change_text_class'] = function (
    block,
    generator
  ) {
    const className =
      generator.valueToCode(block, 'CLASS', generator.ORDER_NONE) || "''";
    const text =
      generator.valueToCode(block, 'TEXT', generator.ORDER_NONE) || "''";
    return `
(function(){
  var els = document.querySelectorAll('.' + ${className});
  els.forEach(function(el) {
    el.innerText = ${text};
  });
})();\n`;
  };

  javascriptGeneratorPretty.forBlock['dom_change_text_class'] = function (
    block,
    generator
  ) {
    const className =
      generator.valueToCode(block, 'CLASS', generator.ORDER_NONE) || "''";
    const text =
      generator.valueToCode(block, 'TEXT', generator.ORDER_NONE) || "''";
    return `document.querySelectorAll('.' + ${className}).forEach((el) => {
  el.innerText = ${text};
});\n`;
  };

  // =========================================================
  // ✅ 폼 값 "설정" (동작)만 유지  // ✅ 기존 그대로
  // =========================================================
  Blockly.Blocks['form_set_field'] = {
    init: function () {
      this.jsonInit({
        type: 'form_set_field',
        message0: '🧾 폼 값 설정 %1 = %2',
        args0: [
          { type: 'field_input', name: 'FIELD', text: 'password' },
          { type: 'input_value', name: 'VALUE', check: null },
        ],
        previousStatement: null,
        nextStatement: null,
        colour: '#ff7043',
        tooltip: '가장 가까운 form의 name/id에 값을 설정합니다.',
      });
    },
  };

  javascriptGenerator.forBlock['form_set_field'] = function (block, generator) {
    const field = (block.getFieldValue('FIELD') || '').trim();
    const safe = JSON.stringify(field || '');
    const val =
      generator.valueToCode(block, 'VALUE', generator.ORDER_NONE) || "''";
    return `(function(){
  try{
    var btn = window.__WC_LAST_EVENT_TARGET__ || null;
    var form = window.wcAuthFindForm ? window.wcAuthFindForm(btn) : (btn && btn.closest ? btn.closest('form') : null);
    if(!form) return;
    var el = form.querySelector('[name=' + ${safe} + ']') || form.querySelector('#' + ${safe});
    if(el) el.value = String(${val});
  }catch(e){}
})();\n`;
  };

  javascriptGeneratorPretty.forBlock['form_set_field'] = function (
    block,
    generator
  ) {
    const field = (block.getFieldValue('FIELD') || '').trim();
    const val =
      generator.valueToCode(block, 'VALUE', generator.ORDER_NONE) || "''";
    return (
      _stripTrailing(
        `{
  const btn = window.__WC_LAST_EVENT_TARGET__;
  const form = btn?.closest?.('form');
  const input = form?.querySelector('[name="${field}"], #${field}');
  if (input) input.value = String(${val});
}\n`
      ) + '\n'
    );
  };

  // ===== auth_set_mode (기존 그대로)
  Blockly.Blocks['auth_set_mode'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🔑 인증방식')
        .appendField(
          new Blockly.FieldDropdown([
            ['쿠키(세션)', 'cookie'],
            ['JWT', 'jwt'],
          ]),
          'MODE'
        )
        .appendField('토큰키')
        .appendField(new Blockly.FieldTextInput('wc_token'), 'KEY');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    },
  };

  javascriptGenerator.forBlock['auth_set_mode'] = function (block) {
    const mode = block.getFieldValue('MODE') || 'cookie';
    const key = (block.getFieldValue('KEY') || 'wc_token').trim();
    return `window.WC_AUTH_MODE=${JSON.stringify(mode)};\nwindow.WC_AUTH_TOKEN_KEY=${JSON.stringify(key)};\n`;
  };

  javascriptGeneratorPretty.forBlock['auth_set_mode'] = function (block) {
    const mode = block.getFieldValue('MODE') || 'cookie';
    const key = (block.getFieldValue('KEY') || 'wc_token').trim();
    return `window.WC_AUTH_MODE=${_q(mode)};\nwindow.WC_AUTH_TOKEN_KEY=${_q(key)};\n`;
  };

  /* =========================================================
    ✅ 중복확인 수행 (순수 로직 블록)  // ✅ 기존 그대로
  ========================================================= */
  Blockly.Blocks['auth_duplicate_check_branch'] = {
    init: function () {
      this.appendDummyInput().appendField('✅ 중복확인 API 호출');

      this.appendStatementInput('ON_AVAILABLE')
        .setCheck(null)
        .appendField('✅ 사용 가능');
      this.appendStatementInput('ON_UNAVAILABLE')
        .setCheck(null)
        .appendField('❌ 사용 중');
      this.appendStatementInput('ON_ERROR')
        .setCheck(null)
        .appendField('⚠️ 오류');

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip(
        '중복확인을 수행하고 결과에 따라 분기 슬롯을 실행합니다. (출력/알림 없음)'
      );
    },
  };

  javascriptGenerator.forBlock['auth_duplicate_check_branch'] = function (
    block,
    generator
  ) {
    const onA = generator.statementToCode(block, 'ON_AVAILABLE');
    const onU = generator.statementToCode(block, 'ON_UNAVAILABLE');
    const onE = generator.statementToCode(block, 'ON_ERROR');

    return `(function(){
  // 초기화
  window.__WC_DUP_OK__ = false;
  window.__WC_DUP_AVAILABLE__ = false;
  window.__WC_DUP_FIELD__ = null;
  window.__WC_DUP_ERROR__ = null;
  window.__WC_DUP_DATA__ = null;

  if(!window.wcAuthRequest || !window.wcAuthCollect || !window.wcAuthFindForm){
    window.__WC_DUP_ERROR__ = "Auth runtime이 없습니다.";
${onE}
    return;
  }

  var btn = window.__WC_LAST_EVENT_TARGET__ || null;
  if(!btn){
    window.__WC_DUP_ERROR__ = "이벤트 대상이 없습니다.";
${onE}
    return;
  }

  var field = (btn.getAttribute('data-wc-target') || "").trim();
  if(!field){
    window.__WC_DUP_ERROR__ = "data-wc-target이 없습니다.";
${onE}
    return;
  }

  var form = window.wcAuthFindForm(btn);
  var body = window.wcAuthCollect(form);
  var value = (body[field] || "").trim();

  if(!value){
    window.__WC_DUP_ERROR__ = field + " 값이 비어있습니다.";
${onE}
    return;
  }

  (async function(){
    try{
      var qs = '?field=' + encodeURIComponent(field) + '&value=' + encodeURIComponent(value);
      var res = await window.wcAuthRequest('/auth/duplicate' + qs, { method:'GET' });

      window.__WC_DUP_OK__ = true;
      window.__WC_DUP_AVAILABLE__ = (res && res.available === true);
      window.__WC_DUP_FIELD__ = field;
      window.__WC_DUP_DATA__ = res;

      if(window.__WC_DUP_AVAILABLE__){
${onA}
      } else {
${onU}
      }
    }catch(err){
      window.__WC_DUP_OK__ = false;
      window.__WC_DUP_AVAILABLE__ = false;
      window.__WC_DUP_FIELD__ = field;
      window.__WC_DUP_ERROR__ = err?.message || "중복확인 실패";
      window.__WC_DUP_DATA__ = null;
${onE}
    }
  })();
})();\n`;
  };

  javascriptGeneratorPretty.forBlock['auth_duplicate_check_branch'] = function (
    block,
    generator
  ) {
    const onA = generator.statementToCode(block, 'ON_AVAILABLE');
    const onU = generator.statementToCode(block, 'ON_UNAVAILABLE');
    const onE = generator.statementToCode(block, 'ON_ERROR');

    return (
      _stripTrailing(
        `(async () => {
  const btn = window.__WC_LAST_EVENT_TARGET__;
  const field = btn?.getAttribute?.('data-wc-target')?.trim();
  const form = btn?.closest?.('form');
  const value = form?.querySelector(\`[name="\${field}"], #\${field}\`)?.value?.trim();

  try {
    const qs = new URLSearchParams({ field, value }).toString();
    const res = await wcAuthRequest('/auth/duplicate?' + qs, { method: 'GET' });

    const available = res?.available === true;
    if (available) {
${_indent(onA, '  ')}
    } else {
${_indent(onU, '  ')}
    }
  } catch (err) {
${_indent(onE, '  ')}
  }
})();\n`
      ) + '\n'
    );
  };

  /* =========================================================
    ✅ 고정 엔드포인트 Auth API 호출 (유지)  // ✅ 기존 그대로
  ========================================================= */
  Blockly.Blocks['auth_register_call_fixed'] = {
    init: function () {
      this.appendDummyInput().appendField('🧾 회원가입 API 호출');
      this.appendStatementInput('ON_SUCCESS')
        .setCheck(null)
        .appendField('✅ 성공했을 때');
      this.appendStatementInput('ON_FAIL')
        .setCheck(null)
        .appendField('❌ 실패했을 때');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('POST /api/auth/register 고정 호출 (URL 변경 불가)');
    },
  };

  javascriptGenerator.forBlock['auth_register_call_fixed'] = function (
    block,
    generator
  ) {
    const okBranch = generator.statementToCode(block, 'ON_SUCCESS');
    const failBranch = generator.statementToCode(block, 'ON_FAIL');

    return `(function(){
  if(!window.wcAuthRequest || !window.wcAuthCollect || !window.wcAuthFindForm){
    alert("Auth runtime이 없습니다.");
    return;
  }
  var btn = window.__WC_LAST_EVENT_TARGET__ || null;
  var form = window.wcAuthFindForm(btn);
  var body = window.wcAuthCollect(form);

  (async function(){
    try{
      var data = await window.wcAuthRequest('/auth/register', { method:'POST', body: body });
      window.__WC_AUTH_LAST_OK__ = true;
      window.__WC_AUTH_LAST_DATA__ = data;
      window.__WC_AUTH_LAST_ERROR__ = null;
${okBranch}
    }catch(err){
      window.__WC_AUTH_LAST_OK__ = false;
      window.__WC_AUTH_LAST_DATA__ = null;
      window.__WC_AUTH_LAST_ERROR__ = (err && err.message) ? String(err.message) : "회원가입 실패";
${failBranch}
    }
  })();
})();\n`;
  };

  javascriptGeneratorPretty.forBlock['auth_register_call_fixed'] = function (
    block,
    generator
  ) {
    const ok = generator.statementToCode(block, 'ON_SUCCESS');
    const fail = generator.statementToCode(block, 'ON_FAIL');

    return (
      _stripTrailing(
        `(async () => {
  const btn = window.__WC_LAST_EVENT_TARGET__;
  const form = btn?.closest?.('form');
  const body = wcAuthCollect(form);

  try {
    await wcAuthRequest('/auth/register', { method: 'POST', body });
${_indent(ok, '  ')}
  } catch (err) {
${_indent(fail, '  ')}
  }
})();\n`
      ) + '\n'
    );
  };

  Blockly.Blocks['auth_login_call_fixed'] = {
    init: function () {
      this.appendDummyInput().appendField('🧾 로그인 API 호출');
      this.appendStatementInput('ON_SUCCESS')
        .setCheck(null)
        .appendField('✅ 성공했을 때');
      this.appendStatementInput('ON_FAIL')
        .setCheck(null)
        .appendField('❌ 실패했을 때');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('POST /api/auth/login 고정 호출 (URL 변경 불가)');
    },
  };

  javascriptGenerator.forBlock['auth_login_call_fixed'] = function (
    block,
    generator
  ) {
    const okBranch = generator.statementToCode(block, 'ON_SUCCESS');
    const failBranch = generator.statementToCode(block, 'ON_FAIL');

    return `(function(){
  if(!window.wcAuthRequest || !window.wcAuthCollect || !window.wcAuthFindForm){
    alert("Auth runtime이 없습니다.");
    return;
  }
  var btn = window.__WC_LAST_EVENT_TARGET__ || null;
  var form = window.wcAuthFindForm(btn);
  var body = window.wcAuthCollect(form);

  (async function(){
    try{
      var data = await window.wcAuthRequest('/auth/login', { method:'POST', body: body });
      window.__WC_AUTH_LAST_OK__ = true;
      window.__WC_AUTH_LAST_DATA__ = data;
      window.__WC_AUTH_LAST_ERROR__ = null;
${okBranch}
    }catch(err){
      window.__WC_AUTH_LAST_OK__ = false;
      window.__WC_AUTH_LAST_DATA__ = null;
      window.__WC_AUTH_LAST_ERROR__ = (err && err.message) ? String(err.message) : "로그인 실패";
${failBranch}
    }
  })();
})();\n`;
  };

  javascriptGeneratorPretty.forBlock['auth_login_call_fixed'] = function (
    block,
    generator
  ) {
    const ok = generator.statementToCode(block, 'ON_SUCCESS');
    const fail = generator.statementToCode(block, 'ON_FAIL');

    return (
      _stripTrailing(
        `(async () => {
  const btn = window.__WC_LAST_EVENT_TARGET__;
  const form = btn?.closest?.('form');
  const body = wcAuthCollect(form);

  try {
    await wcAuthRequest('/auth/login', { method: 'POST', body });
${_indent(ok, '  ')}
  } catch (err) {
${_indent(fail, '  ')}
  }
})();\n`
      ) + '\n'
    );
  };
};

export default {};
</script>
