  <script>
  import * as Blockly from 'blockly';
  import { javascriptGenerator } from 'blockly/javascript';
  // 필요한 필드 임포트
  import { FieldDropdown } from 'blockly';
  import { FieldTextInput } from 'blockly';
  import { FieldNumber } from 'blockly';

  export const category = {
    label: '배치',
    color: '#ff9800', // 주황색 계열 추천
    icon: '🍱'
  }

  export const toolbox = `
  <xml>
    <label text="── 컨테이너(부모) 설정 ──"></label>
    <block type="style_display_flex">
    </block>
    <block type="style_flex_direction"></block>
    <block type="style_flex_wrap"></block>
    
    <label text="── 정렬 (가로/세로) ──"></label>
    <block type="style_justify_content"></block>
    <block type="style_align_items"></block>
    <block type="style_align_content"></block>

    <label text="── 아이템(자식) 개별 설정 ──"></label>
    <block type="style_flex_item_grow"></block>
    <block type="style_flex_item_shrink"></block>
    <block type="style_flex_item_basis"></block>
    <block type="style_flex_item_align_self"></block>
    <block type="style_flex_item_order"></block>
  </xml>
  `;

  export const defineBlocks = () => {
    const BLOCK_COLOR = '#ff9800'; // Flex 카테고리 색상

    // ==============================================================================
    // [부모] 컨테이너 속성
    // ==============================================================================

    // 0. display: flex 선언 (필수)
    if (!Blockly.Blocks['style_display_flex']) {
      Blockly.Blocks['style_display_flex'] = {
        init() {
          this.appendDummyInput()
              .appendField("🍱 Flex 배치 시작하기 (display: flex)");
          this.appendStatementInput("BODY")
              .setCheck("FLEX_CHILD")
          this.setPreviousStatement(true, "STYLE");
          this.setNextStatement(true, "STYLE");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('이 요소를 Flex 컨테이너로 만듭니다. 자식 요소들이 나란히 배치됩니다.');
        }
      };
    }
    javascriptGenerator.forBlock['style_display_flex'] = function(block) {
      // 1. 'BODY' 영역 안에 연결된 블록들의 코드를 가져옵니다.
      const statements_body = javascriptGenerator.statementToCode(block, 'BODY');
      
      // 2. display: flex 선언 후, 줄바꿈(\n)을 하고 내부 코드를 이어 붙입니다.
      return `  display: flex !important;\n${statements_body}`;
    };


    // 1. flex-direction (주축 방향)
    if (!Blockly.Blocks['style_flex_direction']) {
      Blockly.Blocks['style_flex_direction'] = {
        init() {
          this.appendDummyInput()
              .appendField("➡️ 배치 방향")
              .appendField(new FieldDropdown([
                  ["가로 (row)", "row"],
                  ["가로 반대 (row-reverse)", "row-reverse"],
                  ["세로 (column)", "column"],
                  ["세로 반대 (column-reverse)", "column-reverse"]
              ]), "DIR");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_direction'] = (block) => `  flex-direction: ${block.getFieldValue('DIR')} !important;\n`;

    // 2. flex-wrap (줄바꿈)
    if (!Blockly.Blocks['style_flex_wrap']) {
      Blockly.Blocks['style_flex_wrap'] = {
        init() {
          this.appendDummyInput()
              .appendField("↩️ 줄바꿈 설정")
              .appendField(new FieldDropdown([
                  ["줄바꿈 안함 (nowrap)", "nowrap"],
                  ["줄바꿈 (wrap)", "wrap"],
                  ["반대로 줄바꿈 (wrap-reverse)", "wrap-reverse"]
              ]), "WRAP");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_wrap'] = (block) => `  flex-wrap: ${block.getFieldValue('WRAP')} !important;\n`;

    // 3. justify-content (주축 정렬 - 보통 가로)
    if (!Blockly.Blocks['style_justify_content']) {
      Blockly.Blocks['style_justify_content'] = {
        init() {
          this.appendDummyInput()
              .appendField("↔️ 주축 정렬 (Justify)")
              .appendField(new FieldDropdown([
                  ["시작점 (flex-start)", "flex-start"],
                  ["끝점 (flex-end)", "flex-end"],
                  ["가운데 (center)", "center"],
                  ["양끝 정렬 (space-between)", "space-between"],
                  ["균등 여백 (space-around)", "space-around"],
                  ["동일 간격 (space-evenly)", "space-evenly"]
              ]), "JUSTIFY");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
        }
      };
    }
    javascriptGenerator.forBlock['style_justify_content'] = (block) => `  justify-content: ${block.getFieldValue('JUSTIFY')} !important;\n`;

    // 4. align-items (교차축 정렬 - 보통 세로)
    if (!Blockly.Blocks['style_align_items']) {
      Blockly.Blocks['style_align_items'] = {
        init() {
          this.appendDummyInput()
              .appendField("↕️ 교차축 정렬 (Align Items)")
              .appendField(new FieldDropdown([
                  ["늘리기 (stretch)", "stretch"],
                  ["시작점 (flex-start)", "flex-start"],
                  ["끝점 (flex-end)", "flex-end"],
                  ["가운데 (center)", "center"],
                  ["문자 기준선 (baseline)", "baseline"]
              ]), "ALIGN");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
        }
      };
    }
    javascriptGenerator.forBlock['style_align_items'] = (block) => `  align-items: ${block.getFieldValue('ALIGN')} !important;\n`;

    // 5. align-content (여러 줄일 때 교차축 정렬)
    if (!Blockly.Blocks['style_align_content']) {
      Blockly.Blocks['style_align_content'] = {
        init() {
          this.appendDummyInput()
              .appendField("📚 여러 줄 정렬 (Align Content)")
              .appendField(new FieldDropdown([
                  ["늘리기 (stretch)", "stretch"],
                  ["시작점 (flex-start)", "flex-start"],
                  ["끝점 (flex-end)", "flex-end"],
                  ["가운데 (center)", "center"],
                  ["양끝 정렬 (space-between)", "space-between"],
                  ["균등 여백 (space-around)", "space-around"]
              ]), "CONTENT");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('줄바꿈(wrap) 상태에서 여러 줄의 간격을 조정합니다.');
        }
      };
    }
    javascriptGenerator.forBlock['style_align_content'] = (block) => `  align-content: ${block.getFieldValue('CONTENT')} !important;\n`;


    // ==============================================================================
    // [자식] 아이템 개별 속성 (flex item children)
    // ==============================================================================

    // 1. flex-grow (확대 비율)
    if (!Blockly.Blocks['style_flex_item_grow']) {
      Blockly.Blocks['style_flex_item_grow'] = {
        init() {
          this.appendDummyInput()
              .appendField("📈 확대 비율 (Grow)")
              .appendField(new FieldNumber(0, 0), "GROW"); // 기본 0, 최소 0
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('남은 공간을 얼마나 가져갈지 비율로 설정합니다. (0은 안 커짐)');
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_item_grow'] = (block) => `  flex-grow: ${block.getFieldValue('GROW')} !important;\n`;

    // 2. flex-shrink (축소 비율)
    if (!Blockly.Blocks['style_flex_item_shrink']) {
      Blockly.Blocks['style_flex_item_shrink'] = {
        init() {
          this.appendDummyInput()
              .appendField("📉 축소 비율 (Shrink)")
              .appendField(new FieldNumber(1, 0), "SHRINK"); // 기본 1, 최소 0
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('공간이 부족할 때 얼마나 줄어들지 비율로 설정합니다. (0은 안 줄어듦)');
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_item_shrink'] = (block) => `  flex-shrink: ${block.getFieldValue('SHRINK')} !important;\n`;

    // 3. flex-basis (기본 크기)
    if (!Blockly.Blocks['style_flex_item_basis']) {
      Blockly.Blocks['style_flex_item_basis'] = {
        init() {
          this.appendDummyInput()
              .appendField("📏 기본 크기 (Basis)")
              // px, %, auto 등 다양한 단위를 위해 TextInput 사용
              .appendField(new FieldTextInput('auto'), "BASIS");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('예: 200px, 50%, auto');
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_item_basis'] = (block) => `  flex-basis: ${block.getFieldValue('BASIS')} !important;\n`;

    // 4. align-self (개별 정렬)
    if (!Blockly.Blocks['style_flex_item_align_self']) {
      Blockly.Blocks['style_flex_item_align_self'] = {
        init() {
          this.appendDummyInput()
              .appendField("👤 나만 다르게 정렬 (Self)")
              .appendField(new FieldDropdown([
                  ["자동 (auto)", "auto"],
                  ["시작점 (flex-start)", "flex-start"],
                  ["끝점 (flex-end)", "flex-end"],
                  ["가운데 (center)", "center"],
                  ["늘리기 (stretch)", "stretch"],
                  ["문자 기준선 (baseline)", "baseline"]
              ]), "SELF");
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('부모의 align-items 설정을 무시하고 이 아이템만 따로 정렬합니다.');
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_item_align_self'] = (block) => `  align-self: ${block.getFieldValue('SELF')} !important;\n`;

    // 5. order (순서)
    if (!Blockly.Blocks['style_flex_item_order']) {
      Blockly.Blocks['style_flex_item_order'] = {
        init() {
          this.appendDummyInput()
              .appendField("🔢 배치 순서 (Order)")
              .appendField(new FieldNumber(0), "ORDER"); // 음수도 가능
          this.setPreviousStatement(true, "FLEX_CHILD");
          this.setNextStatement(true, "FLEX_CHILD");
          this.setColour(BLOCK_COLOR);
          this.setTooltip('낮은 숫자가 먼저 배치됩니다. (기본 0)');
        }
      };
    }
    javascriptGenerator.forBlock['style_flex_item_order'] = (block) => `  order: ${block.getFieldValue('ORDER')} !important;\n`;

  }
  </script>