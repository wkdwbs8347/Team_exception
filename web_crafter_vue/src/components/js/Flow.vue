<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '흐름',
  color: '#ffab19',
  icon: '🔁'
}

/* =====================
   Toolbox XML
   ✅ flow_try / flow_return 추가
===================== */
export const toolbox = `
<xml>
  <block type="flow_if"></block>
  <block type="flow_else_if"></block>
  <block type="flow_else"></block>

  <sep gap="16"></sep>
  <block type="flow_try"></block>
  <block type="flow_return"></block>

  <sep gap="16"></sep>
  <block type="flow_repeat_count"></block>
  <block type="flow_repeat_while"></block>
  <block type="flow_wait"></block>
</xml>
`

export const defineBlocks = () => {
  /* =====================
     조립식 제어 블록 (Flow Parts)
  ===================== */

  // 1. [만약]
  Blockly.Blocks['flow_if'] = {
    init() {
      this.appendValueInput('CONDITION').setCheck('Boolean').appendField('❓ 만약');
      this.appendStatementInput('DO').appendField('라면 ');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  // 2. [아니면 만약]
  Blockly.Blocks['flow_else_if'] = {
    init() {
      this.appendValueInput('CONDITION').setCheck('Boolean').appendField('❓ 아니면 만약');
      this.appendStatementInput('DO').appendField('라면 ');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  // 3. [아니면]
  Blockly.Blocks['flow_else'] = {
    init() {
      this.appendDummyInput().appendField('❗ 아니면 ');
      this.appendStatementInput('DO');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  // ✅ NEW: try/catch/finally 조립식
  // - API 호출, JSON parse, DOM 접근 등 안전 처리용
  Blockly.Blocks['flow_try'] = {
    init() {
      this.appendDummyInput()
        .appendField('🧯 예외처리')
        .appendField('에러변수')
        .appendField(new Blockly.FieldTextInput('e'), 'ERR_NAME');

      this.appendStatementInput('TRY').appendField('try');
      this.appendStatementInput('CATCH').appendField('catch');
      this.appendStatementInput('FINALLY').appendField('finally');

      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
      this.setTooltip('try/catch/finally 조립식 블록입니다.');
    }
  };

  // ✅ NEW: return (조기 종료)
  // - 회원가입 검증에서 첫 번째 에러 발견 시 즉시 중단 같은 패턴에 필요
  Blockly.Blocks['flow_return'] = {
    init() {
      this.appendDummyInput().appendField('⛔ 중단');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
      this.setTooltip('현재 실행 흐름을 즉시 종료합니다 (return).');
    }
  };

  // 1. [N번 반복]
  Blockly.Blocks['flow_repeat_count'] = {
    init() {
      this.appendValueInput('COUNT').setCheck('Number').appendField('🔁');
      this.appendStatementInput('DO').appendField('번 반복하기 {');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  // 2. [~동안 반복]
  Blockly.Blocks['flow_repeat_while'] = {
    init() {
      this.appendValueInput('CONDITION').setCheck('Boolean').appendField('🔁');
      this.appendStatementInput('DO').appendField('동안 반복하기 {');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  // 3. [기다리기]
  Blockly.Blocks['flow_wait'] = {
    init() {
      this.appendValueInput('SECONDS').setCheck('Number').appendField('⏳');
      this.appendDummyInput().appendField('초 기다리기');
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ffab19');
    }
  };

  /* =====================
     Generators
  ===================== */

  javascriptGenerator.forBlock['flow_if'] = function(block) {
    const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
    const branch = javascriptGenerator.statementToCode(block, 'DO');
    return `if (${condition}) {\n${branch}}`;
  };

  javascriptGenerator.forBlock['flow_else_if'] = function(block) {
    const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
    const branch = javascriptGenerator.statementToCode(block, 'DO');
    return ` else if (${condition}) {\n${branch}}`;
  };

  javascriptGenerator.forBlock['flow_else'] = function(block) {
    const branch = javascriptGenerator.statementToCode(block, 'DO');
    return ` else {\n${branch}}\n`;
  };

  // ✅ NEW: flow_try generator
  javascriptGenerator.forBlock['flow_try'] = function(block, generator) {
    const errNameRaw = (block.getFieldValue('ERR_NAME') || 'e').trim();
    const errName = /^[A-Za-z_$][A-Za-z0-9_$]*$/.test(errNameRaw) ? errNameRaw : 'e';

    const tryBranch = generator.statementToCode(block, 'TRY');
    const catchBranch = generator.statementToCode(block, 'CATCH');
    const finallyBranch = generator.statementToCode(block, 'FINALLY');

    // catch/finally는 비어도 문법상 OK
    return `try {\n${tryBranch}} catch (${errName}) {\n${catchBranch}} finally {\n${finallyBranch}}\n`;
  };

  // ✅ NEW: flow_return generator
  javascriptGenerator.forBlock['flow_return'] = function() {
    return `return;\n`;
  };

  javascriptGenerator.forBlock['flow_repeat_count'] = function(block) {
    const count = javascriptGenerator.valueToCode(block, 'COUNT', javascriptGenerator.ORDER_ATOMIC) || '0';
    const branch = javascriptGenerator.statementToCode(block, 'DO');
    return `for (let i = 0; i < ${count}; i++) {\n${branch}}\n`;
  };

  javascriptGenerator.forBlock['flow_repeat_while'] = function(block) {
    const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
    const branch = javascriptGenerator.statementToCode(block, 'DO');
    return `while (${condition}) {\n${branch}}\n`;
  };

  javascriptGenerator.forBlock['flow_wait'] = function(block) {
    const secondsRaw = javascriptGenerator.valueToCode(block, 'SECONDS', javascriptGenerator.ORDER_ATOMIC) || '0';
    const ms = Number(secondsRaw) * 1000;
    return `await new Promise(resolve => setTimeout(resolve, ${ms}));\n`;
  };
}
</script>