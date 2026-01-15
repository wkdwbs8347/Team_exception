<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

/**
 * 1. 애니메이션 설계도 (20종 대규모 업데이트)
 */
const ANIMATION_KEYFRAMES = `
/* --- 등장 및 퇴장 (Entrance) --- */
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes zoomIn { from { transform: scale(0.5); opacity: 0; } to { transform: scale(1); opacity: 1; } }
@keyframes flipInY { from { transform: perspective(400px) rotateY(90deg); opacity: 0; } to { transform: perspective(400px) rotateY(0deg); opacity: 1; } }
@keyframes backInDown { 0% { transform: translateY(-1200px) scale(0.7); opacity: 0.7; } 80% { transform: translateY(0px) scale(0.7); opacity: 0.7; } 100% { transform: scale(1); opacity: 1; } }
@keyframes rollIn { from { opacity: 0; transform: translateX(-100%) rotate(-120deg); } to { opacity: 1; transform: translateX(0px) rotate(0deg); } }
@keyframes slideInDown { from { transform: translateY(-100%); visibility: visible; } to { transform: translateY(0); } }
@keyframes bounceIn { 0%, 20%, 40%, 60%, 80%, 100% { transition-timing-function: cubic-bezier(0.215, 0.61, 0.355, 1); } 0% { opacity: 0; transform: scale3d(0.3, 0.3, 0.3); } 20% { transform: scale3d(1.1, 1.1, 1.1); } 40% { transform: scale3d(0.9, 0.9, 0.9); } 60% { opacity: 1; transform: scale3d(1.03, 1.03, 1.03); } 80% { transform: scale3d(0.97, 0.97, 0.97); } 100% { opacity: 1; transform: scale3d(1, 1, 1); } }
@keyframes jackInTheBox { 0% { opacity: 0; transform: scale(0.1) rotate(30deg); transform-origin: center bottom; } 50% { transform: rotate(-10deg); } 70% { transform: rotate(3deg); } 100% { opacity: 1; transform: scale(1); } }
@keyframes blurIn { from { filter: blur(20px); opacity: 0; } to { filter: blur(0); opacity: 1; } }
@keyframes swirlIn { from { transform: rotate(-540deg) scale(0); opacity: 0; } to { transform: rotate(0) scale(1); opacity: 1; } }

/* --- 강조 및 루프 (Emphasis) --- */
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }
@keyframes heartbeat { 0% { transform: scale(1); } 14% { transform: scale(1.1); } 28% { transform: scale(1); } 42% { transform: scale(1.1); } 70% { transform: scale(1); } }
@keyframes jello { 11.1% { transform: translate3d(0, 0, 0); } 22.2% { transform: skewX(-12.5deg) skewY(-12.5deg); } 33.3% { transform: skewX(6.25deg) skewY(6.25deg); } 44.4% { transform: skewX(-3.125deg) skewY(-3.125deg); } 55.5% { transform: skewX(1.5625deg) skewY(1.5625deg); } 66.6% { transform: skewX(-0.78125deg) skewY(-0.78125deg); } 77.7% { transform: skewX(0.390625deg) skewY(0.390625deg); } 88.8% { transform: skewX(-0.1953125deg) skewY(-0.1953125deg); } 100% { transform: translate3d(0, 0, 0); } }
@keyframes floating { 0%, 100% { transform: translateY(0px); } 50% { transform: translateY(-15px); } }
@keyframes shake { 0%, 100% { transform: translateX(0); } 10%, 30%, 50%, 70%, 90% { transform: translateX(-5px); } 20%, 40%, 60%, 80% { transform: translateX(5px); } }
@keyframes tada { 0% { transform: scale3d(1, 1, 1); } 10%, 20% { transform: scale3d(0.9, 0.9, 0.9) rotate3d(0, 0, 1, -3deg); } 30%, 50%, 70%, 90% { transform: scale3d(1.1, 1.1, 1.1) rotate3d(0, 0, 1, 3deg); } 40%, 60%, 80% { transform: scale3d(1.1, 1.1, 1.1) rotate3d(0, 0, 1, -3deg); } 100% { transform: scale3d(1, 1, 1); } }
@keyframes rubberBand { 0% { transform: scale3d(1, 1, 1); } 30% { transform: scale3d(1.25, 0.75, 1); } 40% { transform: scale3d(0.75, 1.25, 1); } 50% { transform: scale3d(1.15, 0.85, 1); } 65% { transform: scale3d(0.95, 1.05, 1); } 75% { transform: scale3d(1.05, 0.95, 1); } 100% { transform: scale3d(1, 1, 1); } }
@keyframes swing { 20% { transform: rotate3d(0, 0, 1, 15deg); } 40% { transform: rotate3d(0, 0, 1, -10deg); } 60% { transform: rotate3d(0, 0, 1, 5deg); } 80% { transform: rotate3d(0, 0, 1, -5deg); } 100% { transform: rotate3d(0, 0, 1, 0deg); } }
@keyframes rainbow { 0% { color: #ff0000; } 33% { color: #00ff00; } 66% { color: #0000ff; } 100% { color: #ff0000; } }
@keyframes flip3D { from { transform: perspective(400px) rotateY(0); } to { transform: perspective(400px) rotateY(360deg); } }
@keyframes swinging {0% { transform: rotate(0deg); transform-origin: top center; } 20% { transform: rotate(15deg); }40% { transform: rotate(-10deg); }60% { transform: rotate(5deg); }80% { transform: rotate(-5deg); }100% { transform: rotate(0deg); }}
`;

export const Animation = { ANIMATION_KEYFRAMES };
export const category = { label: '애니메이션', color: '#e91e63', icon: '🎬' };

/**
 * 2. 툴박스 설정 (조립형 구조)
 */
export const toolbox = `
<xml>
  <block type="style_animation_main"></block> 
  <sep gap="32"></sep>
  <block type="effect_entrance"></block>
  <block type="effect_emphasis"></block>
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
  // Keyframes 등록 로직
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

  // 메인 블록: 효과 블록을 끼우는 'EFFECT' 소켓을 가짐
  Blockly.Blocks['style_animation_main'] = {
    init() {
      this.appendValueInput('EFFECT').setCheck('ANIM_EFFECT').appendField('🎬 애니메이션');
      this.appendStatementInput('DETAILS').setCheck('ANIM_DETAIL').appendField('➕ 세부 설정');
      this.setPreviousStatement(true, 'STYLE');
      this.setNextStatement(true, 'STYLE');
      this.setColour('#ff0066');
    }
  };

  // 등장 효과 분류 블록 (10종)
  Blockly.Blocks['effect_entrance'] = {
    init() {
      this.appendDummyInput().appendField('🚀 등장/퇴장:')
          .appendField(new Blockly.FieldDropdown([
            ['서서히 나타나기', 'fadeIn'], ['회전 등장', 'flipInY'], ['툭 떨어지기', 'backInDown'], 
            ['굴러오기', 'rollIn'], ['위에서 툭', 'slideInDown'], ['확대 등장', 'zoomIn'],
            ['통통 튀며 등장', 'bounceIn'], ['상자 탈출', 'jackInTheBox'], ['흐릿하게', 'blurIn'], ['휘리릭', 'swirlIn']
          ]), 'NAME');
      this.setOutput(true, 'ANIM_EFFECT');
      this.setColour('#ff4d94');
    }
  };

  // 강조 효과 분류 블록 (10종)
Blockly.Blocks['effect_emphasis'] = {
  init() {
    this.appendDummyInput().appendField('✨ 강조/반복:')
        .appendField(new Blockly.FieldDropdown([
          ['두근두근', 'pulse'], ['심장 박동', 'heartbeat'], ['둥둥 뜨기', 'floating'], ['젤리처럼', 'jello'], ['좌우 흔들기', 'shake'],['짠! 나타나기', 'tada'],
          ['고무줄', 'rubberBand'], ['추 흔들기', 'swing'], ['3D 회전', 'flip3D'], ['대롱대롱', 'swinging'], ['무지개색', 'rainbow']
        ]), 'NAME');
    this.setOutput(true, 'ANIM_EFFECT');
    this.setColour('#ff4d94');
  }
};

  // 세부 설정 블록들 (기존과 동일)
  Blockly.Blocks['anim_duration'] = { init() { this.appendDummyInput().appendField('⏱️ 재생 시간').appendField(new Blockly.FieldNumber(1, 0.1), 'SEC').appendField('초'); this.setPreviousStatement(true, 'ANIM_DETAIL'); this.setNextStatement(true, 'ANIM_DETAIL'); this.setColour('#ff4d94'); } };
  Blockly.Blocks['anim_iteration'] = { init() { this.appendDummyInput().appendField('🔄 반복').appendField(new Blockly.FieldDropdown([['무한히', 'infinite'], ['1번', '1'], ['2번', '2'], ['5번', '5']]), 'COUNT'); this.setPreviousStatement(true, 'ANIM_DETAIL'); this.setNextStatement(true, 'ANIM_DETAIL'); this.setColour('#ff4d94'); } };
  Blockly.Blocks['anim_direction'] = { init() { this.appendDummyInput().appendField('↔️ 방향').appendField(new Blockly.FieldDropdown([['정방향', 'normal'], ['역방향', 'reverse'], ['왕복', 'alternate']]), 'DIR'); this.setPreviousStatement(true, 'ANIM_DETAIL'); this.setNextStatement(true, 'ANIM_DETAIL'); this.setColour('#ff4d94'); } };
  Blockly.Blocks['anim_timing'] = { init() { this.appendDummyInput().appendField('📈 속도감').appendField(new Blockly.FieldDropdown([['부드럽게', 'ease'], ['일정하게', 'linear'], ['점점 빠르게', 'ease-in'], ['점점 느리게', 'ease-out']]), 'TYPE'); this.setPreviousStatement(true, 'ANIM_DETAIL'); this.setNextStatement(true, 'ANIM_DETAIL'); this.setColour('#ff4d94'); } };
  Blockly.Blocks['anim_delay'] = { init() { this.appendDummyInput().appendField('⏳ 대기').appendField(new Blockly.FieldNumber(0, 0), 'SEC').appendField('초 뒤 시작'); this.setPreviousStatement(true, 'ANIM_DETAIL'); this.setNextStatement(true, 'ANIM_DETAIL'); this.setColour('#ff4d94'); } };
};

javascriptGenerator.forBlock['style_animation_main'] = (block) => {
  // 1. 이름이나 <style> 태그를 여기서 직접 만들지 않습니다.
  const rawEffect = javascriptGenerator.valueToCode(block, 'EFFECT', javascriptGenerator.ORDER_ATOMIC) || "pulse";
  const name = rawEffect.replace(/['"()]/g, "").trim();
  
  const details = javascriptGenerator.statementToCode(block, 'DETAILS');
  let duration = '1s', iteration = '1', direction = 'normal', timing = 'ease', delay = '0s';

  details.split('\n').forEach(line => {
    const trimmed = line.trim();
    if (trimmed.startsWith('dur:')) duration = trimmed.split(':')[1];
    else if (trimmed.startsWith('iter:')) iteration = trimmed.split(':')[1];
    else if (trimmed.startsWith('dir:')) direction = trimmed.split(':')[1];
    else if (trimmed.startsWith('tim:')) timing = trimmed.split(':')[1];
    else if (trimmed.startsWith('del:')) delay = trimmed.split(':')[1];
  });

  // 2. [최종 수정] 클래스명이나 태그 없이 순수 속성만 반환합니다.
  // 이렇게 해야 상위 블록이 만든 중괄호 {} 안에 예쁘게 들어갑니다.
  return `animation: ${name} ${duration} ${timing} ${delay} ${iteration} ${direction} forwards !important;\n`;
};

// 효과 블록들은 단순 문자열만 반환 (메인 블록에서 가공함)
javascriptGenerator.forBlock['effect_entrance'] = (block) => [block.getFieldValue('NAME'), javascriptGenerator.ORDER_ATOMIC];
javascriptGenerator.forBlock['effect_emphasis'] = (block) => [block.getFieldValue('NAME'), javascriptGenerator.ORDER_ATOMIC];

// 세부 설정들
javascriptGenerator.forBlock['anim_duration'] = (block) => `dur:${block.getFieldValue('SEC')}s\n`;
javascriptGenerator.forBlock['anim_iteration'] = (block) => `iter:${block.getFieldValue('COUNT')}\n`;
javascriptGenerator.forBlock['anim_direction'] = (block) => `dir:${block.getFieldValue('DIR')}\n`;
javascriptGenerator.forBlock['anim_timing'] = (block) => `tim:${block.getFieldValue('TYPE')}\n`;
javascriptGenerator.forBlock['anim_delay'] = (block) => `del:${block.getFieldValue('SEC')}s\n`;
// 초기화 실행
defineBlocks();
</script>