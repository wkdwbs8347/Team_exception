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
  <block type="script_tag"></block>
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
      
    // 이제 이 블록은 스크립트 내부에 들어가는 문장 블록입니다.
    this.setPreviousStatement(true, null);
    this.setNextStatement(true, null);
    
    this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.');
    this.setStyle('hat_blocks'); // Hat 스타일 적용
    this.setColour('#ff7043');
  }
};

// 파이썬 제너레이터 (이벤트 위임 방식으로 수정됨)
pythonGenerator.forBlock['event_click'] = function(block, generator) {
  var targetClass = block.getFieldValue('TARGET_CLASS');
  
  // 내부 코드 가져오기
  var body = generator.statementToCode(block, 'DO');

  // [핵심 변경 사항]
  // 1. DOMContentLoaded를 기다리지 않습니다. (document는 이미 존재하므로)
  // 2. querySelectorAll로 미리 찾지 않고, 'click'이 발생했을 때 그 순간 검사합니다.
  // 3. e.target.closest('.' + targetClass)를 사용하여 클릭된 요소 자신이나 부모가 해당 클래스인지 확인합니다.
  var code = `(function() {
  document.addEventListener('click', function(e) {
    var target = e.target.closest('.${targetClass}');
    if (target) {
${body}
    }
  });
})();\n`;
  return code;
};
  /* =========
      2. 페이지 로드 이벤트 (변경 없음)
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init() {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때');
      this.appendStatementInput('DO');
      // 내부에 들어가는 문장 블록으로 설정
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#ff7043');
    }
  };

  pythonGenerator.forBlock['event_page_load'] = (block, generator) => {
    const body = generator.statementToCode(block, 'DO');
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`;
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

  /* =========
      0. 스크립트 태그 래퍼 블록
      이 블록은 최상단 블록(hat)이며 내부에 여러 이벤트/동작 블록을 포함합니다.
  ========= */
  Blockly.Blocks['script_tag'] = {
    init() {
      this.appendDummyInput().appendField('📜 스크립트');
      this.appendStatementInput('BODY').setCheck(null).appendField('내용');
      // 최상단 블록으로 사용
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ff7043');
      this.setTooltip('여기에 스크립트 동작(이벤트 등)을 넣으세요.');
      this.setStyle('hat_blocks');
    }
  };

  pythonGenerator.forBlock['script_tag'] = (block, generator) => {
    const body = generator.statementToCode(block, 'BODY');
    return `<script>\n${body}<\/script>\n`;
  };
};
</script>