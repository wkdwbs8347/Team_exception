<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

// ===== Layout 카테고리 메타데이터 =====
export const category = {
  label: '구조',
  color: '#4c97ff',
  icon: '📐',
};

const BASE_BOX_STYLE =
  'min-width:50px; min-height:50px; border:1px solid #ccc; box-sizing:border-box;';

// ===== Layout 툴박스 XML =====
export const toolbox = `<xml>
  <block type="layout_container"></block>
  <block type="layout_divider"></block>
  <block type="layout_ul"></block>
  <block type="layout_li"></block>
</xml>`;

export const defineBlocks = () => {
  // 공통 유틸
  const safeClass = (raw, fallback) =>
    (raw || fallback)
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

  const getClass = (block, fallback) =>
    safeClass(block.getFieldValue('ELEMENT_CLASS'), fallback);

  // =========================
  // ✅ 컨테이너 (div / nav / section / article 통합)
  // =========================
  if (!Blockly.Blocks['layout_container']) {
    Blockly.Blocks['layout_container'] = {
      init() {
        this.appendDummyInput().appendField('📦 컨테이너');

        this.appendDummyInput()
          .appendField('태그:')
          .appendField(
            new Blockly.FieldDropdown([
              ['DIV', 'div'],
              ['NAV', 'nav'],
              ['SECTION', 'section'],
              ['ARTICLE', 'article'],
            ]),
            'TAG'
          );

        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(
            new Blockly.FieldTextInput('wc-container'),
            'ELEMENT_CLASS'
          );

        this.appendStatementInput('CONTENT').setCheck(null).appendField('내용');

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#4c97ff');
      },
    };
  }

  javascriptGenerator.forBlock['layout_container'] = (block) => {
    const tag = (block.getFieldValue('TAG') || 'div').toLowerCase();
    const allowed = new Set(['div', 'nav', 'section', 'article']);
    const safeTag = allowed.has(tag) ? tag : 'div';

    const cls = getClass(block, 'wc-container');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<${safeTag} class="${cls}" data-block-id="${block.id}" style="${BASE_BOX_STYLE}">
${content}</${safeTag}>\n`;
  };

  // -------------------------
  // DIVIDER
  // -------------------------
  if (!Blockly.Blocks['layout_divider']) {
    Blockly.Blocks['layout_divider'] = {
      init() {
        this.appendDummyInput().appendField('➖ Divider');
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(
            new Blockly.FieldTextInput('wc-divider'),
            'ELEMENT_CLASS'
          );
        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#4c97ff');
      },
    };
  }

  javascriptGenerator.forBlock['layout_divider'] = (block) => {
    const cls = getClass(block, 'wc-divider');
    return `<hr class="${cls}" data-block-id="${block.id}" />\n`;
  };

  // -------------------------
  //  UL / LI
  // -------------------------
  if (!Blockly.Blocks['layout_ul']) {
    Blockly.Blocks['layout_ul'] = {
      init() {
        this.appendDummyInput().appendField('📦 리스트 박스(UL)');
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(
            new Blockly.FieldTextInput('list-container'),
            'ELEMENT_CLASS'
          );
        this.appendStatementInput('CONTENT').setCheck('LI').appendField('항목');
        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#4c97ff');
      },
    };
  }

  javascriptGenerator.forBlock['layout_ul'] = (block) => {
    const cls = getClass(block, 'list-container');
    const items = javascriptGenerator.statementToCode(block, 'CONTENT');
    return `<ul class="${cls}" data-block-id="${block.id}">\n${items}</ul>\n`;
  };

  if (!Blockly.Blocks['layout_li']) {
    Blockly.Blocks['layout_li'] = {
      init() {
        this.appendDummyInput().appendField('📦 리스트 항목(LI)');
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(
            new Blockly.FieldTextInput('list-item'),
            'ELEMENT_CLASS'
          );
        this.appendStatementInput('CONTENT').setCheck(null).appendField('내용');
        this.setPreviousStatement(true, 'LI');
        this.setNextStatement(true, 'LI');
        this.setColour('#4c97ff');
      },
    };
  }

  javascriptGenerator.forBlock['layout_li'] = (block) => {
    const cls = getClass(block, 'list-item');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');
    return `<li class="${cls}">\n${content}</li>\n`;
  };
};

export default {};
</script>
