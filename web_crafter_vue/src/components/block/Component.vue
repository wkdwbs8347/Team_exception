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
  const cleanClassList = (raw, fallback) => {
    const s = (raw || fallback || '').toString().trim();
    if (!s) return fallback || '';
    return s
      .split(/\s+/g)
      .map((token) =>
        token
          .replace(/[_\-]/g, '')
          .replace(/[^a-zA-Z0-9가-힣]/g, '')
      )
      .filter(Boolean)
      .join(' ');
  };

  const escAttr = (s) =>
    String(s ?? '')
      .replace(/&/g, '&amp;')
      .replace(/"/g, '&quot;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

  const buildPresetCss = (overlayCls, panelCls, closeCls, headerCls, bodyCls, footerCls) => `
/* ===== WebCrafter 기본 모달 프리셋(자동 생성) ===== */

/* overlay: 기본은 닫힘(close + display:none) */
.${overlayCls}{
  position: fixed;
  inset: 0;
  display: none;           /* close 기본 */
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(0,0,0,.45);
  z-index: 9999;
}

/* open 상태 */
.${overlayCls}.open{
  display: flex;
}

/* close 상태(명시) */
.${overlayCls}.close{
  display: none;
}

/* panel */
.${panelCls}{
  position: relative;
  width: min(560px, 92vw);
  max-height: 80vh;
  overflow: auto;
  background: #fff;
  border-radius: 14px;
  padding: 18px 18px 14px;
  box-shadow: 0 10px 30px rgba(0,0,0,.25);
}

/* close button */
.${closeCls}{
  position: absolute;
  top: 10px;
  right: 10px;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 0;
  background: rgba(0,0,0,.06);
  cursor: pointer;
  font-size: 14px;
}
.${closeCls}:hover{ background: rgba(0,0,0,.12); }

/* header/body/footer */
.${headerCls}{
  font-weight: 700;
  font-size: 18px;
  padding-right: 44px; /* close 버튼 공간 */
  margin-bottom: 12px;
}
.${bodyCls}{
  font-size: 14px;
  line-height: 1.5;
  margin-bottom: 14px;
}
.${footerCls}{
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 10px;
  border-top: 1px solid rgba(0,0,0,.08);
}
`.trim();

  // =========================
  // ✅ Block
  // =========================
  Blockly.Blocks['component_modal'] = {
    init() {
      this.appendDummyInput().appendField('🧩 모달 만들기');

      // ✅ 기본 모달 스타일 자동 생성 ON/OFF
      this.appendDummyInput()
        .appendField('기본 모달 스타일 자동 생성')
        .appendField(
          new Blockly.FieldDropdown([
            ['ON', 'ON'],
            ['OFF', 'OFF'],
          ]),
          'AUTO_STYLE'
        );

      this.appendDummyInput()
        .appendField('배경(오버레이) 이름')
        .appendField(new Blockly.FieldTextInput('wcmodal'), 'CLS_OVERLAY');

      this.appendDummyInput()
        .appendField('모달 박스(패널) 이름')
        .appendField(new Blockly.FieldTextInput('wcmodalpanel'), 'CLS_PANEL');

      this.appendDummyInput()
        .appendField('닫기 버튼 이름')
        .appendField(new Blockly.FieldTextInput('wcmodalclose'), 'CLS_CLOSE');

      this.appendDummyInput()
        .appendField('헤더(상단 영역) 이름')
        .appendField(new Blockly.FieldTextInput('wcmodalheader'), 'CLS_HEADER');

      this.appendDummyInput()
        .appendField('본문(내용 영역) 이름')
        .appendField(new Blockly.FieldTextInput('wcmodalbody'), 'CLS_BODY');

      this.appendDummyInput()
        .appendField('푸터(버튼 영역) 이름')
        .appendField(new Blockly.FieldTextInput('wcmodalfooter'), 'CLS_FOOTER');

      this.appendDummyInput()
        .appendField('닫기 버튼 생성')
        .appendField(
          new Blockly.FieldDropdown([
            ['만들기', 'ON'],
            ['안 만들기', 'OFF'],
          ]),
          'CLOSE_BTN'
        );

      this.appendStatementInput('HEADER').appendField('📌 헤더에 들어갈 내용');
      this.appendStatementInput('BODY').appendField('📝 본문에 들어갈 내용');
      this.appendStatementInput('FOOTER').appendField('🔘 푸터에 들어갈 내용');

      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour(category.color);
      this.setTooltip(
        [
          '✅ AUTO=ON: 기본 모달 CSS를 "생성된 HTML 안에" 포함시켜 프리뷰에서 바로 보입니다.',
          '✅ AUTO=OFF: 스타일 탭에서 클래스 이름(wcmodal 등)을 사용자가 직접 꾸미면 됩니다.',
          '✅ 동작 탭의 "모달 상태(open/close/toggle)" 블록으로 열고 닫을 수 있어요.',
        ].join('\n')
      );
      this.setHelpUrl('');
    },
  };

  // =========================
  // ✅ Generator
  // - AUTO_STYLE=ON: style 태그를 HTML에 포함(프리뷰에서 100% 적용)
  // - 기본 상태: 닫힘(close + display:none + aria-hidden=true)
  // =========================
  javascriptGenerator.forBlock['component_modal'] = function (block, generator) {
    const overlayCls = cleanClassList(block.getFieldValue('CLS_OVERLAY'), 'wcmodal');
    const panelCls   = cleanClassList(block.getFieldValue('CLS_PANEL'), 'wcmodalpanel');
    const closeCls   = cleanClassList(block.getFieldValue('CLS_CLOSE'), 'wcmodalclose');
    const headerCls  = cleanClassList(block.getFieldValue('CLS_HEADER'), 'wcmodalheader');
    const bodyCls    = cleanClassList(block.getFieldValue('CLS_BODY'), 'wcmodalbody');
    const footerCls  = cleanClassList(block.getFieldValue('CLS_FOOTER'), 'wcmodalfooter');

    const autoStyle = block.getFieldValue('AUTO_STYLE') || 'ON';
    const closeBtn = block.getFieldValue('CLOSE_BTN') || 'ON';

    const header = generator.statementToCode(block, 'HEADER') || '';
    const body   = generator.statementToCode(block, 'BODY') || '';
    const footer = generator.statementToCode(block, 'FOOTER') || '';

    const closeHtml =
      closeBtn === 'ON'
        ? `<button type="button" class="${closeCls}" aria-label="close" datawcmodalclose="1">X</button>`
        : '';

    // ✅ 클래스 조합별로 프리셋 key 생성 (모달마다 다를 수 있으니)
    const presetKey = [overlayCls, panelCls, closeCls, headerCls, bodyCls, footerCls].join('|');

    // ✅ AUTO=ON일 때만 style 포함
    const presetStyle =
      autoStyle === 'ON'
        ? `<style data-wc-modal-preset="${escAttr(presetKey)}">
${buildPresetCss(overlayCls, panelCls, closeCls, headerCls, bodyCls, footerCls)}
</style>`
        : '';

    // ✅ 기본은 닫힘: close + display:none (동작탭 modal_state_class가 open 하면 바로 뜸)
    const html = `
${presetStyle}
<div class="${overlayCls} close" datawccomponent="modal" aria-hidden="true" style="display:none">
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

export default {};
</script>