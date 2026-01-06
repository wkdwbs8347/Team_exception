<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

export const category = {
  label: '애니메이션',
  color: '#e91e63',
  icon: '🎬'
};

export const toolbox = `
<xml>
  <block type="style_animation_preset"></block>
  <block type="style_animation_custom"></block>
</xml>
`;

// CSS 애니메이션 키프레임 정의 (이 부분은 유지해야 애니메이션이 작동합니다)
export const ANIMATION_KEYFRAMES = `
@keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-20px); }
  60% { transform: translateY(-10px); }
}
@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}
@keyframes zoom-in { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes rainbow {
  0% { color: #ff0000; } 50% { color: #00ff00; } 100% { color: #ff0000; }
}
@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-15px); }
}
`;

export const defineBlocks = () => {
  // 1. 효과 블록 (프리셋)
  Blockly.Blocks['style_animation_preset'] = {
    init: function() {
      this.appendDummyInput()
          .appendField("🎬 효과")
          .appendField(new Blockly.FieldDropdown([
              ["서서히 나타나기", "fade-in"],
              ["통통 튀기", "bounce"],
              ["회전하기", "rotate"],
              ["흔들기", "shake"],
              ["커지기", "zoom-in"],
              ["🌈 무지개 텍스트", "rainbow"],
              ["🎈 둥둥 떠있기", "float"]
          ]), "NAME")
          .appendField("속도")
          .appendField(new Blockly.FieldDropdown([
              ["매우 천천히", "3s"], ["천천히", "2s"], ["보통", "1s"], ["빠르게", "0.5s"]
          ]), "SPEED");
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#e91e63');
    }
  };

  // 2. 애니메이션 상세 설정
  Blockly.Blocks['style_animation_custom'] = {
    init: function() {
      this.appendDummyInput().appendField("✨ 애니메이션 상세 설정");
      this.appendDummyInput()
          .appendField("동작")
          .appendField(new Blockly.FieldDropdown([
              ["서서히 나타나기", "fade-in"], ["통통 튀기", "bounce"], ["회전하기", "rotate"]
          ]), "NAME")
          .appendField("시간").appendField(new Blockly.FieldTextInput("1"), "TIME").appendField("초");
      this.appendDummyInput()
          .appendField("반복")
          .appendField(new Blockly.FieldDropdown([["무한히", "infinite"], ["1번", "1"]]), "COUNT")
          .appendField("부드럽기")
          .appendField(new Blockly.FieldDropdown([["보통", "ease-in-out"], ["일정하게", "linear"]]), "TIMING");
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#e91e63');
    }
  };
};

// ==================== 제너레이터 (호버 완전 제거) ====================

javascriptGenerator.forBlock['style_animation_preset'] = function(block) {
  const name = block.getFieldValue('NAME') || 'fade-in';
  const speed = block.getFieldValue('SPEED') || '1s';
  let count = 'infinite';
  if (name === 'fade-in' || name === 'zoom-in') {
    count = '1';
  }
  // 복잡한 조건 없이 바로 CSS 한 줄 생성
  return `animation: ${name} ${speed} ease-in-out infinite forwards; `;
};

javascriptGenerator.forBlock['style_animation_custom'] = function(block) {
  const name = block.getFieldValue('NAME') || 'fade-in';
  const time = block.getFieldValue('TIME') || '1';
  const count = block.getFieldValue('COUNT') || 'infinite';
  const timing = block.getFieldValue('TIMING') || 'ease-in-out';
  return `animation: ${name} ${time}s ${timing} ${count} forwards; `;
};
</script>