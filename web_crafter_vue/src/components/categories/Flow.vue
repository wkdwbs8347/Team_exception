<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

/* =====================
   Flow 카테고리 메타
===================== */
export const category = {
  label: '흐름',
  color: '#ffab19',
  icon: '🔁'
}

/* =====================
   Toolbox
===================== */
export const toolbox = `
<xml>
  <block type="flow_if"></block>
  <block type="flow_if_else"></block>
  <block type="flow_repeat"></block>
  <block type="flow_wait"></block>
</xml>
`

/* =====================
   블록 정의
===================== */
export const defineBlocks = () => {

  /* ==================================================
     IF (아니면 없음)
  ================================================== */
  Blockly.Blocks['flow_if'] = {
    init() {
      this.appendValueInput('COND')
        .setCheck('Boolean')
        .appendField('❓ 만일')

      this.appendStatementInput('THEN')
        .appendField('(이)라면')

      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_if'] = (block, gen) => {
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'false'
    const thenCode = gen.statementToCode(block, 'THEN')

    return `
if (${cond}) {
${thenCode}
}
`
  }

  /* ==================================================
     IF + ELSE
  ================================================== */
  Blockly.Blocks['flow_if_else'] = {
    init() {
      this.appendValueInput('COND')
        .setCheck('Boolean')
        .appendField('❓ 만일')

      this.appendStatementInput('THEN')
        .appendField('(이)라면')

      this.appendStatementInput('ELSE')
        .appendField('아니면')

      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_if_else'] = (block, gen) => {
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'false'
    const thenCode = gen.statementToCode(block, 'THEN')
    const elseCode = gen.statementToCode(block, 'ELSE')

    return `
if (${cond}) {
${thenCode}
} else {
${elseCode}
}
`
  }

  /* ==================================================
     반복
  ================================================== */
  Blockly.Blocks['flow_repeat'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔁 반복')
        .appendField(new Blockly.FieldNumber(3, 1), 'COUNT')
        .appendField('번')

      this.appendStatementInput('DO')

      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_repeat'] = (block, gen) => {
    const count = block.getFieldValue('COUNT')
    const body = gen.statementToCode(block, 'DO')

    return `
for (let i = 0; i < ${count}; i++) {
${body}
}
`
  }

  /* ==================================================
     기다리기
  ================================================== */
  Blockly.Blocks['flow_wait'] = {
    init() {
      this.appendDummyInput()
        .appendField('⏱️ 기다리기')
        .appendField(new Blockly.FieldNumber(1, 0), 'SEC')
        .appendField('초')

      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_wait'] = (block) => {
    const sec = block.getFieldValue('SEC')

    return `
await new Promise(r => setTimeout(r, ${sec * 1000}));
`
  }
}
</script>

<template>
  <!-- 이 컴포넌트는 화면에 렌더링되지 않습니다 -->
</template>
