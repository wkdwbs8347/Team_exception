<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '반응형',
  color: '#FF9800',
  icon: '📱'
};

export const toolbox = `
<xml>
    <block type="style_responsive_easy"></block>
    <block type="style_responsive_mobile_font_size"></block>
    <block type="style_responsive_stack"></block>
    <block type="style_responsive_hide_mobile"></block>
    <block type="style_responsive_mobile_spacing"></block>
    <block type="style_responsive_mobile_align"></block>
</xml>
`;

export const defineBlocks = () => {
  if (Blockly.Blocks['style_responsive_easy']) return;

  const applyOrangeStyle = (block) => {
    block.setColour('#FF9800');
    block.style = {
      "colourPrimary": "#FF9800",
      "colourSecondary": "#FF9800",
      "colourTertiary": "#FF9800"
    };
  };

  // ✅ [수정 핵심] null 방지 및 클래스 점(.) 자동 추가 헬퍼 함수
const getTarget = (block) => {
  const rootBlock = block.getSurroundParent();
  let targetName = rootBlock ? rootBlock.getFieldValue('TARGET') : 'container';
  if (!targetName || targetName === 'null') targetName = 'container';
  return `.${targetName}`;
};

  /* 1. 자동 너비 설정 */
  Blockly.Blocks['style_responsive_easy'] = {
    init() {
      this.appendDummyInput()
          .appendField("✨ 모바일 너비 보정") // 이름도 직관적으로 변경
          .appendField("모바일:")
          .appendField(new Blockly.FieldDropdown([
            ["꽉 차게", "100"], 
            ["여백 있게", "90"], 
            ["절반만", "50"]
          ]), "MOB_W");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

  javascriptGenerator.forBlock['style_responsive_easy'] = (block) => {
    const target = getTarget(block);
    const mobW = block.getFieldValue('MOB_W');

    // ✅ PC 수치는 뱉지 않습니다. 일반 블록의 설정을 그대로 따르다가(상속)
    // 상단 아이콘이 '모바일'일 때만 이 값이 !important로 강제 적용됩니다.
    return `
      } 
      .is-mobile-mode ${target} { 
        width: ${mobW}% !important; 
        max-width: none !important; 
        margin-left: auto !important;
        margin-right: auto !important;
      }
      ${target} {
    `;
  };

  /* 2. 모바일 전용 글자 크기 보정 */
  Blockly.Blocks['style_responsive_mobile_font_size'] = {
    init() {
      this.appendDummyInput()
          .appendField("📱 모바일 글자 크기만 보정")
          .appendField(new Blockly.FieldTextInput("20"), "MOB_FS")
          .appendField("px");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

javascriptGenerator.forBlock['style_responsive_mobile_font_size'] = (block) => {
  const target = getTarget(block);
  const mobFS = block.getFieldValue('MOB_FS');
  
  return `
    }
    .is-mobile-mode ${target} { font-size: ${mobFS}px !important; }
    ${target} {
  `;
};

  /* 3. 모바일 세로 전환 */
  Blockly.Blocks['style_responsive_stack'] = {
    init() {
      this.appendDummyInput().appendField("↕️ 모바일에서 세로로 쌓기");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

javascriptGenerator.forBlock['style_responsive_stack'] = (block) => {
  const target = getTarget(block);
  return `
    }
    .is-mobile-mode ${target} { display: flex !important; flex-direction: column !important; }
    ${target} {
  `;
};

  /* 4. 모바일에서 숨기기 */
  Blockly.Blocks['style_responsive_hide_mobile'] = {
    init() {
      this.appendDummyInput().appendField("🚫 모바일에서 숨기기");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

javascriptGenerator.forBlock['style_responsive_hide_mobile'] = (block) => {
  const target = getTarget(block);
  return `
    }
    .is-mobile-mode ${target} { display: none !important; }
    ${target} {
  `;
};

/* 5. 모바일 전용 통합 여백 보정 (Padding & Margin) */
  Blockly.Blocks['style_responsive_mobile_spacing'] = {
    init() {
      this.appendDummyInput()
          .appendField("📱 모바일")
          .appendField(new Blockly.FieldDropdown([
            ["안쪽 여백(Padding)", "padding"], 
            ["바깥 여백(Margin)", "margin"]
          ]), "TYPE")
          .appendField(new Blockly.FieldDropdown([
            ["전체", ""], 
            ["위쪽", "-top"], 
            ["아래쪽", "-bottom"], 
            ["왼쪽", "-left"], 
            ["오른쪽", "-right"]
          ]), "DIR")
          .appendField(new Blockly.FieldTextInput("10"), "VALUE")
          .appendField("px");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

  /* 6. 모바일 전용 정렬 보정 */
  Blockly.Blocks['style_responsive_mobile_align'] = {
    init() {
      this.appendDummyInput()
          .appendField("📱 모바일 정렬")
          .appendField(new Blockly.FieldDropdown([
            ["왼쪽", "left"], 
            ["가운데", "center"], 
            ["오른쪽", "right"]
          ]), "ALIGN");
      this.setPreviousStatement(true, "STYLE");
      this.setNextStatement(true, "STYLE");
      applyOrangeStyle(this);
    }
  };

  // 5. 모바일 전용 안쪽 여백 보정 생성기
javascriptGenerator.forBlock['style_responsive_mobile_spacing'] = (block) => {
  const target = getTarget(block);
  const type = block.getFieldValue('TYPE'); // padding 또는 margin
  const dir = block.getFieldValue('DIR');   // 빈값, -top, -bottom 등
  const value = block.getFieldValue('VALUE');
  
  // 최종 CSS 속성명 조립 (예: padding-top, margin-left 등)
  const property = `${type}${dir}`;

  return `
    }
    .is-mobile-mode ${target} { 
      ${property}: ${value}px !important; 
    }
    ${target} {
  `;
};

// 6. 모바일 전용 정렬 보정 생성기
javascriptGenerator.forBlock['style_responsive_mobile_align'] = (block) => {
  const target = getTarget(block);
  const align = block.getFieldValue('ALIGN');
  
  return `
    }
    .is-mobile-mode ${target} { 
      text-align: ${align} !important; 
      display: ${align === 'center' ? 'flex' : 'block'} !important;
      justify-content: ${align === 'center' ? 'center' : (align === 'right' ? 'flex-end' : 'flex-start')} !important;
    }
    ${target} {
  `;
};

};
</script>