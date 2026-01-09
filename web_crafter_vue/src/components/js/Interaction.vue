<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

/* =========================
  카테고리 정의
========================= */
export const category = {
  label: '동작',
  color: '#ff7043',
  icon: '⚡',
}

/* =========================
  툴박스
========================= */
export const toolbox = `
<xml>
  <block type="script_tag"></block>
  <block type="event_click"></block>
  <block type="event_page_load"></block>
  <block type="action_alert"></block>
  <block type="dom_change_text_class"></block>
  <block type="action_navigate_internal"></block>
</xml>
`

/* =========================
  블록 정의
========================= */
export const defineBlocks = () => {
  /* =========
    1. 클릭 이벤트 (클래스 기반)
  ========= */
  Blockly.Blocks['event_click'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('⚡ 클래스가')
        .appendField(new Blockly.FieldTextInput('btn-login'), 'TARGET_CLASS')
        .appendField('인 요소 클릭 시')

      this.appendStatementInput('DO').setCheck(null)

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
      this.setTooltip('해당 클래스를 가진 요소를 클릭했을 때 실행됩니다.')
    },
  }

  javascriptGenerator.forBlock['event_click'] = function (block, generator) {
    const targetClass = block.getFieldValue('TARGET_CLASS') || ''
    const body = generator.statementToCode(block, 'DO')

    return `(function() {
  document.addEventListener('click', function(e) {
    var target = e.target && e.target.closest ? e.target.closest('.${targetClass}') : null;
    if (target) {
${body}
    }
  });
})();\n`
  }

  /* =========
    2. 페이지 로드 이벤트
  ========= */
  Blockly.Blocks['event_page_load'] = {
    init: function () {
      this.appendDummyInput().appendField('⚡ 페이지가 열렸을 때')
      this.appendStatementInput('DO').setCheck(null)
      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['event_page_load'] = function (block, generator) {
    const body = generator.statementToCode(block, 'DO')
    return `window.addEventListener('DOMContentLoaded', function() {\n${body}});\n`
  }

  /* =========
    3. 알림 액션
  ========= */
  Blockly.Blocks['action_alert'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🔔 알림')
        .appendField(new Blockly.FieldTextInput('안녕하세요'), 'MESSAGE')

      this.setPreviousStatement(true, null)
      this.setNextStatement(true, null)
      this.setColour('#ff7043')
    },
  }

  javascriptGenerator.forBlock['action_alert'] = function (block) {
    const msg = block.getFieldValue('MESSAGE') ?? ''
    return `alert(${JSON.stringify(msg)});\n`
  }

  /* =========
    4. 페이지 이동 액션
  ========= */
// [Logic.vue] (또는 Flow.vue)

  if (!Blockly.Extensions.isRegistered('dynamic_page_dropdown')) {
    Blockly.Extensions.register('dynamic_page_dropdown', function() {
      // 이 블록의 입력단(INPUT)에 드롭다운을 꽂습니다.
      this.getInput('DUMMY')
        .appendField(new Blockly.FieldDropdown(function() {
          // Vue에서 만든 전역 함수를 호출!
          return window.WC_GET_PAGES ? window.WC_GET_PAGES() : [['로딩중...', '']];
        }), 'PAGE_ID');
    });
  }

  // 3. 블록 정의 (Extension 사용)
  Blockly.Blocks['action_navigate_internal'] = {
    init: function() {
      this.jsonInit({
        "type": "action_navigate_internal",
        "message0": "페이지 이동하기 📄 %1", // %1 위치에 드롭다운이 들어감
        "args0": [
          { "type": "input_dummy", "name": "DUMMY" } // 드롭다운이 들어갈 자리(Dummy)
        ],
        "extensions": ["dynamic_page_dropdown"], // 👈 위에서 만든 확장기능 연결
        "previousStatement": null,
        "nextStatement": null,
        "colour": "#ff7043",
        "tooltip": "이동할 페이지를 선택하세요."
      });
    }
  };

  // 4. 코드 생성기
  javascriptGenerator.forBlock['action_navigate_internal'] = function(block, generator) {
    // 드롭다운의 값(Value = Page ID)을 가져옵니다.
    const pageId = block.getFieldValue('PAGE_ID');
    
    // 값이 없을 때 처리
    if (!pageId) return '';

    // 문자열로 감싸서 반환
    return `goToPage('${pageId}');\n`;
  }
  /* =========
    0. 스크립트 태그 래퍼
  ========= */
  Blockly.Blocks['script_tag'] = {
    init: function () {
      this.appendDummyInput().appendField('📜 스크립트')
      this.appendStatementInput('BODY').setCheck(null).appendField('내용')
      this.setColour('#ff7043')
      this.setTooltip('여기에 이벤트와 동작을 넣으세요.')
      this.setStyle('hat_blocks')
    },
  }

  javascriptGenerator.forBlock['script_tag'] = function (block, generator) {
    const body = generator.statementToCode(block, 'BODY')
    // ⚠️ 생성 결과가 HTML 문자열이 되도록 script 태그 래핑
    return `<script>\n${body}<\/script>\n`
  }
// 1. 블록 정의
Blockly.Blocks['dom_change_text_class'] = {
  init: function() {
    this.jsonInit({
      "type": "dom_change_text_class", // 구분을 위해 type 이름도 살짝 바꿨습니다
      "message0": "요소 내용 바꾸기 (Class: %1) ➡️ %2", // ID -> Class로 변경
      "args0": [
        { "type": "input_value", "name": "CLASS", "check": "String" }, // 변수명 ID -> CLASS
        { "type": "input_value", "name": "TEXT", "check": "String" }
      ],
      "previousStatement": null,
      "nextStatement": null,
      "colour": "#ff7043"
    });
  }
};

// 2. 코드 생성
javascriptGenerator.forBlock['dom_change_text_class'] = function(block, generator) {
  const className = generator.valueToCode(block, 'CLASS', generator.ORDER_NONE) || "''";
  const text = generator.valueToCode(block, 'TEXT', generator.ORDER_NONE) || "''";
  
  // ✅ 클래스는 여러 개일 수 있으므로 querySelectorAll + forEach 사용
  // 입력된 클래스명 앞에 점(.)을 붙여서 CSS 선택자로 만듭니다.
  return `
  (function(){
    var els = document.querySelectorAll('.' + ${className});
    els.forEach(function(el) {
      el.innerText = ${text};
    });
  })();\n`;
};
};
</script>