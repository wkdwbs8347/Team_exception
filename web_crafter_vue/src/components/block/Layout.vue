<!-- =========================
- ✅ layout.vue (구조)
- ✅ 태그에 data-block-id 직접 부착
- ✅ 드래그/좌표 스타일은 여기서 넣지 않음(프리뷰에서만 제어)
- ✅ 타이틀(어떤 블럭인지)은 항상 최상단 단독 1줄
- ✅ 속성(PROPS)은 항상 1줄(수평) + "이름:"이 항상 첫번째
- ✅ CONTENT(내용/항목)는 별도 줄
- ❌ setInputsInline(true) 사용 안함
========================= -->
<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

// ===== Layout 카테고리 메타데이터 =====
export const category = {
  label: '구조',
  color: '#4c97ff',
  icon: '📐',
};

// ✅ 박스 기본 테두리(시각적 가이드)
const BASE_BOX_STYLE =
  'min-width:50px; min-height:50px; border:1px solid #ccc; box-sizing:border-box;';

export const toolbox = `<xml>
  <block type="layout_region"></block>
  <block type="layout_box"></block>
  <block type="layout_divider"></block>
  <block type="layout_ul"></block>
  <block type="layout_li"></block>
  <block type="layout_form"></block>
  <block type="layout_form_field"></block>
</xml>`;

export const defineBlocks = () => {
  // -------------------------
  // 공통 유틸
  // -------------------------
  const safeClass = (raw, fallback) =>
    (raw || fallback)
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_\-가-힣]/g, '');

  const getClass = (block, fallback) =>
    safeClass(block.getFieldValue('ELEMENT_CLASS'), fallback);

  const safeClassChunk = (raw) =>
    (raw ?? '')
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_\-가-힣]/g, '');

  // ✅ 공통: 최상위 draggable 태그에 붙일 속성
  const draggableAttrs = (block) =>
    `data-block-id="${block.id}" data-draggable="true"`;

  // =========================================================
  // [REGION]
  // =========================================================
  Blockly.Blocks['layout_region'] = {
    init() {
      // ✅ 타이틀 단독 1줄
      this.appendDummyInput().appendField('🧱 영역');

      // ✅ 속성 1줄 (이름이 항상 첫번째)
      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('wc-region'), 'ELEMENT_CLASS')
        .appendField('태그:')
        .appendField(
          new Blockly.FieldDropdown([
            ['HEADER', 'header'],
            ['NAV', 'nav'],
            ['MAIN', 'main'],
            ['SECTION', 'section'],
            ['ASIDE', 'aside'],
            ['FOOTER', 'footer'],
          ]),
          'TAG'
        );

      // ✅ 내용
      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#4c97ff');
      // ❌ setInputsInline(true) 사용 X
    },
  };

  javascriptGenerator.forBlock['layout_region'] = (block) => {
    const tag = (block.getFieldValue('TAG') || 'section').toLowerCase();
    const allowed = new Set(['header', 'nav', 'main', 'section', 'aside', 'footer']);
    const safeTag = allowed.has(tag) ? tag : 'section';

    const cls = getClass(block, 'wc-region');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<${safeTag} class="${cls}" style="${BASE_BOX_STYLE}" ${draggableAttrs(block)}>
${content}</${safeTag}>\n`;
  };

  // =========================================================
  // [BOX]
  // =========================================================
  Blockly.Blocks['layout_box'] = {
    init() {
      this.appendDummyInput().appendField('📦 컨텐츠 박스');

      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('wc-box'), 'ELEMENT_CLASS')
        .appendField('태그:')
        .appendField(
          new Blockly.FieldDropdown([
            ['DIV', 'div'],
            ['ARTICLE', 'article'],
          ]),
          'TAG'
        );

      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#4c97ff');
    },
  };

  javascriptGenerator.forBlock['layout_box'] = (block) => {
    const tag = (block.getFieldValue('TAG') || 'div').toLowerCase();
    const allowed = new Set(['div', 'article']);
    const safeTag = allowed.has(tag) ? tag : 'div';

    const cls = getClass(block, 'wc-box');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<${safeTag} class="${cls}" style="${BASE_BOX_STYLE}" ${draggableAttrs(block)}>
${content}</${safeTag}>\n`;
  };

  // =========================================================
  // [DIVIDER] (드래그 X)
  // =========================================================
  Blockly.Blocks['layout_divider'] = {
    init() {
      this.appendDummyInput().appendField('➖ 구분선');

      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('wc-divider'), 'ELEMENT_CLASS');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#4c97ff');
    },
  };

  javascriptGenerator.forBlock['layout_divider'] = (block) => {
    const cls = getClass(block, 'wc-divider');
    return `<hr class="${cls}" data-block-id="${block.id}" />\n`;
  };

  // =========================================================
  // [UL] (최상위 draggable 가능)
  // =========================================================
  Blockly.Blocks['layout_ul'] = {
    init() {
      this.appendDummyInput().appendField('📦 리스트 박스(UL)');

      this.appendDummyInput('PROPS')
        .appendField('이름:')
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

  javascriptGenerator.forBlock['layout_ul'] = (block) => {
    const cls = getClass(block, 'list-container');
    const items = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<ul class="${cls}" style="${BASE_BOX_STYLE}" ${draggableAttrs(block)}>
${items}</ul>\n`;
  };

  // =========================================================
  // [LI] (드래그 X)
  // =========================================================
  Blockly.Blocks['layout_li'] = {
    init() {
      this.appendDummyInput().appendField('📦 리스트 항목(LI)');

      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('list-item'), 'ELEMENT_CLASS');

      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용');

      this.setPreviousStatement(true, 'LI');
      this.setNextStatement(true, 'LI');
      this.setColour('#4c97ff');
    },
  };

  javascriptGenerator.forBlock['layout_li'] = (block) => {
    const cls = getClass(block, 'list-item');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<li class="${cls}" data-block-id="${block.id}">
${content}</li>\n`;
  };

  // =========================================================
  // [FORM] (최상위 draggable 가능)
  // =========================================================
  Blockly.Blocks['layout_form'] = {
    init() {
      this.appendDummyInput().appendField('🧾 폼(Form)');

      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('wc-form'), 'ELEMENT_CLASS');

      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#4c97ff');
    },
  };

  javascriptGenerator.forBlock['layout_form'] = (block) => {
    const cls = getClass(block, 'wc-form');
    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    return `<form class="${cls}" style="${BASE_BOX_STYLE}" ${draggableAttrs(block)}>
${content}</form>\n`;
  };

  // =========================================================
  // [FORM FIELD]
  // - ✅ 라벨 없음 (라벨은 content_label로 따로)
  // - ✅ 고급 체크 시 "추가 클래스"가 내용(CONTENT) 위에 뜸
  // - ✅ 드래그 X (폼 내부 흐름 요소)
  // =========================================================
  Blockly.Blocks['layout_form_field'] = {
    init() {
      this.appendDummyInput().appendField('🧩 폼 항목(Field)');

      // ✅ 속성 1줄 (이름이 항상 첫번째)
      this.appendDummyInput('PROPS')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('wc-field'), 'ELEMENT_CLASS')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      // ✅ 내용
      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#4c97ff');

      this.updateShape_();
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.updateShape_();
    },

    onchange() {
      this.updateShape_();
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE';

      // ✅ 고급 ON이면 "추가 클래스" 입력을 CONTENT 위로
      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('이름 추가:')
          .appendField(new Blockly.FieldTextInput(''), 'EXTRA_CLASS');

        // append는 맨 아래에 붙으므로, CONTENT 앞에 오도록 재정렬
        const advInput = this.getInput('ADV_ROW');
        const contentInput = this.getInput('CONTENT');
        const list = this.inputList;

        const advIdx = list.indexOf(advInput);
        const contentIdx = list.indexOf(contentInput);
        if (advIdx > -1 && contentIdx > -1 && advIdx > contentIdx) {
          list.splice(advIdx, 1);
          list.splice(contentIdx, 0, advInput);
        }
      }

      if (!adv && this.getInput('ADV_ROW')) {
        this.removeInput('ADV_ROW');
      }
    },
  };

  javascriptGenerator.forBlock['layout_form_field'] = (block) => {
    const cls = getClass(block, 'wc-field');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const extra = adv ? safeClassChunk(block.getFieldValue('EXTRA_CLASS') || '') : '';
    const extraAttr = extra ? ` ${extra}` : '';

    const content = javascriptGenerator.statementToCode(block, 'CONTENT');

    // ✅ 드래그 X: data-draggable 안 붙임
    return `<div class="${cls}${extraAttr}" style="${BASE_BOX_STYLE}" data-block-id="${block.id}">
${content}</div>\n`;
  };
};

export default {};
</script>