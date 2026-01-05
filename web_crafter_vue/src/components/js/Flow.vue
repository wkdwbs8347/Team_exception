<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript';

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

  javascriptGenerator.forBlock['flow_if'] = (block, gen) => {
    // 1. Python의 'False' 대신 JS의 'false' 사용
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'false';
    
    // 2. JS는 빈 블록({})이 허용되므로 'pass'가 필요 없음
    const thenCode = gen.statementToCode(block, 'THEN');
    
    // 3. if (조건) { 실행문 } 형태
    return `if (${cond}) {\n${thenCode}}\n`;
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

  javascriptGenerator.forBlock['flow_if_else'] = (block, gen) => {
    // 1. 조건값 가져오기 (Python의 'False' 대신 소문자 'false' 사용)
    const cond = gen.valueToCode(block, 'COND', gen.ORDER_NONE) || 'false';
    
    // 2. 내부 실행 코드 가져오기 (JS는 빈 블록이 허용되므로 pass 불필요)
    const thenCode = gen.statementToCode(block, 'THEN');
    const elseCode = gen.statementToCode(block, 'ELSE');
    
    // 3. JS 문법으로 조립 (괄호와 중괄호 주의)
    return `if (${cond}) {\n${thenCode}} else {\n${elseCode}}\n`;
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

  javascriptGenerator.forBlock['flow_repeat'] = (block, gen) => {
    const count = block.getFieldValue('COUNT');
    const body = gen.statementToCode(block, 'DO');
    return `for (let i = 0; i < ${count}; i++) {\n${body}}\n`;
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

  javascriptGenerator.forBlock['flow_wait'] = (block, gen) => {
    const sec = block.getFieldValue('SEC') || '0';
    return `await new Promise(resolve => setTimeout(resolve, ${sec * 1000}));\n`;
  }
}
</script>