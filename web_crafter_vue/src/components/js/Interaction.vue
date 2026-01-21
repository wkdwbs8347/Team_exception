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
  <block type="event_click"></block>
  <block type="event_page_load"></block>

  <sep gap="16"></sep>
  <block type="action_alert"></block>

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
const _v = (gen, block, name) =>
  gen.valueToCode(block, name, gen.ORDER_NONE) || "''";
const _s = (gen, block, name) => gen.statementToCode(block, name) || '';

const _indent = (code, pad = '  ') =>
  (code || '')
    .split('\n')
    .map((l) => (l.trim() ? pad + l : l))
    .join('\n');

const _stripTrailing = (s) => (s || '').replace(/\s+$/g, '');

export const defineBlocks = () => {
  /* =========
    1) 클릭 이벤트 (클래스 기반)
  ========= */
  Blockly.Blocks['event_click'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('⚡ 클래스가')
        .appendField(new Blockly.FieldTextInput('btn'), 'TARGET_CLASS')
        .appendField('인 요소 클릭 시');

      this.appendStatementInput('DO').setCheck(null);

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.');
    },
  };

  // ===== RUN
  javascriptGenerator.forBlock['event_click'] = function (block, generator) {
    const targetClass = (block.getFieldValue('TARGET_CLASS') || '').trim();
    const body = generator.statementToCode(block, 'DO');
    if (!targetClass) return '';

    return `(function() {
  document.addEventListener('click', function(e) {
    var target = e.target && e.target.closest ? e.target.closest('.${targetClass}') : null;
    if (target) {
      // ✅ 폼 submit/기본 동작 방지 (페이지 리로드 때문에 분기 슬롯이 안 도는 문제 해결)
      try { if(e && e.preventDefault) e.preventDefault(); } catch(_) {}
      try { if(e && e.stopPropagation) e.stopPropagation(); } catch(_) {}

      window.__WC_LAST_EVENT_TARGET__ = target;
      window.__WC_LAST_EVENT__ = e; // (옵션) 나중에 쓰고 싶으면 유지
${body}
    }
  }, true); // ✅ 캡처 단계에서 잡으면 submit보다 먼저 막기 쉬움
})();\n`;
  };

  // ===== PRETTY (의미 중심, 순수 코드)
  javascriptGeneratorPretty.forBlock['event_click'] = function (
    block,
    generator
  ) {
    const cls = (block.getFieldValue('TARGET_CLASS') || '').trim();
    const body = generator.statementToCode(block, 'DO');
    if (!cls) return '';

    return (
      _stripTrailing(
        `document.addEventListener('click', (e) => {
  const el = e.target?.closest?.('.${cls}');
  if (!el) return;

  e?.preventDefault?.();
  e?.stopPropagation?.();

  window.__WC_LAST_EVENT_TARGET__ = el;
  window.__WC_LAST_EVENT__ = e;
${_indent(body, '  ')}
}, true);\n`
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

  // ===== RUN
  javascriptGenerator.forBlock['event_page_load'] = function (
    block,
    generator
  ) {
    const body = generator.statementToCode(block, 'DO');
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`;
  };

  // ===== PRETTY (의미 중심, 순수 코드)
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

  // ===== RUN
  javascriptGenerator.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    return `alert(${JSON.stringify(msg)});\n`;
  };

  // ===== PRETTY (동일)
  javascriptGeneratorPretty.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? '';
    return `alert(${_q(msg)});\n`;
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

  // ===== RUN
  javascriptGenerator.forBlock['action_navigate_internal'] = function (block) {
    const pageId = block.getFieldValue('PAGE_ID');
    if (!pageId) return '';
    return `goToPage('${pageId}');\n`;
  };

  // ===== PRETTY (순수 코드)
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

  // ===== RUN
  javascriptGenerator.forBlock['script_tag'] = function (block, generator) {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };

  // ===== PRETTY (코드보기에서도 script 태그 형식)
  javascriptGeneratorPretty.forBlock['script_tag'] = function (
    block,
    generator
  ) {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };

  /* =========
    5) 요소 내용 바꾸기 (Class)
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

  // ===== RUN
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

  // ===== PRETTY (의미 중심, 순수 코드)
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
  // ✅ 폼 값 "설정" (동작)만 유지
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

  // ===== RUN
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

  // ===== PRETTY (의미 중심, 순수 코드)
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

  // ===== RUN
  javascriptGenerator.forBlock['auth_set_mode'] = function (block) {
    const mode = block.getFieldValue('MODE') || 'cookie';
    const key = (block.getFieldValue('KEY') || 'wc_token').trim();
    return `window.WC_AUTH_MODE=${JSON.stringify(mode)};\nwindow.WC_AUTH_TOKEN_KEY=${JSON.stringify(key)};\n`;
  };

  // ===== PRETTY (순수 코드)
  javascriptGeneratorPretty.forBlock['auth_set_mode'] = function (block) {
    const mode = block.getFieldValue('MODE') || 'cookie';
    const key = (block.getFieldValue('KEY') || 'wc_token').trim();
    return `window.WC_AUTH_MODE=${_q(mode)};\nwindow.WC_AUTH_TOKEN_KEY=${_q(key)};\n`;
  };

  /* =========================================================
  ✅ 중복확인 수행 (순수 로직 블록)
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

  // ===== RUN (실제 동작)
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

  // ===== PRETTY (코드보기용, 의미 중심)
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
    ✅ 고정 엔드포인트 Auth API 호출 (유지)
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

  // ===== RUN
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

  // ===== PRETTY (의미 중심, 순수 코드)
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

  // ===== RUN
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

  // ===== PRETTY (의미 중심, 순수 코드)
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
