<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

/**
 * 1. 애니메이션 설계도 (Keyframes)
 */
const ANIMATION_KEYFRAMES = `
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes zoomIn { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes slideDown { from { transform: translateY(-20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); }
  20%, 40%, 60%, 80% { transform: translateX(5px); }
}
@keyframes bounce {
  0%, 20%, 50%, 80%, 100% { transform: translateY(0); }
  40% { transform: translateY(-20px); }
  60% { transform: translateY(-10px); }
}
`;

export const Animation = { ANIMATION_KEYFRAMES };
export const category = { label: '애니메이션', color: '#e91e63', icon: '🎬' };

export const toolbox = `
<xml>
  <block type="style_animation_main"></block> 
  <sep gap="32"></sep>
  <block type="anim_duration"></block> 
  <block type="anim_iteration"></block>
  <block type="anim_timing"></block>
  <block type="anim_direction"></block>
  <block type="anim_delay"></block>
</xml>
`;

/**
 * 3. 블록 정의
 */
export const defineBlocks = () => {
  if (typeof document !== 'undefined') {
    const styleId = 'web-crafter-animation-defs';
    let styleTag = document.getElementById(styleId);
    if (!styleTag) {
      styleTag = document.createElement('style');
      styleTag.id = styleId;
      document.head.appendChild(styleTag);
    }
    styleTag.textContent = ANIMATION_KEYFRAMES;
  }

  Blockly.Blocks['style_animation_main'] = {
    init() {
      this.appendDummyInput().appendField('🎬 애니메이션 효과')
          .appendField(new Blockly.FieldDropdown([
            ['서서히 나타나기', 'fadeIn'], ['커지며 나타나기', 'zoomIn'], ['흔들기', 'shake'], ['통통 튀기', 'bounce'], ['두근두근', 'pulse']
          ]), 'NAME');
      this.appendStatementInput('DETAILS').setCheck('ANIM_DETAIL').appendField('➕ 세부 설정');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ff0066');
    }
  };

  Blockly.Blocks['anim_duration'] = {
    init() {
      this.appendDummyInput().appendField('⏱️ 재생 시간').appendField(new Blockly.FieldNumber(1, 0.1), 'SEC').appendField('초');
      this.setPreviousStatement(true, 'ANIM_DETAIL');
      this.setNextStatement(true, 'ANIM_DETAIL');
      this.setColour('#ff4d94');
    }
  };

  Blockly.Blocks['anim_iteration'] = {
    init() {
      this.appendDummyInput().appendField('🔄 반복').appendField(new Blockly.FieldDropdown([['무한히', 'infinite'], ['1번', '1'], ['2번', '2'], ['5번', '5']]), 'COUNT');
      this.setPreviousStatement(true, 'ANIM_DETAIL');
      this.setNextStatement(true, 'ANIM_DETAIL');
      this.setColour('#ff4d94');
    }
  };

  Blockly.Blocks['anim_direction'] = {
    init() {
      this.appendDummyInput().appendField('↔️ 방향').appendField(new Blockly.FieldDropdown([['정방향', 'normal'], ['역방향', 'reverse'], ['왕복(자연스러움)', 'alternate']]), 'DIR');
      this.setPreviousStatement(true, 'ANIM_DETAIL');
      this.setNextStatement(true, 'ANIM_DETAIL');
      this.setColour('#ff4d94');
    }
  };

  Blockly.Blocks['anim_timing'] = {
    init() {
      this.appendDummyInput().appendField('📈 속도감').appendField(new Blockly.FieldDropdown([['부드럽게', 'ease'], ['일정하게', 'linear'], ['점점 빠르게', 'ease-in'], ['점점 느리게', 'ease-out']]), 'TYPE');
      this.setPreviousStatement(true, 'ANIM_DETAIL');
      this.setNextStatement(true, 'ANIM_DETAIL');
      this.setColour('#ff4d94');
    }
  };

  Blockly.Blocks['anim_delay'] = {
    init() {
      this.appendDummyInput().appendField('⏳ 대기').appendField(new Blockly.FieldNumber(0, 0), 'SEC').appendField('초 뒤 시작');
      this.setPreviousStatement(true, 'ANIM_DETAIL');
      this.setNextStatement(true, 'ANIM_DETAIL');
      this.setColour('#ff4d94');
    }
  };
};

/**
 * 4. 코드 생성기 (Generator)
 */
javascriptGenerator.forBlock['style_animation_main'] = (block) => {
  const name = block.getFieldValue('NAME'); 
  const details = javascriptGenerator.statementToCode(block, 'DETAILS');
  
  let duration = '1s', iteration = '1', direction = 'normal', timing = 'ease', delay = '0s';

  const lines = details.split('\n');
  lines.forEach(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith('dur:')) duration = trimmed.split(':')[1];
    else if (trimmed.startsWith('iter:')) iteration = trimmed.split(':')[1];
    else if (trimmed.startsWith('dir:')) direction = trimmed.split(':')[1];
    else if (trimmed.startsWith('tim:')) timing = trimmed.split(':')[1];
    else if (trimmed.startsWith('del:')) delay = trimmed.split(':')[1];
  });

  const css = `${name} ${duration} ${timing} ${delay} ${iteration} ${direction} forwards !important;`;
  
  // image_c5fb2a 처럼 중괄호가 꼬이는 걸 방지하기 위해 깔끔하게 속성만 반환
  return `animation: ${css}\n`;
};

javascriptGenerator.forBlock['anim_duration'] = (block) => `dur:${block.getFieldValue('SEC')}s\n`;
javascriptGenerator.forBlock['anim_iteration'] = (block) => `iter:${block.getFieldValue('COUNT')}\n`;
javascriptGenerator.forBlock['anim_direction'] = (block) => `dir:${block.getFieldValue('DIR')}\n`;
javascriptGenerator.forBlock['anim_timing'] = (block) => `tim:${block.getFieldValue('TYPE')}\n`;
javascriptGenerator.forBlock['anim_delay'] = (block) => `del:${block.getFieldValue('SEC')}s\n`;

// 필수 실행
defineBlocks();
</script>