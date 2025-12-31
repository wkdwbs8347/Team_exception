<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

// ===== Layout 카테고리 메타데이터 =====
export const category = {
  label: '구조',
  color: '#4c97ff',
  icon: '📐'
}

// ===== Layout 툴박스 XML =====
export const toolbox = `<xml>
  <block type="layout_div"></block>
  <block type="layout_article"></block>
  <block type="layout_ul"></block>
  <block type="layout_li"></block>
</xml>`

// ===== Layout 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  // 공통 로직: 드래그 좌표 및 이름 추출
  const getBlockMeta = (block, defaultName) => {
    const nameInput = block.getFieldValue('ELEMENT_CLASS') || defaultName;
    const safeName = nameInput.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_가-힣]/g, '');
    
    let x = 0, y = 0;
    if (block.data) {
      try {
        const coords = JSON.parse(block.data);
        x = coords.x || 0;
        y = coords.y || 0;
      } catch (e) { console.error("좌표 파싱 실패", e); }
    }
    return { safeName, x, y };
  };

  // ----- [1] DIV 박스 -----
  if (!Blockly.Blocks['layout_div']) {
    Blockly.Blocks['layout_div'] = {
      init() {
        this.appendDummyInput().appendField("📦 DIV 박스");
        this.appendDummyInput()
          .appendField("클래스명:")
          .appendField(new Blockly.FieldTextInput("box-container"), "ELEMENT_CLASS");
        this.appendStatementInput("CONTENT").setCheck(null); 
        this.setPreviousStatement(true, "ELEMENT"); // 무적 해제: ELEMENT 타입 지정
        this.setNextStatement(true, "ELEMENT");
        this.setColour('#4c97ff');
      }
    };
  }
  pythonGenerator.forBlock['layout_div'] = (block) => {
    const { safeName, x, y } = getBlockMeta(block, 'layout_div');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<div class="${safeName}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); border:1px solid #ccc; padding:10px; background: rgba(255,255,255,0.5); min-width: 50px; min-height: 50px;">\n${content}</div>\n`;
  };

  // ----- [2] 게시물 박스 (Article) -----
  if (!Blockly.Blocks['layout_article']) {
    Blockly.Blocks['layout_article'] = {
      init() {
        this.appendDummyInput().appendField("📦 게시물 박스");
        this.appendDummyInput()
          .appendField("클래스명:")
          .appendField(new Blockly.FieldTextInput("article-box"), "ELEMENT_CLASS");
        this.appendStatementInput("CONTENT").setCheck(null);
        this.setPreviousStatement(true, "ELEMENT");
        this.setNextStatement(true, "ELEMENT");
        this.setColour('#4c97ff');
      }
    };
  }
  pythonGenerator.forBlock['layout_article'] = (block) => {
    const { safeName, x, y } = getBlockMeta(block, 'layout_article');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<article class="${safeName}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); border:1px solid #ccc; padding:10px; background: rgba(255,255,255,0.5); min-width: 50px; min-height: 50px;">\n${content}</article>\n`;
  };

  // ----- [3] UI 박스 (UL) -----
  if (!Blockly.Blocks['layout_ul']) {
    Blockly.Blocks['layout_ul'] = {
      init() {
        this.appendDummyInput().appendField("📦 리스트 박스(UL)");
        this.appendDummyInput()
          .appendField("클래스명:")
          .appendField(new Blockly.FieldTextInput("list-container"), "ELEMENT_CLASS");
        this.appendStatementInput("CONTENT").setCheck("LI"); // 오직 LI만 허용
        this.setPreviousStatement(true, "ELEMENT");
        this.setNextStatement(true, "ELEMENT");
        this.setColour('#4c97ff');
      }
    };
  }
  pythonGenerator.forBlock['layout_ul'] = (block) => {
    const { safeName, x, y } = getBlockMeta(block, 'layout_ul');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    return `<ul class="${safeName}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); list-style: none; padding: 15px; border: 1px solid #4c97ff; background: #fff; min-width: 100px; margin: 0;">\n${content}</ul>\n`;
  };

  // ----- [4] LI 항목 (자식) -----
  if (!Blockly.Blocks['layout_li']) {
    Blockly.Blocks['layout_li'] = {
      init() {
        this.appendDummyInput().appendField("📦 리스트 항목(LI)");
        this.appendDummyInput()
          .appendField("클래스명:")
          .appendField(new Blockly.FieldTextInput("list-item"), "ELEMENT_CLASS");
        this.appendStatementInput("CONTENT").setCheck(null); 
        this.setPreviousStatement(true, "LI"); // 부모 UL의 입구와 타입 일치
        this.setNextStatement(true, "LI");
        this.setColour('#4c97ff');
      }
    };
  }
  pythonGenerator.forBlock['layout_li'] = (block) => {
    const { safeName } = getBlockMeta(block, 'layout_li');
    const content = pythonGenerator.statementToCode(block, 'CONTENT');
    // 자식은 absolute 좌표를 제거하여 부모 UL을 따라다니게 설정함
    return `<li class="${safeName}" style="list-style: none; padding: 8px; border-bottom: 1px dashed #ccc; background: #fdfdfd; position: relative;">\n${content}</li>\n`;
  };
}
</script>