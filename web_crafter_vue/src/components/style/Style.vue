<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import { FieldColour } from '@blockly/field-colour';
import Picker from 'vanilla-picker';

const withUnit = (value, defaultUnit = 'px') => {
  if (!value) return '';
  const v = value.toString().trim();
  if (/^\d+(\.\d+)?$/.test(v)) return v + defaultUnit;
  const validUnitPattern =
    /^-?\d+(\.\d+)?(px|%|em|rem|vw|vh|vmin|vmax|ch|ex|cm|mm|in|pt|pc)$/;
  if (validUnitPattern.test(v)) return v;
  if (v === 'auto') return v;
  console.warn('[Style ignored: invalid unit]', v);
  return '';
};

// ✅ 딱 한 번만 선언
class FieldModalColor extends FieldColour {
  constructor(value = '#000000') {
    super(value);
  }

  showEditor_() {
    const blockSvg = this.sourceBlock_.getSvgRoot();
    const rect = blockSvg.getBoundingClientRect();

    const anchor = document.createElement('div');
    anchor.style.cssText = `
      position: fixed;
      left: ${rect.left + rect.width / 2}px;
      top: ${rect.top - 10}px;
      z-index: 9999;
    `;
    document.body.appendChild(anchor);

    const picker = new Picker({
      parent: anchor,
      popup: 'top',
      alpha: false,
      color: this.getValue(),
      onDone: (color) => {
        this.setValue(color.hex.slice(0, 7));
      },
      onClose: () => {
        anchor.remove();
      },
    });

    picker.openHandler();
  }
}

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨',
};

export const toolbox = `
<xml>
  <block type="style_tag"></block>
  <block type="style_tag_all"></block>
  <label text="──────────────────────"></label>
  <block type="style_size"></block>
  <block type="style_size_limits"></block>
  <block type="style_overflow"></block>
  <block type="style_display"></block>
  <block type="style_transition_move"></block>
  <block type="style_filter"></block>
  <block type="style_background_gradient"></block>
  <block type="style_shadow"></block>
  <block type="style_transition"></block>
  <block type="style_border_radius"></block>
  <label text="──────────────────────"></label>
  <block type="style_padding"></block>
  <block type="style_margin"></block>
  <block type="style_list_style"></block>
  <label text="──────────────────────"></label>
  <block type="style_position"></block>
  <block type="style_z_index"></block>
  <block type="style_offset"></block>
  <block type="style_inset_0"></block>
</xml>
`;

export const defineBlocks = () => {
  const safeClass = (raw, fallback) =>
    (raw || fallback)
      .toString()
      .trim()
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_\-가-힣]/g, '');

  // --- 블록 정의 시작 ---

  Blockly.Blocks['style_tag'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🎨 스타일 적용 대상')
        .appendField(new Blockly.FieldTextInput('container'), 'SELECTOR')
        .appendField('상태')
        .appendField(
          new Blockly.FieldDropdown([
            ['기본(항상)', ''],
            ['마우스 올렸을 때', ':hover'],
            ['클릭했을 때', ':active'],
          ]),
          'STATE'
        );
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };

  Blockly.Blocks['style_tag_all'] = {
    init: function () {
      this.appendDummyInput().appendField('🎨 전체 스타일');
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };
  Blockly.Blocks['style_size'] = {
    init() {
      this.appendDummyInput()
        .appendField('📐 크기 너비')
        .appendField(new Blockly.FieldTextInput('100%'), 'WIDTH')
        .appendField('높이')
        .appendField(new Blockly.FieldTextInput('auto'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_border_radius'] = {
    init() {
      this.appendDummyInput()
        .appendField('🔘 둥근 모서리')
        .appendField(new Blockly.FieldTextInput('10'), 'RADIUS')
        .appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_display'] = {
    init() {
      this.appendDummyInput()
        .appendField('📍 배치 방식')
        .appendField(
          new Blockly.FieldDropdown([
            ['가로로 나열 (inline-block)', 'inline-block'],
            ['한 줄 전체 차지 (block)', 'block'],
            ['글자처럼 취급 (inline)', 'inline'],
            ['숨기기 (none)', 'none'],
          ]),
          'DISPLAY'
        );
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_list_style'] = {
    init() {
      this.appendDummyInput()
        .appendField('📋 리스트 스타일')
        .appendField('종류')
        .appendField(
          new Blockly.FieldDropdown([
            ['● 기본(disc)', 'disc'],
            ['○ 원(circle)', 'circle'],
            ['■ 사각(square)', 'square'],
            ['1. 숫자(decimal)', 'decimal'],
            ['a. 알파벳(lower-alpha)', 'lower-alpha'],
            ['없음(none)', 'none'],
          ]),
          'TYPE'
        )
        .appendField('마커 위치')
        .appendField(
          new Blockly.FieldDropdown([
            ['바깥(outside)', 'outside'],
            ['안쪽(inside)', 'inside'],
          ]),
          'POSITION'
        );
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_overflow'] = {
    init() {
      this.appendDummyInput()
        .appendField('🌊 넘침 처리(overflow)')
        .appendField(
          new Blockly.FieldDropdown([
            ['자동 (스크롤 자동)', 'auto'],
            ['숨기기 (hidden)', 'hidden'],
            ['항상 스크롤 (scroll)', 'scroll'],
            ['보여주기 (visible)', 'visible'],
            ['가로 스크롤만', 'overflow-x: scroll; overflow-y: hidden;'],
            ['세로 스크롤만', 'overflow-y: scroll; overflow-x: hidden;'],
          ]),
          'OVERFLOW'
        );
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_shadow'] = {
    init() {
      this.appendDummyInput()
        .appendField(
          new Blockly.FieldDropdown([
            ['👥 박스 그림자', 'box-shadow'],
            ['✍️ 글자 그림자', 'text-shadow'],
          ]),
          'TYPE'
        )
        .appendField('우측') // X 대신 '우측으로 얼마나'
        .appendField(new Blockly.FieldTextInput('0'), 'X')
        .appendField('하단') // Y 대신 '아래로 얼마나'
        .appendField(new Blockly.FieldTextInput('4'), 'Y')
        .appendField('퍼짐') // B(Blur) 대신 '얼마나 뿌옇게'
        .appendField(new Blockly.FieldTextInput('10'), 'BLUR')
        .appendField('🎨')
        .appendField(new FieldModalColor('#000000'), 'COLOR');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_transition'] = {
    init() {
      this.appendDummyInput()
        .appendField('⏱️ 부드러운 변화')
        // 0.3은 기본값, 0은 최소값(음수 방지), 뒤의 숫자를 지우면 최대 제한이 없어집니다.
        .appendField(new Blockly.FieldNumber(0.3, 0), 'DURATION')
        .appendField('초 동안')
        .appendField(
          new Blockly.FieldDropdown([
            ['부드럽게 시작/끝', 'ease-in-out'],
            ['일정하게', 'linear'],
            ['천천히 시작', 'ease-in'],
            ['천천히 끝', 'ease-out'],
          ]),
          'TIMING'
        );
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_transition_move'] = {
    init() {
      this.appendDummyInput()
        .appendField('🚀 상대 이동')
        .appendField(
          new Blockly.FieldDropdown([
            ['오른쪽으로', 'translateX'],
            ['왼쪽으로', 'translateX_minus'],
            ['아래로', 'translateY'],
            ['위로', 'translateY_minus'],
          ]),
          'DIR'
        )
        .appendField(new Blockly.FieldTextInput('20'), 'DISTANCE')
        .appendField('px 만큼');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_size_limits'] = {
    init() {
      this.appendDummyInput()
        .appendField('📏 크기 제한')
        .appendField(
          new Blockly.FieldDropdown([
            ['너비(Width)', 'width'],
            ['높이(Height)', 'height'],
          ]),
          'TYPE'
        )
        .appendField(
          new Blockly.FieldDropdown([
            ['최소(min)', 'min'],
            ['최대(max)', 'max'],
          ]),
          'LIMIT'
        )
        // FieldTextInput을 사용해야 %, rem 등을 직접 타이핑할 수 있습니다.
        .appendField(new Blockly.FieldTextInput('100'), 'VALUE');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_background_gradient'] = {
    init() {
      this.appendDummyInput()
        .appendField('🌈')
        .appendField(
          new Blockly.FieldDropdown([
            ['배경', 'background'],
            ['글자', 'text'],
          ]),
          'TARGET'
        )
        .appendField(
          new Blockly.FieldDropdown([
            ['→', 'to right'],
            ['←', 'to left'],
            ['↓', 'to bottom'],
            ['↑', 'to top'],
            ['↘', 'to bottom right'],
            ['○', 'circle'],
          ]),
          'DIR'
        )
        .appendField(new FieldModalColor('#ff0000'), 'COLOR1')
        .appendField(new Blockly.FieldTextInput('0'), 'POS1')
        .appendField('%')
        .appendField(new FieldModalColor('#0000ff'), 'COLOR2')
        .appendField(new Blockly.FieldTextInput('100'), 'POS2')
        .appendField('%');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_filter'] = {
    init() {
      this.appendDummyInput()
        .appendField('✨ 필터 효과')
        .appendField(
          new Blockly.FieldDropdown([
            ['흐림(blur)', 'blur'],
            ['밝기(brightness)', 'brightness'],
            ['대비(contrast)', 'contrast'],
            ['회색조(grayscale)', 'grayscale'],
            ['색상 회전(hue-rotate)', 'hue-rotate'],
            ['반전(invert)', 'invert'],
          ]),
          'TYPE'
        )
        /* 이제 여기에 '10', '50%', '180deg' 등을 한 번에 쓰시면 됩니다 */
        .appendField(new Blockly.FieldTextInput('5'), 'VAL');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  // =========================
  // ✅ [추가] Position / Z-Index / Offset / Inset
  // =========================

  Blockly.Blocks['style_position'] = {
    init() {
      this.appendDummyInput()
        .appendField('📌 위치 기준 (position)')
        .appendField(
          new Blockly.FieldDropdown([
            ['기본(static)', 'static'],
            ['상대(relative)', 'relative'],
            ['절대(absolute)', 'absolute'],
            ['화면고정(fixed)', 'fixed'],
            ['스크롤고정(sticky)', 'sticky'],
          ]),
          'POS'
        );

      this.appendDummyInput()
        .appendField('sticky top')
        .appendField(new Blockly.FieldTextInput('0'), 'STICKY_TOP')
        .appendField('(sticky일 때만)');

      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_z_index'] = {
    init() {
      this.appendDummyInput()
        .appendField('🧱 레이어 순서 (z-index)')
        .appendField(new Blockly.FieldNumber(1, -999999, 999999, 1), 'Z');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_offset'] = {
    init() {
      this.appendDummyInput()
        .appendField('↔️ 위치 이동 (offset)')
        .appendField(
          new Blockly.FieldDropdown([
            ['top', 'top'],
            ['right', 'right'],
            ['bottom', 'bottom'],
            ['left', 'left'],
          ]),
          'SIDE'
        )
        .appendField(new Blockly.FieldTextInput('0'), 'VAL');

      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_inset_0'] = {
    init() {
      this.appendDummyInput().appendField('🟦 오버레이 전체 채우기 (inset: 0)');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  // --- 제너레이터 정의 시작 (defineBlocks 함수 안에 포함) ---
  javascriptGenerator.forBlock['style_tag'] = function (block, generator) {
    const raw = (block.getFieldValue('SELECTOR') || 'container').trim();
    const cls = raw.replace(/^[.#]/, '');
    const state = block.getFieldValue('STATE') || '';
    const bodyCode = generator.statementToCode(block, 'BODY') || '';

    // ❌ [삭제] block.data를 이용한 posCSS 생성 로직을 완전히 제거했습니다.
    // 이제 에디터 내 블록 위치(x, y)가 실제 요소의 위치(top, left)를 결정하지 않습니다.

    // bodyCode 내부의 스타일 속성만 깔끔하게 반환합니다.
    return `<style>\n.${cls}${state} {\n  ${bodyCode.trim()}\n}\n<\/style>\n`;
  };

  // 1. 전체 스타일 태그 (#wrapper 대상)
  javascriptGenerator.forBlock['style_tag_all'] = function (block, generator) {
    const bodyCode = generator.statementToCode(block, 'BODY') || '';

    if (!bodyCode.trim()) return '';

    return `<style>\n#wrapper {\n  ${bodyCode.trim()}\n}\n</style>\n`;
  };

  // 2. 크기(너비/높이) 설정 블록
  javascriptGenerator.forBlock['style_size'] = (block) => {
    const w = block.getFieldValue('WIDTH');
    const h = block.getFieldValue('HEIGHT');
    let code = '';

    // 너비 처리: 숫자만 있으면 px 추가, 단위가 있으면 그대로 유지
    if (w && w !== 'auto') {
      const widthValue = /^\d+(\.\d+)?$/.test(w) ? w + 'px' : w;
      code += `width: ${widthValue} !important; `;
    }

    // 높이 처리: 숫자만 있으면 px 추가, 단위가 있으면 그대로 유지
    if (h && h !== 'auto') {
      const heightValue = /^\d+(\.\d+)?$/.test(h) ? h + 'px' : h;
      code += `height: ${heightValue} !important; `;
    }

    return code + '\n';
  };

  javascriptGenerator.forBlock['style_list_style'] = (block) => {
    const type = block.getFieldValue('TYPE');
    const position = block.getFieldValue('POSITION');
    if (type === 'none') return `list-style: none;\npadding-left: 0;\n`;
    return `list-style-type: ${type};\nlist-style-position: ${position};\n`;
  };
  javascriptGenerator.forBlock['style_border_radius'] = (block) =>
    `border-radius: ${block.getFieldValue('RADIUS')}px;\n`;

  javascriptGenerator.forBlock['style_display'] = (block) => {
    const display = block.getFieldValue('DISPLAY');
    return `display: ${display};\n`;
  };
};

javascriptGenerator.forBlock['style_overflow'] = (block) => {
  const value = block.getFieldValue('OVERFLOW');
  // 가로/세로 전용 옵션이 선택된 경우 직접 코드를 반환하고, 아니면 기본 overflow 속성 적용
  if (value.includes(':')) {
    return `${value}\n`;
  }
  return `overflow: ${value};\n`;
};

javascriptGenerator.forBlock['style_shadow'] = function (block) {
  // getFieldValue 뒤의 인자가 위 init()의 대문자 이름들과 '완벽히' 같아야 합니다.
  const type = block.getFieldValue('TYPE') || 'box-shadow';
  const x = block.getFieldValue('X') || '0';
  const y = block.getFieldValue('Y') || '0';
  const blur = block.getFieldValue('BLUR') || '0';
  const color = block.getFieldValue('COLOR');

  // 브라우저 인식 오류를 막기 위해 px 단위를 여기서 강제 결합합니다.
  return `${type}: ${x}px ${y}px ${blur}px ${color} !important;\n`;
};

javascriptGenerator.forBlock['style_transition'] = (block) => {
  const duration = block.getFieldValue('DURATION'); // 사용자가 입력한 숫자값 (예: 1.5, 3, 0.7)
  const timing = block.getFieldValue('TIMING') || 'ease-in-out';

  // 만약 값이 없으면 기본값 0.3s를 사용하도록 처리
  const finalDuration = duration !== null ? duration : 0.3;

  return `transition: all ${finalDuration}s ${timing};\n`;
};

javascriptGenerator.forBlock['style_transition_move'] = (block) => {
  const dir = block.getFieldValue('DIR');
  const dist = block.getFieldValue('DISTANCE') || '0';

  let transformValue = '';

  switch (dir) {
    case 'translateX':
      transformValue = `translateX(${dist}px)`;
      break;
    case 'translateX_minus':
      transformValue = `translateX(-${dist}px)`;
      break;
    case 'translateY':
      transformValue = `translateY(${dist}px)`;
      break;
    case 'translateY_minus':
      transformValue = `translateY(-${dist}px)`;
      break;
    // ✨ 여기에 '크기 늘리기' 로직 하나만 추가하세요
    case 'scale':
      transformValue = `scale(${dist})`;
      break;
  }

  return `--wc-transform: ${transformValue};\ntransform: var(--wc-transform) translateZ(0);\n`;
};

javascriptGenerator.forBlock['style_size_limits'] = (block) => {
  const type = block.getFieldValue('TYPE'); // width / height
  const limit = block.getFieldValue('LIMIT'); // min / max
  const value = block.getFieldValue('VALUE');

  const property = `${limit}-${type}`;

  // withUnit 함수가 입력값에 따라 px을 붙이거나 단위를 유지해줍니다.
  return `${property}: ${withUnit(value)};\n`;
};

javascriptGenerator.forBlock['style_background_gradient'] = (block) => {
  const target = block.getFieldValue('TARGET');
  const dir = block.getFieldValue('DIR');
  const color1 = block.getFieldValue('COLOR1');
  const color2 = block.getFieldValue('COLOR2');

  const gradient = `linear-gradient(${dir}, ${color1}, ${color2})`;

  if (target === 'text') {
    return (
      `background: ${gradient};\n` +
      `-webkit-background-clip: text;\n` +
      `-webkit-text-fill-color: transparent;\n` +
      `display: block;\n`
    );
  }

  return `background: ${gradient};\n`;
};

javascriptGenerator.forBlock['style_filter'] = (block) => {
  const type = block.getFieldValue('TYPE');
  const val = block.getFieldValue('VAL') || '0';

  // withUnit 함수를 써서 숫자만 입력하면 px, 단위를 쓰면 그대로 유지
  // 단, brightness나 contrast처럼 %가 기본인 것들을 위해 필터별 기본 단위 설정
  let defaultUnit = 'px';
  if (['brightness', 'contrast', 'grayscale', 'invert'].includes(type)) {
    defaultUnit = '%';
  } else if (type === 'hue-rotate') {
    defaultUnit = 'deg';
  }

  const finalValue = withUnit(val, defaultUnit);

  return `filter: ${type}(${finalValue}) !important;\n`;
};
/* =========================================================
   [Padding] 안쪽 여백 (수정됨)
   - 값 통일: all, vertical, horizontal, top, bottom...
========================================================= */
Blockly.Blocks['style_padding'] = {
  init() {
    this.appendDummyInput('MAIN')
      .appendField('📦 안쪽 여백')
      .appendField(
        new Blockly.FieldDropdown(
          [
            ['전체', 'all'], // 🔥 수정: 'padding' -> 'all' (AI랑 맞춤)
            ['위+아래', 'vertical'],
            ['왼쪽+오른쪽', 'horizontal'],
            ['위', 'top'],
            ['아래', 'bottom'],
            ['왼쪽', 'left'],
            ['오른쪽', 'right'],
          ],
          this.validate.bind(this)
        ),
        'SIDE'
      )
      .appendField(new Blockly.FieldTextInput('0'), 'VAL1');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ab47bc');
  },
  validate(newValue) {
    const mainInput = this.getInput('MAIN');
    if (this.getField('TEXT_PRE')) mainInput.removeField('TEXT_PRE');
    if (this.getField('TEXT_AND')) mainInput.removeField('TEXT_AND');
    if (this.getField('VAL2')) mainInput.removeField('VAL2');

    if (newValue === 'vertical' || newValue === 'horizontal') {
      const labelPre = newValue === 'vertical' ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = newValue === 'vertical' ? ' 아래: ' : ' 오른쪽: ';
      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      mainInput
        .appendField(labelAnd, 'TEXT_AND')
        .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
    return newValue;
  },
};

// 🔥 [제너레이터 수정] 올바른 CSS 속성명 생성 (padding-top 등)
javascriptGenerator.forBlock['style_padding'] = (block) => {
  const side = block.getFieldValue('SIDE');
  const v1 = block.getFieldValue('VAL1');
  const v2 = block.getFieldValue('VAL2');

  if (side === 'all') return `padding: ${withUnit(v1)} !important;\n`;
  if (side === 'vertical')
    return `padding-top: ${withUnit(v1)} !important; padding-bottom: ${withUnit(v2 || v1)} !important;\n`;
  if (side === 'horizontal')
    return `padding-left: ${withUnit(v1)} !important; padding-right: ${withUnit(v2 || v1)} !important;\n`;

  // top, bottom, left, right 인 경우
  return `padding-${side}: ${withUnit(v1)} !important;\n`;
};

/* =========================================================
[Margin] 바깥 여백 (수정됨)
========================================================= */
Blockly.Blocks['style_margin'] = {
  init() {
    this.appendDummyInput('MAIN')
      .appendField('↔️ 바깥 여백')
      .appendField(
        new Blockly.FieldDropdown(
          [
            ['전체', 'all'], // 🔥 수정: 'margin' -> 'all'
            ['위+아래', 'vertical'],
            ['왼쪽+오른쪽', 'horizontal'],
            ['위', 'top'],
            ['아래', 'bottom'],
            ['왼쪽', 'left'],
            ['오른쪽', 'right'],
          ],
          this.validate.bind(this)
        ),
        'SIDE'
      )
      .appendField(new Blockly.FieldTextInput('0'), 'VAL1');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ab47bc');
  },
  validate(newValue) {
    const mainInput = this.getInput('MAIN');
    if (this.getField('TEXT_PRE')) mainInput.removeField('TEXT_PRE');
    if (this.getField('TEXT_AND')) mainInput.removeField('TEXT_AND');
    if (this.getField('VAL2')) mainInput.removeField('VAL2');

    if (newValue === 'vertical' || newValue === 'horizontal') {
      const labelPre = newValue === 'vertical' ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = newValue === 'vertical' ? ' 아래: ' : ' 오른쪽: ';
      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      mainInput
        .appendField(labelAnd, 'TEXT_AND')
        .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
    return newValue;
  },
};

// 🔥 [제너레이터 수정]
javascriptGenerator.forBlock['style_margin'] = (block) => {
  const side = block.getFieldValue('SIDE');
  const v1 = block.getFieldValue('VAL1');
  const v2 = block.getFieldValue('VAL2');

  if (side === 'all') return `margin: ${withUnit(v1)} !important;\n`;
  if (side === 'vertical')
    return `margin-top: ${withUnit(v1)} !important; margin-bottom: ${withUnit(v2 || v1)} !important;\n`;
  if (side === 'horizontal')
    return `margin-left: ${withUnit(v1)} !important; margin-right: ${withUnit(v2 || v1)} !important;\n`;

  return `margin-${side}: ${withUnit(v1)} !important;\n`;
};

// =========================
// ✅ [추가] Position / Z-Index / Offset / Inset Generators
// =========================

// position
javascriptGenerator.forBlock['style_position'] = (block) => {
  const pos = block.getFieldValue('POS') || 'static';
  const stickyTop = block.getFieldValue('STICKY_TOP') || '0';

  let code = `position: ${pos} !important;\n`;

  if (pos === 'sticky') {
    code += `top: ${withUnit(stickyTop)} !important;\n`;
  }

  return code;
};

// z-index
javascriptGenerator.forBlock['style_z_index'] = (block) => {
  const z = block.getFieldValue('Z');
  return `z-index: ${z} !important;\n`;
};

// top/right/bottom/left
javascriptGenerator.forBlock['style_offset'] = (block) => {
  const side = block.getFieldValue('SIDE') || 'top';
  const val = block.getFieldValue('VAL') || '0';
  return `${side}: ${withUnit(val)} !important;\n`;
};

// inset: 0
javascriptGenerator.forBlock['style_inset_0'] = () => {
  return `inset: 0 !important;\n`;
};
</script>
