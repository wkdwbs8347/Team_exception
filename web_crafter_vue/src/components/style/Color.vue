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

  // --- 제너레이터 완성본 ---

javascriptGenerator.forBlock['style_tag'] = (block) => {
  let selector = (block.getFieldValue('SELECTOR') || '제목').trim();
  if (selector && !selector.startsWith('.') && !selector.startsWith('#')) selector = '.' + selector;
  let rawBody = javascriptGenerator.statementToCode(block, 'BODY').trim();
  let fontFaceDeclarations = '';
  let styleProperties = '';
  const lines = rawBody.split('\n');
  const fontFaceRegex = /\/\*\s*@FONT-FACE:\s*([^|]+)\|([^|]+)\|([^|]+)\|([^\*]+)\s*\*\//;
  lines.forEach(line => {
    const match = line.match(fontFaceRegex);
    if (match) {
      const name = match[1].trim(); const url = match[2].trim(); const display = match[4].trim();
      fontFaceDeclarations += `@font-face {\n  font-family: '${name}';\n  src: url('${url}') format('woff2');\n  font-weight: 100 900; /* 가짜 굵기 활성화 */\n  font-display: ${display};\n}\n\n`;
    } else {
      const trimmedLine = line.trim();
      if (trimmedLine) styleProperties += `  ${trimmedLine}\n`;
    }
  });
  return `<style>\n${fontFaceDeclarations}${selector} {\n${styleProperties}}\n</style>\n`;
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
</script>

<style>
/* 피커가 다른 요소에 가리지 않게 z-index 보정 */
.picker_wrapper {
  z-index: 10000 !important;
}
</style>