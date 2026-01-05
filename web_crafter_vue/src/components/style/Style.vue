<script>
import * as Blockly from 'blockly'
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '스타일',
  color: '#ab47bc',
  icon: '🎨'
}

export const toolbox = `
<xml>
  <block type="style_tag">
     <value name="SELECTOR">
        <shadow type="text">
           <field name="TEXT">.my-class</field>
        </shadow>
     </value>
  </block>
  <label text="──────────────────────"></label>

  <block type="style_custom_font"></block>
  <block type="style_font_family"></block>
  <block type="style_font_text_style"></block>

  <label text="──────────────────────"></label>

  <block type="style_size"></block>
  <block type="style_visibility"></block>
  <block type="style_opacity"></block>
  <block type="style_shadow_smart"></block>
  <block type="style_text_shadow"></block>
 
  <label text="──────────────────────"></label>

  <block type="style_padding"></block>
  <block type="style_margin"></block>

  <label text="──────────────────────"></label>

  <block type="style_text_align"></block>
  <block type="style_border_radius"></block>
</xml>
`

export const defineBlocks = () => {

  javascriptGenerator.forBlock['toolbox_separator'] = () => '';

  // 1. 스타일 적용 대상
  if (!Blockly.Blocks['style_tag']) {
    Blockly.Blocks['style_tag'] = {
      init() {
        this.appendDummyInput()
            .appendField('🎨 스타일 적용 대상')
            .appendField(new Blockly.FieldTextInput('.classname'), 'SELECTOR');
        this.appendStatementInput('BODY').setCheck(null).appendField('내용');
        this.setColour('#ab47bc');
      }
    };
  }

  javascriptGenerator.forBlock['style_tag'] = (block) => {
    let selector = (block.getFieldValue('SELECTOR') || '').trim();
    if (selector && !selector.startsWith('.') && !selector.startsWith('#')) {
      selector = '.' + selector;
    }
    let rawBody = javascriptGenerator.statementToCode(block, 'BODY').trim();
    let fontFaceDeclarations = '';
    let styleProperties = '';
    const lines = rawBody.split('\n');
    const fontFaceRegex = /\/\*\s*@FONT-FACE:\s*([^|]+)\|([^\*]+)\s*\*\//;

    lines.forEach(line => {
      const match = line.match(fontFaceRegex);
      if (match) {
        const name = match[1].trim();
        const url = match[2].trim();
        fontFaceDeclarations += `@font-face {\n  font-family: '${name}';\n  src: url('${url}');\n  font-display: swap;\n}\n\n`;
      } else {
        const trimmedLine = line.trim();
        if (trimmedLine) styleProperties += `  ${trimmedLine}\n`;
      }
    });
    return `<style>\n${fontFaceDeclarations}${selector} {\n${styleProperties}}\n</style>\n`;
  };

  // 2. 커스텀 폰트
  if (!Blockly.Blocks['style_custom_font']) {
    Blockly.Blocks['style_custom_font'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔤 폰트 이름")
            .appendField(new Blockly.FieldTextInput("폰트이름"), "NAME")
        this.appendDummyInput()
            .appendField("주소")
            .appendField(new Blockly.FieldTextInput("https://..."), "URL");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_custom_font'] = (block) => {
    const name = block.getFieldValue('NAME');
    const url = block.getFieldValue('URL');
    return `/* @FONT-FACE: ${name}|${url} */\nfont-family: '${name}', sans-serif !important;`;
  };

  // 3. 기본 폰트
  if (!Blockly.Blocks['style_font_family']) {
    Blockly.Blocks['style_font_family'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔤 기본 글꼴")
            .appendField(new Blockly.FieldDropdown([
              ["맑은 고딕",'Malgun Gothic', "sans-serif"], 
              ["기본 명조", "serif"],
              ["코딩체", "monospace"],
              ["굴림", "Gulim, sans-serif"]
            ]), "FONT");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_font_family'] = (block) => `  font-family: ${block.getFieldValue('FONT')} !important;\n`;

  // 4. 글자 설정
  if (!Blockly.Blocks['style_font_text_style']) {
    Blockly.Blocks['style_font_text_style'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔠 글자 설정")
            .appendField("크기")
            .appendField(new Blockly.FieldTextInput("20"), "SIZE")
            .appendField("px ,")
            .appendField("두께")
            .appendField(new Blockly.FieldDropdown([
                ["보통", "normal"], 
                ["진하게", "bold"], 
                ["가늘게", "100"]
            ]), "WEIGHT");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_font_text_style'] = (block) => {
    const size = block.getFieldValue('SIZE') || '16';
    const weight = block.getFieldValue('WEIGHT');
    return `  font-size: ${size}px !important;\n  font-weight: ${weight} !important;\n`;
  };

  // 5. 크기
  if (!Blockly.Blocks['style_size']) {
    Blockly.Blocks['style_size'] = {
      init() {
        this.appendDummyInput()
            .appendField("📐 크기 너비")
            .appendField(new Blockly.FieldTextInput("100%"), "WIDTH")
            .appendField("높이")
            .appendField(new Blockly.FieldTextInput("auto"), "HEIGHT");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_size'] = (block) => {
    const w = block.getFieldValue('WIDTH');
    const h = block.getFieldValue('HEIGHT');
    let code = '';
    if (w && w !== 'auto') code += `  width: ${/^\d+$/.test(w) ? w + 'px' : w} !important;\n`;
    if (h && h !== 'auto') code += `  height: ${/^\d+$/.test(h) ? h + 'px' : h} !important;\n`;
    return code;
  };

  // 6. 보이기 설정
  if (!Blockly.Blocks['style_visibility']) {
    Blockly.Blocks['style_visibility'] = {
      init: function() {
        this.appendDummyInput()
            .appendField("👁️ 보이기 설정")
            .appendField(new Blockly.FieldDropdown([
                ["항상 보이기", "initial"],
                ["숨기기", "none"]
            ]), "VISUAL");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_visibility'] = (block) => `  display: ${block.getFieldValue('VISUAL')} !important;\n`;

  // 7. 투명도
  if (!Blockly.Blocks['style_opacity']) {
    Blockly.Blocks['style_opacity'] = {
      init: function() {
        this.appendDummyInput()
            .appendField("🏁 투명도")
            .appendField(new Blockly.FieldNumber(100, 0, 100), "OPACITY")
            .appendField("%");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      }
    };
  }

  javascriptGenerator.forBlock['style_opacity'] = function(block) {
  const opacity = block.getFieldValue('OPACITY') / 100;
  // 앞뒤 공백을 제거하여 반환합니다.
  return `opacity: ${opacity} !important;\n`; 
};

// 8. 스마트 그림자 (수정완료: 동기화 로직 추가)
  if (!Blockly.Blocks['style_shadow_smart']) {
    Blockly.Blocks['style_shadow_smart'] = {
      init: function() {
        this.appendDummyInput("MAIN")
            .appendField("🌑 그림자")
            .appendField(new Blockly.FieldDropdown([
              ["없음", "none"],
              ["은은하게", "0px 4px 10px 0px rgba(0,0,0,0.2)"],
              ["보통", "0px 8px 20px 0px rgba(0,0,0,0.3)"],
              ["강하게", "0px 12px 30px 0px rgba(0,0,0,0.4)"],
              ["상단 강조", "0px -2px 10px 0px rgba(0,0,0,0.1)"],
              ["💡 직접 설정", "custom"]
            ], function(newValue) {
              // 중요: 값이 바뀔 때 즉시 형태 업데이트
              const block = this.getSourceBlock();
              setTimeout(() => { block.updateShape_(newValue); }, 10);
              return newValue;
            }), "TYPE");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      },
      mutationToDom: function() {
        var container = document.createElement('mutation');
        container.setAttribute('type', this.getFieldValue('TYPE') || 'none');
        return container;
      },
      domToMutation: function(xmlElement) {
        this.updateShape_(xmlElement.getAttribute('type'));
      },
      updateShape_: function(type) {
        var isCustom = (type === 'custom');
        var inputExists = this.getInput('FIELDS');
        if (isCustom && !inputExists) {
          this.appendDummyInput('FIELDS')
              .appendField("↕").appendField(new Blockly.FieldNumber(4), "Y")
              .appendField("↔").appendField(new Blockly.FieldNumber(0), "X")
              .appendField("흐림").appendField(new Blockly.FieldNumber(10, 0), "BLUR")
              .appendField("두께").appendField(new Blockly.FieldNumber(0), "SPREAD")
              .appendField("색").appendField(new Blockly.FieldColor("#000000"), "COLOR");
        } else if (!isCustom && inputExists) {
          // 중요: 직접 설정이 아니면 입력창 세트를 삭제
          this.removeInput('FIELDS');
        }
      }
    };
  }

  javascriptGenerator.forBlock['style_shadow_smart'] = (block) => {
    const type = block.getFieldValue('TYPE');
    let shadowCode = '';

    if (type === 'none') {
      shadowCode = 'none';
    } else if (type === 'custom') {
      const x = block.getFieldValue('X') || 0;
      const y = block.getFieldValue('Y') || 4;
      const blur = block.getFieldValue('BLUR') || 10;
      const spread = block.getFieldValue('SPREAD') || 0;
      const color = block.getFieldValue('COLOR') || '#000000';
      shadowCode = `${x}px ${y}px ${blur}px ${spread}px ${color}`;
    } else {
      // 상단 강조 등 프리셋 처리
      shadowCode = type;
  }

  // 최종적으로 box-shadow 속성 한 줄을 완성해서 반환
  return `box-shadow: ${shadowCode} !important;\n`;
};

// --- 글자 그림자(text-shadow) 블록 ---
  if (!Blockly.Blocks['style_text_shadow']) {
    Blockly.Blocks['style_text_shadow'] = {
      init: function() {
        this.appendDummyInput("MAIN")
          .appendField("🔡 글자 그림자")
          .appendField(new Blockly.FieldDropdown([
            ["없음", "none"],
            ["은은하게", "1px 1px 2px rgba(0,0,0,0.3)"],
            ["강하게", "2px 2px 4px rgba(0,0,0,0.5)"],
            ["💡 직접 설정", "custom"]
          ], function(newValue) {
            // 핵심: 값이 실제로 바뀐 후 형태를 업데이트하도록 처리
            const block = this.getSourceBlock();
            setTimeout(() => {
              block.updateShape_(newValue);
            }, 10);
            return newValue;
          }), "TYPE");
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour('#ab47bc');
      },
      mutationToDom: function() {
        var container = document.createElement('mutation');
        container.setAttribute('type', this.getFieldValue('TYPE'));
        return container;
      },
      domToMutation: function(xmlElement) {
        this.updateShape_(xmlElement.getAttribute('type'));
      },
      updateShape_: function(type) {
        var isCustom = (type === 'custom');
        var inputExists = this.getInput('FIELDS');
        if (isCustom && !inputExists) {
          this.appendDummyInput('FIELDS')
              .appendField("↕").appendField(new Blockly.FieldNumber(2), "Y")
              .appendField("↔").appendField(new Blockly.FieldNumber(2), "X")
              .appendField("흐림").appendField(new Blockly.FieldNumber(4, 0), "BLUR")
              .appendField("색").appendField(new Blockly.FieldColor("#000000"), "COLOR");
        } else if (!isCustom && inputExists) {
          this.removeInput('FIELDS');
        }
      }
    };
  }

// 글자 그림자 제너레이터
javascriptGenerator.forBlock['style_text_shadow'] = (block) => {
  const type = block.getFieldValue('TYPE');
  let res = '';

  if (type === 'custom') {
    const x = block.getFieldValue('X') || 0;
    const y = block.getFieldValue('Y') || 0;
    const blur = block.getFieldValue('BLUR') || 0;
    const color = block.getFieldValue('COLOR') || '#000000';
    res = `${x}px ${y}px ${blur}px ${color}`;
  } else if (type !== 'none') {
    res = type; // 프리셋 값 사용
  }

  // 값이 있을 때만 속성 생성, 없을 때는 빈 문자열 반환
  return res ? `text-shadow: ${res} !important;\n` : '';
};

// 일반 그림자 제너레이터
javascriptGenerator.forBlock['style_shadow_smart'] = (block) => {
  const type = block.getFieldValue('TYPE');
  let res = '';

  if (type === 'custom') {
    const x = block.getFieldValue('X') || 0;
    const y = block.getFieldValue('Y') || 0;
    const blur = block.getFieldValue('BLUR') || 0;
    const spread = block.getFieldValue('SPREAD') || 0;
    const color = block.getFieldValue('COLOR') || '#000000';
    res = `${x}px ${y}px ${blur}px ${spread}px ${color}`;
  } else if (type !== 'none') {
    res = type;
  }

  return res ? `box-shadow: ${res} !important;\n` : '';
};

  // 9. 패딩
  if (!Blockly.Blocks['style_padding']) {
    Blockly.Blocks['style_padding'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 안쪽 여백(패딩)")
            .appendField(new Blockly.FieldDropdown([
                ["전체", "padding"], ["위", "padding-top"], 
                ["아래", "padding-bottom"], ["왼쪽", "padding-left"], ["오른쪽", "padding-right"]
            ]), "SIDE")
            .appendField(new Blockly.FieldTextInput("10"), "VAL")
            .appendField("px");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_padding'] = (block) => `  ${block.getFieldValue('SIDE')}: ${block.getFieldValue('VAL')}px !important;\n`;

  // 10. 마진
  if (!Blockly.Blocks['style_margin']) {
    Blockly.Blocks['style_margin'] = {
      init() {
        this.appendDummyInput()
            .appendField("📦 바깥 여백(마진)")
            .appendField(new Blockly.FieldDropdown([
                ["전체", "margin"], ["위", "margin-top"], 
                ["아래", "margin-bottom"], ["왼쪽", "margin-left"], ["오른쪽", "margin-right"]
            ]), "SIDE")
            .appendField(new Blockly.FieldTextInput("10"), "VAL")
            .appendField("px");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_margin'] = (block) => `  ${block.getFieldValue('SIDE')}: ${block.getFieldValue('VAL')}px !important;\n`;

  // 11. 정렬
  if (!Blockly.Blocks['style_text_align']) {
    Blockly.Blocks['style_text_align'] = {
      init() {
        this.appendDummyInput()
            .appendField("📝 텍스트 정렬")
            .appendField(new Blockly.FieldDropdown([
                ["왼쪽", "left"], ["가운데", "center"], ["오른쪽", "right"]
            ]), "ALIGN");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_text_align'] = (block) => `  text-align: ${block.getFieldValue('ALIGN')} !important;\n`;

  // 12. 둥근 모서리
  if (!Blockly.Blocks['style_border_radius']) {
    Blockly.Blocks['style_border_radius'] = {
      init() {
        this.appendDummyInput()
            .appendField("🔘 둥근 모서리")
            .appendField(new Blockly.FieldTextInput("10"), "RADIUS")
            .appendField("px");
        this.setPreviousStatement(true); this.setNextStatement(true); this.setColour('#ab47bc');
      }
    };
  }
  javascriptGenerator.forBlock['style_border_radius'] = (block) => `  border-radius: ${block.getFieldValue('RADIUS')}px !important;\n`;
}
</script>