<!-- =========================
✅ Content.vue (컨텐츠)
- ✅ 컨텐츠는 무조건 문서 흐름 (absolute/transform/position 제거)
- ✅ 좌표 관련 처리 전부 제거
- ✅ HMR 안전: 블럭 정의는 항상 덮어쓰기
- ✅ 모든 블럭 속성 UI "수평(한 줄)" 정렬
- ✅ 생성기 기본 inline style → data-wc-style 로만 저장 (코드보기에서 완전 숨김 가능)
- ✅ OFF: 기존처럼 TEXT만 렌더
- ✅ ON: TEXT 유지 + 자식 INLINE HTML이 바로 뒤에 붙어서 렌더 (간격 없음)
- ✅ content_input: "서버필드(고급)" 옵션 추가 (name + data-wc-field 출력)
- ✅ 중복확인 버튼(content_duplicate_check) 추가
- ✅ 버튼(content_button): type(button/submit/reset) 선택 추가
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

// ✅ 서버에 안전하게 매핑 가능한 필드 목록
const SERVER_FIELDS = [
  { label: '아이디', key: 'loginId', defaultType: 'text' },
  { label: '이메일', key: 'email', defaultType: 'email' },
  { label: '이름', key: 'name', defaultType: 'text' },
  { label: '닉네임', key: 'nickname', defaultType: 'text' },
  { label: '비밀번호', key: 'password', defaultType: 'password' },
  { label: '비밀번호 확인', key: 'passwordConfirm', defaultType: 'password' },
  { label: '생년월일', key: 'birth', defaultType: 'date' },
];

// ✅ 중복확인 가능한 필드 (id/email/nickname)
const DUPLICATE_FIELDS = [
  { label: '아이디', key: 'loginId' },
  { label: '이메일', key: 'email' },
  { label: '닉네임', key: 'nickname' },
];

// ===== Content 툴박스 XML =====
export const toolbox = `
<xml>
  <block type="content_heading"></block>
  <block type="content_button"></block>
  <block type="content_duplicate_check"></block>
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

  // ✅ INLINE_CONTENT에 연결된 자식 블록들을 "생성기 함수 직접 호출"로 HTML을 이어붙임
  // - javascriptGenerator.blockToCode()는 JS scrub_를 타서 HTML이 누락될 수 있음
  const getInlineChildrenHtml = (parentBlock, inputName = 'INLINE_CONTENT') => {
    let b = parentBlock.getInputTargetBlock(inputName);
    let html = '';

    while (b) {
      const genFn = javascriptGenerator.forBlock?.[b.type];
      let chunk = '';

      if (typeof genFn === 'function') {
        const out = genFn(b, javascriptGenerator);
        chunk = Array.isArray(out) ? out[0] : out;
      }

      html += chunk || '';
      b = b.getNextBlock();
    }

    return html;
  };

  // ✅ 체크 OFF면 TEXT만
  // ✅ 체크 ON면 TEXT + (INLINE 자식 HTML) 그대로 뒤에 붙임 (구분자/공백 추가 없음)
  const renderTextPlusInlineChildren = (
    block,
    textFieldName,
    fallback = ''
  ) => {
    const text = safeText(block.getFieldValue(textFieldName) || fallback);
    const on = block.getFieldValue('INLINE_WRAP') === 'TRUE';
    if (!on) return text;

    if (!block.getInput('INLINE_CONTENT')) return text;

    const childHtml = (
      getInlineChildrenHtml(block, 'INLINE_CONTENT') || ''
    ).trim();
    return childHtml ? `${text}${childHtml}` : text;
  };

  // ✅ 토글 ON/OFF에 따라 "자식 소켓"을 삭제/추가하지 않고
  // ✅ 항상 존재시키되, 보이기/숨기기만 한다 (코드생성 시점 안정화)
  const updateInlineShape = (block) => {
    const on = block.getFieldValue('INLINE_WRAP') === 'TRUE';

    let input = block.getInput('INLINE_CONTENT');
    if (!input) {
      input = block
        .appendStatementInput('INLINE_CONTENT')
        .setCheck('INLINE')
        .appendField('자식');
    }

    input.setVisible(on);

    if (!on) {
      const child = block.getInputTargetBlock('INLINE_CONTENT');
      if (child) child.unplug(true);
    }

    block.setInputsInline(true);
    if (block.rendered) block.render();
  };

  // =========================================================
  // [Heading] (블록요소) - ON일 때만 INLINE 자식 허용
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
        .appendField(new Blockly.FieldTextInput('제목을 입력하세요'), 'TEXT')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_heading'] = (block) => {
    const { safeName } = getBlockMeta(block, '제목');
    const level = block.getFieldValue('LEVEL') || 'h2';
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<${level} class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</${level}>\n`;
  };

  // =========================================================
  // [Button] (type 선택 추가)
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
        )
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_button'] = (block) => {
    const { safeName } = getBlockMeta(block, '버튼');
    const inner = renderTextPlusInlineChildren(block, 'LABEL', '');
    const btnType = safeAttr(block.getFieldValue('BTN_TYPE') || 'button');
    const style = getStyle('color:inherit; cursor: pointer;');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<button class="${safeName}" type="${btnType}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</button>\n`;
  };

  // =========================================================
  // ✅ [Duplicate Check Button]
  // - data-wc-action="duplicate-check"
  // - data-wc-target="loginId/email/nickname"
  // =========================================================
  Blockly.Blocks['content_duplicate_check'] = {
    init() {
      titleWithSep(this, '✅ 중복확인');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('dup_btn'), 'NAME')
        .appendField('대상:')
        .appendField(
          new Blockly.FieldDropdown(
            DUPLICATE_FIELDS.map((f) => [f.label, f.key])
          ),
          'TARGET_KEY'
        )
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('중복확인'), 'LABEL')
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

  javascriptGenerator.forBlock['content_duplicate_check'] = (block) => {
    const { safeName } = getBlockMeta(block, 'dup_btn');
    const targetKey = (block.getFieldValue('TARGET_KEY') || '').trim();
    const label = safeText(block.getFieldValue('LABEL') || '중복확인');
    const btnType = safeAttr(block.getFieldValue('BTN_TYPE') || 'button');

    const builderStyleAttr = getBuilderStyleAttr(
      'padding:8px 12px; border:1px solid #00c853; border-radius:8px; cursor:pointer;'
    );

    return `<button class="${safeName}" type="${btnType}"
  data-wc-action="duplicate-check"
  data-wc-target="${safeAttr(targetKey)}"
  data-block-id="${block.id}"${builderStyleAttr}>${label}</button>\n`;
  };

  // =========================================================
  // [Text] (span) - 인라인 요소 + 인라인 컨테이너 가능
  // =========================================================
  Blockly.Blocks['content_text'] = {
    init() {
      titleWithSep(this, '📝 텍스트');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('텍스트_요소'), 'NAME')
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('내용을 입력하세요'), 'TEXT')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_text'] = (block) => {
    const { safeName } = getBlockMeta(block, '텍스트');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<span class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</span>\n`;
  };

  // =========================================================
  // [Image]
  // =========================================================
  Blockly.Blocks['content_image'] = {
    init() {
      titleWithSep(this, '🖼️ 이미지');

      this.appendDummyInput()
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
        .appendField(new Blockly.FieldTextInput('https://example.com'), 'HREF')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_link'] = (block) => {
    const { safeName } = getBlockMeta(block, '링크');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    let href = (block.getFieldValue('HREF') || '#').trim();

    const looksInternal =
      href.startsWith('/') ||
      /^wc:\/\//i.test(href) ||
      /^page:/i.test(href) ||
      href.startsWith('#');
    if (looksInternal) href = 'https://example.com';

    if (href && href !== '#' && !/^https?:\/\//i.test(href))
      href = 'https://' + href;

    const targetAttr = ' target="_blank" rel="noopener noreferrer"';
    const style = getStyle(
      'color:#1a73e8; text-decoration:underline; cursor:pointer;'
    );
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<a class="${safeName}" href="${safeAttr(href)}"${targetAttr} data-block-id="${block.id}"${builderStyleAttr}>${inner}</a>\n`;
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
        )
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_paragraph'] = (block) => {
    const { safeName } = getBlockMeta(block, '문단');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<p class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</p>\n`;
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
        .appendField(new Blockly.FieldTextInput('Label'), 'TEXT')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);

      this.updateShape_();
      updateInlineShape(this);
    },

    saveExtraState() {
      return {
        adv: this.getFieldValue('ADV') === 'TRUE',
        inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE',
      };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      this.updateShape_();
      updateInlineShape(this);
    },

    onchange() {
      this.updateShape_();
      updateInlineShape(this);
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE';

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('for(id):')
          .appendField(new Blockly.FieldTextInput(''), 'FOR_ID');
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_label'] = (block) => {
    const { safeName } = getBlockMeta(block, 'label');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', 'Label');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const forIdRaw = adv ? (block.getFieldValue('FOR_ID') || '').trim() : '';
    const forAttr = forIdRaw ? ` for="${safeAttr(forIdRaw)}"` : '';

    const style = getStyle('font-weight:600;');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}"${forAttr} data-block-id="${block.id}"${builderStyleAttr}>${inner}</label>\n`;
  };

  // =========================================================
  // ✅ [Input] (서버필드(고급) 추가 + date 포함)
  // =========================================================
  Blockly.Blocks['content_input'] = {
  init() {
    titleWithSep(this, '⌨️ 입력(Input)');

    // 1. 기본 행 (항상 보임)
    this.appendDummyInput('MAIN_ROW')
      .appendField('이름:')
      .appendField(new Blockly.FieldTextInput('input'), 'NAME')
      .appendField('type:')
      .appendField(new Blockly.FieldDropdown([
        ['text', 'text'], ['email', 'email'], ['password', 'password'],
        ['number', 'number'], ['date', 'date'],
      ]), 'TYPE')
      .appendField('placeholder:')
      .appendField(new Blockly.FieldTextInput(''), 'PLACEHOLDER')
      .appendField('고급')
      .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

    // 2. 고급 설정 행 (미리 만들어두고 숨김 처리)
    this.appendDummyInput('ADV_ROW')
      .appendField('서버필드:')
      .appendField(new Blockly.FieldDropdown([
        ['(없음)', ''], ...SERVER_FIELDS.map((f) => [f.label, f.key]),
      ]), 'SERVER_KEY')
      .appendField('value:')
      .appendField(new Blockly.FieldTextInput(''), 'VALUE')
      .appendField('required')
      .appendField(new Blockly.FieldCheckbox('FALSE'), 'REQUIRED')
      .appendField('disabled')
      .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED');

    this.setPreviousStatement(true, 'ELEMENT');
    this.setNextStatement(true, 'ELEMENT');
    this.setColour('#00c853');
    this.setInputsInline(true);

    // 최초 1회 실행하여 초기 상태(숨김) 적용
    this.updateShape_();
  },

  // XML/JSON 저장 시 상태 저장
  saveExtraState() {
    return { adv: this.getFieldValue('ADV') === 'TRUE' };
  },

  // 불러올 때 상태 복원
  loadExtraState(state) {
    this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
    this.updateShape_();
  },

  onchange() {
    this.updateShape_();
  },

  updateShape_() {
    const adv = this.getFieldValue('ADV') === 'TRUE';
    const advInput = this.getInput('ADV_ROW');
    
    if (advInput) {
      // ✅ 핵심: removeInput() 대신 setVisible()을 사용하여 필드와 데이터를 보존합니다.
      advInput.setVisible(adv);
    }

    if (this.rendered) {
      this.render();
    }
  }
};

  javascriptGenerator.forBlock['content_input'] = (block) => {
    const { safeName } = getBlockMeta(block, 'input');

    const type = safeAttr(block.getFieldValue('TYPE') || 'text');
    const placeholder = safeAttr(block.getFieldValue('PLACEHOLDER') || '');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const serverKey = adv
      ? (block.getFieldValue('SERVER_KEY') || '').trim()
      : '';

    const value = adv ? safeAttr(block.getFieldValue('VALUE') || '') : '';
    const required = adv && block.getFieldValue('REQUIRED') === 'TRUE';
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE';

    const nameAttr = serverKey ? ` name="${safeAttr(serverKey)}"` : '';
    const fieldAttr = serverKey
      ? ` data-wc-field="${safeAttr(serverKey)}"`
      : '';

    const phAttr = placeholder ? ` placeholder="${placeholder}"` : '';
    const valueAttr = value ? ` value="${value}"` : '';
    const reqAttr = required ? ' required' : '';
    const disAttr = disabled ? ' disabled' : '';

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<input class="${safeName}" type="${type}"${nameAttr}${phAttr}${valueAttr}${reqAttr}${disAttr}${fieldAttr} data-block-id="${block.id}"${builderStyleAttr} />\n`;
  };

  // =========================================================
  // [Textarea]
  // =========================================================
  Blockly.Blocks['content_textarea'] = {
    init() {
      titleWithSep(this, '🧾 입력(Textarea)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('textarea'), 'NAME')
        .appendField('placeholder:')
        .appendField(new Blockly.FieldTextInput(''), 'PLACEHOLDER')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');

      this.setInputsInline(true);
      this.updateShape_();
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.updateShape_();
    },

    onchange() {
      this.updateShape_();
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE';

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('rows:')
          .appendField(new Blockly.FieldNumber(4, 1, 30, 1), 'ROWS')
          .appendField('required')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'REQUIRED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED');

        this.appendDummyInput('ADV_ROW2')
          .appendField('내용:')
          .appendField(new Blockly.FieldTextInput(''), 'TEXT');
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW');
      if (!adv && this.getInput('ADV_ROW2')) this.removeInput('ADV_ROW2');

      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_textarea'] = (block) => {
    const { safeName } = getBlockMeta(block, 'textarea');
    const placeholder = safeAttr(block.getFieldValue('PLACEHOLDER') || '');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const rows = adv ? Number(block.getFieldValue('ROWS') || 4) : 4;
    const required = adv && block.getFieldValue('REQUIRED') === 'TRUE';
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE';
    const text = adv ? safeText(block.getFieldValue('TEXT') || '') : '';

    const rowsAttr = ` rows="${rows}"`;
    const phAttr = placeholder ? ` placeholder="${placeholder}"` : '';
    const reqAttr = required ? ' required' : '';
    const disAttr = disabled ? ' disabled' : '';

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<textarea class="${safeName}"${rowsAttr}${phAttr}${reqAttr}${disAttr} data-block-id="${block.id}"${builderStyleAttr}>${text}</textarea>\n`;
  };

  // =========================================================
  // [Select]
  // =========================================================
  Blockly.Blocks['content_select'] = {
    init() {
      titleWithSep(this, '🔽 선택(Select)');

      this.appendDummyInput('MAIN_ROW')
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('select'), 'NAME')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      const fireOptionCountChange = (block, oldCount, newCount) => {
        try {
          if (!block.workspace) return;
          Blockly.Events.setGroup(true);
          Blockly.Events.fire(
            new Blockly.Events.BlockChange(
              block,
              'mutation',
              'optionCount',
              oldCount,
              newCount
            )
          );
        } finally {
          Blockly.Events.setGroup(false);
        }
      };

      const addBtn = new Blockly.FieldImage(
        'data:image/svg+xml;utf8,' +
          encodeURIComponent(
            `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
        <rect x="0" y="0" width="16" height="16" rx="3" fill="#43a047"/>
        <path d="M8 3.5v9M3.5 8h9" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
      </svg>`
          ),
        16,
        16,
        '＋',
        () => {
          const old = this.optionCount_ || 2;
          const next = old + 1;

          this.optionCount_ = next;
          this.updateShape_();

          if (this.rendered) this.render();
          fireOptionCountChange(this, old, next);
        }
      );

      const removeBtn = new Blockly.FieldImage(
        'data:image/svg+xml;utf8,' +
          encodeURIComponent(
            `<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16">
        <rect x="0" y="0" width="16" height="16" rx="3" fill="#c62828"/>
        <path d="M3.5 8h9" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
      </svg>`
          ),
        16,
        16,
        '－',
        () => {
          const old = this.optionCount_ || 2;
          const next = Math.max(1, old - 1);

          this.optionCount_ = next;
          this.updateShape_();

          if (this.rendered) this.render();
          fireOptionCountChange(this, old, next);
        }
      );

      this.appendDummyInput('BTN_ROW')
        .appendField('옵션')
        .appendField(addBtn, 'ADD_OPT')
        .appendField(removeBtn, 'DEL_OPT');

      this.optionCount_ = 2;

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');

      this.setInputsInline(true);
      this.updateShape_();
    },

    saveExtraState() {
      return {
        adv: this.getFieldValue('ADV') === 'TRUE',
        optionCount: this.optionCount_ || 1,
      };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.optionCount_ = Math.max(1, Number(state?.optionCount || 2));
      this.updateShape_();
    },

    onchange() {
      this.updateShape_();
    },

    updateShape_() {
      const count = Math.max(1, this.optionCount_ || 1);
      const adv = this.getFieldValue('ADV') === 'TRUE';

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('placeholder:')
          .appendField(new Blockly.FieldTextInput('선택하세요'), 'PH')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED');
      }
      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW');

      let i = 1;
      while (this.getInput(`OPT_${i}`)) {
        this.removeInput(`OPT_${i}`);
        i++;
      }

      for (let idx = 1; idx <= count; idx++) {
        const defaultText =
          idx === 1 ? '옵션1' : idx === 2 ? '옵션2' : `옵션${idx}`;
        this.appendDummyInput(`OPT_${idx}`)
          .appendField('-')
          .appendField(
            new Blockly.FieldTextInput(defaultText),
            `OPT_TEXT_${idx}`
          );
      }

      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_select'] = (block) => {
    const { safeName } = getBlockMeta(block, 'select');
    const count = Math.max(1, block.optionCount_ || 1);

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const ph = adv ? safeText(block.getFieldValue('PH') || '선택하세요') : '';
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE';
    const disAttr = disabled ? ' disabled' : '';

    let optionsHtml = '';
    if (adv && ph)
      optionsHtml += `<option value="" selected disabled>${ph}</option>\n`;

    for (let i = 1; i <= count; i++) {
      const text = safeText(block.getFieldValue(`OPT_TEXT_${i}`) || `옵션${i}`);
      const value = safeAttr(text);
      optionsHtml += `<option value="${value}">${text}</option>\n`;
    }

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<select class="${safeName}"${disAttr} data-block-id="${block.id}"${builderStyleAttr}>
${optionsHtml}</select>\n`;
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
        .appendField('라벨:')
        .appendField(new Blockly.FieldTextInput('동의합니다'), 'LABEL')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');

      this.setInputsInline(true);
      this.updateShape_();
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.updateShape_();
    },

    onchange() {
      this.updateShape_();
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE';

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('checked')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'CHECKED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED');
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_checkbox'] = (block) => {
    const { safeName } = getBlockMeta(block, 'checkbox');
    const label = safeText(block.getFieldValue('LABEL') || '동의합니다');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const checked = adv && block.getFieldValue('CHECKED') === 'TRUE';
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE';

    const checkedAttr = checked ? ' checked' : '';
    const disAttr = disabled ? ' disabled' : '';

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>
  <input type="checkbox"${checkedAttr}${disAttr} />
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
        .appendField('라벨:')
        .appendField(new Blockly.FieldTextInput('선택'), 'LABEL')
        .appendField('그룹:')
        .appendField(new Blockly.FieldTextInput('group1'), 'GROUP')
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV');

      this.setPreviousStatement(true, 'ELEMENT');
      this.setNextStatement(true, 'ELEMENT');
      this.setColour('#00c853');

      this.setInputsInline(true);
      this.updateShape_();
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV');
      this.updateShape_();
    },

    onchange() {
      this.updateShape_();
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE';

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('value:')
          .appendField(new Blockly.FieldTextInput('option1'), 'VALUE')
          .appendField('checked')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'CHECKED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED');
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_radio'] = (block) => {
    const { safeName } = getBlockMeta(block, 'radio');

    const label = safeText(block.getFieldValue('LABEL') || '선택');
    const group = safeAttr(block.getFieldValue('GROUP') || 'group1');

    const adv = block.getFieldValue('ADV') === 'TRUE';
    const value = adv
      ? safeAttr(block.getFieldValue('VALUE') || 'option1')
      : '';
    const checked = adv && block.getFieldValue('CHECKED') === 'TRUE';
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE';

    const valueAttr = value ? ` value="${value}"` : '';
    const checkedAttr = checked ? ' checked' : '';
    const disAttr = disabled ? ' disabled' : '';

    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<label class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>
  <input type="radio" name="${group}"${valueAttr}${checkedAttr}${disAttr} />
  <span>${label}</span>
</label>\n`;
  };

  // =========================================================
  // [BR]
  // =========================================================
  Blockly.Blocks['content_br'] = {
    init() {
      titleWithSep(this, '↩️ 줄바꿈');

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('br'), 'NAME');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);
    },
  };

  javascriptGenerator.forBlock['content_br'] = (block) => {
    const { safeName } = getBlockMeta(block, 'br');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<br class="${safeName}" data-block-id="${block.id}"${builderStyleAttr} />\n`;
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
        .appendField(new Blockly.FieldTextInput('강조 텍스트'), 'TEXT')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_strong'] = (block) => {
    const { safeName } = getBlockMeta(block, 'strong');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<strong class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</strong>\n`;
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
        .appendField(new Blockly.FieldTextInput('강조 텍스트'), 'TEXT')
        .appendField('추가')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'INLINE_WRAP');

      this.setPreviousStatement(true, ['ELEMENT', 'INLINE']);
      this.setNextStatement(true, ['ELEMENT', 'INLINE']);
      this.setColour('#00c853');
      this.setInputsInline(true);

      updateInlineShape(this);
    },

    saveExtraState() {
      return { inlineWrap: this.getFieldValue('INLINE_WRAP') === 'TRUE' };
    },
    loadExtraState(state) {
      this.setFieldValue(state?.inlineWrap ? 'TRUE' : 'FALSE', 'INLINE_WRAP');
      updateInlineShape(this);
    },
    onchange() {
      updateInlineShape(this);
    },
  };

  javascriptGenerator.forBlock['content_em'] = (block) => {
    const { safeName } = getBlockMeta(block, 'em');
    const inner = renderTextPlusInlineChildren(block, 'TEXT', '');
    const style = getStyle('');
    const builderStyleAttr = getBuilderStyleAttr(style);

    return `<em class="${safeName}" data-block-id="${block.id}"${builderStyleAttr}>${inner}</em>\n`;
  };
};

export default {};
</script>
