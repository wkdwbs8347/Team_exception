<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '컴포넌트',
  color: '#5c6bc0',
  icon: '🧱',
};

export const toolbox = `
<xml>
  <block type="component_modal"></block>
</xml>
`;

export const defineBlocks = () => {
  // =========================
  // ✅ utils
  // =========================
  // - 클래스는 여러 개 입력 가능(공백으로 구분)하게 허용
  // - 단, 특수문자 제거 (원하면 더 빡세게)
  // - 너가 원한대로 "_" "-" 같은 것도 제거함
  const cleanClassList = (raw, fallback) => {
    const s = (raw || fallback || '').toString().trim();
    if (!s) return fallback || '';
    return s
      .split(/\s+/g)
      .map((token) =>
        token
          .replace(/[_\-]/g, '')               // ✅ "_" "-" 제거
          .replace(/[^a-zA-Z0-9가-힣]/g, '')    // ✅ 나머지 특수문자 제거
      )
      .filter(Boolean)
      .join(' ');
  };

  // =========================
  // ✅ Block
  // =========================
  Blockly.Blocks['component_modal'] = {
    init() {
      this.appendDummyInput()
        .appendField('모달');

      // ✅ 사용자가 직접 정의하는 클래스들
      this.appendDummyInput()
        .appendField('오버레이 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodal'), 'CLS_OVERLAY');

      this.appendDummyInput()
        .appendField('패널 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodalpanel'), 'CLS_PANEL');

      this.appendDummyInput()
        .appendField('닫기 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodalclose'), 'CLS_CLOSE');

      this.appendDummyInput()
        .appendField('헤더 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodalheader'), 'CLS_HEADER');

      this.appendDummyInput()
        .appendField('본문 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodalbody'), 'CLS_BODY');

      this.appendDummyInput()
        .appendField('푸터 이름:')
        .appendField(new Blockly.FieldTextInput('wcmodalfooter'), 'CLS_FOOTER');

      this.appendDummyInput()
        .appendField('닫기 버튼')
        .appendField(
          new Blockly.FieldDropdown([
            ['ON', 'ON'],
            ['OFF', 'OFF'],
          ]),
          'CLOSE_BTN'
        );

      this.appendStatementInput('HEADER').appendField('헤더');
      this.appendStatementInput('BODY').appendField('본문');
      this.appendStatementInput('FOOTER').appendField('푸터');

      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour(category.color);
      this.setTooltip('모달(overlay+panel+header/body/footer). 클래스명은 사용자가 직접 지정.');
      this.setHelpUrl('');
    },
  };

  // =========================
  // ✅ Generator
  // =========================
  javascriptGenerator.forBlock['component_modal'] = function (block, generator) {
    const overlayCls = cleanClassList(block.getFieldValue('CLS_OVERLAY'), 'wcmodal');
    const panelCls   = cleanClassList(block.getFieldValue('CLS_PANEL'), 'wcmodalpanel');
    const closeCls   = cleanClassList(block.getFieldValue('CLS_CLOSE'), 'wcmodalclose');
    const headerCls  = cleanClassList(block.getFieldValue('CLS_HEADER'), 'wcmodalheader');
    const bodyCls    = cleanClassList(block.getFieldValue('CLS_BODY'), 'wcmodalbody');
    const footerCls  = cleanClassList(block.getFieldValue('CLS_FOOTER'), 'wcmodalfooter');

    const closeBtn = block.getFieldValue('CLOSE_BTN') || 'ON';

    const header = generator.statementToCode(block, 'HEADER') || '';
    const body   = generator.statementToCode(block, 'BODY') || '';
    const footer = generator.statementToCode(block, 'FOOTER') || '';

    // ✅ 닫기 버튼은 "동작 탭"에서 잡기 위한 표식
    // - datawcmodalclose="1"
    const closeHtml =
      closeBtn === 'ON'
        ? `<button type="button" class="${closeCls}" aria-label="close" datawcmodalclose="1">X</button>`
        : '';

    const html = `
<div class="${overlayCls}" datawccomponent="modal">
  <div class="${panelCls}" role="dialog" aria-modal="true">
    ${closeHtml}
    <div class="${headerCls}">
      ${header}
    </div>
    <div class="${bodyCls}">
      ${body}
    </div>
    <div class="${footerCls}">
      ${footer}
    </div>
  </div>
</div>
`.trim();

    return html + '\n';
  };
};
</script>