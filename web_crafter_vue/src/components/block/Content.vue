<!-- =========================
✅ ContentBlocks.js (컨텐츠)
- ✅ 컨텐츠는 무조건 문서 흐름 (absolute/transform/position 제거)
- ✅ 좌표 관련 처리 전부 제거
- ✅ 모든 생성 요소에 고유 id 부여 (wc_<block.id>)
- ✅ HMR 안전: 블럭 정의는 항상 덮어쓰기
========================= -->
<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

// ===== Content 카테고리 메타데이터 =====
export const category = {
  label: '컨텐츠',
  color: '#00c853',
  icon: '🧩',
}

// ===== Content 툴박스 XML =====
export const toolbox = `
<xml>
  <block type="content_heading"></block>
  <block type="content_button"></block>
  <block type="content_text"></block>
  <block type="content_image"></block>
  <block type="content_link"></block>
  <block type="content_paragraph"></block>

  <block type="content_form_field"></block>
  <block type="content_input"></block>
  <block type="content_textarea"></block>
  <block type="content_select"></block>
  <block type="content_checkbox"></block>
  <block type="content_radio"></block>
</xml>
`

export const defineBlocks = () => {
  // ✅ 안전한 이름 생성 (좌표는 사용/반환 X)
  const getBlockMeta = (block, defaultName) => {
    const nameInput = block.getFieldValue('NAME') || defaultName
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '')

    const displayId = `${safeName}_${block.id.substring(0, 3)}`
    const domId = `wc_${block.id}` // ✅ 고유 DOM id (block.id는 workspace 내 유니크)

    return { displayId, safeName, domId }
  }

  const safeAttr = (v) => (v ?? '').toString().trim().replace(/"/g, '&quot;')

  const safeText = (v) =>
    (v ?? '')
      .toString()
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')

  const safeClassChunk = (raw) =>
    (raw ?? '')
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '')

  // ✅ 컨텐츠: 문서 흐름 전용 스타일
  // - position/left/top/transform 절대 넣지 말 것
  const getStyle = (baseStyle = '') => `${baseStyle || ''}`

  // =========================================================
  // [Heading]
  // =========================================================
  Blockly.Blocks['content_heading'] = {
    init() {
      this.appendDummyInput().appendField('🔤 제목')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('제목'), 'NAME')

      this.appendDummyInput()
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

      this.appendDummyInput()
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('제목을 입력하세요'), 'TEXT')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_heading'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '제목')
    const level = block.getFieldValue('LEVEL') || 'h2'
    const text = safeText(block.getFieldValue('TEXT') || '')
    const style = getStyle('') // flow

    return `<${level} id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">${text}</${level}>\n`
  }

  // =========================================================
  // [Button]
  // =========================================================
  Blockly.Blocks['content_button'] = {
    init() {
      this.appendDummyInput().appendField('🆗 버튼')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('버튼'), 'NAME')

      this.appendDummyInput()
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('클릭'), 'LABEL')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_button'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '버튼')
    const label = safeText(block.getFieldValue('LABEL') || '')
    const style = getStyle('color:inherit; display:inline-block;')

    return `<button id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">${label}</button>\n`
  }

  // =========================================================
  // [Text]
  // =========================================================
  Blockly.Blocks['content_text'] = {
    init() {
      this.appendDummyInput().appendField('📝 텍스트')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('텍스트_요소'), 'NAME')

      this.appendDummyInput()
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('내용을 입력하세요'), 'TEXT')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_text'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '텍스트')
    const text = safeText(block.getFieldValue('TEXT') || '')
    const style = getStyle('') // flow

    return `<span id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">${text}</span>\n`
  }

  // =========================================================
  // [Image]
  // =========================================================
  Blockly.Blocks['content_image'] = {
    init() {
      this.appendDummyInput().appendField('🖼️ 이미지')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('이미지_요소'), 'NAME')

      this.appendDummyInput()
        .appendField('SRC:')
        .appendField(new Blockly.FieldTextInput('https://via.placeholder.com/150'), 'SRC')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_image'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '이미지')
    const imgSrc = safeAttr(block.getFieldValue('SRC') || 'https://via.placeholder.com/150')
    const style = getStyle('display:block; max-width:100%; height:auto;')

    return `<img id="${domId}" class="${safeName}" src="${imgSrc}" data-block-id="${block.id}" style="${style}" />\n`
  }

  // =========================================================
  // [Link]
  // =========================================================
  Blockly.Blocks['content_link'] = {
    init() {
      this.appendDummyInput().appendField('🔗 링크')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('링크'), 'NAME')

      this.appendDummyInput()
        .appendField('텍스트:')
        .appendField(new Blockly.FieldTextInput('링크 텍스트'), 'TEXT')

      this.appendDummyInput()
        .appendField('주소:')
        .appendField(new Blockly.FieldTextInput('https://example.com'), 'HREF')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_link'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '링크')
    const text = safeText(block.getFieldValue('TEXT') || '')
    let href = (block.getFieldValue('HREF') || '#').trim()

    // (네 기존 정책 유지) 내부 링크처럼 보이면 외부로 치환
    const looksInternal =
      href.startsWith('/') ||
      /^wc:\/\//i.test(href) ||
      /^page:/i.test(href) ||
      href.startsWith('#')
    if (looksInternal) href = 'https://example.com'

    if (href && href !== '#' && !/^https?:\/\//i.test(href)) href = 'https://' + href

    const targetAttr = ' target="_blank" rel="noopener noreferrer"'
    const style = getStyle('color:#1a73e8; text-decoration:underline; cursor:pointer;')

    return `<a id="${domId}" class="${safeName}" href="${safeAttr(href)}"${targetAttr} data-block-id="${block.id}" style="${style}">${text}</a>\n`
  }

  // =========================================================
  // [Paragraph]
  // =========================================================
  Blockly.Blocks['content_paragraph'] = {
    init() {
      this.appendDummyInput().appendField('📄 문단(P)')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('문단'), 'NAME')

      this.appendDummyInput()
        .appendField('내용:')
        .appendField(new Blockly.FieldTextInput('문단 내용을 입력하세요'), 'TEXT')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },
  }

  javascriptGenerator.forBlock['content_paragraph'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, '문단')
    const text = safeText(block.getFieldValue('TEXT') || '')
    const style = getStyle('display:block;')

    return `<p id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">${text}</p>\n`
  }

  // =========================================================
  // 0) 폼 항목 (라벨 + 컨트롤 슬롯)
  // =========================================================
  Blockly.Blocks['content_form_field'] = {
    init() {
      this.appendDummyInput().appendField('🧩 폼 항목(Field)')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('field'), 'NAME')

      this.appendDummyInput()
        .appendField('라벨:')
        .appendField(new Blockly.FieldTextInput('Label'), 'LABEL')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

      this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')

      this.updateShape_()
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('추가 클래스:')
          .appendField(new Blockly.FieldTextInput(''), 'EXTRA_CLASS')
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')
    },
  }

  javascriptGenerator.forBlock['content_form_field'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'field')
    const label = safeText(block.getFieldValue('LABEL') || 'Label')

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const extra = adv ? safeClassChunk(block.getFieldValue('EXTRA_CLASS') || '') : ''
    const extraAttr = extra ? ` ${extra}` : ''

    const content = javascriptGenerator.statementToCode(block, 'CONTENT')
    const style = getStyle('') // flow

    return `<div id="${domId}" class="${safeName}${extraAttr}" data-block-id="${block.id}" style="${style}">
  <label class="wc-label">${label}</label>
  <div class="wc-control">
${content}  </div>
</div>\n`
  }

  // =========================================================
  // 1) INPUT
  // =========================================================
  Blockly.Blocks['content_input'] = {
    init() {
      this.appendDummyInput().appendField('⌨️ 입력(Input)')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('input'), 'NAME')

      this.appendDummyInput()
        .appendField('type:')
        .appendField(
          new Blockly.FieldDropdown([
            ['text', 'text'],
            ['email', 'email'],
            ['password', 'password'],
            ['number', 'number'],
          ]),
          'TYPE'
        )

      this.appendDummyInput()
        .appendField('placeholder:')
        .appendField(new Blockly.FieldTextInput(''), 'PLACEHOLDER')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')

      this.updateShape_()
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('value:')
          .appendField(new Blockly.FieldTextInput(''), 'VALUE')
          .appendField('required')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'REQUIRED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED')
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')
    },
  }

  javascriptGenerator.forBlock['content_input'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'input')

    const type = safeAttr(block.getFieldValue('TYPE') || 'text')
    const placeholder = safeAttr(block.getFieldValue('PLACEHOLDER') || '')

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const value = adv ? safeAttr(block.getFieldValue('VALUE') || '') : ''
    const required = adv && block.getFieldValue('REQUIRED') === 'TRUE'
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE'

    const phAttr = placeholder ? ` placeholder="${placeholder}"` : ''
    const valueAttr = value ? ` value="${value}"` : ''
    const reqAttr = required ? ' required' : ''
    const disAttr = disabled ? ' disabled' : ''

    const style = getStyle('') // flow

    return `<input id="${domId}" class="${safeName}" type="${type}"${phAttr}${valueAttr}${reqAttr}${disAttr} data-block-id="${block.id}" style="${style}" />\n`
  }

  // =========================================================
  // 2) TEXTAREA
  // =========================================================
  Blockly.Blocks['content_textarea'] = {
    init() {
      this.appendDummyInput().appendField('🧾 입력(Textarea)')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('textarea'), 'NAME')

      this.appendDummyInput()
        .appendField('placeholder:')
        .appendField(new Blockly.FieldTextInput(''), 'PLACEHOLDER')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')

      this.updateShape_()
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('rows:')
          .appendField(new Blockly.FieldNumber(4, 1, 30, 1), 'ROWS')
          .appendField('required')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'REQUIRED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED')

        this.appendDummyInput('ADV_ROW2')
          .appendField('내용:')
          .appendField(new Blockly.FieldTextInput(''), 'TEXT')
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')
      if (!adv && this.getInput('ADV_ROW2')) this.removeInput('ADV_ROW2')
    },
  }

  javascriptGenerator.forBlock['content_textarea'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'textarea')
    const placeholder = safeAttr(block.getFieldValue('PLACEHOLDER') || '')

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const rows = adv ? Number(block.getFieldValue('ROWS') || 4) : 4
    const required = adv && block.getFieldValue('REQUIRED') === 'TRUE'
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE'
    const text = adv ? safeText(block.getFieldValue('TEXT') || '') : ''

    const rowsAttr = ` rows="${rows}"`
    const phAttr = placeholder ? ` placeholder="${placeholder}"` : ''
    const reqAttr = required ? ' required' : ''
    const disAttr = disabled ? ' disabled' : ''

    const style = getStyle('') // flow

    return `<textarea id="${domId}" class="${safeName}"${rowsAttr}${phAttr}${reqAttr}${disAttr} data-block-id="${block.id}" style="${style}">${text}</textarea>\n`
  }

  // =========================================================
  // 3) SELECT
  // =========================================================
  Blockly.Blocks['content_select'] = {
    init() {
      this.appendDummyInput().appendField('🔽 선택(Select)')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('select'), 'NAME')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

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
          this.optionCount_ = (this.optionCount_ || 0) + 1
          this.updateShape_()
        }
      )

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
          this.optionCount_ = Math.max(1, (this.optionCount_ || 1) - 1)
          this.updateShape_()
        }
      )

      this.appendDummyInput('BTN_ROW')
        .appendField('옵션')
        .appendField(addBtn, 'ADD_OPT')
        .appendField(removeBtn, 'DEL_OPT')

      this.optionCount_ = 2
      this.updateShape_()

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')
    },

    saveExtraState() {
      return {
        adv: this.getFieldValue('ADV') === 'TRUE',
        optionCount: this.optionCount_ || 1,
      }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.optionCount_ = Math.max(1, Number(state?.optionCount || 2))
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const count = Math.max(1, this.optionCount_ || 1)
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('placeholder:')
          .appendField(new Blockly.FieldTextInput('선택하세요'), 'PH')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED')
      }
      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')

      let i = 1
      while (this.getInput(`OPT_${i}`)) {
        this.removeInput(`OPT_${i}`)
        i++
      }

      for (let idx = 1; idx <= count; idx++) {
        const defaultText = idx === 1 ? '옵션1' : idx === 2 ? '옵션2' : `옵션${idx}`
        this.appendDummyInput(`OPT_${idx}`)
          .appendField('-')
          .appendField(new Blockly.FieldTextInput(defaultText), `OPT_TEXT_${idx}`)
      }
    },
  }

  javascriptGenerator.forBlock['content_select'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'select')
    const count = Math.max(1, block.optionCount_ || 1)

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const ph = adv ? safeText(block.getFieldValue('PH') || '선택하세요') : ''
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE'
    const disAttr = disabled ? ' disabled' : ''

    let optionsHtml = ''
    if (adv && ph) optionsHtml += `<option value="" selected disabled>${ph}</option>\n`

    for (let i = 1; i <= count; i++) {
      const text = safeText(block.getFieldValue(`OPT_TEXT_${i}`) || `옵션${i}`)
      const value = safeAttr(text)
      optionsHtml += `<option value="${value}">${text}</option>\n`
    }

    const style = getStyle('') // flow

    return `<select id="${domId}" class="${safeName}"${disAttr} data-block-id="${block.id}" style="${style}">
${optionsHtml}</select>\n`
  }

  // =========================================================
  // 4) CHECKBOX
  // =========================================================
  Blockly.Blocks['content_checkbox'] = {
    init() {
      this.appendDummyInput().appendField('☑️ 체크박스')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('checkbox'), 'NAME')

      this.appendDummyInput()
        .appendField('라벨:')
        .appendField(new Blockly.FieldTextInput('동의합니다'), 'LABEL')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')

      this.updateShape_()
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('checked')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'CHECKED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED')
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')
    },
  }

  javascriptGenerator.forBlock['content_checkbox'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'checkbox')
    const label = safeText(block.getFieldValue('LABEL') || '동의합니다')

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const checked = adv && block.getFieldValue('CHECKED') === 'TRUE'
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE'

    const checkedAttr = checked ? ' checked' : ''
    const disAttr = disabled ? ' disabled' : ''

    const style = getStyle('') // flow

    return `<label id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">
  <input type="checkbox"${checkedAttr}${disAttr} />
  <span>${label}</span>
</label>\n`
  }

  // =========================================================
  // 5) RADIO
  // =========================================================
  Blockly.Blocks['content_radio'] = {
    init() {
      this.appendDummyInput().appendField('🔘 라디오')

      this.appendDummyInput()
        .appendField('이름:')
        .appendField(new Blockly.FieldTextInput('radio'), 'NAME')

      this.appendDummyInput()
        .appendField('라벨:')
        .appendField(new Blockly.FieldTextInput('선택'), 'LABEL')

      this.appendDummyInput()
        .appendField('그룹:')
        .appendField(new Blockly.FieldTextInput('group1'), 'GROUP')

      this.appendDummyInput()
        .appendField('고급')
        .appendField(new Blockly.FieldCheckbox('FALSE'), 'ADV')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#00c853')

      this.updateShape_()
    },

    saveExtraState() {
      return { adv: this.getFieldValue('ADV') === 'TRUE' }
    },
    loadExtraState(state) {
      this.setFieldValue(state?.adv ? 'TRUE' : 'FALSE', 'ADV')
      this.updateShape_()
    },

    onchange() {
      this.updateShape_()
    },

    updateShape_() {
      const adv = this.getFieldValue('ADV') === 'TRUE'

      if (adv && !this.getInput('ADV_ROW')) {
        this.appendDummyInput('ADV_ROW')
          .appendField('value:')
          .appendField(new Blockly.FieldTextInput('option1'), 'VALUE')
          .appendField('checked')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'CHECKED')
          .appendField('disabled')
          .appendField(new Blockly.FieldCheckbox('FALSE'), 'DISABLED')
      }

      if (!adv && this.getInput('ADV_ROW')) this.removeInput('ADV_ROW')
    },
  }

  javascriptGenerator.forBlock['content_radio'] = (block) => {
    const { safeName, domId } = getBlockMeta(block, 'radio')

    const label = safeText(block.getFieldValue('LABEL') || '선택')
    const group = safeAttr(block.getFieldValue('GROUP') || 'group1')

    const adv = block.getFieldValue('ADV') === 'TRUE'
    const value = adv ? safeAttr(block.getFieldValue('VALUE') || 'option1') : ''
    const checked = adv && block.getFieldValue('CHECKED') === 'TRUE'
    const disabled = adv && block.getFieldValue('DISABLED') === 'TRUE'

    const valueAttr = value ? ` value="${value}"` : ''
    const checkedAttr = checked ? ' checked' : ''
    const disAttr = disabled ? ' disabled' : ''

    const style = getStyle('') // flow

    return `<label id="${domId}" class="${safeName}" data-block-id="${block.id}" style="${style}">
  <input type="radio" name="${group}"${valueAttr}${checkedAttr}${disAttr} />
  <span>${label}</span>
</label>\n`
  }
}

export default {}
</script>