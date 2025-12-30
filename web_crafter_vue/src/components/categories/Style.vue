<script>
import * as Blockly from 'blockly'
// 파이썬 생성기 사용 (원하시는 대로 유지)
import { pythonGenerator } from 'blockly/python'

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨'
}

export const toolbox = `
<xml>
  <block type="style_tag"></block>
  <block type="style_color"></block>
  <block type="style_size"></block>
  <block type="style_padding"></block>
  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
</xml>
`

export const defineBlocks = () => {

  /* ===== style_tag: 부모 블록 (껍데기 담당) ===== */
  if (!Blockly.Blocks['style_tag']) {
    Blockly.Blocks['style_tag'] = {
      init() {
        this.appendDummyInput()
            .appendField('🎨 스타일')
            .appendField('적용 대상')
            .appendField(new Blockly.FieldTextInput('my-element'), 'SELECTOR');
        this.appendStatementInput('BODY').setCheck(null).appendField('내용');
        this.setPreviousStatement(false, null);
        this.setNextStatement(false, null);
        this.setColour('#ab47bc');
        this.setTooltip('여기에 스타일 규칙을 넣으세요.');
        this.setStyle('hat_blocks');
      }
    };
  }
  pythonGenerator.forBlock['style_tag'] = (block) => {
    // 1. 여기서만 선택자를 처리합니다.
    const selector = '.' + (block.getFieldValue('SELECTOR') || '').trim();
    // 2. 내부 블록들의 코드를 가져옵니다.
    const body = pythonGenerator.statementToCode(block, 'BODY');
    // 3. 최종 조립: <style> 선택자 { 내용 } </style>
    return `<style>\n${selector} {\n${body}}\n</style>\n`;
  };

  /* ===== style_color: 자식 (속성만 리턴) ===== */
  if (!Blockly.Blocks['style_color']) {
    Blockly.Blocks['style_color'] = {
      init() {
        this.appendDummyInput()
            .appendField("🎨 색상")
            .appendField("배경")
            .appendField(new Blockly.FieldTextInput('#ffffff'), "BG_COLOR")
            .appendField("글자")
            .appendField(new Blockly.FieldTextInput('#000000'), "TEXT_COLOR");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
        this.setTooltip("배경색과 글자색 설정");
      }
    };
  }
  pythonGenerator.forBlock['style_color'] = (block) => {
    const bgColor = block.getFieldValue('BG_COLOR');
    const textColor = block.getFieldValue('TEXT_COLOR');
    return `  background-color: ${bgColor};\n  color: ${textColor};\n`;
  };

  /* ===== style_size: 자식 (속성만 리턴) ===== */
  if (!Blockly.Blocks['style_size']) {
    Blockly.Blocks['style_size'] = {
      init() {
        this.appendDummyInput()
            .appendField("📏 크기")
            .appendField("너비")
            .appendField(new Blockly.FieldTextInput("200"), "WIDTH")
            .appendField("높이")
            .appendField(new Blockly.FieldTextInput("100"), "HEIGHT");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_size'] = (block) => {
    const width = block.getFieldValue('WIDTH');
    const height = block.getFieldValue('HEIGHT');
    return `  width: ${width}px;\n  height: ${height}px;\n`;
  };

  /* ===== style_padding: 자식 (속성만 리턴) ===== */
  if (!Blockly.Blocks['style_padding']) {
    Blockly.Blocks['style_padding'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 여백")
            .appendField("안쪽")
            .appendField(new Blockly.FieldNumber(20, 0), "PADDING")
            .appendField("바깥")
            .appendField(new Blockly.FieldNumber(10, 0), "MARGIN");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_padding'] = (block) => {
    const padding = block.getFieldValue('PADDING');
    const margin = block.getFieldValue('MARGIN');
    return `  padding: ${padding}px;\n  margin: ${margin}px;\n`;
  };

  /* ===== style_text_align: 자식 (속성만 리턴) ===== */
  if (!Blockly.Blocks['style_text_align']) {
    Blockly.Blocks['style_text_align'] = {
      init() {
        this.appendDummyInput()
            .appendField("📝 정렬")
            .appendField(new Blockly.FieldDropdown([
              ["왼쪽", "left"],
              ["가운데", "center"],
              ["오른쪽", "right"]
            ]), "ALIGN");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_text_align'] = (block) => {
    const align = block.getFieldValue('ALIGN');
    return `  text-align: ${align};\n`;
  };

  /* ===== style_border_radius: 자식 (속성만 리턴) ===== */
  if (!Blockly.Blocks['style_border_radius']) {
    Blockly.Blocks['style_border_radius'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔘 둥근 모서리")
            .appendField(new Blockly.FieldNumber(10, 0, 100), "RADIUS")
            .appendField("px");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_border_radius'] = (block) => {
    const radius = block.getFieldValue('RADIUS');
    return `  border-radius: ${radius}px;\n`;
  };
}
</script>