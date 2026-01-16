<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript';

/* [PART 1] 카테고리 설정
  - 블록 메뉴에서 '폼' 카테고리의 이름, 색상, 아이콘을 지정합니다.
*/
export const category = {
  label: '폼',
  color: '#43a047',
  icon: '📝'
}

/* [PART 2] 도구함(Toolbox) 구성
  - 사용자가 화면에서 조립할 수 있는 폼 관련 블록 5종을 정의합니다.
*/
export const toolbox = `
  <xml>
    <block type="form_container"></block>  
    <block type="form_label"></block>      
    <block type="form_input"></block>      
    <block type="form_textarea"></block>   
    <block type="form_submit"></block>     
    </xml>
`

/* [PART 3] 블록 정의 (UI 및 기능 설정)
  - 각 블록이 화면에서 어떻게 보일지, 어떤 설정값(드롭다운, 텍스트)을 가질지 정의합니다.
*/
export const defineBlocks = () => {
  
  // 1. 폼 컨테이너: HTML의 <form> 태그 역할
  // - 폼의 가장 바깥 테두리와 안쪽 여백을 생성하여 입력 요소들을 그룹화합니다.
  Blockly.Blocks['form_container'] = {
    init: function() {
      this.appendDummyInput().appendField("폼 박스 생성");
      this.appendStatementInput("CONTENT").setCheck(null); // 다른 폼 블록들이 들어갈 자박스
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#43a047');
    }
  }

  // 2. 텍스트 입력창: HTML의 <input> 태그 역할
  // - 데이터 성격에 따라 텍스트, 비밀번호, 이메일, 숫자 타입을 선택할 수 있습니다.
  Blockly.Blocks['form_input'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("입력창 유형")
          .appendField(new Blockly.FieldDropdown([
            ["텍스트", "text"], ["비밀번호", "password"], ["이메일", "email"], ["숫자", "number"]
          ]), "TYPE")
          .appendField(new Blockly.FieldTextInput("내용을 입력하세요"), "PLACEHOLDER");
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#43a047');
    }
  }

  // 3. 여러 줄 입력창: HTML의 <textarea> 태그 역할
  // - 긴 문장의 문의 내용이나 설명글을 입력받기에 적합한 넓은 입력창입니다.
  Blockly.Blocks['form_textarea'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("여러 줄 입력창")
          .appendField(new Blockly.FieldTextInput("문의 내용을 입력하세요"), "PLACEHOLDER");
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#43a047');
    }
  }

  // 4. 라벨: HTML의 <label> 태그 역할
  // - 입력창 바로 위에 위치하여 해당 칸에 무엇을 적어야 하는지 알려주는 제목입니다.
  Blockly.Blocks['form_label'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("라벨 이름")
          .appendField(new Blockly.FieldTextInput("제목"), "TEXT");
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#43a047');
    }
  }

  // 5. 전송 버튼: HTML의 <button type="submit"> 역할
  // - 작성을 완료한 후 폼 데이터를 서버로 보내거나 확인하는 마지막 단계의 버튼입니다.
  Blockly.Blocks['form_submit'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("전송 버튼")
          .appendField(new Blockly.FieldTextInput("확인"), "LABEL");
      this.setPreviousStatement(true, null);
      this.setNextStatement(true, null);
      this.setColour('#2e7d32');
    }
  }

  /* [PART 4] 코드 생성기 (HTML 제작 규칙)
     - 블록을 조립했을 때 실제로 웹 화면에 그려질 HTML/CSS 코드를 생성합니다.
     - 모든 블록에 기본 스타일(여백, 테두리, 너비 등)이 내장되어 있습니다.
  */

  // 폼 박스 생성 (테두리 및 둥근 모서리 적용)
  javascriptGenerator.forBlock['form_container'] = function(block, gen) {
    // 내부 블록의 코드를 가져옵니다. (PythonGenerator -> JavascriptGenerator로 변경)
    // 값이 없을 경우 'null'이 찍히지 않도록 빈 문자열 처리(|| '')를 추가했습니다.
    const content = javascriptGenerator.statementToCode(block, 'CONTENT') || '';
    return `<form style="border: 1px solid #ddd; padding: 15px; border-radius: 8px; margin: 10px 0;">\n${content}</form>\n`;
  }

  // 입력창 생성 (박스 크기 계산 방식 box-sizing: border-box 적용)
  javascriptGenerator.forBlock['form_input'] = function(block, gen) {
    const type = block.getFieldValue('TYPE');
    const placeholder = block.getFieldValue('PLACEHOLDER');
    return `<input type="${type}" placeholder="${placeholder}" style="display: block; width: 100%; padding: 8px; margin-bottom: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box;">\n`;
  }

  // 여러 줄 입력창 생성 (높이 80px 지정 및 세로 조절 허용)
  javascriptGenerator.forBlock['form_textarea'] = function(block, gen) {
    const placeholder = block.getFieldValue('PLACEHOLDER');
    return `<textarea placeholder="${placeholder}" style="display: block; width: 100%; height: 80px; padding: 8px; margin-bottom: 10px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; resize: vertical;"></textarea>\n`;
  }

  // 라벨 생성 (굵은 글씨 및 아래쪽 여백 적용)
  javascriptGenerator.forBlock['form_label'] = function(block, gen) {
    const text = block.getFieldValue('TEXT');
    return `<label style="display: block; font-weight: bold; margin-bottom: 5px; color: #333;">${text}</label>\n`;
  }

  // 버튼 생성 (배경색 지정 및 마우스 커서 변경 효과)
  javascriptGenerator.forBlock['form_submit'] = function(block, gen) {
    const label = block.getFieldValue('LABEL');
    return `<button type="submit" style="background-color: #43a047; color: white; border: none; padding: 10px 20px; border-radius: 4px; cursor: pointer; width: 100%; font-weight: bold;">${label}</button>\n`;
  }
}
</script>