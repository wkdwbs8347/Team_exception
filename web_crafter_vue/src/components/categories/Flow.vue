<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

export const category = {
  label: '흐름',
  color: '#ffab19',
  icon: '🔁'
}

export const toolbox = `
<xml>
  <block type="flow_if"></block>
  <block type="flow_if_else"></block>
  <block type="flow_repeat"></block>
  <block type="flow_wait"></block>
</xml>
`

export const defineBlocks = () => {
  /* --- IF 블록 --- */
  Blockly.Blocks['flow_if'] = {
    init() {
      this.appendValueInput('COND').setCheck('Boolean').appendField('❓ 만일')
      this.appendStatementInput('THEN').appendField('(이)라면')
      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  // 핵심: 리턴 시 문자열만 내보내면 플랫폼이 '텍스트 객체'로 오해합니다.
  // 코드 끝에 줄바꿈과 pass 처리를 확실히 하여 '실행 코드'임을 명시합니다.
  pythonGenerator.forBlock['flow_if'] = (block, gen) => {
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'False'
    const thenCode = gen.statementToCode(block, 'THEN') || '  pass\n'
    return `if ${cond}:\n${thenCode}`
  }

  /* --- IF + ELSE 블록 --- */
  Blockly.Blocks['flow_if_else'] = {
    init() {
      this.appendValueInput('COND').setCheck('Boolean').appendField('❓ 만일')
      this.appendStatementInput('THEN').appendField('(이)라면')
      this.appendStatementInput('ELSE').appendField('아니면')
      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_if_else'] = (block, gen) => {
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'False'
    const thenCode = gen.statementToCode(block, 'THEN') || '  pass\n'
    const elseCode = gen.statementToCode(block, 'ELSE') || '  pass\n'
    return `if ${cond}:\n${thenCode}else:\n${elseCode}`
  }

  /* --- 반복 블록 --- */
  Blockly.Blocks['flow_repeat'] = {
    init() {
      this.appendDummyInput().appendField('🔁 반복').appendField(new Blockly.FieldNumber(3, 1), 'COUNT').appendField('번')
      this.appendStatementInput('DO')
      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_repeat'] = (block, gen) => {
    const count = block.getFieldValue('COUNT')
    const body = gen.statementToCode(block, 'DO') || '  pass\n'
    return `for i in range(${count}):\n${body}`
  }

  /* --- 기다리기 블록 --- */
  Blockly.Blocks['flow_wait'] = {
    init() {
      this.appendDummyInput().appendField('⏱️ 기다리기').appendField(new Blockly.FieldNumber(1, 0), 'SEC').appendField('초')
      this.setPreviousStatement(true)
      this.setNextStatement(true)
      this.setColour('#ffab19')
    }
  }

  pythonGenerator.forBlock['flow_wait'] = (block) => {
    const sec = block.getFieldValue('SEC')
    return `import time\ntime.sleep(${sec})\n`
  }
}
</script>