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
   블록 정의 및 생성기
===================== */
export const defineBlocks = () => {

  /* 🔐 로그인 되어 있다 */
  Blockly.Blocks['cond_login'] = {
    init() {
      this.appendDummyInput().appendField('🔐 로그인 되어 있다');
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['cond_login'] = () => {
    return ['isLogin', javascriptGenerator.ORDER_ATOMIC];
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
        .appendField('이다');
      this.setOutput(true, 'Boolean');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['cond_page'] = (block) => {
    const page = block.getFieldValue('PAGE');
    return [`currentPage === '${page}'`, javascriptGenerator.ORDER_RELATIONAL];
  }

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

  /* ✅ 참 / 거짓 */
  Blockly.Blocks['value_true'] = { init() { this.appendDummyInput().appendField('✅ 참'); this.setOutput(true, 'Boolean'); this.setColour('#4ca454'); } }
  javascriptGenerator.forBlock['value_true'] = () => ['true', javascriptGenerator.ORDER_ATOMIC];

  Blockly.Blocks['value_false'] = { init() { this.appendDummyInput().appendField('❌ 거짓'); this.setOutput(true, 'Boolean'); this.setColour('#4ca454'); } }
  javascriptGenerator.forBlock['value_false'] = () => ['false', javascriptGenerator.ORDER_ATOMIC];

  /* 🔢 숫자 */
  Blockly.Blocks['value_number'] = {
    init() {
      this.appendDummyInput().appendField('🔢').appendField(new Blockly.FieldNumber(0), 'NUM');
      this.setOutput(true, 'Number');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['value_number'] = (block) => [String(block.getFieldValue('NUM')), javascriptGenerator.ORDER_ATOMIC];

  /* 📝 텍스트 */
  Blockly.Blocks['value_text'] = {
    init() {
      this.appendDummyInput().appendField('📝').appendField(new Blockly.FieldTextInput('텍스트'), 'TEXT');
      this.setOutput(true, 'String');
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['value_text'] = (block) => [`'${block.getFieldValue('TEXT')}'`, javascriptGenerator.ORDER_ATOMIC];

  /* 💾 변수 시스템 */
  Blockly.Blocks['value_variable'] = {
    init() {
      this.appendDummyInput().appendField('💾 변수').appendField(new Blockly.FieldDropdown([
          ['사용자이름', 'username'], ['이메일', 'email'], ['방문횟수', 'visitCount'], ['현재페이지', 'currentPage'], ['로그인상태', 'isLogin']
        ]), 'VAR');
      this.setOutput(true);
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['value_variable'] = (block) => [block.getFieldValue('VAR'), javascriptGenerator.ORDER_ATOMIC];

  Blockly.Blocks['value_custom_variable'] = {
    init() {
      this.appendDummyInput().appendField('✏️ 변수').appendField(new Blockly.FieldTextInput('내변수'), 'VAR');
      this.setOutput(true);
      this.setColour('#4ca454');
    }
  }
  javascriptGenerator.forBlock['value_custom_variable'] = (block) => [block.getFieldValue('VAR'), javascriptGenerator.ORDER_ATOMIC];

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