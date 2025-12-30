<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

// ===== Content 카테고리 메타데이터 =====
export const category = {
  label: '콘텐츠',
  color: '#00c853',
  icon: '🧩'
}

// ===== Content 툴박스 XML =====
export const toolbox = `
<xml>
  <block type="content_button"></block>
  <block type="content_text"></block>
  <block type="content_image"></block>
</xml>
`

// ===== Content 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  // [1] 버튼
  if (!Blockly.Blocks['content_button']) {
    Blockly.Blocks['content_button'] = {
      init() {
        this.appendDummyInput()
            .appendField("🆗 버튼");
        this.appendDummyInput()
            .appendField("이름:")
            .appendField(new Blockly.FieldTextInput("버튼"), "NAME");
        this.appendDummyInput()
            .appendField("라벨:")
            .appendField(new Blockly.FieldTextInput("클릭"), "LABEL");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }
  pythonGenerator.forBlock['content_button'] = (block) => {
    const label = block.getFieldValue('LABEL');
    const name = block.getFieldValue('NAME') || '';
    const safeName = name.replace(/\"/g, '&quot;');
    const classAttr = name ? ` class=\"${safeName}\"` : '';
    return `<button${classAttr} style=\"color:inherit\">${label}</button>\n`;
};


  // [2] 텍스트
  if (!Blockly.Blocks['content_text']) {
    Blockly.Blocks['content_text'] = {
      init() {
        this.appendDummyInput()
            .appendField("📝 텍스트");
        this.appendDummyInput()
            .appendField("이름:")
            .appendField(new Blockly.FieldTextInput("텍스트 요소"), "NAME");
        this.appendDummyInput()
            .appendField("내용:")
            .appendField(new Blockly.FieldTextInput("내용"), "TEXT");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }
  pythonGenerator.forBlock['content_text'] = (block) => {
    const text = block.getFieldValue('TEXT');
    const name = block.getFieldValue('NAME') || '';
    const safeName = name.replace(/\"/g, '&quot;');
    const classAttr = name ? ` class=\"${safeName}\"` : '';
    return `<p${classAttr}>${text}</p>\n`;
  };

  // [3] 이미지
  if (!Blockly.Blocks['content_image']) {
    Blockly.Blocks['content_image'] = {
      init() {
        this.appendDummyInput()
            .appendField("🖼️ 이미지");
        this.appendDummyInput()
            .appendField("이름:")
            .appendField(new Blockly.FieldTextInput("이미지 요소"), "NAME");
        this.appendDummyInput()
            .appendField("SRC:")
            .appendField(new Blockly.FieldTextInput("https://via.placeholder.com/150"), "SRC");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }
  pythonGenerator.forBlock['content_image'] = (block) => {
    const src = block.getFieldValue('SRC');
    const name = block.getFieldValue('NAME') || '';
    const safeName = name.replace(/\"/g, '&quot;');
    const classAttr = name ? ` class=\"${safeName}\"` : '';
    return `<img src=\"${src}\"${classAttr} style=\"max-width: 100%; border-radius: 8px;\" />\n`;
  };
}
</script>