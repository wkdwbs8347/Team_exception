<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

const withUnit = (value, defaultUnit = 'px') => {
  if (!value) return '';
  const v = value.toString().trim();
  if (/^\d+(\.\d+)?$/.test(v)) return v + defaultUnit;
  const validUnitPattern = /^-?\d+(\.\d+)?(px|%|em|rem|vw|vh|vmin|vmax|ch|ex|cm|mm|in|pt|pc)$/;
  if (validUnitPattern.test(v)) return v;
  if (v === 'auto') return v;
  console.warn('[Style ignored: invalid unit]', v);
  return '';
};

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨',
};

export const toolbox = `
<xml>
  <block type="style_tag"></block>
  <block type="style_tag_all"></block>
  <label text="──────────────────────"></label>
  <block type="style_font_size"></block>
  <block type="style_font_weight"></block>
  <block type="style_line_height"></block>
  <block type="style_letter_spacing"></block>
  <label text="──────────────────────"></label>
  <block type="style_size"></block>
  <block type="style_display"></block>
  <block type="style_opacity"></block>
  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
  <label text="──────────────────────"></label>
  <block type="style_padding"></block>
  <block type="style_margin"></block>
  <block type="style_list_style"></block>
</xml>
`;

export const defineBlocks = () => {
  const safeClass = (raw, fallback) =>
    (raw || fallback).toString()
                      .trim()
                      .replace(/\s+/g, '_')
                      .replace(/[^a-zA-Z0-9_\-가-힣]/g, '');

  // --- 블록 정의 시작 ---

  Blockly.Blocks['style_tag'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🎨 스타일 적용 대상')
        .appendField(new Blockly.FieldTextInput('container'), 'SELECTOR')
        .appendField('상태')
        .appendField(new Blockly.FieldDropdown([['기본(항상)', ''], ['마우스 올렸을 때', ':hover'], ['클릭했을 때', ':active']]), 'STATE');
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };

  Blockly.Blocks['style_tag_all'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🎨 전체 스타일');
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };
  Blockly.Blocks['style_font_size'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 크기').appendField(new Blockly.FieldTextInput('20'), 'SIZE').appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_font_weight'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 두께').appendField(new Blockly.FieldDropdown([['100 Thin', '100'], ['300 Light', '300'], ['400 Regular', '400'], ['500 Medium', '500'], ['700 Bold', '700'], ['900 Black', '900']]), 'WEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_line_height'] = {
    init() {
      this.appendDummyInput().appendField('📏 줄 간격').appendField(new Blockly.FieldTextInput('1.6'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_letter_spacing'] = {
    init() {
      this.appendDummyInput().appendField('↔️ 자간').appendField(new Blockly.FieldTextInput('0.05'), 'SPACE').appendField('em');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_size'] = {
    init() {
      this.appendDummyInput().appendField('📐 크기 너비').appendField(new Blockly.FieldTextInput('100%'), 'WIDTH').appendField('높이').appendField(new Blockly.FieldTextInput('auto'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_opacity'] = {
    init: function () {
      this.appendDummyInput().appendField('🏁 투명도').appendField(new Blockly.FieldNumber(100, 0, 100), 'OPACITY').appendField('%');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_text_align'] = {
    init() {
      this.appendDummyInput().appendField('📝 정렬').appendField(new Blockly.FieldDropdown([['왼쪽', 'left'], ['가운데', 'center'], ['오른쪽', 'right']]), 'ALIGN');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_border_radius'] = {
    init() {
      this.appendDummyInput().appendField('🔘 둥근 모서리').appendField(new Blockly.FieldTextInput('10'), 'RADIUS').appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_padding'] = {
    init() {
      this.appendDummyInput('MAIN')
        .appendField('📦 안쪽 여백')
        .appendField(new Blockly.FieldDropdown([
          ['전체', 'padding'],
          ['위+아래', 'vertical'],
          ['왼쪽+오른쪽', 'horizontal'],
          ['위', 'padding-top'],
          ['아래', 'padding-bottom'],
          ['왼쪽', 'padding-left'],
          ['오른쪽', 'padding-right']
        ], this.validate.bind(this)), 'SIDE')
      .appendField(new Blockly.FieldTextInput('0'), 'VAL1');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
    validate(newValue) {
    const mainInput = this.getInput('MAIN');
    if (this.getField('TEXT_PRE')) mainInput.removeField('TEXT_PRE');
    if (this.getField('TEXT_AND')) mainInput.removeField('TEXT_AND');
    if (this.getField('VAL2')) mainInput.removeField('VAL2');

    if (newValue === 'vertical' || newValue === 'horizontal') {
      const labelPre = (newValue === 'vertical') ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = (newValue === 'vertical') ? ' 아래: ' : ' 오른쪽: ';

      // 첫 번째 입력창(VAL1) 앞에 라벨 삽입
      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      // 뒤쪽에 라벨과 두 번째 입력창 추가
      mainInput.appendField(labelAnd, 'TEXT_AND')
                .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
      return newValue;
    }
  };

  Blockly.Blocks['style_margin'] = {
    init() {
      this.appendDummyInput('MAIN')
        .appendField('↔️ 바깥 여백')
        .appendField(new Blockly.FieldDropdown([
          ['전체', 'margin'],
          ['위+아래', 'vertical'],
          ['왼쪽+오른쪽', 'horizontal'],
          ['위', 'margin-top'],
          ['아래', 'margin-bottom'],
          ['왼쪽', 'margin-left'],
          ['오른쪽', 'margin-right']
        ],this.validate.bind(this)), 'SIDE')
          .appendField(new Blockly.FieldTextInput('0'), 'VAL1');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
    validate(newValue) {
    const mainInput = this.getInput('MAIN');
    if (this.getField('TEXT_PRE')) mainInput.removeField('TEXT_PRE');
    if (this.getField('TEXT_AND')) mainInput.removeField('TEXT_AND');
    if (this.getField('VAL2')) mainInput.removeField('VAL2');

    if (newValue === 'vertical' || newValue === 'horizontal') {
      const labelPre = (newValue === 'vertical') ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = (newValue === 'vertical') ? ' 아래: ' : ' 오른쪽: ';

      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      mainInput.appendField(labelAnd, 'TEXT_AND')
                .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
      return newValue;
    }
  };

  Blockly.Blocks['style_display'] = {
    init() {
      this.appendDummyInput()
        .appendField('📍 배치 방식')
        .appendField(new Blockly.FieldDropdown([
          ['가로로 나열 (inline-block)', 'inline-block'],
          ['한 줄 전체 차지 (block)', 'block'],
          ['글자처럼 취급 (inline)', 'inline'],
          ['숨기기 (none)', 'none']
        ]), 'DISPLAY');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    }
  };

  Blockly.Blocks['style_list_style'] = {
    init() {
      this.appendDummyInput().appendField("📋 리스트 스타일")
        .appendField("종류")
        .appendField(new Blockly.FieldDropdown([["● 기본(disc)", "disc"], ["○ 원(circle)", "circle"], ["■ 사각(square)", "square"], 
          ["1. 숫자(decimal)", "decimal"], ["a. 알파벳(lower-alpha)", "lower-alpha"], ["없음(none)", "none"]]), "TYPE")
        .appendField("마커 위치").appendField(new Blockly.FieldDropdown([["바깥(outside)", "outside"], ["안쪽(inside)", "inside"]]), "POSITION");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#ab47bc');
    }
  };

  // --- 제너레이터 정의 시작 (defineBlocks 함수 안에 포함) ---

  javascriptGenerator.forBlock['style_tag'] = function (block, generator) {
    const raw = (block.getFieldValue('SELECTOR') || 'container').trim();
    const cls = safeClass(raw.replace(/^[.#]/, ''), 'container');
    const state = block.getFieldValue('STATE') || '';
    const bodyCode = generator.statementToCode(block, 'BODY') || '';
    return `<style>\n.${cls}${state} {\n${bodyCode.trim()}}\n <\/style>\n`;
  };

  javascriptGenerator.forBlock['style_tag_all'] = function (block, generator) {
    const bodyCode = generator.statementToCode(block, 'BODY') || '';
    return `<style> \n#wrapper {\n ${bodyCode.trim()} }<\/style>\n`;
  };

  javascriptGenerator.forBlock['style_font_size'] = (block) => `font-size: ${block.getFieldValue('SIZE')}px;\n`;
  javascriptGenerator.forBlock['style_font_weight'] = (block) => `font-weight: ${block.getFieldValue('WEIGHT')};\n`;
  javascriptGenerator.forBlock['style_line_height'] = (block) => `line-height: ${block.getFieldValue('HEIGHT')};\n`;
  javascriptGenerator.forBlock['style_letter_spacing'] = (block) => `letter-spacing: ${block.getFieldValue('SPACE')}em;\n`;

  javascriptGenerator.forBlock['style_size'] = (block) => {
    const w = block.getFieldValue('WIDTH');
    const h = block.getFieldValue('HEIGHT');
    let code = '';
    if (w && w !== 'auto') code += `width: ${/^\d+$/.test(w) ? w + 'px' : w}; `;
    if (h && h !== 'auto') code += `height: ${/^\d+$/.test(h) ? h + 'px' : h}; `;
    return code + '\n';
  };

  javascriptGenerator.forBlock['style_list_style'] = (block) => {
    const type = block.getFieldValue('TYPE');
    const position = block.getFieldValue('POSITION');
    if (type === 'none') return `list-style: none;\npadding-left: 0;\n`;
    return `list-style-type: ${type};\nlist-style-position: ${position};\n`;
  };

  javascriptGenerator.forBlock['style_opacity'] = (block) => `opacity: ${block.getFieldValue('OPACITY') / 100};\n`;
  javascriptGenerator.forBlock['style_text_align'] = (block) => `text-align: ${block.getFieldValue('ALIGN')};\n`;
  javascriptGenerator.forBlock['style_border_radius'] = (block) => `border-radius: ${block.getFieldValue('RADIUS')}px;\n`;

  javascriptGenerator.forBlock['style_padding'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const v1 = block.getFieldValue('VAL1');
    const v2 = block.getFieldValue('VAL2');
    if (side === 'vertical') return `padding-top: ${withUnit(v1)}; padding-bottom: ${withUnit(v2)};\n`;
    if (side === 'horizontal') return `padding-left: ${withUnit(v1)}; padding-right: ${withUnit(v2)};\n`;
    return `${side}: ${withUnit(v1)};\n`;
  };

  javascriptGenerator.forBlock['style_margin'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const v1 = block.getFieldValue('VAL1');
    const v2 = block.getFieldValue('VAL2');
    if (side === 'vertical') return `margin-top: ${withUnit(v1)}; margin-bottom: ${withUnit(v2)};\n`;
    if (side === 'horizontal') return `margin-left: ${withUnit(v1)}; margin-right: ${withUnit(v2)};\n`;
    return `${side}: ${withUnit(v1)};\n`;
  };

  javascriptGenerator.forBlock['style_display'] = (block) => {
    const display = block.getFieldValue('DISPLAY');
    return `display: ${display};\n`;
  };
};
</script>