<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import { FieldColour } from '@blockly/field-colour';
import Picker from 'vanilla-picker';

const withUnit = (value, defaultUnit = 'px') => {
  if (!value) return '';
  const v = value.toString().trim();
  if (/^\d+(\.\d+)?$/.test(v)) return v + defaultUnit;
  const validUnitPattern = /^-?\d+(\.\d+)?(px|%|em|rem|vw|vh|vmin|vmax|ch|ex|cm|mm|in|pt|pc)$/;
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
      }
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
  <block type="style_font_size"></block>
  <block type="style_font_weight"></block>
  <block type="style_line_height"></block>
  <block type="style_letter_spacing"></block>
  <block type="style_white_space"></block>
  <label text="──────────────────────"></label>
  <block type="style_size"></block>
  <block type="style_overflow"></block>
  <block type="style_display"></block>
  <block type="style_transition_move"></block>
  <block type="style_opacity"></block>
  <block type="style_shadow"></block>
  <block type="style_text_align"></block>
  <block type="style_transition"></block>
  <block type="style_border_radius"></block>
  <label text="──────────────────────"></label>
  <block type="style_padding"></block>
  <block type="style_margin"></block>
  <block type="style_list_style"></block>
</xml>
`;

export const defineBlocks = () => {
  const safeClass = (raw, fallback) =>
    (raw || fallback).toString()
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
        .appendField(new Blockly.FieldDropdown([['기본(항상)', ''], ['마우스 올렸을 때', ':hover'], ['클릭했을 때', ':active']]), 'STATE');
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };

  Blockly.Blocks['style_tag_all'] = {
    init: function () {
      this.appendDummyInput()
        .appendField('🎨 전체 스타일');
      this.appendStatementInput('BODY').setCheck('STYLE').appendField('속성들');
      this.setPreviousStatement(false, null);
      this.setNextStatement(false, null);
      this.setColour('#ab47bc');
      this.hat = 'cap';
    },
  };
  Blockly.Blocks['style_font_size'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 크기').appendField(new Blockly.FieldTextInput('20'), 'SIZE').appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_font_weight'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 두께').appendField(new Blockly.FieldDropdown([['100 Thin', '100'], ['300 Light', '300'], ['400 Regular', '400'], ['500 Medium', '500'], ['700 Bold', '700'], ['900 Black', '900']]), 'WEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_line_height'] = {
    init() {
      this.appendDummyInput().appendField('📏 줄 간격').appendField(new Blockly.FieldTextInput('1.6'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_letter_spacing'] = {
    init() {
      this.appendDummyInput().appendField('↔️ 자간').appendField(new Blockly.FieldTextInput('0.05'), 'SPACE').appendField('em');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_size'] = {
    init() {
      this.appendDummyInput().appendField('📐 크기 너비').appendField(new Blockly.FieldTextInput('100%'), 'WIDTH').appendField('높이').appendField(new Blockly.FieldTextInput('auto'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_opacity'] = {
    init: function () {
      this.appendDummyInput().appendField('🏁 투명도').appendField(new Blockly.FieldNumber(100, 0, 100), 'OPACITY').appendField('%');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_text_align'] = {
    init() {
      this.appendDummyInput().appendField('📝 정렬').appendField(new Blockly.FieldDropdown([['왼쪽', 'left'], ['가운데', 'center'], ['오른쪽', 'right']]), 'ALIGN');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_border_radius'] = {
    init() {
      this.appendDummyInput().appendField('🔘 둥근 모서리').appendField(new Blockly.FieldTextInput('10'), 'RADIUS').appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };

  Blockly.Blocks['style_padding'] = {
    init() {
      this.appendDummyInput('MAIN')
        .appendField('📦 안쪽 여백')
        .appendField(new Blockly.FieldDropdown([
          ['전체', 'padding'],
          ['위+아래', 'vertical'],
          ['왼쪽+오른쪽', 'horizontal'],
          ['위', 'padding-top'],
          ['아래', 'padding-bottom'],
          ['왼쪽', 'padding-left'],
          ['오른쪽', 'padding-right']
        ], this.validate.bind(this)), 'SIDE')
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
      const labelPre = (newValue === 'vertical') ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = (newValue === 'vertical') ? ' 아래: ' : ' 오른쪽: ';

      // 첫 번째 입력창(VAL1) 앞에 라벨 삽입
      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      // 뒤쪽에 라벨과 두 번째 입력창 추가
      mainInput.appendField(labelAnd, 'TEXT_AND')
                .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
      return newValue;
    }
  };

  Blockly.Blocks['style_margin'] = {
    init() {
      this.appendDummyInput('MAIN')
        .appendField('↔️ 바깥 여백')
        .appendField(new Blockly.FieldDropdown([
          ['전체', 'margin'],
          ['위+아래', 'vertical'],
          ['왼쪽+오른쪽', 'horizontal'],
          ['위', 'margin-top'],
          ['아래', 'margin-bottom'],
          ['왼쪽', 'margin-left'],
          ['오른쪽', 'margin-right']
        ],this.validate.bind(this)), 'SIDE')
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
      const labelPre = (newValue === 'vertical') ? ' 위: ' : ' 왼쪽: ';
      const labelAnd = (newValue === 'vertical') ? ' 아래: ' : ' 오른쪽: ';

      mainInput.insertFieldAt(2, new Blockly.FieldLabel(labelPre), 'TEXT_PRE');
      mainInput.appendField(labelAnd, 'TEXT_AND')
                .appendField(new Blockly.FieldTextInput('0'), 'VAL2');
    }
      return newValue;
    }
  };

  Blockly.Blocks['style_display'] = {
    init() {
      this.appendDummyInput()
        .appendField('📍 배치 방식')
        .appendField(new Blockly.FieldDropdown([
          ['가로로 나열 (inline-block)', 'inline-block'],
          ['한 줄 전체 차지 (block)', 'block'],
          ['글자처럼 취급 (inline)', 'inline'],
          ['숨기기 (none)', 'none']
        ]), 'DISPLAY');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    }
  };

  Blockly.Blocks['style_list_style'] = {
    init() {
      this.appendDummyInput().appendField("📋 리스트 스타일")
        .appendField("종류")
        .appendField(new Blockly.FieldDropdown([["● 기본(disc)", "disc"], ["○ 원(circle)", "circle"], ["■ 사각(square)", "square"], 
          ["1. 숫자(decimal)", "decimal"], ["a. 알파벳(lower-alpha)", "lower-alpha"], ["없음(none)", "none"]]), "TYPE")
        .appendField("마커 위치").appendField(new Blockly.FieldDropdown([["바깥(outside)", "outside"], ["안쪽(inside)", "inside"]]), "POSITION");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#ab47bc');
    }
  };

  Blockly.Blocks['style_overflow'] = {
  init() {
    this.appendDummyInput()
      .appendField('🌊 넘침 처리(overflow)')
      .appendField(new Blockly.FieldDropdown([
        ['자동 (스크롤 자동)', 'auto'],
        ['숨기기 (hidden)', 'hidden'],
        ['항상 스크롤 (scroll)', 'scroll'],
        ['보여주기 (visible)', 'visible'],
        ['가로 스크롤만', 'overflow-x: scroll; overflow-y: hidden;'],
        ['세로 스크롤만', 'overflow-y: scroll; overflow-x: hidden;']
      ]), 'OVERFLOW');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ab47bc');
  },
};

  Blockly.Blocks['style_white_space'] = {
    init() {
      this.appendDummyInput()
        .appendField('📖 줄 바꿈 설정')
        .appendField(new Blockly.FieldDropdown([
          ['자동 (normal)', 'normal'],
          ['줄바꿈 안함 (nowrap)', 'nowrap'],
          ['엔터 유지+옆으로 (pre)', 'pre'],
          ['엔터 유지+자동 줄바꿈 (pre-wrap)', 'pre-wrap'], // 추천 옵션
          ['엔터만 유지 (pre-line)', 'pre-line'],
          ['부모 설정 따름 (inherit)', 'inherit']
        ]), 'WS');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    }
  };

  Blockly.Blocks['style_shadow'] = {
    init() {
      this.appendDummyInput()
        .appendField(new Blockly.FieldDropdown([
          ['👥 박스 그림자', 'box-shadow'],
          ['✍️ 글자 그림자', 'text-shadow']
        ]), 'TYPE')
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
    }
  };

  Blockly.Blocks['style_transition'] = {
    init() {
      this.appendDummyInput()
        .appendField('⏱️ 부드러운 변화')
        // 0.3은 기본값, 0은 최소값(음수 방지), 뒤의 숫자를 지우면 최대 제한이 없어집니다.
        .appendField(new Blockly.FieldNumber(0.3, 0), 'DURATION') 
        .appendField('초 동안')
        .appendField(new Blockly.FieldDropdown([
          ['부드럽게 시작/끝', 'ease-in-out'],
          ['일정하게', 'linear'],
          ['천천히 시작', 'ease-in'],
          ['천천히 끝', 'ease-out']
        ]), 'TIMING');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    },
  };


  Blockly.Blocks['style_transition_move'] = {
    init() {
      this.appendDummyInput()
        .appendField('🚀 상대 이동')
        .appendField(new Blockly.FieldDropdown([
          ['오른쪽으로', 'translateX'],
          ['왼쪽으로', 'translateX(-'], // 직접 마이너스 기호 시작
          ['아래로', 'translateY'],
          ['위로', 'translateY(-']     // 직접 마이너스 기호 시작
        ]), 'DIR')
        .appendField(new Blockly.FieldTextInput('20'), 'DISTANCE')
        .appendField('px 만큼');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ab47bc');
    }
  };

  // --- 제너레이터 정의 시작 (defineBlocks 함수 안에 포함) ---

javascriptGenerator.forBlock['style_tag'] = function (block, generator) {
  const raw = (block.getFieldValue('SELECTOR') || 'container').trim();
  const cls = raw.replace(/^[.#]/, '');
  const state = block.getFieldValue('STATE') || '';
  const bodyCode = generator.statementToCode(block, 'BODY') || '';

  let posCSS = '';
  // ✨ 마우스로 옮긴 데이터가 있다면 CSS 문구로 직접 생성
  if (block.data && state === '') {
    try {
      const pos = JSON.parse(block.data);
      if (typeof pos.x === 'number' && typeof pos.y === 'number') {
        posCSS = `  position: absolute !important;\n  left: ${pos.x}px !important;\n  top: ${pos.y}px !important;\n  margin: 0 !important;\n`;
      }
    } catch (e) {}
  }

  return `<style>\n.${cls}${state} {\n${posCSS}${bodyCode.trim()}\n}\n<\/style>\n`;
};

  javascriptGenerator.forBlock['style_tag_all'] = function (block, generator) {
    const bodyCode = generator.statementToCode(block, 'BODY') || '';
    return `<style> \n#wrapper {\n ${bodyCode.trim()} }<\/style>\n`;
  };

  javascriptGenerator.forBlock['style_font_size'] = (block) => `font-size: ${block.getFieldValue('SIZE')}px;\n`;
  javascriptGenerator.forBlock['style_font_weight'] = (block) => `font-weight: ${block.getFieldValue('WEIGHT')};\n`;
  javascriptGenerator.forBlock['style_line_height'] = (block) => `line-height: ${block.getFieldValue('HEIGHT')};\n`;
  javascriptGenerator.forBlock['style_letter_spacing'] = (block) => `letter-spacing: ${block.getFieldValue('SPACE')}em;\n`;

  javascriptGenerator.forBlock['style_size'] = (block) => {
    const w = block.getFieldValue('WIDTH');
    const h = block.getFieldValue('HEIGHT');
    let code = '';
    if (w && w !== 'auto') code += `width: ${/^\d+$/.test(w) ? w + 'px' : w}; `;
    if (h && h !== 'auto') code += `height: ${/^\d+$/.test(h) ? h + 'px' : h}; `;
    return code + '\n';
  };

  javascriptGenerator.forBlock['style_list_style'] = (block) => {
    const type = block.getFieldValue('TYPE');
    const position = block.getFieldValue('POSITION');
    if (type === 'none') return `list-style: none;\npadding-left: 0;\n`;
    return `list-style-type: ${type};\nlist-style-position: ${position};\n`;
  };

  javascriptGenerator.forBlock['style_opacity'] = (block) => `opacity: ${block.getFieldValue('OPACITY') / 100};\n`;
  javascriptGenerator.forBlock['style_text_align'] = (block) => `text-align: ${block.getFieldValue('ALIGN')};\n`;
  javascriptGenerator.forBlock['style_border_radius'] = (block) => `border-radius: ${block.getFieldValue('RADIUS')}px;\n`;

  javascriptGenerator.forBlock['style_padding'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const v1 = block.getFieldValue('VAL1');
    const v2 = block.getFieldValue('VAL2');
    if (side === 'vertical') return `padding-top: ${withUnit(v1)}; padding-bottom: ${withUnit(v2)};\n`;
    if (side === 'horizontal') return `padding-left: ${withUnit(v1)}; padding-right: ${withUnit(v2)};\n`;
    return `${side}: ${withUnit(v1)};\n`;
  };

  javascriptGenerator.forBlock['style_margin'] = (block) => {
    const side = block.getFieldValue('SIDE');
    const v1 = block.getFieldValue('VAL1');
    const v2 = block.getFieldValue('VAL2');
    if (side === 'vertical') return `margin-top: ${withUnit(v1)}; margin-bottom: ${withUnit(v2)};\n`;
    if (side === 'horizontal') return `margin-left: ${withUnit(v1)}; margin-right: ${withUnit(v2)};\n`;
    return `${side}: ${withUnit(v1)};\n`;
  };

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

  javascriptGenerator.forBlock['style_white_space'] = (block) => {
    return `white-space: ${block.getFieldValue('WS')};\n`;
  }

  javascriptGenerator.forBlock['style_shadow'] = function(block) {
    // getFieldValue 뒤의 인자가 위 init()의 대문자 이름들과 '완벽히' 같아야 합니다.
    const type  = block.getFieldValue('TYPE') || 'box-shadow';
    const x     = block.getFieldValue('X')    || '0';
    const y     = block.getFieldValue('Y')    || '0';
    const blur  = block.getFieldValue('BLUR') || '0';
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
    case 'translateX': transformValue = `translateX(${dist}px)`; break;
    case 'translateX_minus': transformValue = `translateX(-${dist}px)`; break;
    case 'translateY': transformValue = `translateY(${dist}px)`; break;
    case 'translateY_minus': transformValue = `translateY(-${dist}px)`; break;
    // ✨ 여기에 '크기 늘리기' 로직 하나만 추가하세요
    case 'scale': transformValue = `scale(${dist})`; break; 
  }

  return `transform: ${transformValue} translateZ(0) !important;\n`;
};
</script>