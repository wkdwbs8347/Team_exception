<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python' // 이름은 python이지만 HTML 생성용으로 사용

// ===== Layout 카테고리 메타데이터 =====
export const category = {
  label: '구조',
  color: '#4c97ff',
  icon: '📐'
}

// ===== Layout 툴박스 XML =====
export const toolbox = `<xml>
  <block type="layout_div"></block>
</xml>`

// ===== Layout 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  // [수정됨] DIV 박스 (ID 입력 기능 추가)
  if (!Blockly.Blocks['layout_div']) {
    Blockly.Blocks['layout_div'] = {
      init() {
        this.appendDummyInput()
          .appendField("Class:")
          .appendField(new Blockly.FieldTextInput("btn-group"), "ELEMENT_CLASS");
          
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#4c97ff');
      }
    };
  }

  // HTML 생성 로직
  pythonGenerator.forBlock['layout_div'] = (block) => {
    // 입력받은 클래스명 가져오기
    const className = block.getFieldValue('ELEMENT_CLASS');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    
    // HTML 태그에 class="..." 속성 추가
    return `<div class="${className}" style="border:1px solid #ccc; padding:10px; margin:5px; cursor:pointer;">\n${content}</div>\n`;
  };
}
</script>