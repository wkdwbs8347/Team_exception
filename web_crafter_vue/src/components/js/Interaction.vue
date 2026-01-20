<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

export const category = {
  label: '동작',
  color: '#ff7043',
  icon: '⚡',
}

export const toolbox = `
<xml>
  <block type="script_tag"></block>
  <block type="event_click"></block>
  <block type="event_page_load"></block>

  <sep gap="16"></sep>
  <block type="action_alert"></block>
  <block type="ui_alert_value"></block>

  <block type="dom_change_text_class"></block>
  <block type="action_navigate_internal"></block>

  <sep gap="16"></sep>
  <!-- ✅ 폼 "변경"은 동작이라 유지 -->
  <block type="form_set_field"></block>

  <sep gap="16"></sep>
  <block type="auth_set_api_base"></block>
  <block type="auth_set_mode"></block>

  <sep gap="16"></sep>
  <block type="auth_duplicate_check_run"></block>

  <sep gap="16"></sep>
  <block type="auth_register_call_fixed"></block>
  <block type="auth_login_call_fixed"></block>

  <sep gap="16"></sep>
  <block type="action_api_request">
    <value name="URL">
      <shadow type="value_text">
        <field name="TEXT">https://jsonplaceholder.typicode.com/todos/1</field>
      </shadow>
    </value>
  </block>
  <block type="get_api_data"></block>
  <block type="get_api_error"></block>
</xml>
`

export const defineBlocks = () => {
  /* =========
    1) 클릭 이벤트 (클래스 기반)
  ========= */
  Blockly.Blocks['event_click'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('⚡ 클래스가')
        .appendField(new Blockly.FieldTextInput('btn'), 'TARGET_CLASS')
        .appendField('인 요소 클릭 시')

      this.appendStatementInput('DO').setCheck(null)

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
      this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.')
    },
  }

  javascriptGenerator.forBlock['event_click'] = function (block, generator) {
    const targetClass = (block.getFieldValue('TARGET_CLASS') || '').trim()
    const body = generator.statementToCode(block, 'DO')
    if (!targetClass) return ''

    return `(function() {
  document.addEventListener('click', function(e) {
    var target = e.target && e.target.closest ? e.target.closest('.${targetClass}') : null;
    if (target) {
      window.__WC_LAST_EVENT_TARGET__ = target;
${body}
    }
  });
})();\n`
  }

  /* =========
    2) 페이지 로드 이벤트
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init: function () {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때')
      this.appendStatementInput('DO').setCheck(null)
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['event_page_load'] = function (block, generator) {
    const body = generator.statementToCode(block, 'DO')
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`
  }

  /* =========
    3) 알림 액션
  ========= */
  Blockly.Blocks['action_alert'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🔔 알림')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE')

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? ''
    return `alert(${JSON.stringify(msg)});\n`
  }

  /* =========
    ✅ 알림(값 입력형)
  ========= */
  Blockly.Blocks['ui_alert_value'] = {
    init: function () {
      this.jsonInit({
        type: 'ui_alert_value',
        message0: '🔔 알림(값) %1',
        args0: [{ type: 'input_value', name: 'MSG', check: null }],
        previousStatement: null,
        nextStatement: null,
        colour: '#ff7043',
        tooltip: '값을 문자열로 바꿔 alert로 보여줍니다.',
      })
    },
  }

  javascriptGenerator.forBlock['ui_alert_value'] = function (block, generator) {
    const msg = generator.valueToCode(block, 'MSG', generator.ORDER_NONE) || "''"
    return `alert(String(${msg}));\n`
  }

  /* =========
    4) 페이지 이동 액션
  ========= */
  if (!Blockly.Extensions.isRegistered('dynamic_page_dropdown')) {
    Blockly.Extensions.register('dynamic_page_dropdown', function () {
      this.getInput('DUMMY').appendField(
        new Blockly.FieldDropdown(function () {
          return window.WC_GET_PAGES ? window.WC_GET_PAGES() : [['로딩중...', '']]
        }),
        'PAGE_ID'
      )
    })
  }

  Blockly.Blocks['action_navigate_internal'] = {
    init: function () {
      this.jsonInit({
        type: 'action_navigate_internal',
        message0: '페이지 이동하기 📄 %1',
        args0: [{ type: 'input_dummy', name: 'DUMMY' }],
        extensions: ['dynamic_page_dropdown'],
        previousStatement: null,
        nextStatement: null,
        colour: '#ff7043',
        tooltip: '이동할 페이지를 선택하세요.',
      })
    },
  }

  javascriptGenerator.forBlock['action_navigate_internal'] = function (block) {
    const pageId = block.getFieldValue('PAGE_ID')
    if (!pageId) return ''
    return `goToPage('${pageId}');\n`
  }

  /* =========
    0) 스크립트 태그 래퍼
  ========= */
  Blockly.Blocks['script_tag'] = {
    init: function () {
      this.appendDummyInput().appendField('📜 스크립트')
      this.appendStatementInput('BODY').setCheck(null).appendField('내용')
      this.setColour('#ff7043')
      this.setTooltip('여기에 이벤트와 동작을 넣으세요.')
      this.setStyle('hat_blocks')
    },
  }

  javascriptGenerator.forBlock['script_tag'] = function (block, generator) {
    const body = generator.statementToCode(block, 'BODY')
    return `<script>\n${body}<\/script>\n`
  }

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
      })
    },
  }

  javascriptGenerator.forBlock['dom_change_text_class'] = function (block, generator) {
    const className = generator.valueToCode(block, 'CLASS', generator.ORDER_NONE) || "''"
    const text = generator.valueToCode(block, 'TEXT', generator.ORDER_NONE) || "''"
    return `
(function(){
  var els = document.querySelectorAll('.' + ${className});
  els.forEach(function(el) {
    el.innerText = ${text};
  });
})();\n`
  }

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
      })
    },
  }

  javascriptGenerator.forBlock['form_set_field'] = function (block, generator) {
    const field = (block.getFieldValue('FIELD') || '').trim()
    const safe = JSON.stringify(field || '')
    const val = generator.valueToCode(block, 'VALUE', generator.ORDER_NONE) || "''"
    return `(function(){
  try{
    var btn = window.__WC_LAST_EVENT_TARGET__ || null;
    var form = window.wcAuthFindForm ? window.wcAuthFindForm(btn) : (btn && btn.closest ? btn.closest('form') : null);
    if(!form) return;
    var el = form.querySelector('[name=' + ${safe} + ']') || form.querySelector('#' + ${safe});
    if(el) el.value = String(${val});
  }catch(e){}
})();\n`
  }

  /* =========
    ✅ Auth 설정 (유지)
  ========= */
  Blockly.Blocks['auth_set_api_base'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🌐 API Base')
        .appendField(new Blockly.FieldTextInput('http://localhost:8080/api'), 'BASE')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['auth_set_api_base'] = function (block) {
    const base = (block.getFieldValue('BASE') || '').trim()
    if (!base) return ''
    return `window.WC_API_BASE=${JSON.stringify(base)};\n`
  }

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
        .appendField(new Blockly.FieldTextInput('wc_token'), 'KEY')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['auth_set_mode'] = function (block) {
    const mode = block.getFieldValue('MODE') || 'cookie'
    const key = (block.getFieldValue('KEY') || 'wc_token').trim()
    return `window.WC_AUTH_MODE=${JSON.stringify(mode)};\nwindow.WC_AUTH_TOKEN_KEY=${JSON.stringify(key)};\n`
  }

  /* =========================================================
    ✅ 중복확인 실행(유지)
  ========================================================= */
  Blockly.Blocks['auth_duplicate_check_run'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('✅ 중복확인 실행')
        .appendField('결과표시 Class(선택):')
        .appendField(new Blockly.FieldTextInput(''), 'RESULT_CLASS')

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['auth_duplicate_check_run'] = function (block) {
    const resultClass = (block.getFieldValue('RESULT_CLASS') || '').trim()
    const renderResult = resultClass
      ? `
  try{
    var els = document.querySelectorAll('.${resultClass}');
    els.forEach(function(el){ el.innerText = msg; });
  }catch(e){}`
      : ''

    return `(function(){
  if(!window.wcAuthRequest || !window.wcAuthCollect || !window.wcAuthFindForm){
    return alert("Auth runtime이 없습니다.");
  }
  var btn = window.__WC_LAST_EVENT_TARGET__ || null;
  if(!btn) return alert("중복확인 버튼을 찾지 못했습니다.");

  var field = (btn.getAttribute('data-wc-target') || "").trim();
  if(!field) return alert("중복확인 대상(data-wc-target)이 없습니다.");

  var form = window.wcAuthFindForm(btn);
  var body = window.wcAuthCollect(form);
  var value = (body[field] || "").trim();
  if(!value) return alert(field + " 값을 입력하세요.");

  (async function(){
    var qs = '?field=' + encodeURIComponent(field) + '&value=' + encodeURIComponent(value);
    var result = await window.wcAuthRequest('/auth/duplicate' + qs, { method:'GET' });

    var ok = !!(result && result.available === true);
    var msg = ok ? "사용 가능합니다 ✅" : ((result && result.message) ? result.message : "이미 사용 중입니다 ❌");

    alert(msg);
${renderResult}
  })().catch(function(err){
    alert((err && err.message) ? err.message : "중복확인 실패");
  });
})();\n`
  }

  /* =========================================================
    ✅ 고정 엔드포인트 Auth API 호출 (유지)
  ========================================================= */
  Blockly.Blocks['auth_register_call_fixed'] = {
    init: function () {
      this.appendDummyInput().appendField('🧾 회원가입 API 호출')
      this.appendStatementInput('ON_SUCCESS').setCheck(null).appendField('✅ 성공했을 때')
      this.appendStatementInput('ON_FAIL').setCheck(null).appendField('❌ 실패했을 때')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
      this.setTooltip('POST /api/auth/register 고정 호출 (URL 변경 불가)')
    },
  }

  javascriptGenerator.forBlock['auth_register_call_fixed'] = function (block, generator) {
    const okBranch = generator.statementToCode(block, 'ON_SUCCESS')
    const failBranch = generator.statementToCode(block, 'ON_FAIL')

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
})();\n`
  }

  Blockly.Blocks['auth_login_call_fixed'] = {
    init: function () {
      this.appendDummyInput().appendField('🧾 로그인 API 호출')
      this.appendStatementInput('ON_SUCCESS').setCheck(null).appendField('✅ 성공했을 때')
      this.appendStatementInput('ON_FAIL').setCheck(null).appendField('❌ 실패했을 때')
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
      this.setTooltip('POST /api/auth/login 고정 호출 (URL 변경 불가)')
    },
  }

  javascriptGenerator.forBlock['auth_login_call_fixed'] = function (block, generator) {
    const okBranch = generator.statementToCode(block, 'ON_SUCCESS')
    const failBranch = generator.statementToCode(block, 'ON_FAIL')

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
})();\n`
  }

  // =========================================================
  // 1) API 요청 블록 (기존 유지)
  // =========================================================
  Blockly.Blocks['action_api_request'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🌐 API 요청')
        .appendField(
          new Blockly.FieldDropdown([
            ['가져오기 (GET)', 'GET'],
            ['보내기 (POST)', 'POST'],
            ['수정하기 (PUT)', 'PUT'],
            ['삭제하기 (DELETE)', 'DELETE'],
          ]),
          'METHOD'
        )

      this.appendValueInput('URL').setCheck('String').appendField('주소(URL)')
      this.appendValueInput('DATA').setCheck(null).appendField('데이터(Body/JSON)')

      this.appendStatementInput('ON_SUCCESS').appendField('✅ 성공했을 때')
      this.appendStatementInput('ON_ERROR').appendField('❌ 실패했을 때')

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#5c6bc0')
      this.setTooltip('서버에 데이터를 요청하거나 보냅니다.')
    },
  }

  javascriptGenerator.forBlock['action_api_request'] = function (block, generator) {
    const method = block.getFieldValue('METHOD')
    const url =
      generator.valueToCode(block, 'URL', generator.ORDER_ATOMIC) ||
      "'https://jsonplaceholder.typicode.com/posts/1'"
    const dataCode = generator.valueToCode(block, 'DATA', generator.ORDER_ATOMIC) || '{}'
    const successBranch = generator.statementToCode(block, 'ON_SUCCESS')
    const errorBranch = generator.statementToCode(block, 'ON_ERROR')
    const hasBody = method === 'POST' || method === 'PUT'

    return `
fetch(${url}, {
  method: '${method}',
  headers: { 'Content-Type': 'application/json' },
  ${hasBody ? `body: JSON.stringify(${dataCode})` : ''}
})
.then(response => {
  if (!response.ok) throw new Error(response.statusText);
  return response.json();
})
.then(apiData => {
  console.log("API Success:", apiData);
  ${successBranch}
})
.catch(apiError => {
  console.error("API Error:", apiError);
  ${errorBranch}
});\n`
  }

  Blockly.Blocks['get_api_data'] = {
    init: function () {
      this.appendDummyInput().appendField('📦 응답 데이터 (apiData)')
      this.setOutput(true, null)
      this.setColour('#7986cb')
      this.setTooltip('성공 블록 내부에서만 사용하세요.')
    },
  }

  javascriptGenerator.forBlock['get_api_data'] = function (_, generator) {
    return ['apiData', generator.ORDER_ATOMIC]
  }

  Blockly.Blocks['get_api_error'] = {
    init: function () {
      this.appendDummyInput().appendField('⚠️ 에러 내용 (apiError)')
      this.setOutput(true, 'String')
      this.setColour('#7986cb')
      this.setTooltip('실패 블록 내부에서만 사용하세요.')
    },
  }

  javascriptGenerator.forBlock['get_api_error'] = function (_, generator) {
    return ['apiError', generator.ORDER_ATOMIC]
  }
}

export default {}
</script>