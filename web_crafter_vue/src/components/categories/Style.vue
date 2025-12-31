<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨'
}

export const toolbox = `
<xml>
  <block type="style_tag"></block>
  <label text="──────────────────────"></label>
  <block type="style_bg_color"></block>
  <block type="style_text_color"></block>
  <block type="style_size"></block>
 
  <label text="──────────────────────"></label>

  <block type="style_padding"></block>
  <block type="style_margin"></block>

  <label text="──────────────────────"></label>

  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
</xml>
`

export const defineBlocks = () => {

// 생성기는 아무것도 만들지 않음
pythonGenerator.forBlock['toolbox_separator'] = () => '';
  // 1. 스타일 적용 대상 (부모)
  if (!Blockly.Blocks['style_tag']) {
    Blockly.Blocks['style_tag'] = {
      init() {
        this.appendDummyInput()
            .appendField('🎨 스타일 적용 대상')
            .appendField(new Blockly.FieldTextInput('요소_이름'), 'SELECTOR');
        this.appendStatementInput('BODY').setCheck(null).appendField('내용');
        this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_tag'] = (block) => {
    const selector = (block.getFieldValue('SELECTOR') || '요소').trim();
    const body = pythonGenerator.statementToCode(block, 'BODY');
    return `<style>\n.${selector} {\n${body}}\n</style>\n`;
  };

  // 2. 배경 색상
  if (!Blockly.Blocks['style_bg_color']) {
    Blockly.Blocks['style_bg_color'] = {
      init() {
        this.appendDummyInput().appendField("🎨 배경색").appendField(new Blockly.FieldTextInput("#ffffff"), "COLOR");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_bg_color'] = (block) => `  background-color: ${block.getFieldValue('COLOR')} !important;\n`;

  // 3. 글자 색상
  if (!Blockly.Blocks['style_text_color']) {
    Blockly.Blocks['style_text_color'] = {
      init() {
        this.appendDummyInput().appendField("🎨 글자색").appendField(new Blockly.FieldTextInput("#000000"), "COLOR");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_text_color'] = (block) => `  color: ${block.getFieldValue('COLOR')} !important;\n`;

  // 4. 크기
  if (!Blockly.Blocks['style_size']) {
    Blockly.Blocks['style_size'] = {
      init() {
        this.appendDummyInput().appendField("📐 크기 너비").appendField(new Blockly.FieldTextInput("200"), "WIDTH")
            .appendField("높이").appendField(new Blockly.FieldTextInput("100"), "HEIGHT");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_size'] = (block) => {
    const w = block.getFieldValue('WIDTH');
    const h = block.getFieldValue('HEIGHT');
    return `  width: ${w}px !important;\n  height: ${h}px !important;\n`;
  };

/* ===== [통합] 선택형 패딩 블록 (전체/위/아래/왼/오) ===== */
  if (!Blockly.Blocks['style_padding']) {
    Blockly.Blocks['style_padding'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 패딩")
            .appendField(new Blockly.FieldDropdown([
                ["전체", "all"], 
                ["위쪽", "top"], 
                ["아래쪽", "bottom"], 
                ["왼쪽", "left"], 
                ["오른쪽", "right"]
            ]), "SIDE")
            .appendField(new Blockly.FieldTextInput("10"), "VAL")
            .appendField("px");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }

  pythonGenerator.forBlock['style_padding'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const val = block.getFieldValue('VAL');
    const property = side === 'all' ? 'padding' : `padding-${side}`;
    return `  ${property}: ${val}px !important;\n`;
  };

  /* ===== [통합] 선택형 마진 블록 (전체/위/아래/왼/오) ===== */
  if (!Blockly.Blocks['style_margin']) {
    Blockly.Blocks['style_margin'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 마진")
            .appendField(new Blockly.FieldDropdown([
                ["전체", "all"], 
                ["위쪽", "top"], 
                ["아래쪽", "bottom"], 
                ["왼쪽", "left"], 
                ["오른쪽", "right"]
            ]), "SIDE")
            .appendField(new Blockly.FieldTextInput("10"), "VAL")
            .appendField("px");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }

  pythonGenerator.forBlock['style_margin'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const val = block.getFieldValue('VAL');
    const property = side === 'all' ? 'margin' : `margin-${side}`;
    return `  ${property}: ${val}px !important;\n`;
  };

  // 7. 정렬
  if (!Blockly.Blocks['style_text_align']) {
    Blockly.Blocks['style_text_align'] = {
      init() {
        this.appendDummyInput().appendField("📝 정렬").appendField(new Blockly.FieldDropdown([["왼쪽", "left"], ["가운데", "center"], ["오른쪽", "right"]]), "ALIGN");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_text_align'] = (block) => {
    const align = block.getFieldValue('ALIGN');
    const flexAlign = align === 'left' ? 'flex-start' : (align === 'center' ? 'center' : 'flex-end');
    return `  display: inline-flex !important;\n  justify-content: ${flexAlign} !important;\n  text-align: ${align} !important;\n`;
  };

  // 8. 둥근 모서리
  if (!Blockly.Blocks['style_border_radius']) {
    Blockly.Blocks['style_border_radius'] = {
      init() {
        this.appendDummyInput().appendField("🔘 둥근 모서리").appendField(new Blockly.FieldTextInput("10"), "RADIUS").appendField("px");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  pythonGenerator.forBlock['style_border_radius'] = (block) => `  border-radius: ${block.getFieldValue('RADIUS')}px !important;\n`;
}
</script>