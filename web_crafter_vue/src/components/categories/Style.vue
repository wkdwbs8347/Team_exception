<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

// ===== Style 카테고리 메타데이터 =====
export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨'
}

// ===== Style 툴박스 XML =====
export const toolbox = `
<xml>
  <block type="style_color"></block>
  <block type="style_size"></block>
  <block type="style_padding"></block>
  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
</xml>
`

// ===== Style 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  
  // [1] 색상 설정 (배경색 + 텍스트 색)
  if (!Blockly.Blocks['style_color']) {
    Blockly.Blocks['style_color'] = {
      init() {
        this.appendDummyInput()
            .appendField("🎨 색상")
            .appendField("배경")
            .appendField(new Blockly.FieldTextInput('#ffffff'), "BG_COLOR")
            .appendField("글자")
            .appendField(new Blockly.FieldTextInput('#000000'), "TEXT_COLOR");
        this.appendStatementInput("CONTENT").setCheck(null);
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
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="background-color:${bgColor}; color:${textColor}; padding:10px;">\n${content}</div>\n`;
  };

  // [2] 크기 설정 (너비 + 높이)
  if (!Blockly.Blocks['style_size']) {
    Blockly.Blocks['style_size'] = {
      init() {
        this.appendDummyInput()
            .appendField("📏 크기")
            .appendField("너비")
            .appendField(new Blockly.FieldTextInput("200"), "WIDTH")
            .appendField("높이")
            .appendField(new Blockly.FieldTextInput("100"), "HEIGHT");
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
        this.setTooltip("너비와 높이 설정 (px)");
      }
    };
  }
  pythonGenerator.forBlock['style_size'] = (block) => {
    const width = block.getFieldValue('WIDTH');
    const height = block.getFieldValue('HEIGHT');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="width:${width}px; height:${height}px; border:1px solid #ddd;">\n${content}</div>\n`;
  };

  // [3] 여백 설정 (padding + margin)
  if (!Blockly.Blocks['style_padding']) {
    Blockly.Blocks['style_padding'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 여백")
            .appendField("안쪽")
            .appendField(new Blockly.FieldNumber(20, 0), "PADDING")
            .appendField("바깥")
            .appendField(new Blockly.FieldNumber(10, 0), "MARGIN");
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
        this.setTooltip("안쪽 여백(padding)과 바깥 여백(margin) 설정");
      }
    };
  }
  pythonGenerator.forBlock['style_padding'] = (block) => {
    const padding = block.getFieldValue('PADDING');
    const margin = block.getFieldValue('MARGIN');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="padding:${padding}px; margin:${margin}px; background:#f5f5f5;">\n${content}</div>\n`;
  };

  // [4] 텍스트 정렬
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
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
        this.setTooltip("텍스트 정렬");
      }
    };
  }
  pythonGenerator.forBlock['style_text_align'] = (block) => {
    const align = block.getFieldValue('ALIGN');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="text-align:${align};">\n${content}</div>\n`;
  };

  // [5] 둥근 모서리
  if (!Blockly.Blocks['style_border_radius']) {
    Blockly.Blocks['style_border_radius'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔘 둥근 모서리")
            .appendField(new Blockly.FieldNumber(10, 0, 100), "RADIUS")
            .appendField("px");
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
        this.setTooltip("모서리를 둥글게");
      }
    };
  }
  pythonGenerator.forBlock['style_border_radius'] = (block) => {
    const radius = block.getFieldValue('RADIUS');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div style="border-radius:${radius}px; border:2px solid #ddd; padding:15px; overflow:hidden;">\n${content}</div>\n`;
  };
}
</script>