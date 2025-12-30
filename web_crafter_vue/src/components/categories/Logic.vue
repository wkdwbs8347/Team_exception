<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

/* =====================
   Logic 카테고리 메타
===================== */
export const category = {
  label: '조건',
  color: '#4ca454',
  icon: '🔗'
}

/* =====================
   Toolbox
===================== */
export const toolbox = `
<xml>
  <block type="cond_login"></block>
  <block type="cond_page"></block>
  <block type="cond_compare"></block>
  
  <sep gap="16"></sep>
  <block type="logic_and"></block>
  <block type="logic_or"></block>
  <block type="logic_not"></block>
  
  <sep gap="16"></sep>
  <block type="value_true"></block>
  <block type="value_false"></block>
  <block type="value_number"></block>
  <block type="value_text"></block>
  <block type="value_variable"></block>
  <block type="value_custom_variable"></block>
  <block type="value_concat"></block>
</xml>
`

/* =====================
   블록 정의
===================== */
export const defineBlocks = () => {

  /* ==================================================
     조건 블록들
  ================================================== */

  /* 🔐 로그인 되어 있다 */
  Blockly.Blocks['cond_login'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔐 로그인 되어 있다')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_login'] = () => {
    return ['isLogin', pythonGenerator.ORDER_ATOMIC]
  }

  /* 📄 현재 페이지가 ○○이다 */
  Blockly.Blocks['cond_page'] = {
    init() {
      this.appendDummyInput()
        .appendField('📄 현재 페이지가')
        .appendField(new Blockly.FieldDropdown([
          ['홈', 'home'],
          ['로그인', 'login'],
          ['회원가입', 'join']
        ]), 'PAGE')
        .appendField('이다')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_page'] = (block) => {
    const page = block.getFieldValue('PAGE')
    return [`currentPage == '${page}'`, pythonGenerator.ORDER_RELATIONAL]
  }

  /* 비교 연산자 */
  Blockly.Blocks['cond_compare'] = {
    init() {
      this.appendValueInput('A')
      this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([
          ['=', '=='],
          ['≠', '!='],
          ['<', '<'],
          ['≤', '<='],
          ['>', '>'],
          ['≥', '>=']
        ]), 'OP')
      this.appendValueInput('B')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_compare'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || '0'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || '0'
    const op = block.getFieldValue('OP')
    return [`(${a} ${op} ${b})`, pythonGenerator.ORDER_RELATIONAL]
  }

  /* ==================================================
     논리 블록들
  ================================================== */

  /* 🔗 그리고 (AND) */
  Blockly.Blocks['logic_and'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('그리고')
      this.appendValueInput('B').setCheck('Boolean')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['logic_and'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'False'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || 'False'
    return [`(${a} and ${b})`, pythonGenerator.ORDER_LOGICAL_AND]
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
    }
  }

  pythonGenerator.forBlock['logic_or'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'False'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || 'False'
    return [`(${a} or ${b})`, pythonGenerator.ORDER_LOGICAL_OR]
  }

  /* ❗ 아니다 (NOT) */
  Blockly.Blocks['logic_not'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('아니다')
      this.setInputsInline(true)
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['logic_not'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'False'
    return [`(not ${a})`, pythonGenerator.ORDER_LOGICAL_NOT]
  }

  /* ==================================================
     값 블록들
  ================================================== */

  /* ✅ 참 */
  Blockly.Blocks['value_true'] = {
    init() {
      this.appendDummyInput()
        .appendField('✅ 참')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['value_true'] = () => {
    return ['True', pythonGenerator.ORDER_ATOMIC]
  }

  /* ❌ 거짓 */
  Blockly.Blocks['value_false'] = {
    init() {
      this.appendDummyInput()
        .appendField('❌ 거짓')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['value_false'] = () => {
    return ['False', pythonGenerator.ORDER_ATOMIC]
  }

  /* 🔢 숫자 */
  Blockly.Blocks['value_number'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔢')
        .appendField(new Blockly.FieldNumber(0), 'NUM')
      this.setOutput(true, 'Number')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['value_number'] = (block) => {
    const num = block.getFieldValue('NUM')
    return [num, pythonGenerator.ORDER_ATOMIC]
  }

  /* 📝 텍스트 */
  Blockly.Blocks['value_text'] = {
    init() {
      this.appendDummyInput()
        .appendField('📝')
        .appendField(new Blockly.FieldTextInput('텍스트'), 'TEXT')
      this.setOutput(true, 'String')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['value_text'] = (block) => {
    const text = block.getFieldValue('TEXT')
    return [`'${text}'`, pythonGenerator.ORDER_ATOMIC]
  }

  /* 💾 변수 */
  Blockly.Blocks['value_variable'] = {
    init() {
      this.appendDummyInput()
        .appendField('💾 변수')
        .appendField(new Blockly.FieldDropdown([
          ['사용자이름', 'username'],
          ['이메일', 'email'],
          ['방문횟수', 'visitCount'],
          ['현재페이지', 'currentPage'],
          ['로그인상태', 'isLogin']
        ]), 'VAR')
      this.setOutput(true)
      this.setColour('#4ca454')
      this.setTooltip('미리 정의된 변수를 사용합니다')
    }
  }

  pythonGenerator.forBlock['value_variable'] = (block) => {
    const varName = block.getFieldValue('VAR')
    return [varName, pythonGenerator.ORDER_ATOMIC]
  }

  /* ✏️ 커스텀 변수 */
  Blockly.Blocks['value_custom_variable'] = {
    init() {
      this.appendDummyInput()
        .appendField('✏️ 변수')
        .appendField(new Blockly.FieldTextInput('내변수'), 'VAR')
      this.setOutput(true)
      this.setColour('#4ca454')
      this.setTooltip('원하는 변수 이름을 직접 입력합니다')
    }
  }

  pythonGenerator.forBlock['value_custom_variable'] = (block) => {
    const varName = block.getFieldValue('VAR')
    return [varName, pythonGenerator.ORDER_ATOMIC]
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
      this.setTooltip('두 텍스트를 합칩니다')
    }
  }

  pythonGenerator.forBlock['value_concat'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || '""'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || '""'
    return [`str(${a}) + str(${b})`, pythonGenerator.ORDER_ADDITION]
  }
}
</script>

<template>
  <!-- 렌더링 없음 -->
</template>