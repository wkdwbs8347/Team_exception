<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨'
};

export const toolbox = `
<xml>
  <block type="style_tag"></block>
  <label text="──────────────────────"></label>
  <block type="style_font_text_style"></block>
  <block type="style_size"></block>
  <block type="style_opacity"></block>
  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
  <label text="──────────────────────"></label>
  <block type="style_padding"></block>
  <block type="style_margin"></block> </xml>
`;

export const defineBlocks = () => {
  // 1. 스타일 적용 대상 (Wrapper)
  Blockly.Blocks['style_tag'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("🎨 스타일 적용 대상")
          .appendField(new Blockly.FieldTextInput(".container"), "SELECTOR");
      this.appendStatementInput("BODY")
          .setCheck(null)
          .appendField("속성들");
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#ab47bc');
    }
  };

  // 2. 글자 설정
  Blockly.Blocks['style_font_text_style'] = {
    init() {
      this.appendDummyInput()
          .appendField("🔠 글자 설정")
          .appendField("크기").appendField(new Blockly.FieldTextInput("20"), "SIZE").appendField("px ,")
          .appendField("두께").appendField(new Blockly.FieldDropdown([["보통", "normal"], ["진하게", "bold"]]), "WEIGHT");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 3. 크기 설정
  Blockly.Blocks['style_size'] = {
    init() {
      this.appendDummyInput()
          .appendField("📐 크기 너비").appendField(new Blockly.FieldTextInput("100%"), "WIDTH")
          .appendField("높이").appendField(new Blockly.FieldTextInput("auto"), "HEIGHT");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 4. 투명도
  Blockly.Blocks['style_opacity'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("🏁 투명도").appendField(new Blockly.FieldNumber(100, 0, 100), "OPACITY").appendField("%");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 5. 정렬
  Blockly.Blocks['style_text_align'] = {
    init() {
      this.appendDummyInput()
          .appendField("📝 정렬").appendField(new Blockly.FieldDropdown([["왼쪽", "left"], ["가운데", "center"], ["오른쪽", "right"]]), "ALIGN");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 6. 둥근 모서리
  Blockly.Blocks['style_border_radius'] = {
    init() {
      this.appendDummyInput()
          .appendField("🔘 둥근 모서리").appendField(new Blockly.FieldTextInput("10"), "RADIUS").appendField("px");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 7. 안쪽 여백 (Padding)
  Blockly.Blocks['style_padding'] = {
    init() {
      this.appendDummyInput()
          .appendField("📦 안쪽 여백")
          .appendField(new Blockly.FieldDropdown([
            ["전체", "padding"], 
            ["위", "padding-top"], 
            ["아래", "padding-bottom"],
            ["왼쪽", "padding-left"],
            ["오른쪽", "padding-right"]
          ]), "SIDE")
          .appendField(new Blockly.FieldTextInput("10"), "VAL").appendField("px");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };

  // 8. ✅ [누락되었던 부분 추가] 바깥 여백 (Margin)
  Blockly.Blocks['style_margin'] = {
    init() {
      this.appendDummyInput()
          .appendField("↔️ 바깥 여백")
          .appendField(new Blockly.FieldDropdown([
            ["전체", "margin"], 
            ["위", "margin-top"], 
            ["아래", "margin-bottom"],
            ["왼쪽", "margin-left"],
            ["오른쪽", "margin-right"]
          ]), "SIDE")
          .appendField(new Blockly.FieldTextInput("10"), "VAL").appendField("px");
      this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
    }
  };
};

// ==================== 제너레이터 ====================

javascriptGenerator.forBlock['style_tag'] = function(block, generator) {
  const selector = block.getFieldValue('SELECTOR') || '.container';
  const bodyCode = generator.statementToCode(block, 'BODY');
  return `<style>\n  ${selector} {\n    ${bodyCode.trim()}\n  }\n</style>\n`;
};

javascriptGenerator.forBlock['style_font_text_style'] = (block) => `font-size: ${block.getFieldValue('SIZE')}px; font-weight: ${block.getFieldValue('WEIGHT')};\n`;

javascriptGenerator.forBlock['style_size'] = (block) => {
  const w = block.getFieldValue('WIDTH');
  const h = block.getFieldValue('HEIGHT');
  let code = '';
  if (w && w !== 'auto') code += `width: ${/^\d+$/.test(w) ? w + 'px' : w}; `;
  if (h && h !== 'auto') code += `height: ${/^\d+$/.test(h) ? h + 'px' : h}; `;
  return code + '\n';
};

javascriptGenerator.forBlock['style_opacity'] = (block) => `opacity: ${block.getFieldValue('OPACITY') / 100};\n`;

javascriptGenerator.forBlock['style_text_align'] = (block) => `text-align: ${block.getFieldValue('ALIGN')};\n`;

javascriptGenerator.forBlock['style_border_radius'] = (block) => `border-radius: ${block.getFieldValue('RADIUS')}px;\n`;

javascriptGenerator.forBlock['style_padding'] = (block) => `${block.getFieldValue('SIDE')}: ${block.getFieldValue('VAL')}px;\n`;

// ✅ Margin 제너레이터 (이미 있었지만 정의와 짝을 맞춤)
javascriptGenerator.forBlock['style_margin'] = (block) => `${block.getFieldValue('SIDE')}: ${block.getFieldValue('VAL')}px;\n`;
</script>