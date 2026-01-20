<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

/* =====================
   Logic 카테고리 메타
===================== */
export const category = {
  label: '조건',
  color: '#4ca454',
  icon: '🔗',
}

/* =====================
   ✅ Content.vue의 SERVER_FIELDS와 동일한 키 목록
===================== */
const SERVER_FIELDS = [
  { label: '아이디', key: 'loginId' },
  { label: '이메일', key: 'email' },
  { label: '이름', key: 'name' },
  { label: '닉네임', key: 'nickname' },
  { label: '비밀번호', key: 'password' },
  { label: '비밀번호 확인', key: 'passwordConfirm' },
  { label: '생년월일', key: 'birth' },
]

/* =====================
   Toolbox XML
===================== */
export const toolbox = `
<xml>
  <block type="cond_compare"></block>

  <sep gap="16"></sep>
  <block type="logic_and"></block>
  <block type="logic_or"></block>
  <block type="logic_not"></block>

  <sep gap="16"></sep>
  <block type="value_boolean"></block>
  <block type="value_number"></block>
  <block type="value_text"></block>
  <block type="value_concat"></block>

  <sep gap="16"></sep>
  <!-- ✅ 폼/검증 유틸 -->
  <block type="form_value"></block>
  <block type="value_is_empty"></block>
  <block type="text_length"></block>
  <block type="text_has_space"></block>
  <block type="text_matches_regex"></block>
</xml>
`

/* =====================
   블록 정의 및 생성기
===================== */
export const defineBlocks = () => {
  /* ⚖️ 비교 연산자 */
  Blockly.Blocks['cond_compare'] = {
    init() {
      this.appendValueInput('A')
      this.appendDummyInput().appendField(
        new Blockly.FieldDropdown([
          ['=', '==='],
          ['≠', '!=='],
          ['<', '<'],
          ['≤', '<='],
          ['>', '>'],
          ['≥', '>='],
        ]),
        'OP'
      )
      this.appendValueInput('B')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['cond_compare'] = (block) => {
    const a =
      javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) ||
      '0'
    const b =
      javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) ||
      '0'
    const op = block.getFieldValue('OP')
    return [`(${a} ${op} ${b})`, javascriptGenerator.ORDER_RELATIONAL]
  }

  /* 🔗 그리고 (AND) */
  Blockly.Blocks['logic_and'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('그리고')
      this.appendValueInput('B').setCheck('Boolean')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['logic_and'] = (block) => {
    const a =
      javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) ||
      'false'
    const b =
      javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) ||
      'false'
    return [`(${a} && ${b})`, javascriptGenerator.ORDER_LOGICAL_AND]
  }

  /* 🔗 또는 (OR) */
  Blockly.Blocks['logic_or'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('또는')
      this.appendValueInput('B').setCheck('Boolean')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['logic_or'] = (block) => {
    const a =
      javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) ||
      'false'
    const b =
      javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) ||
      'false'
    return [`(${a} || ${b})`, javascriptGenerator.ORDER_LOGICAL_OR]
  }

  /* ❗ 아니다 (NOT) */
  Blockly.Blocks['logic_not'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('아니다')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['logic_not'] = (block) => {
    const a =
      javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) ||
      'false'
    return [`(!${a})`, javascriptGenerator.ORDER_LOGICAL_NOT]
  }

  /* ✅ 참 / 거짓 */
  Blockly.Blocks['value_boolean'] = {
    init() {
      this.appendDummyInput().appendField(
        new Blockly.FieldDropdown([
          ['✅ 참', 'true'],
          ['❌ 거짓', 'false'],
        ]),
        'BOOL'
      )
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['value_boolean'] = (block) => {
    return [block.getFieldValue('BOOL'), javascriptGenerator.ORDER_ATOMIC]
  }

  /* 🔢 숫자 */
  Blockly.Blocks['value_number'] = {
    init() {
      this.appendDummyInput().appendField('🔢').appendField(new Blockly.FieldNumber(0), 'NUM')
      this.setOutput(true, null)
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['value_number'] = (block) => {
    return [String(block.getFieldValue('NUM')), javascriptGenerator.ORDER_ATOMIC]
  }

  /* 📝 텍스트 */
  Blockly.Blocks['value_text'] = {
    init() {
      this.appendDummyInput()
        .appendField('📝')
        .appendField(new Blockly.FieldTextInput('텍스트'), 'TEXT')
      this.setOutput(true, null)
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['value_text'] = (block) => {
    const v = block.getFieldValue('TEXT') || ''
    return [`'${v}'`, javascriptGenerator.ORDER_ATOMIC]
  }

  /* ➕ 텍스트 합치기 */
  Blockly.Blocks['value_concat'] = {
    init() {
      this.appendValueInput('A')
      this.appendDummyInput().appendField('➕')
      this.appendValueInput('B')
      this.setInputsInline(true)
      this.setOutput(true, 'String')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['value_concat'] = (block) => {
    const a =
      javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) ||
      "''"
    const b =
      javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) ||
      "''"
    return [`String(${a}) + String(${b})`, javascriptGenerator.ORDER_ADDITION]
  }

  /* =====================================================
     ✅ 폼 값 가져오기 (드롭다운 + 커스텀)
  ===================================================== */
  Blockly.Blocks['form_value'] = {
    init() {
      const options = SERVER_FIELDS.map((f) => [f.label, f.key])
      options.push(['직접입력...', '__custom__'])

      this.appendDummyInput()
        .appendField('📥 폼 값')
        .appendField(new Blockly.FieldDropdown(options), 'FIELD_KEY')

      this.appendDummyInput('CUSTOM_ROW')
        .appendField('name=')
        .appendField(new Blockly.FieldTextInput(''), 'FIELD_CUSTOM')

      this.setOutput(true, null)
      this.setColour('#4ca454')
      this.setTooltip('현재 이벤트 기준 폼에서 해당 name 값을 가져옵니다.')

      this.updateShape_()
    },

    updateShape_() {
      const isCustom = this.getFieldValue('FIELD_KEY') === '__custom__'
      const row = this.getInput('CUSTOM_ROW')
      if (row) row.setVisible(!!isCustom)
      if (this.rendered) this.render()
    },

    onchange(e) {
      if (!this.workspace || this.isInFlyout) return
      if (
        e &&
        e.type === Blockly.Events.BLOCK_CHANGE &&
        e.blockId === this.id &&
        e.name === 'FIELD_KEY'
      ) {
        this.updateShape_()
      } else if (!e) {
        this.updateShape_()
      }
    },
  }

  javascriptGenerator.forBlock['form_value'] = (block) => {
    const key = block.getFieldValue('FIELD_KEY') || 'email'
    const custom = (block.getFieldValue('FIELD_CUSTOM') || '').trim()
    const field = key === '__custom__' ? custom : key
    const fieldSafe = String(field || '').replace(/"/g, '\\"')

    if (!fieldSafe) return ["''", javascriptGenerator.ORDER_ATOMIC]

    return [
      `(function(){
  try{
    var btn = window.__WC_LAST_EVENT_TARGET__ || null;

    // 1) auth runtime 있으면 우선 사용
    var form = null;
    if(window.wcAuthFindForm){
      form = window.wcAuthFindForm(btn);
    }

    // 2) fallback: closest('form')
    if(!form && btn && btn.closest){
      form = btn.closest('form');
    }

    // 3) 그래도 없으면 document에서 첫 form
    if(!form){
      form = document.querySelector('form');
    }
    if(!form) return '';

    var el = form.querySelector('[name="${fieldSafe}"]');
    if(!el) return '';

    if(typeof el.value !== 'undefined' && el.value !== null){
      return String(el.value);
    }

    if(typeof el.checked !== 'undefined'){
      return el.checked ? 'true' : 'false';
    }

    return '';
  }catch(e){ return ''; }
})()`,
      javascriptGenerator.ORDER_FUNCTION_CALL,
    ]
  }

  /* =====================================================
     ✅ 비어있음 체크
  ===================================================== */
  Blockly.Blocks['value_is_empty'] = {
    init() {
      this.appendValueInput('VAL').appendField('❓ 비어있다')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    },
  }

  javascriptGenerator.forBlock['value_is_empty'] = (block) => {
    const v =
      javascriptGenerator.valueToCode(block, 'VAL', javascriptGenerator.ORDER_NONE) ||
      "''"
    return [
      `(${v} === null || ${v} === undefined || String(${v}).trim() === '')`,
      javascriptGenerator.ORDER_LOGICAL_NOT,
    ]
  }

  /* =====================================================
     ✅ 문자열 길이
     - text_length("abc") => 3
  ===================================================== */
  Blockly.Blocks['text_length'] = {
    init() {
      this.appendValueInput('TEXT').appendField('📏 글자수')
      this.setInputsInline(true)
      this.setOutput(true, null)
      this.setColour('#4ca454')
      this.setTooltip('문자열의 길이를 반환합니다.')
    },
  }

  javascriptGenerator.forBlock['text_length'] = (block) => {
    const t =
      javascriptGenerator.valueToCode(block, 'TEXT', javascriptGenerator.ORDER_NONE) ||
      "''"
    return [`(String(${t}).length)`, javascriptGenerator.ORDER_FUNCTION_CALL]
  }

  /* =====================================================
     ✅ 공백 포함 여부
     - text_has_space("a b") => true
  ===================================================== */
  Blockly.Blocks['text_has_space'] = {
    init() {
      this.appendValueInput('TEXT').appendField('⛔ 공백이 포함됨')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
      this.setTooltip('문자열에 공백(스페이스/탭/줄바꿈)이 포함되면 true')
    },
  }

  javascriptGenerator.forBlock['text_has_space'] = (block) => {
    const t =
      javascriptGenerator.valueToCode(block, 'TEXT', javascriptGenerator.ORDER_NONE) ||
      "''"
    return [
      `(/\\s/.test(String(${t})))`,
      javascriptGenerator.ORDER_FUNCTION_CALL,
    ]
  }

  /* =====================================================
     ✅ 정규식 매칭
     - text_matches_regex("abc@a.com", "^[^@]+@[^@]+\\.[^@]+$") => true
     - flags: "i" 같은 옵션 가능(선택)
  ===================================================== */
  Blockly.Blocks['text_matches_regex'] = {
    init() {
      this.appendValueInput('TEXT').appendField('🧩 정규식 통과')
      this.appendValueInput('PATTERN').appendField('패턴')
      this.appendValueInput('FLAGS').appendField('옵션(flags)')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
      this.setTooltip('정규식 패턴에 매칭되면 true')
    },
  }

  javascriptGenerator.forBlock['text_matches_regex'] = (block) => {
    const text =
      javascriptGenerator.valueToCode(block, 'TEXT', javascriptGenerator.ORDER_NONE) ||
      "''"
    const pattern =
      javascriptGenerator.valueToCode(block, 'PATTERN', javascriptGenerator.ORDER_NONE) ||
      "''"
    const flags =
      javascriptGenerator.valueToCode(block, 'FLAGS', javascriptGenerator.ORDER_NONE) ||
      "''"

    // 안전: pattern/flags가 문자열이 아닐 수도 있으니 String() 처리
    return [
      `(function(){
  try{
    var _t = String(${text});
    var _p = String(${pattern});
    var _f = String(${flags} || '');
    var re = new RegExp(_p, _f);
    return re.test(_t);
  }catch(e){
    return false;
  }
})()`,
      javascriptGenerator.ORDER_FUNCTION_CALL,
    ]
  }
}

export default {}
</script>