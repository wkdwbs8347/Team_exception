<script>
import * as Blockly from 'blockly';
import { pythonGenerator } from 'blockly/python';

/* =========================
  카테고리 정의
========================= */
export const category = {
  label: '동작',
  color: '#ff7043',
  icon: '⚡'
};

/* =========================
  툴박스
========================= */
export const toolbox = `
<xml>
  <block type="script_tag"></block>
  <block type="event_click"></block>
  <block type="event_page_load"></block>
  <block type="action_alert"></block>
  <block type="action_navigate"></block>
</xml>
`;

/* =========================
  블록 정의
========================= */
export const defineBlocks = () => {

  /* =========
    1. 클릭 이벤트 (클래스 기반)
  ========= */
  Blockly.Blocks['event_click'] = {
    init: function() {
      this.appendDummyInput()
        .appendField('⚡ 클래스가')
        .appendField(new Blockly.FieldTextInput('btn-login'), 'TARGET_CLASS')
        .appendField('인 요소 클릭 시');

      this.appendStatementInput('DO').setCheck(null);

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.');
    }
  };

  pythonGenerator.forBlock['event_click'] = function(block, generator) {
    const targetClass = block.getFieldValue('TARGET_CLASS');
    const body = generator.statementToCode(block, 'DO');

    return `(function() {
  document.addEventListener('click', function(e) {
    var target = e.target.closest('.${targetClass}');
    if (target) {
${body}
    }
  });
})();\n`;
  };

  /* =========
    2. 페이지 로드 이벤트
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init() {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때');
      this.appendStatementInput('DO').setCheck(null);
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    }
  };

  pythonGenerator.forBlock['event_page_load'] = (block, generator) => {
    const body = generator.statementToCode(block, 'DO');
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`;
  };

  /* =========
    3. 알림 액션
  ========= */
  Blockly.Blocks['action_alert'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔔 알림')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    }
  };

  pythonGenerator.forBlock['action_alert'] = (block) => {
    const msg = block.getFieldValue('MESSAGE');
    return `alert(${JSON.stringify(msg)});\n`;
  };

  /* =========
    4. 페이지 이동 액션 (🔥 핵심 추가)
  ========= */
  Blockly.Blocks['action_navigate'] = {
    init() {
      this.appendDummyInput()
        .appendField('➡️ 페이지 이동')
        .appendField(
          new Blockly.FieldTextInput('page_login'),
          'PAGE_ID'
        );

      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
      this.setTooltip('지정한 페이지로 이동합니다.');
    }
  };

  pythonGenerator.forBlock['action_navigate'] = function(block) {
    const pageId = block.getFieldValue('PAGE_ID');
    return `navigateToPage(${JSON.stringify(pageId)});\n`;
  };

  /* =========
    0. 스크립트 태그 래퍼
  ========= */
  Blockly.Blocks['script_tag'] = {
    init() {
      this.appendDummyInput().appendField('📜 스크립트');
      this.appendStatementInput('BODY').setCheck(null).appendField('내용');
      this.setColour('#ff7043');
      this.setTooltip('여기에 이벤트와 동작을 넣으세요.');
      this.setStyle('hat_blocks');
    }
  };

  pythonGenerator.forBlock['script_tag'] = (block, generator) => {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };
};
</script>
