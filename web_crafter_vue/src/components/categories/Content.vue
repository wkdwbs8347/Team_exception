<script>
import * as Blockly from 'blockly'
import { pythonGenerator } from 'blockly/python'

// ===== Content 카테고리 메타데이터 / 툴박스 UI를 구성하는 곳에서 이 값을 참조 =====
export const category = {
  label: '콘텐츠',
  color: '#00c853',
  icon: '🧩'
}

// ===== Content 툴박스 XML / 툴박스에 어떤 블록들을 보여줄지 =====
export const toolbox = `
<xml>
  <block type="content_button"></block>
  <block type="content_text"></block>
  <block type="content_image"></block>
  </xml>
`

// ===== Content 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  // 공통 로직: 안전한 이름 생성 및 좌표 데이터 추출 함수
  const getBlockMeta = (block, defaultName) => {
    const nameInput = block.getFieldValue('NAME') || defaultName;
    const safeName = nameInput.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_가-힣]/g, '');
    const displayId = `${safeName}_${block.id.substring(0, 3)}`;
    
    let x = 0, y = 0;
    if (block.data) {
      try {
        const coords = JSON.parse(block.data);
        x = coords.x || 0;
        y = coords.y || 0;
      } catch (e) { console.error("좌표 파싱 실패", e); }
    }
    return { displayId, x, y };
  };

  // ----- [1] 블록의 '모양' 정의 (이 부분이 있어야 툴박스에 나타납니다) -----
  
  // 버튼 블록 모양
  if (!Blockly.Blocks['content_button']) {
    Blockly.Blocks['content_button'] = {
      init() {
        this.appendDummyInput().appendField("🆗 버튼");
        this.appendDummyInput().appendField("이름:").appendField(new Blockly.FieldTextInput("버튼"), "NAME");
        this.appendDummyInput().appendField("라벨:").appendField(new Blockly.FieldTextInput("클릭"), "LABEL");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }

  // 텍스트 블록 모양
  if (!Blockly.Blocks['content_text']) {
    Blockly.Blocks['content_text'] = {
      init() {
        this.appendDummyInput().appendField("📝 텍스트");
        this.appendDummyInput().appendField("이름:").appendField(new Blockly.FieldTextInput("텍스트_요소"), "NAME");
        this.appendDummyInput().appendField("내용:").appendField(new Blockly.FieldTextInput("내용을 입력하세요"), "TEXT");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }

  // 이미지 블록 모양
  if (!Blockly.Blocks['content_image']) {
    Blockly.Blocks['content_image'] = {
      init() {
        this.appendDummyInput().appendField("🖼️ 이미지");
        this.appendDummyInput().appendField("이름:").appendField(new Blockly.FieldTextInput("이미지_요소"), "NAME");
        this.appendDummyInput().appendField("SRC:").appendField(new Blockly.FieldTextInput("https://via.placeholder.com/150"), "SRC");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#00c853');
      }
    };
  }

  // ----- [2] 블록이 생성할 'HTML 코드' 정의 -----

  pythonGenerator.forBlock['content_button'] = (block) => {
    // getBlockMeta에서 safeName을 직접 가져오거나, displayId 대신 safeName을 클래스에 넣으세요.
    const nameInput = block.getFieldValue('NAME') || '버튼';
    const safeName = nameInput.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_가-힣]/g, '');
    
    const { x, y } = getBlockMeta(block, '버튼');
    const label = block.getFieldValue('LABEL');

    // class에는 깔끔한 safeName만 넣고, 시스템 연동은 data-block-id로만 합니다.
    return `<button class="${safeName}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); color:inherit">${label}</button>\n`;
  };

  // [2] 텍스트 생성기 수정
  pythonGenerator.forBlock['content_text'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '텍스트';
    const safeName = nameInput.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_가-힣]/g, '');
    const { x, y } = getBlockMeta(block, '텍스트');
    const text = block.getFieldValue('TEXT');
    
    return `<p class="${safeName}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); margin:0;">${text}</p>\n`;
  };

  // [3] 이미지 생성기 수정 (복사해서 덮어씌우세요)
    pythonGenerator.forBlock['content_image'] = (block) => {
      const nameInput = block.getFieldValue('NAME') || '이미지';
      const safeName = nameInput.replace(/\s+/g, '_').replace(/[^a-zA-Z0-9_가-힣]/g, '');
      const { x, y } = getBlockMeta(block, '이미지');
      
      // 이 부분이 핵심입니다: SRC 값을 확실히 가져옵니다.
      const imgSrc = block.getFieldValue('SRC') || 'https://via.placeholder.com/150';
      
      // 문자열 생성 시 오타 방지를 위해 명확하게 줄바꿈과 따옴표를 확인하세요.
      return `<img class="${safeName}" src="${imgSrc}" data-block-id="${block.id}" data-x="${x}" data-y="${y}" style="position: absolute; transform: translate(${x}px, ${y}px); display: block;" />\n`;
    };
}
</script>