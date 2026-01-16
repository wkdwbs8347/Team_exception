<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

/* =====================
   Logic 카테고리 메타
===================== */
export const category = {
  label: '조건',
  color: '#4ca454',
  icon: '🔗'
}

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
</xml>
`
/* =====================
   블록 정의 및 생성기
===================== */
export const defineBlocks = () => {

  /* ⚖️ 비교 연산자 */
  Blockly.Blocks['cond_compare'] = {
    init() {
      this.appendValueInput('A');
      this.appendDummyInput().appendField(new Blockly.FieldDropdown([
          ['=', '==='],
          ['≠', '!=='],
          ['<', '<'],
          ['≤', '<='],
          ['>', '>'],
          ['≥', '>=']
        ]), 'OP');
      this.appendValueInput('B');
      this.setInputsInline(true);
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['cond_compare'] = (block) => {
    const a = javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) || '0';
    const b = javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) || '0';
    const op = block.getFieldValue('OP');
    return [`(${a} ${op} ${b})`, javascriptGenerator.ORDER_RELATIONAL];
  }

  /* 🔗 그리고 (AND) */
  Blockly.Blocks['logic_and'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean');
      this.appendDummyInput().appendField('그리고');
      this.appendValueInput('B').setCheck('Boolean');
      this.setInputsInline(true);
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['logic_and'] = (block) => {
    const a = javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) || 'false';
    const b = javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) || 'false';
    return [`(${a} && ${b})`, javascriptGenerator.ORDER_LOGICAL_AND];
  }

  /* 🔗 또는 (OR) */
  Blockly.Blocks['logic_or'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean');
      this.appendDummyInput().appendField('또는');
      this.appendValueInput('B').setCheck('Boolean');
      this.setInputsInline(true);
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['logic_or'] = (block) => {
    const a = javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) || 'false';
    const b = javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) || 'false';
    return [`(${a} || ${b})`, javascriptGenerator.ORDER_LOGICAL_OR];
  }

  /* ❗ 아니다 (NOT) */
  Blockly.Blocks['logic_not'] = {
    init() {
      this.appendValueInput('A').setCheck('Boolean');
      this.appendDummyInput().appendField('아니다');
      this.setInputsInline(true);
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['logic_not'] = (block) => {
    const a = javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) || 'false';
    return [`(!${a})`, javascriptGenerator.ORDER_LOGICAL_NOT];
  }

/* ✅ 참/거짓 통합 블록 */
Blockly.Blocks['value_boolean'] = {
  init() {
    this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([
          ['✅ 참', 'true'],
          ['❌ 거짓', 'false']
        ]), 'BOOL');
    this.setOutput(true, 'Boolean'); // 출력 타입은 여전히 Boolean
    this.setColour('#4ca454');
  }
}

javascriptGenerator.forBlock['value_boolean'] = (block) => {
  const code = block.getFieldValue('BOOL');
  return [code, javascriptGenerator.ORDER_ATOMIC];
};
/* 🔢 숫자 블록 수정 */
Blockly.Blocks['value_number'] = {
  init() {
    this.appendDummyInput().appendField('🔢').appendField(new Blockly.FieldNumber(0), 'NUM');
    this.setOutput(true, null); // 'Number' 대신 null로 변경
    this.setColour('#4ca454');
  }
}

javascriptGenerator.forBlock['value_number'] = (block) => {
  // block.getFieldValue만 쓰면 입력한 숫자만 가져오고, 
  // 다른 블록과 연결되었을 때 그 값을 가져오지 못할 수 있습니다.
  const number = block.getFieldValue('NUM');
  return [String(number), javascriptGenerator.ORDER_ATOMIC];
};


/* 📝 텍스트 블록 수정 */
Blockly.Blocks['value_text'] = {
  init() {
    this.appendDummyInput().appendField('📝').appendField(new Blockly.FieldTextInput('텍스트'), 'TEXT');
    this.setOutput(true, null); // 'String' 대신 null로 변경
    this.setColour('#4ca454');
  }
}

javascriptGenerator.forBlock['value_text'] = (block) => {
  // getFieldValue('TEXT')가 전체 문자열을 가져옵니다.
  const textValue = block.getFieldValue('TEXT') || '';
  // 반드시 전체를 따옴표로 감싸서 반환해야 합니다.
  return [`'${textValue}'`, javascriptGenerator.ORDER_ATOMIC];
};

  /* ➕ 텍스트 합치기 */
  Blockly.Blocks['value_concat'] = {
    init() {
      this.appendValueInput('A');
      this.appendDummyInput().appendField('➕');
      this.appendValueInput('B');
      this.setInputsInline(true);
      this.setOutput(true, 'String');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['value_concat'] = (block) => {
    const a = javascriptGenerator.valueToCode(block, 'A', javascriptGenerator.ORDER_NONE) || "''";
    const b = javascriptGenerator.valueToCode(block, 'B', javascriptGenerator.ORDER_NONE) || "''";
    return [`String(${a}) + String(${b})`, javascriptGenerator.ORDER_ADDITION];
  }
}
</script>

<template></template>