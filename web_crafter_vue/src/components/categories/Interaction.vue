<script>
import * as Blockly from 'blockly';
import { pythonGenerator } from 'blockly/python';

export const category = {
  label: '동작',
  color: '#ff7043',
  icon: '⚡'
}

export const toolbox = `
<xml>
  <block type="event_click"></block>
  <block type="event_page_load"></block>
  <block type="action_alert"></block>
</xml>
`

export const defineBlocks = () => {

  /* =========
      1. 클릭 이벤트 (ID 직접 입력 버전)
  ========= */
Blockly.Blocks['event_click'] = {
  init: function() {
    this.appendDummyInput()
        // [1] 아이콘 및 텍스트 설정
        .appendField(new Blockly.FieldImage(
          "https://www.gstatic.com/codesite/ph/images/star_on.gif", 
          15, 15, "*"
        ))
        .appendField('클래스가')
        .appendField(new Blockly.FieldTextInput("btn-group"), 'TARGET_CLASS')
        .appendField('인 요소들 클릭 시');

    // [2] 핵심 수정: 코드를 감싸기 위해 Statement Input('DO')은 살려둡니다.
    // 이것이 있어야 생성기에서 statementToCode를 통해 내부 코드를 { } 안에 넣을 수 있습니다.
    this.appendStatementInput('DO')
        .setCheck(null);
      
    // [3] Hat Block 설정
    // 위쪽 연결을 막아 최상단 블록으로 만듭니다.
    this.setPreviousStatement(false, null); 
    
    // 이벤트 블록 자체의 아래쪽 연결은 보통 막습니다. (이벤트끼리 연결하지 않으므로)
    // 대신 내용은 위 'DO' 안에 넣습니다.
    this.setNextStatement(false, null);
    
    this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.');
    this.setStyle('hat_blocks'); // Hat 스타일 적용
    this.setColour('#ff7043');
  }
};

// 파이썬 제너레이터 (하지만 내용은 HTML/JS 문자열 생성)
pythonGenerator.forBlock['event_click'] = function(block, generator) {
  var targetClass = block.getFieldValue('TARGET_CLASS');
  
  // [핵심 로직]
  // appendStatementInput('DO')에 연결된 블록들의 코드를 가져옵니다.
  var body = generator.statementToCode(block, 'DO');

  // 내부 코드가 없을 경우를 대비해 공백 처리 (선택사항)
  if (!body) body = '// 실행할 코드가 없습니다.\n';

  // 1. querySelectorAll로 요소 찾기
  // 2. forEach로 리스너 부착
  // 3. body(내부 블록 코드)를 addEventListener 안에 삽입
  var code = `
<script>
window.addEventListener('DOMContentLoaded', function() {
  var targets = document.querySelectorAll('.${targetClass}');
  
  if (targets.length > 0) {
    targets.forEach(function(el) {
      el.addEventListener('click', function() {
${body}      });
    });
  } else {
    console.warn("Class가 '${targetClass}'인 요소를 하나도 찾지 못했습니다.");
  }
});
<\/script>
`;
  return code;
};
  /* =========
      2. 페이지 로드 이벤트 (변경 없음)
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init() {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때');
      this.appendStatementInput('DO');
      this.setColour('#ff7043');
    }
  };

  pythonGenerator.forBlock['event_page_load'] = (block, generator) => {
    const body = generator.statementToCode(block, 'DO');
    return `
<script>
window.addEventListener('DOMContentLoaded', function() {
${body}
});
<\/script>
`;
  };

  /* =========
      3. 알림 동작 (변경 없음)
  ========= */
  Blockly.Blocks['action_alert'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔔 알림')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE');
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    }
  };

  pythonGenerator.forBlock['action_alert'] = (block, generator) => {
    const msg = block.getFieldValue('MESSAGE');
    return `alert(${JSON.stringify(msg)});\n`;
  };
};
</script>