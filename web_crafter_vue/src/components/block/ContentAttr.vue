<!-- =========================
✅ ContentAttr.vue (컨텐츠속성) - "속성은 코드 생성 X, HTML에 병합만"
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

// ===== 툴박스 (간격 통일) =====
export const toolbox = `
<xml>
  <block type="wc_attr_apply" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_id" gap="10"></block>
  <block type="wc_attr_class_add" gap="10"></block>
  <block type="wc_attr_data" gap="10"></block>
  <block type="wc_attr_aria" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_style" gap="10"></block>
  <block type="wc_attr_placeholder" gap="10"></block>
  <block type="wc_attr_value" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_required" gap="10"></block>
  <block type="wc_attr_disabled" gap="10"></block>
  <block type="wc_attr_readonly" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_target_blank" gap="10"></block>
  <block type="wc_attr_rel_noopener" gap="10"></block>
  <block type="wc_attr_for" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_server_field" gap="10"></block>
  <block type="wc_attr_duplicate_target" gap="10"></block>

  <sep gap="18"></sep>
  <block type="wc_attr_text_segment" gap="10"></block>
</xml>
`

// ============================================================
// ✅ 대상 문자열 → selector 변환 (IDE/프리뷰에서도 재사용 가능)
// ============================================================
export const parseTargetToSelector = (raw) => {
  const s = (raw || '').trim()
  if (!s) return ''
  if (s.startsWith('[') && s.endsWith(']')) return s
  if (s.startsWith('#')) return s
  const parts = s.split(':')
  if (parts.length === 2) return '.' + (parts[1] || '').trim()
  return '.' + s
}

// ============================================================
// ✅ workspace에서 "속성 적용 대상" 블록들을 수집 (코드 생성 X)
// - 반환: [{ target: '...', ops:[...]}]
// ============================================================
export const collectContentAttrsFromWorkspace = (workspace) => {
  if (!workspace) return []

  const bundles = []
  const blocks = workspace.getAllBlocks(false)

  for (const b of blocks) {
    if (b.type !== 'wc_attr_apply') continue

    const target = (b.getFieldValue('TARGET') || '').trim()
    if (!target) continue

    const ops = []
    let cur = b.getInputTargetBlock('ATTRS')

    while (cur) {
      const t = cur.type

      if (t === 'wc_attr_id') ops.push({ t: 'id', v: (cur.getFieldValue('ID') || '').trim() })
      else if (t === 'wc_attr_class_add') ops.push({ t: 'class_add', v: (cur.getFieldValue('CLS') || '').trim() })
      else if (t === 'wc_attr_data')
        ops.push({ t: 'data', k: (cur.getFieldValue('KEY') || '').trim(), v: (cur.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_aria')
        ops.push({ t: 'aria', k: (cur.getFieldValue('KEY') || '').trim(), v: (cur.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_style') ops.push({ t: 'style', v: (cur.getFieldValue('CSS') || '').trim() })
      else if (t === 'wc_attr_placeholder') ops.push({ t: 'placeholder', v: (cur.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_value') ops.push({ t: 'value', v: (cur.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_required') ops.push({ t: 'required' })
      else if (t === 'wc_attr_disabled') ops.push({ t: 'disabled' })
      else if (t === 'wc_attr_readonly') ops.push({ t: 'readonly' })
      else if (t === 'wc_attr_target_blank') ops.push({ t: 'target_blank' })
      else if (t === 'wc_attr_rel_noopener') ops.push({ t: 'rel_noopener' })
      else if (t === 'wc_attr_for') ops.push({ t: 'for', v: (cur.getFieldValue('VAL') || '').trim() })
      else if (t === 'wc_attr_server_field') ops.push({ t: 'server_field', v: (cur.getFieldValue('KEY') || '').trim() })
      else if (t === 'wc_attr_duplicate_target') ops.push({ t: 'dup_target', v: (cur.getFieldValue('KEY') || '').trim() })
      else if (t === 'wc_attr_text_segment')
        ops.push({
          t: 'text_segment',
          base: (cur.getFieldValue('BASE') || '').trim(),
          text: (cur.getFieldValue('TEXT') || '').trim(),
          cls: (cur.getFieldValue('CLS') || '').trim(),
          mode: cur.getFieldValue('MODE') || 'after',
        })

      cur = cur.getNextBlock()
    }

    if (ops.length === 0) continue
    bundles.push({ target, ops })
  }

  return bundles
}

// ============================================================
// ✅ 블록 정의 (⚠️ 기능은 그대로, “보이는 라벨만” 초보자용으로 변경)
// ============================================================
export const defineBlocks = () => {
  const ATTR_CHECK = 'WC_ATTR'

  // ✅ 공통: 한 줄 DummyInput
  const makeRow = (block, label) => {
    const row = block.appendDummyInput('ROW')
    row.appendField(label)
    return row
  }

  // =========================================================
  // 1) ✅ 속성 적용 대상(컨테이너)
  // =========================================================
  Blockly.Blocks['wc_attr_apply'] = {
    init() {
      this.appendDummyInput('HEAD').appendField('🏷️ 속성 적용 대상')

      // ✅ TARGET 필드 키는 유지
      this.appendDummyInput('ROW')
        .appendField('대상')
        .appendField(new Blockly.FieldTextInput('이름'), 'TARGET')

      this.appendStatementInput('ATTRS').setCheck(ATTR_CHECK).appendField('속성')

      this.setPreviousStatement(true, 'ELEMENT')
      this.setNextStatement(true, 'ELEMENT')
      this.setColour('#607d8b')
      this.setInputsInline(false)
    },
  }

  // =========================================================
  // 2) ✅ 속성 블록들 (statement)
  // =========================================================

  Blockly.Blocks['wc_attr_id'] = {
    init() {
      const row = makeRow(this, '🆔 요소에 ID 붙이기')
      row.appendField('ID 값').appendField(new Blockly.FieldTextInput(''), 'ID')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_class_add'] = {
    init() {
      const row = makeRow(this, '🏷️ 클래스 추가하기')
      row.appendField('클래스명').appendField(new Blockly.FieldTextInput('extra-class'), 'CLS')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_data'] = {
    init() {
      const row = makeRow(this, '📦 데이터(data-*) 저장하기')
      row
        .appendField('이름')
        .appendField(new Blockly.FieldTextInput('foo'), 'KEY')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput('bar'), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_aria'] = {
    init() {
      const row = makeRow(this, '♿ 접근성(aria-*) 설정')
      row
        .appendField('이름')
        .appendField(new Blockly.FieldTextInput('label'), 'KEY')
        .appendField('값')
        .appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_style'] = {
    init() {
      const row = makeRow(this, '🎨 스타일(CSS) 추가')
      row.appendField('CSS').appendField(new Blockly.FieldTextInput(''), 'CSS')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_placeholder'] = {
    init() {
      const row = makeRow(this, '💬 입력 안내문(placeholder)')
      row.appendField('안내문').appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_value'] = {
    init() {
      const row = makeRow(this, '✍️ 기본 입력값(value) 넣기')
      row.appendField('기본값').appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_required'] = {
    init() {
      makeRow(this, '✅ 꼭 입력해야 함(필수)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_disabled'] = {
    init() {
      makeRow(this, '⛔ 사용 못하게 막기(비활성)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_readonly'] = {
    init() {
      makeRow(this, '👀 읽기만 가능(수정 불가)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_target_blank'] = {
    init() {
      makeRow(this, '🔗 링크를 새 탭에서 열기')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_rel_noopener'] = {
    init() {
      makeRow(this, '🛡️ 새 탭 보안 설정(noopener)')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_for'] = {
    init() {
      const row = makeRow(this, '🏷️ 라벨과 입력칸 연결(for)')
      row.appendField('연결할 ID').appendField(new Blockly.FieldTextInput(''), 'VAL')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_server_field'] = {
    init() {
      const row = makeRow(this, '🔗 서버로 보낼 이름(name) 지정')
      row.appendField('필드 선택').appendField(new Blockly.FieldDropdown(SERVER_FIELDS), 'KEY')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_duplicate_target'] = {
    init() {
      const row = makeRow(this, '✅ 중복확인 대상 지정')
      row.appendField('무엇을?').appendField(new Blockly.FieldDropdown(DUPLICATE_FIELDS), 'KEY')
      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(true)
    },
  }

  Blockly.Blocks['wc_attr_text_segment'] = {
    init() {
      this.appendDummyInput('HEAD').appendField('✏️ 텍스트 옆에 문구 추가')

      // ✅ BASE/TEXT/CLS/MODE 필드 키 유지
      this.appendDummyInput('ROW1')
        .appendField('기준 문구')
        .appendField(new Blockly.FieldTextInput('텍스트'), 'BASE')

      this.appendDummyInput('ROW2')
        .appendField('추가할 문구')
        .appendField(new Blockly.FieldTextInput(''), 'TEXT')
        .appendField('추가문구 이름')
        .appendField(new Blockly.FieldTextInput(''), 'CLS')

      this.appendDummyInput('ROW3')
        .appendField('추가 위치')
        .appendField(
          new Blockly.FieldDropdown([
            ['기준 앞', 'before'],
            ['기준 뒤', 'after'],
            ['기준을 바꾸기', 'replace'],
          ]),
          'MODE'
        )

      this.setPreviousStatement(true, ATTR_CHECK)
      this.setNextStatement(true, ATTR_CHECK)
      this.setColour('#607d8b')
      this.setInputsInline(false)
    },
  }

  // =========================================================
  // ✅ Generator: 전부 빈 문자열 유지
  // =========================================================
  javascriptGenerator.forBlock['wc_attr_apply'] = () => ''

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
    'wc_attr_text_segment',
  ].forEach((k) => (javascriptGenerator.forBlock[k] = EMPTY))
}

export default {}
</script>