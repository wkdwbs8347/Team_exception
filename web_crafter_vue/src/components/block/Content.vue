<!-- =========================
✅ Content.vue (컨텐츠)
- ✅ 컨텐츠는 무조건 문서 흐름 (absolute/transform/position 제거)
- ✅ 좌표 관련 처리 전부 제거
- ✅ HMR 안전: 블럭 정의는 항상 덮어쓰기
- ✅ 모든 블럭 속성 UI "수평(한 줄)" 정렬
- ✅ 생성기 기본 inline style → data-wc-style 로만 저장 (코드보기에서 완전 숨김 가능)
- ✅ "추가(자식)" 기능 전부 제거 (INLINE_WRAP / INLINE_CONTENT 삭제)
- ✅ (정리) 컨텐츠 내부 "고급 속성" 기능 제거 → ContentAttr(컨텐츠속성)에서만 속성 적용
========================= -->
<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

// ===== Content 카테고리 메타데이터 =====
export const category = {
  label: '컨텐츠',
  color: '#00c853',
  icon: '🧩',
};

// ===== Content 툴박스 XML =====
export const toolbox = `
<xml>
  <block type="content_heading"></block>
  <block type="content_button"></block>
  <block type="content_text"></block>
  <block type="content_image"></block>
  <block type="content_link"></block>
  <block type="content_paragraph"></block>
  <block type="content_label"></block>
  <block type="content_input"></block>
  <block type="content_textarea"></block>
  <block type="content_select"></block>
  <block type="content_checkbox"></block>
  <block type="content_radio"></block>
  <block type="content_br"></block>
  <block type="content_strong"></block>
  <block type="content_em"></block>
</xml>
`;

// 공통유틸
export const defineBlocks = () => {
  // ✅ 블록 타이틀 + 속성 구분자(가독성)
  const titleWithSep = (block, title) => {
    block.appendDummyInput('TITLE_ROW').appendField(title).appendField('▶');
  };

  // ✅ 안전한 이름 생성 (좌표는 사용/반환 X)
  const getBlockMeta = (block, defaultName) => {
    const nameInput = block.getFieldValue('NAME') || defaultName;
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');
    const displayId = `${safeName}_${block.id.substring(0, 3)}`;
    return { displayId, safeName };
  };

  const safeAttr = (v) => (v ?? '').toString().trim().replace(/"/g, '&quot;');

  const safeText = (v) =>
    (v ?? '')
      .toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;');

  // ✅ 컨텐츠: 문서 흐름 전용 스타일
  // - position/left/top/transform 절대 넣지 말 것
  const getStyle = (baseStyle = '') => `${baseStyle || ''}`;

  // ✅ 핵심: 생성기 스타일은 style=""로 직접 박지 않고 data-wc-style로만 저장
  const getBuilderStyleAttr = (baseStyle = '') => {
    const css = (baseStyle || '').trim();
    return css ? ` data-wc-style="${safeAttr(css)}"` : '';
  };

  // =========================================================
  // [Heading] (블록요소)
  // =========================================================
  Blockly.Blocks['content_heading'] = {
    init() {
      titleWithSep(this, '🔤 제목');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('제목'), 'NAME')
        .appendField('레벨:')
        .appendField(
          new Blockly.FieldDropdown([
            ['H1', 'h1'],
            ['H2', 'h2'],
            ['H3', 'h3'],
            ['H4', 'h4'],
            ['H5', 'h5'],
            ['H6', 'h6'],
          ]),
          'LEVEL'
        )
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('제목을 입력하세요'), 'TEXT');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_heading'] = (block) => {
    const { safeName } = getBlockMeta(block, '제목');
    const level = block.getFieldValue('LEVEL') || 'h2';
    const text = safeText(block.getFieldValue('TEXT') || '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<${level} class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</${level}>\n`;
  };

  // =========================================================
  // [Button] (type 선택 유지)
  // =========================================================
  Blockly.Blocks['content_button'] = {
    init() {
      titleWithSep(this, '🆗 버튼');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('버튼'), 'NAME')
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('클릭'), 'LABEL')
        .appendField('type:')
        .appendField(
          new Blockly.FieldDropdown([
            ['button', 'button'],
            ['submit', 'submit'],
            ['reset', 'reset'],
          ]),
          'BTN_TYPE'
        );

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_button'] = (block) => {
    const { safeName } = getBlockMeta(block, '버튼');
    const label = safeText(block.getFieldValue('LABEL') || '');
    const btnType = safeAttr(block.getFieldValue('BTN_TYPE') || 'button');
    const style = getStyle('color:inherit; cursor: pointer;');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<button class="${safeName}" type="${btnType}" data-block-id="${block.id}"${builderStyleAttr}>${label}</button>\n`;
  };

  // =========================================================
  // [Text] (span)
  // =========================================================
  Blockly.Blocks['content_text'] = {
    init() {
      titleWithSep(this, '📝 텍스트');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('텍스트_요소'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('내용을 입력하세요'), 'TEXT');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_text'] = (block) => {
    const { safeName } = getBlockMeta(block, '텍스트');
    const text = safeText(block.getFieldValue('TEXT') || '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<span class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</span>\n`;
  };

  // =========================================================
  // [Image]
  // =========================================================
  Blockly.Blocks['content_image'] = {
    init() {
      titleWithSep(this, '🖼️ 이미지');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('이미지_요소'), 'NAME')
        .appendField('SRC:')
        .appendField(
          new Blockly.FieldTextInput('https://via.placeholder.com/150'),
          'SRC'
        );

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_image'] = (block) => {
    const { safeName } = getBlockMeta(block, '이미지');
    const imgSrc = safeAttr(
      block.getFieldValue('SRC') || 'https://via.placeholder.com/150'
    );
    const style = getStyle('max-width:100%; height:auto;');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<img class="${safeName}" src="${imgSrc}" data-block-id="${block.id}"${builderStyleAttr} />\n`;
  };

  // =========================================================
  // [Link]
  // =========================================================
  Blockly.Blocks['content_link'] = {
    init() {
      titleWithSep(this, '🔗 링크');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('링크'), 'NAME')
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('링크 텍스트'), 'TEXT')
        .appendField('주소:')
        .appendField(new Blockly.FieldTextInput('https://example.com'), 'HREF');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_link'] = (block) => {
    const { safeName } = getBlockMeta(block, '링크');
    const text = safeText(block.getFieldValue('TEXT') || '');
    let href = (block.getFieldValue('HREF') || '#').trim();

    const looksInternal =
      href.startsWith('/') ||
      /^wc:\/\//i.test(href) ||
      /^page:/i.test(href) ||
      href.startsWith('#');
    if (looksInternal) href = 'https://example.com';

    if (href && href !== '#' && !/^https?:\/\//i.test(href))
      href = 'https://' + href;

    // ✅ 기본 target/_blank, rel 제거 (컨텐츠속성에서 추가)
    const style = getStyle(
      'color:#1a73e8; text-decoration:underline; cursor:pointer;'
    );
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<a class="${safeName}" href="${safeAttr(href)}" data-block-id="${block.id}"${builderStyleAttr}>${text}</a>\n`;
  };

  // =========================================================
  // [Paragraph]
  // =========================================================
  Blockly.Blocks['content_paragraph'] = {
    init() {
      titleWithSep(this, '📄 문단(P)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('문단'), 'NAME')
        .appendField('내용:')
        .appendField(
          new Blockly.FieldTextInput('문단 내용을 입력하세요'),
          'TEXT'
        );

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_paragraph'] = (block) => {
    const { safeName } = getBlockMeta(block, '문단');
    const text = safeText(block.getFieldValue('TEXT') || '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<p class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</p>\n`;
  };

  // =========================================================
  // [Label]
  // =========================================================
  Blockly.Blocks['content_label'] = {
    init() {
      titleWithSep(this, '🏷️ 라벨(Label)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('label'), 'NAME')
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('Label'), 'TEXT');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_label'] = (block) => {
    const { safeName } = getBlockMeta(block, 'label');
    const text = safeText(block.getFieldValue('TEXT') || 'Label');
    const style = getStyle('font-weight:600;');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</label>\n`;
  };

  // =========================================================
  // [Input]
  // =========================================================
  Blockly.Blocks['content_input'] = {
    init() {
      titleWithSep(this, '⌨️ 입력(Input)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('input'), 'NAME')
        .appendField('type:')
        .appendField(
          new Blockly.FieldDropdown([
            ['text', 'text'],
            ['email', 'email'],
            ['password', 'password'],
            ['number', 'number'],
            ['date', 'date'],
          ]),
          'TYPE'
        );

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_input'] = (block) => {
    const { safeName } = getBlockMeta(block, 'input');
    const type = safeAttr(block.getFieldValue('TYPE') || 'text');
    const typeAttr = type ? ` type="${type}"` : '';
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<input class="${safeName}"${typeAttr} data-block-id="${block.id}"${builderStyleAttr} />\n`;
  };

  // =========================================================
  // [Textarea]
  // =========================================================
  Blockly.Blocks['content_textarea'] = {
    init() {
      titleWithSep(this, '🧾 입력(Textarea)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('textarea'), 'NAME');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_textarea'] = (block) => {
    const { safeName } = getBlockMeta(block, 'textarea');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<textarea class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}></textarea>\n`;
  };

  // =========================================================
  // [Select] (옵션 추가/삭제 기능 제거 → 고정 2개 옵션)
  // =========================================================
  Blockly.Blocks['content_select'] = {
    init() {
      titleWithSep(this, '🔽 선택(Select)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('select'), 'NAME')
        .appendField('옵션1:')
        .appendField(new Blockly.FieldTextInput('옵션1'), 'OPT_TEXT_1')
        .appendField('옵션2:')
        .appendField(new Blockly.FieldTextInput('옵션2'), 'OPT_TEXT_2');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_select'] = (block) => {
    const { safeName } = getBlockMeta(block, 'select');

    const t1 = safeText(block.getFieldValue('OPT_TEXT_1') || '옵션1');
    const t2 = safeText(block.getFieldValue('OPT_TEXT_2') || '옵션2');

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<select class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>
<option value="${safeAttr(t1)}">${t1}</option>
<option value="${safeAttr(t2)}">${t2}</option>
</select>\n`;
  };

  // =========================================================
  // [Checkbox]
  // =========================================================
  Blockly.Blocks['content_checkbox'] = {
    init() {
      titleWithSep(this, '☑️ 선택(CheckBox)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('checkbox'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('동의합니다'), 'LABEL');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_checkbox'] = (block) => {
    const { safeName } = getBlockMeta(block, 'checkbox');
    const label = safeText(block.getFieldValue('LABEL') || '동의합니다');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>
  <input type="checkbox" />
  <span>${label}</span>
</label>\n`;
  };

  // =========================================================
  // [Radio]
  // =========================================================
  Blockly.Blocks['content_radio'] = {
    init() {
      titleWithSep(this, '🔘 선택(Radio)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('radio'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('선택'), 'LABEL')
        .appendField('그룹:')
        .appendField(new Blockly.FieldTextInput('group1'), 'GROUP');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_radio'] = (block) => {
    const { safeName } = getBlockMeta(block, 'radio');
    const label = safeText(block.getFieldValue('LABEL') || '선택');
    const group = safeAttr(block.getFieldValue('GROUP') || 'group1');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>
  <input type="radio" name="${group}" />
  <span>${label}</span>
</label>\n`;
  };

  // =========================================================
  // [BR] (타이틀만)
  // =========================================================
  Blockly.Blocks['content_br'] = {
    init() {
      titleWithSep(this, '↩️ 줄바꿈');
      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_br'] = (block) => {
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);
    return `<br data-block-id="${block.id}"${builderStyleAttr} />\n`;
  };

  // =========================================================
  // [Strong]
  // =========================================================
  Blockly.Blocks['content_strong'] = {
    init() {
      titleWithSep(this, '💪 강조(Strong)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('strong'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('강조 텍스트'), 'TEXT');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_strong'] = (block) => {
    const { safeName } = getBlockMeta(block, 'strong');
    const text = safeText(block.getFieldValue('TEXT') || '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<strong class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</strong>\n`;
  };

  // =========================================================
  // [Em]
  // =========================================================
  Blockly.Blocks['content_em'] = {
    init() {
      titleWithSep(this, '✨ 강조(Em)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('em'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('강조 텍스트'), 'TEXT');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_em'] = (block) => {
    const { safeName } = getBlockMeta(block, 'em');
    const text = safeText(block.getFieldValue('TEXT') || '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<em class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${text}</em>\n`;
  };
};

export default {};
</script>
