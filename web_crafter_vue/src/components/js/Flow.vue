<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '흐름',
  color: '#ffab19',
  icon: '🔁'
}

/* =====================
   Toolbox XML (이름 수정됨)
===================== */
export const toolbox = `
<xml>
  <block type="flow_if"></block>
  <block type="flow_else_if"></block>
  <block type="flow_else"></block>
  <block type="flow_repeat_count"></block>
  <block type="flow_repeat_while"></block>
  <block type="flow_wait"></block>
</xml>
`

export const defineBlocks = () => {
  /* =====================
     조립식 제어 블록 (Flow Parts)
  ===================== */

// 1. [만약] 블록 - if () { 시작점
Blockly.Blocks['flow_if'] = {
  init() {
    this.appendValueInput('CONDITION').setCheck('Boolean').appendField('❓ 만약');
    this.appendStatementInput('DO').appendField('라면 ');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};

// 2. [아니면 만약] 블록 - } else if () { 중간 다리
Blockly.Blocks['flow_else_if'] = {
  init() {
    this.appendValueInput('CONDITION').setCheck('Boolean').appendField('❓ 아니면 만약');
    this.appendStatementInput('DO').appendField('라면 ');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};

// 3. [아니면] 블록 - } else { 마무리
Blockly.Blocks['flow_else'] = {
  init() {
    this.appendDummyInput().appendField('❗ 아니면 ');
    this.appendStatementInput('DO');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};

// 1. [N번 반복] - for (let i = 0; i < n; i++) { 시작점
Blockly.Blocks['flow_repeat_count'] = {
  init() {
    this.appendValueInput('COUNT').setCheck('Number').appendField('🔁');
    this.appendStatementInput('DO').appendField('번 반복하기 {');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};

// 2. [~동안 반복] - while (조건) { 시작점
Blockly.Blocks['flow_repeat_while'] = {
  init() {
    this.appendValueInput('CONDITION').setCheck('Boolean').appendField('🔁');
    this.appendStatementInput('DO').appendField('동안 반복하기 {');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};

// 3. [기다리기] - 시간 지연 (초 단위)
Blockly.Blocks['flow_wait'] = {
  init() {
    this.appendValueInput('SECONDS').setCheck('Number').appendField('⏳');
    this.appendDummyInput().appendField('초 기다리기');
    this.setPreviousStatement(true);
    this.setNextStatement(true);
    this.setColour('#ffab19');
  }
};


// 1. [만약] 생성기
javascriptGenerator.forBlock['flow_if'] = function(block) {
  const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
  const branch = javascriptGenerator.statementToCode(block, 'DO');
  return `if (${condition}) {\n${branch}}`; // } 뒤에 줄바꿈을 넣지 않아야 다음 else가 붙음
};

// 2. [아니면 만약] 생성기
javascriptGenerator.forBlock['flow_else_if'] = function(block) {
  const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
  const branch = javascriptGenerator.statementToCode(block, 'DO');
  // ✨ 앞 블록의 } 바로 뒤에 이어서 붙도록 한 칸 띄우고 시작
  return ` else if (${condition}) {\n${branch}}`;
};

// 3. [아니면] 생성기
javascriptGenerator.forBlock['flow_else'] = function(block) {
  const branch = javascriptGenerator.statementToCode(block, 'DO');
  // ✨ 마찬가지로 앞의 } 와 연결되도록 설계
  return ` else {\n${branch}}\n`; // 마지막 조각이므로 문장 종료 줄바꿈 추가
};

// 1. [N번 반복] 생성기
javascriptGenerator.forBlock['flow_repeat_count'] = function(block) {
  const count = javascriptGenerator.valueToCode(block, 'COUNT', javascriptGenerator.ORDER_ATOMIC) || '0';
  const branch = javascriptGenerator.statementToCode(block, 'DO');
  
  // 반복 변수는 i 대신 겹치지 않게 고유한 이름을 쓰기도 하지만, 일단 기본형으로 드립니다.
  return `for (let i = 0; i < ${count}; i++) {\n${branch}}\n`;
};

// 2. [~동안 반복] 생성기
javascriptGenerator.forBlock['flow_repeat_while'] = function(block) {
  const condition = javascriptGenerator.valueToCode(block, 'CONDITION', javascriptGenerator.ORDER_ATOMIC) || 'false';
  const branch = javascriptGenerator.statementToCode(block, 'DO');
  
  return `while (${condition}) {\n${branch}}\n`;
};

// Flow.vue 내 수정된 flow_wait 생성기
javascriptGenerator.forBlock['flow_wait'] = function(block) {
  const secondsRaw = javascriptGenerator.valueToCode(block, 'SECONDS', javascriptGenerator.ORDER_ATOMIC) || '0';
  
  // 찾으신 자바 코드처럼 밀리초(ms)로 변환
  const ms = Number(secondsRaw) * 1000;
  
  // 자바스크립트 전용 'Sleep' 코드 반환
  return `await new Promise(resolve => setTimeout(resolve, ${ms}));\n`;
};
  // flow_repeat, flow_wait 등 나머지 블록 정의도 이 아래에 계속 작성하시면 됩니다.
}
</script>