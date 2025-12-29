<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

/* =====================
   조건 카테고리 메타
===================== */
export const category = {
  label: '조건',
  color: '#4ca454',
  icon: '❓'
}

/* =====================
   Toolbox
===================== */
export const toolbox = `
<xml>
  <block type="cond_login"></block>
  <block type="cond_page"></block>
  <block type="cond_and"></block>
  <block type="cond_or"></block>
  <block type="cond_not"></block>
</xml>
`

/* =====================
   블록 정의
===================== */
export const defineBlocks = () => {

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
    return [`currentPage === '${page}'`, pythonGenerator.ORDER_RELATIONAL]
  }

  /* 🔗 그리고 (AND) */
  Blockly.Blocks['cond_and'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('그리고')
      this.appendValueInput('B').setCheck('Boolean')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_and'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'false'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || 'false'
    return [`(${a} and ${b})`, pythonGenerator.ORDER_LOGICAL_AND]
  }

  /* 🔗 또는 (OR) */
  Blockly.Blocks['cond_or'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('또는')
      this.appendValueInput('B').setCheck('Boolean')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_or'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'false'
    const b = gen.valueToCode(block, 'B', gen.ORDER_NONE) || 'false'
    return [`(${a} or ${b})`, pythonGenerator.ORDER_LOGICAL_OR]
  }

  /* ❗ NOT */
  Blockly.Blocks['cond_not'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean')
      this.appendDummyInput().appendField('아니다')
      this.setOutput(true, 'Boolean')
      this.setColour('#4ca454')
    }
  }

  pythonGenerator.forBlock['cond_not'] = (block, gen) => {
    const a = gen.valueToCode(block, 'A', gen.ORDER_NONE) || 'false'
    return [`(not ${a})`, pythonGenerator.ORDER_LOGICAL_NOT]
  }
}
</script>

<template>
  <!-- 렌더링 없음 -->
</template>
