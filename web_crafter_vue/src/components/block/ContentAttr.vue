<!-- =========================
✅ ContentAttr.vue (컨텐츠속성)
- ✅ "속성 적용 대상" 블록(컨테이너) + 아래로 속성 블록을 쌓는 구조
- ✅ 속성 블록은 WC_ATTR statement 체인으로 무한 연결
- ✅ HTML에 직접 style="" 박지 않음 / data-wc-attrs 로만 전달 → iframe에서 selector로 적용
- ✅ 대상 입력 규칙:
    1) "이름:클래스"  -> ".클래스"
    2) "클래스"       -> ".클래스"
    3) "#id"          -> "#id"
    4) "[...]"        -> 그대로 selector
========================= -->
<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript'

// ===== 카테고리 메타데이터 =====
export const category = {
  label: '컨텐츠속성',
  color: '#607d8b',
  icon: '🏷️',
}

// ✅ 서버필드 드롭다운(원하면 여기 목록만 수정)
const SERVER_FIELDS = [
  ['(없음)', ''],
  ['아이디(loginId)', 'loginId'],
  ['이메일(email)', 'email'],
  ['이름(name)', 'name'],
  ['닉네임(nickname)', 'nickname'],
  ['비밀번호(password)', 'password'],
  ['비밀번호확인(passwordConfirm)', 'passwordConfirm'],
  ['생년월일(birth)', 'birth'],
]

// ✅ 중복확인 타겟(원하면 여기만 수정)
const DUPLICATE_FIELDS = [
  ['아이디(loginId)', 'loginId'],
  ['이메일(email)', 'email'],
  ['닉네임(nickname)', 'nickname'],
]

// ===== 툴박스 =====
export const toolbox = `
<xml>
  <block type="wc_attr_apply"></block>

  <sep></sep>
  <block type="wc_attr_id"></block>
  <block type="wc_attr_class_add"></block>
  <block type="wc_attr_data"></block>
  <block type="wc_attr_aria"></block>

  <sep></sep>
  <block type="wc_attr_style"></block>
  <block type="wc_attr_placeholder"></block>
  <block type="wc_attr_value"></block>

  <sep></sep>
  <block type="wc_attr_required"></block>
  <block type="wc_attr_disabled"></block>
  <block type="wc_attr_readonly"></block>

  <sep></sep>
  <block type="wc_attr_target_blank"></block>
  <block type="wc_attr_rel_noopener"></block>
  <block type="wc_attr_for"></block>

  <sep></sep>
  <block type="wc_attr_server_field"></block>
  <block type="wc_attr_duplicate_target"></block>
</xml>
`

// ============================================================
// ✅ 블록 정의
// ============================================================
export const defineBlocks = () => {
  // ---------------------------
  // 공통 UI helper
  // ---------------------------
  const title = (block, label) => {
    block.appendDummyInput('T').appendField(label)
  }

  // ✅ 여기로 "속성 블록"만 들어오게 제한
  const ATTR_CHECK = 'WC_ATTR'

  // =========================================================
  // 1) ✅ 속성 적용 대상(컨테이너)
  // =========================================================
  Blockly.Blocks['wc_attr_apply'] = {
    init() {
      title(this, '🏷️ 속성 적용 대상')

      this.appendDummyInput('ROW')
        .appendField('대상')
        .appendField(new Blockly.FieldTextInput('예) 제목:titleClass'), 'TARGET')

      this.appendStatementInput('ATTRS').setCheck(ATTR_CHECK).appendField('속성들')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#607d8b')
      this.setInputsInline(false)
    },
  }

  // =========================================================
  // 2) ✅ 속성 블록들 (모두 statement)
  // =========================================================

  // (a) id
  Blockly.Blocks['wc_attr_id'] = {
    init() {
      title(this, '🆔 아이디(id)')
      this.appendDummyInput('ROW')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'ID')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (b) class+ (추가)
  Blockly.Blocks['wc_attr_class_add'] = {
    init() {
      title(this, '🏷️ 클래스 추가')
      this.appendDummyInput('ROW')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput('extra-class'), 'CLS')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (c) data-*
  Blockly.Blocks['wc_attr_data'] = {
    init() {
      title(this, '📦 data-*')
      this.appendDummyInput('ROW')
        .appendField('키')
        .appendField(new Blockly.FieldTextInput('foo'), 'KEY')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput('bar'), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (d) aria-*
  Blockly.Blocks['wc_attr_aria'] = {
    init() {
      title(this, '♿ aria-*')
      this.appendDummyInput('ROW')
        .appendField('키')
        .appendField(new Blockly.FieldTextInput('label'), 'KEY')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (e) style (data-wc-style에 누적)
  Blockly.Blocks['wc_attr_style'] = {
    init() {
      title(this, '🎨 스타일')
      this.appendDummyInput('ROW')
        .appendField('CSS')
        .appendField(new Blockly.FieldTextInput(''), 'CSS')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (f) placeholder
  Blockly.Blocks['wc_attr_placeholder'] = {
    init() {
      title(this, '💬 안내문(placeholder)')
      this.appendDummyInput('ROW')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (g) value
  Blockly.Blocks['wc_attr_value'] = {
    init() {
      title(this, '✍️ 기본값(value)')
      this.appendDummyInput('ROW')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (h) required
  Blockly.Blocks['wc_attr_required'] = {
    init() {
      title(this, '✅ 필수(required)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (i) disabled
  Blockly.Blocks['wc_attr_disabled'] = {
    init() {
      title(this, '⛔ 비활성(disabled)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (j) readonly
  Blockly.Blocks['wc_attr_readonly'] = {
    init() {
      title(this, '👀 읽기전용(readonly)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (k) target _blank
  Blockly.Blocks['wc_attr_target_blank'] = {
    init() {
      title(this, '🔗 새탭(target=_blank)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (l) rel noopener
  Blockly.Blocks['wc_attr_rel_noopener'] = {
    init() {
      title(this, '🛡️ rel(noopener)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (m) label for
  Blockly.Blocks['wc_attr_for'] = {
    init() {
      title(this, '🏷️ for(라벨 연결)')
      this.appendDummyInput('ROW')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (n) 서버필드(name + data-wc-field)
  Blockly.Blocks['wc_attr_server_field'] = {
    init() {
      title(this, '🔗 서버필드')
      this.appendDummyInput('ROW')
        .appendField('선택')
        .appendField(new Blockly.FieldDropdown(SERVER_FIELDS), 'KEY')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // (o) 중복확인 target (data-wc-action/target)
  Blockly.Blocks['wc_attr_duplicate_target'] = {
    init() {
      title(this, '✅ 중복확인 target')
      this.appendDummyInput('ROW')
        .appendField('대상')
        .appendField(new Blockly.FieldDropdown(DUPLICATE_FIELDS), 'KEY')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  // =========================================================
  // ✅ Generator
  // - wc_attr_apply만 HTML에 "숨김 데이터"를 출력
  // - 나머지 속성 블록은 단독 출력 금지(빈 문자열)
  // =========================================================

  const safeAttr = (v) => (v ?? '').toString().replace(/"/g, '&quot;').trim()

  const collectOps = (applyBlock) => {
    const ops = []
    let b = applyBlock.getInputTargetBlock('ATTRS')
    while (b) {
      const t = b.type
      if (t === 'wc_attr_id') ops.push({ t: 'id', v: (b.getFieldValue('ID') || '').trim() })
      else if (t === 'wc_attr_class_add')
        ops.push({ t: 'class_add', v: (b.getFieldValue('CLS') || '').trim() })
      else if (t === 'wc_attr_data')
        ops.push({
          t: 'data',
          k: (b.getFieldValue('KEY') || '').trim(),
          v: (b.getFieldValue('VAL') || '').trim(),
        })
      else if (t === 'wc_attr_aria')
        ops.push({
          t: 'aria',
          k: (b.getFieldValue('KEY') || '').trim(),
          v: (b.getFieldValue('VAL') || '').trim(),
        })
      else if (t === 'wc_attr_style') ops.push({ t: 'style', v: (b.getFieldValue('CSS') || '').trim() })
      else if (t === 'wc_attr_placeholder')
        ops.push({ t: 'placeholder', v: (b.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_value') ops.push({ t: 'value', v: (b.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_required') ops.push({ t: 'required' })
      else if (t === 'wc_attr_disabled') ops.push({ t: 'disabled' })
      else if (t === 'wc_attr_readonly') ops.push({ t: 'readonly' })
      else if (t === 'wc_attr_target_blank') ops.push({ t: 'target_blank' })
      else if (t === 'wc_attr_rel_noopener') ops.push({ t: 'rel_noopener' })
      else if (t === 'wc_attr_for') ops.push({ t: 'for', v: (b.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_server_field') ops.push({ t: 'server_field', v: (b.getFieldValue('KEY') || '').trim() })
      else if (t === 'wc_attr_duplicate_target')
        ops.push({ t: 'dup_target', v: (b.getFieldValue('KEY') || '').trim() })

      b = b.getNextBlock()
    }
    return ops
  }

  javascriptGenerator.forBlock['wc_attr_apply'] = (block) => {
    const target = (block.getFieldValue('TARGET') || '').trim()
    const ops = collectOps(block)

    // ✅ 아무 속성 없으면 데이터도 안 만든다
    if (!target || ops.length === 0) return ''

    const payload = { target, ops }
    const json = safeAttr(JSON.stringify(payload))

    // ✅ script 안 쓰고 숨김 div로 전달(디자인 모드에서도 안전)
    return `<div data-wc-block="wc_attrs" data-wc-attrs="${json}" style="display:none"></div>\n`
  }

  // ✅ 속성 블록 단독 출력 방지
  const EMPTY = () => ''
  ;[
    'wc_attr_id',
    'wc_attr_class_add',
    'wc_attr_data',
    'wc_attr_aria',
    'wc_attr_style',
    'wc_attr_placeholder',
    'wc_attr_value',
    'wc_attr_required',
    'wc_attr_disabled',
    'wc_attr_readonly',
    'wc_attr_target_blank',
    'wc_attr_rel_noopener',
    'wc_attr_for',
    'wc_attr_server_field',
    'wc_attr_duplicate_target',
  ].forEach((k) => (javascriptGenerator.forBlock[k] = EMPTY))
}

export default {}
</script>