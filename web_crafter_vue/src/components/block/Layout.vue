<!-- =========================
✅ LayoutBlocks.js (구조)
- ✅ 실제 태그(HEADER/SECTION/DIV/UL/FORM...)는 고유 특성 그대로 (position/transform 안 넣음)
- ✅ 드래그/좌표는 wrapper(.wc-drag)만 담당
- ✅ wrapper는 data-block-id를 가지며, preview interact 타겟도 wrapper로 잡으면 됨
========================= -->
<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

// ===== Layout 카테고리 메타데이터 =====
export const category = {
  label: '구조',
  color: '#4c97ff',
  icon: '📐',
}

// ✅ 박스 기본 테두리(시각적 가이드)
// - min-width/min-height는 "내용이 커지면 자동으로 늘어남" (min이라서)
// - display는 건드리지 않음(태그 고유 display 그대로)
const BASE_BOX_STYLE =
  'min-width:50px; min-height:50px; border:1px solid #ccc; box-sizing:border-box;'

// ===== Layout 툴박스 XML =====
export const toolbox = `<xml>
  <block type="layout_region"></block>
  <block type="layout_box"></block>
  <block type="layout_divider"></block>
  <block type="layout_ul"></block>
  <block type="layout_li"></block>
  <block type="layout_form"></block>
</xml>`

export const defineBlocks = () => {
  // 공통 유틸
  const safeClass = (raw, fallback) =>
    (raw || fallback)
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '')

  const getClass = (block, fallback) =>
    safeClass(block.getFieldValue('ELEMENT_CLASS'), fallback)

  // ✅ 좌표는 block.data에서만 관리 (wrapper가 이동)
  const getLayoutXY = (block) => {
    let x = 0,
      y = 0
    if (block.data) {
      try {
        const coords = JSON.parse(block.data)
        x = Number(coords.x || 0)
        y = Number(coords.y || 0)
      } catch (e) {}
    }
    return { x, y }
  }

  // ✅ wrapper만 transform으로 이동 (태그 고유 특성 유지)
  const getDragWrapperStyle = (block) => {
    const { x, y } = getLayoutXY(block)
    // wrapper는 레이아웃 영향 없이 "움직이기"만 담당
    // inline-block로 두면 내부 태그 크기(콘텐츠)에 맞춰 wrapper도 같이 커짐
    return `display:inline-block; transform: translate(${x}px, ${y}px);`
  }

  // ✅ 공통 wrapper 생성
  const wrapDraggable = ({ block, innerHtml }) => {
    const wrapperStyle = getDragWrapperStyle(block)
    return `<div class="wc-drag" data-block-id="${block.id}" data-draggable="true" style="${wrapperStyle}">
${innerHtml}
</div>\n`
  }

  // =========================
  // 영역(Region)
  // =========================
  if (!Blockly.Blocks['layout_region']) {
    Blockly.Blocks['layout_region'] = {
      init() {
        this.appendDummyInput().appendField('🧱 영역')

        this.appendDummyInput()
          .appendField('태그:')
          .appendField(
            new Blockly.FieldDropdown([
              ['HEADER', 'header'],
              ['NAV', 'nav'],
              ['MAIN', 'main'],
              ['SECTION', 'section'],
              ['ASIDE', 'aside'],
              ['FOOTER', 'footer'],
            ]),
            'TAG'
          )

        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('wc-region'), 'ELEMENT_CLASS')

        this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용')

        this.setPreviousStatement(true, 'ELEMENT')
        this.setNextStatement(true, 'ELEMENT')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_region'] = (block) => {
    const tag = (block.getFieldValue('TAG') || 'section').toLowerCase()
    const allowed = new Set(['header', 'nav', 'main', 'section', 'aside', 'footer'])
    const safeTag = allowed.has(tag) ? tag : 'section'

    const cls = getClass(block, 'wc-region')
    const content = javascriptGenerator.statementToCode(block, 'CONTENT')

    // ✅ 실제 태그에는 position/transform 절대 넣지 않음
    const inner = `<${safeTag} class="${cls}" style="${BASE_BOX_STYLE}">
${content}</${safeTag}>`

    return wrapDraggable({ block, innerHtml: inner })
  }

  // =========================
  // 컨텐츠 BOX
  // =========================
  if (!Blockly.Blocks['layout_box']) {
    Blockly.Blocks['layout_box'] = {
      init() {
        this.appendDummyInput().appendField('📦 컨텐츠 박스')

        this.appendDummyInput()
          .appendField('태그:')
          .appendField(
            new Blockly.FieldDropdown([
              ['DIV', 'div'],
              ['ARTICLE', 'article'],
            ]),
            'TAG'
          )

        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('wc-box'), 'ELEMENT_CLASS')

        this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용')

        this.setPreviousStatement(true, 'ELEMENT')
        this.setNextStatement(true, 'ELEMENT')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_box'] = (block) => {
    const tag = (block.getFieldValue('TAG') || 'div').toLowerCase()
    const allowed = new Set(['div', 'article'])
    const safeTag = allowed.has(tag) ? tag : 'div'

    const cls = getClass(block, 'wc-box')
    const content = javascriptGenerator.statementToCode(block, 'CONTENT')

    const inner = `<${safeTag} class="${cls}" style="${BASE_BOX_STYLE}">
${content}</${safeTag}>`

    return wrapDraggable({ block, innerHtml: inner })
  }

  // -------------------------
  // DIVIDER (흐름 요소: 드래그 X 유지)
  // -------------------------
  if (!Blockly.Blocks['layout_divider']) {
    Blockly.Blocks['layout_divider'] = {
      init() {
        this.appendDummyInput().appendField('➖ 구분선')
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('wc-divider'), 'ELEMENT_CLASS')
        this.setPreviousStatement(true, 'ELEMENT')
        this.setNextStatement(true, 'ELEMENT')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_divider'] = (block) => {
    const cls = getClass(block, 'wc-divider')
    return `<hr class="${cls}" data-block-id="${block.id}" />\n`
  }

  // -------------------------
  // UL / LI
  // -------------------------
  if (!Blockly.Blocks['layout_ul']) {
    Blockly.Blocks['layout_ul'] = {
      init() {
        this.appendDummyInput().appendField('📦 리스트 박스(UL)')
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('list-container'), 'ELEMENT_CLASS')
        this.appendStatementInput('CONTENT').setCheck('LI').appendField('항목')
        this.setPreviousStatement(true, 'ELEMENT')
        this.setNextStatement(true, 'ELEMENT')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_ul'] = (block) => {
    const cls = getClass(block, 'list-container')
    const items = javascriptGenerator.statementToCode(block, 'CONTENT')

    const inner = `<ul class="${cls}" style="${BASE_BOX_STYLE}">
${items}</ul>`

    return wrapDraggable({ block, innerHtml: inner })
  }

  if (!Blockly.Blocks['layout_li']) {
    Blockly.Blocks['layout_li'] = {
      init() {
        this.appendDummyInput().appendField('📦 리스트 항목(LI)')
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('list-item'), 'ELEMENT_CLASS')
        this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용')
        this.setPreviousStatement(true, 'LI')
        this.setNextStatement(true, 'LI')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_li'] = (block) => {
    const cls = getClass(block, 'list-item')
    const content = javascriptGenerator.statementToCode(block, 'CONTENT')
    // li는 원래 ul 흐름에 종속되므로 드래그 대상 아님
    return `<li class="${cls}" data-block-id="${block.id}">
${content}</li>\n`
  }

  // -------------------------
  // FORM (드래그 대상: wrapper만 이동)
  // -------------------------
  if (!Blockly.Blocks['layout_form']) {
    Blockly.Blocks['layout_form'] = {
      init() {
        this.appendDummyInput().appendField('🧾 폼(Form)')
        this.appendDummyInput()
          .appendField('클래스명:')
          .appendField(new Blockly.FieldTextInput('wc-form'), 'ELEMENT_CLASS')

        this.appendStatementInput('CONTENT').setCheck('ELEMENT').appendField('내용')

        this.setPreviousStatement(true, 'ELEMENT')
        this.setNextStatement(true, 'ELEMENT')
        this.setColour('#4c97ff')
      },
    }
  }

  javascriptGenerator.forBlock['layout_form'] = (block) => {
    const cls = getClass(block, 'wc-form')
    const content = javascriptGenerator.statementToCode(block, 'CONTENT')

    const inner = `<form class="${cls}" style="${BASE_BOX_STYLE}">
${content}</form>`

    return wrapDraggable({ block, innerHtml: inner })
  }
}

export default {}
</script>