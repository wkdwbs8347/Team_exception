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
        this.setPreviousStatement(true); 
        this.setNextStatement(true); 
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
        this.setPreviousStatement(true); 
        this.setNextStatement(true); 
        this.setColour('#e91e63');
      }
    };
  }
  javascriptGenerator.forBlock['style_text_color'] = (block) => {
    const color = block.getFieldValue('COLOR');
    return `  color: ${color} !important;\n`;
  };
}
</script>

<style>
/* 피커가 다른 요소에 가리지 않게 z-index 보정 */
.picker_wrapper {
  z-index: 10000 !important;
}
</style>