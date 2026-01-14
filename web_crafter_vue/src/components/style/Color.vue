<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';
import { FieldColour } from '@blockly/field-colour';
// [추가] 설치한 컬러 피커 라이브러리 임포트
import Picker from 'vanilla-picker';

export const category = {
  label: '폰트＆색상',
  color: '#e91e63',
  icon: '🎨'
}

export const toolbox = `
<xml>
<block type="style_font_size"></block>
    <block type="style_font_weight"></block>
    <block type="style_font_family"></block>
    <block type="style_text_transform"></block>

        <block type="style_text_stroke"></block>

    <block type="style_line_height"></block>
    <block type="style_letter_spacing"></block>
    <block type="style_white_space"></block>
    <block type="style_text_align"></block>
    <block type="style_opacity"></block>
    <block type="style_text_decoration"></block>
    <label text="──────────────────────"></label>
    <block type="style_text_color"></block>
    <block type="style_bg_color"></block>
    <block type="style_border_combined"></block>
    <label text="──────────────────────"></label>
    <block type="style_custom_font"></block>
    <block type="font_url"></block>
    <block type="font_weight"></block>
    <block type="font_display"></block>
</xml>
`;

export const defineBlocks = () => {

  /**
   * 🎨 [커스텀] 바닐라 피커를 이용한 모달형 색상 필드
   * - 블록 바로 위에 뜹니다.
   * - 배경을 클릭하면 닫힙니다.
   */
  class FieldModalColor extends FieldColour {
    constructor(value) {
      super(value);
    }

    // 블록 클릭 시 실행되는 함수 (오버라이딩)
    showEditor_() {
      // 1. 현재 블록의 위치(좌표)를 가져옵니다.
      const blockSvg = this.sourceBlock_.getSvgRoot();
      const rect = blockSvg.getBoundingClientRect();

      // 2. 피커를 붙일 '앵커(Anchor)' 투명 div를 생성합니다.
      // (블록 바로 위에 위치하도록 설정)
      const anchor = document.createElement('div');
      anchor.style.cssText = `
        position: fixed;
        left: ${rect.left + (rect.width / 2)}px; 
        top: ${rect.top - 10}px;
        z-index: 9999;
      `;
      document.body.appendChild(anchor);

      // 3. 바닐라 피커 생성
      const picker = new Picker({
        parent: anchor,
        popup: 'top',       // 🌟 핵심: 위쪽으로 펼쳐짐
        alpha: false,       // 투명도 사용 안 함 (필요하면 true)
        color: this.getValue(),
        onDone: (color) => {
          // [선택 완료] 버튼 누르면 값 적용
          this.setValue(color.hex.substring(0, 7)); // #RRGGBB 형태
        },
        onClose: () => {
          // [닫기] 앵커 삭제 (청소)
          if (anchor.parentNode) {
            document.body.removeChild(anchor);
          }
        }
      });

      // 4. 피커 열기 (딜레이 없이 즉시)
      picker.openHandler();
      
      // (선택 사항) 실시간 변경을 원하면 onChange 사용
      // picker.onChange = (color) => { this.setValue(color.hex.substring(0, 7)); };
    }
  }

  // 1. 배경 색상 블록
  if (!Blockly.Blocks['style_bg_color']) {
    Blockly.Blocks['style_bg_color'] = {
      init() {
        this.appendDummyInput()
            .appendField("🎨 배경색")
            // [변경] 커스텀 필드 적용
            .appendField(new FieldModalColor('#ffffff'), 'COLOR');
        this.setPreviousStatement(true, "STYLE");
        this.setNextStatement(true, "STYLE");
        this.setColour('#e91e63');
      }
    };
  }
  javascriptGenerator.forBlock['style_bg_color'] = (block) => {
    const color = block.getFieldValue('COLOR');
    return `  background-color: ${color} !important;\n`;
  };

  // 2. 글자 색상 블록
  if (!Blockly.Blocks['style_text_color']) {
    Blockly.Blocks['style_text_color'] = {
      init() {
        this.appendDummyInput()
            .appendField("🎨 글자색")
            // [변경] 커스텀 필드 적용
            .appendField(new FieldModalColor('#000000'), 'COLOR');
        this.setPreviousStatement(true, "STYLE");
        this.setNextStatement(true, "STYLE");
        this.setColour('#e91e63');
      }
    };
  }
  javascriptGenerator.forBlock['style_text_color'] = (block) => {
    const color = block.getFieldValue('COLOR');
    return `  color: ${color} !important;\n`;
  };


// 블록 정의 (defineBlocks 함수 내부에 추가)
if (!Blockly.Blocks['style_border_combined']) {
  Blockly.Blocks['style_border_combined'] = {
    init() {
      // 첫 번째 줄: 제목과 두께
      this.appendDummyInput()
          .appendField("🔲 테두리")
          .appendField(new Blockly.FieldTextInput("1"), "WIDTH")
          .appendField("px");
      
      // 두 번째 줄: 종류와 색상 (줄바꿈)
      this.appendDummyInput()
          .appendField("종류")
          .appendField(new Blockly.FieldDropdown([
            ["실선", "solid"], 
            ["점선(짧은)", "dotted"], 
            ["점선(긴)", "dashed"],
            ["이중선", "double"]
          ]), "STYLE")
          .appendField("색")
          .appendField(new FieldModalColor('#cccccc'), 'COLOR');
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#e91e63');
    }
  };
}

Blockly.Blocks['style_text_stroke'] = {
  init() {
    this.appendDummyInput()
        .appendField('🖋️ 글자 외곽선')
        .appendField(new Blockly.FieldTextInput('1'), 'WIDTH')
        .appendField('px')
        // 클래스 스코프 에러 방지를 위해 안전하게 참조
        .appendField(new (typeof FieldModalColor !== 'undefined' ? FieldModalColor : Blockly.FieldColour)('#000000'), 'COLOR');

    // ✨ [핵심 수정] 조립이 가능하도록 위아래 홈을 만듭니다.
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    
    this.setColour('#ff3366'); // 폰트 카테고리 색상
  }
};

}

  // 2. 조립형 폰트 설정 블록들
  if (!Blockly.Blocks['style_custom_font']) {
    Blockly.Blocks['style_custom_font'] = {
      init() {
        this.appendDummyInput().appendField("🔤 폰트 이름").appendField(new Blockly.FieldTextInput("PyeojinGothic"), "NAME");
        this.appendStatementInput("PROPERTIES").setCheck(null).appendField("설정 내용");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#e91e63');
      }
    };
  }
  if (!Blockly.Blocks['font_url']) {
    Blockly.Blocks['font_url'] = {
      init() {
        this.appendDummyInput().appendField("🔗 폰트 주소").appendField(new Blockly.FieldTextInput("https://..."), "VAL");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#e91e63');
      }
    };
  }
  if (!Blockly.Blocks['font_weight']) {
    Blockly.Blocks['font_weight'] = {
      init() {
        this.appendDummyInput().appendField("⚖️ 두께").appendField(new Blockly.FieldDropdown([["300(Light)","300"],["400(Regular)","400"],["700(Bold)","700"]]), "VAL");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#e91e63');
      }
    };
  }
  if (!Blockly.Blocks['font_display']) {
    Blockly.Blocks['font_display'] = {
      init() {
        this.appendDummyInput().appendField("📺 출력").appendField(new Blockly.FieldDropdown([["교체(swap)","swap"],["숨김(block)","block"]]), "VAL");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      this.setColour('#e91e63');
      }
    };
  }

    Blockly.Blocks['style_font_size'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 크기').appendField(new Blockly.FieldTextInput('20'), 'SIZE').appendField('px');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

  Blockly.Blocks['style_font_weight'] = {
    init() {
      this.appendDummyInput().appendField('🔠 글자 두께').appendField(new Blockly.FieldDropdown([['100 Thin', '100'], ['300 Light', '300'], ['400 Regular', '400'], ['500 Medium', '500'], ['700 Bold', '700'], ['900 Black', '900']]), 'WEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

  Blockly.Blocks['style_line_height'] = {
    init() {
      this.appendDummyInput().appendField('📏 줄 간격').appendField(new Blockly.FieldTextInput('1.6'), 'HEIGHT');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

  Blockly.Blocks['style_letter_spacing'] = {
    init() {
      this.appendDummyInput().appendField('↔️ 자간').appendField(new Blockly.FieldTextInput('0.05'), 'SPACE').appendField('em');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

    Blockly.Blocks['style_opacity'] = {
    init: function () {
      this.appendDummyInput().appendField('🏁 투명도').appendField(new Blockly.FieldNumber(100, 0, 100), 'OPACITY').appendField('%');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

  Blockly.Blocks['style_text_align'] = {
    init() {
      this.appendDummyInput().appendField('📝 정렬').appendField(new Blockly.FieldDropdown([['왼쪽', 'left'], ['가운데', 'center'], ['오른쪽', 'right']]), 'ALIGN');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    },
  };

    Blockly.Blocks['style_white_space'] = {
    init() {
      this.appendDummyInput().appendField('📖 줄 바꿈 설정').appendField(new Blockly.FieldDropdown([['자동 (normal)', 'normal'],['줄바꿈 안함 (nowrap)', 'nowrap'],['엔터 유지+옆으로 (pre)', 'pre'],['엔터 유지+자동 줄바꿈 (pre-wrap)', 'pre-wrap'], ['엔터만 유지 (pre-line)', 'pre-line'],['부모 설정 따름 (inherit)', 'inherit']]), 'WS');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#e91e63');
    }
  };

  Blockly.Blocks['style_text_decoration'] = {
  init() {
    this.appendDummyInput().appendField('📝 글자 장식').appendField(new Blockly.FieldDropdown([['안함', 'none'],['밑줄', 'underline'],['취소선', 'line-through'],['윗줄', 'overline']]), 'DECO');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ff3366');
  }
};

Blockly.Blocks['style_font_family'] = {
  init() {
    const dropdown = new Blockly.FieldDropdown([
      ['맑은 고딕', '"Malgun Gothic", sans-serif'],
      ['굴림', 'Gulim, sans-serif'],
      ['굴림체', 'GulimChe, monospace'],
      ['돋움', 'Dotum, sans-serif'],
      ['돋움체', 'DotumChe, monospace'],
      ['바탕', 'Batang, serif'],
      ['바탕체', 'BatangChe, monospace'],
      ['궁서', 'Gungsuh, serif'],
      ['궁서체', 'GungsuhChe, monospace'],
      ['코딩체(기본)', 'monospace'],
      ['직접 입력', 'custom']
    ], (newValue) => {
      // 드롭다운 값이 바뀔 때마다 실행됨
      this.updateShape_(newValue === 'custom');
      return newValue;
    });
    this.appendDummyInput('MAIN')
        .appendField('🔠 글꼴')
        .appendField(dropdown, 'PRESET');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ff3366');
  },
  // 블록 로드 시 상태 복원
  domToMutation(xmlElement) {
    const isCustom = (xmlElement.getAttribute('is_custom') === 'true');
    this.updateShape_(isCustom);
  },
  // 블록 상태 저장
  mutationToDom() {
    const container = Blockly.utils.xml.createElement('mutation');
    const isCustom = !!this.getField('CUSTOM');
    container.setAttribute('is_custom', isCustom);
    return container;
  },
  // 입력창을 보이기/숨기기 하는 핵심 함수
  updateShape_(isCustom) {
    const input = this.getInput('MAIN');
    if (isCustom) {
      if (!this.getField('CUSTOM')) {
        input.appendField(new Blockly.FieldTextInput('폰트이름 입력'), 'CUSTOM');
      }
    } else {
      if (this.getField('CUSTOM')) {
        input.removeField('CUSTOM');
      }
    }
  }
};

  Blockly.Blocks['style_text_transform'] = {
  init() {
    this.appendDummyInput()
        .appendField('🔠 대소문자 변환')
        .appendField(new Blockly.FieldDropdown([
          ['원본 유지', 'none'],
          ['모두 대문자 (ABC)', 'uppercase'],
          ['모두 소문자 (abc)', 'lowercase'],
          ['첫글자만 대문자 (Abc)', 'capitalize']
        ]), 'TRANS');
    this.setPreviousStatement(true, 'STYLE');
    this.setNextStatement(true, 'STYLE');
    this.setColour('#ff3366');
  }
};

  // --- 제너레이터 완성본 ---

javascriptGenerator.forBlock['style_tag'] = (block) => {
  let selector = (block.getFieldValue('SELECTOR') || 'container').trim();
  if (selector && !selector.startsWith('.') && !selector.startsWith('#')) selector = '.' + selector;

  const rawBody = javascriptGenerator.statementToCode(block, 'BODY');
  let fontFaceDeclarations = '';
  
  const fontFaceRegex = /@FONT-FACE:\s*([^|]+)\|([^|]+)\|([^|]+)\|([^|\s\*]+)/g;
  let match;

  while ((match = fontFaceRegex.exec(rawBody)) !== null) {
    const name = match[1].trim();
    let urlInput = match[2].trim(); // 사용자가 입력한 값 전체
    const weight = match[3].trim();
    const display = match[4].trim();

    // 🌟 [초보자 배려 로직] 
    // 사용자가 url('...') format('...') 통째로 넣었을 경우 주소만 쏙 뽑아냅니다.
    let pureUrl = urlInput;
    if (urlInput.includes('url(')) {
      // url(' 와 ') 사이의 내용만 추출
      const matchUrl = urlInput.match(/url\(['"]?([^'"]+)['"]?\)/);
      if (matchUrl) pureUrl = matchUrl[1];
    } else {
      // url()이 없더라도 format() 등이 붙어있다면 공백이나 따옴표 기준으로 앞부분 주소만 취함
      pureUrl = urlInput.split("'")[0].split('"')[0].split(' ')[0].trim();
    }

    fontFaceDeclarations += `@font-face {\n  font-family: '${name}';\n  src: url('${pureUrl}') format('woff');\n  font-weight: ${weight};\n  font-display: ${display};\n}\n\n`;
  }

  const styleProperties = rawBody.replace(/\/\*[\s\S]*?\*\//g, '').trim();

  return `<style>\n${fontFaceDeclarations}${selector} {\n  ${styleProperties}\n}\n</style>\n`;
};

javascriptGenerator.forBlock['style_custom_font'] = (block) => {
  const name = block.getFieldValue('NAME');
  const properties = javascriptGenerator.statementToCode(block, 'PROPERTIES');
  const urlMatch = properties.match(/URL:([^\n]+)/);
  const weightMatch = properties.match(/WEIGHT:([^\n]+)/);
  const displayMatch = properties.match(/DISPLAY:([^\n]+)/);
  const url = urlMatch ? urlMatch[1].trim() : '';
  const weight = weightMatch ? weightMatch[1].trim() : '400';
  const display = displayMatch ? displayMatch[1].trim() : 'swap';
  return `/* @FONT-FACE: ${name}|${url}|${weight}|${display} */\nfont-family: '${name}', sans-serif !important;\nfont-weight: ${weight} !important;\n`;
};

javascriptGenerator.forBlock['font_url'] = (block) => `URL:${block.getFieldValue('VAL')}\n`;
javascriptGenerator.forBlock['font_weight'] = (block) => `WEIGHT:${block.getFieldValue('VAL')}\n`;
javascriptGenerator.forBlock['font_display'] = (block) => `DISPLAY:${block.getFieldValue('VAL')}\n`;
javascriptGenerator.forBlock['style_bg_color'] = (block) => `background-color: ${block.getFieldValue('COLOR')} !important;\n`;
javascriptGenerator.forBlock['style_text_color'] = (block) => `color: ${block.getFieldValue('COLOR')} !important;\n`;
javascriptGenerator.forBlock['style_border_combined'] = (block) => {
  return `border: ${block.getFieldValue('WIDTH')}px ${block.getFieldValue('STYLE')} ${block.getFieldValue('COLOR')} !important;\n`;
};

  javascriptGenerator.forBlock['style_font_size'] = (block) => `font-size: ${block.getFieldValue('SIZE')}px !important;\n`;
  javascriptGenerator.forBlock['style_font_weight'] = (block) => `font-weight: ${block.getFieldValue('WEIGHT')} !important;\n`;
  javascriptGenerator.forBlock['style_line_height'] = (block) => `line-height: ${block.getFieldValue('HEIGHT')};\n`;
  javascriptGenerator.forBlock['style_letter_spacing'] = (block) => `letter-spacing: ${block.getFieldValue('SPACE')}em;\n`;
  javascriptGenerator.forBlock['style_opacity'] = (block) => `opacity: ${block.getFieldValue('OPACITY') / 100};\n`;
  javascriptGenerator.forBlock['style_text_align'] = (block) => `text-align: ${block.getFieldValue('ALIGN')};\n`;

  javascriptGenerator.forBlock['style_white_space'] = (block) => {
    return `white-space: ${block.getFieldValue('WS')};\n`;
  }

  javascriptGenerator.forBlock['style_text_decoration'] = (block) => {
  const deco = block.getFieldValue('DECO');
  return `text-decoration: ${deco};\n`;
};

javascriptGenerator.forBlock['style_font_family'] = (block) => {
  const preset = block.getFieldValue('PRESET');
  const customField = block.getField('CUSTOM');
  
  // CUSTOM 필드가 존재하면 그 안의 값을 쓰고, 없으면 드롭다운 값을 씀
  const font = (customField) ? customField.getValue().trim() : preset;
  
  return `font-family: ${font};\n`;
};

javascriptGenerator.forBlock['style_text_transform'] = (block) => {
  return `text-transform: ${block.getFieldValue('TRANS')};\n`;
};

javascriptGenerator.forBlock['style_text_stroke'] = (block) => {
  const width = block.getFieldValue('WIDTH') || '0';
  const color = block.getFieldValue('COLOR') || '#000000';
  return `-webkit-text-stroke: ${width}px ${color};\n`;
};

</script>

<style>
/* 피커가 다른 요소에 가리지 않게 z-index 보정 */
.picker_wrapper {
  z-index: 10000 !important;
}


</style>