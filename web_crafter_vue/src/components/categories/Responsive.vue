<script>
import * as Blockly from 'blockly';
import { pythonGenerator } from 'blockly/python';

/* [PART 1] 카테고리 메타데이터 정의
  - 블록 선택 메뉴(Toolbox)에서 보여질 이름, 색상, 아이콘을 설정합니다.
*/
export const category = {
  label: '반응형',
  color: '#0091ea',
  icon: '📱'
};

/* [PART 2] Toolbox(도구함) 구성
  - 사용자가 화면에서 실제로 보고 선택할 수 있는 블록들의 목록과 순서를 XML 형태로 정의합니다.
*/
export const toolbox = `
<xml>
    <block type="responsive_container"></block>
    <sep gap="32"></sep>
    
    <block type="responsive_screen_type"></block>
    <block type="responsive_screen_width"></block>
    <block type="responsive_screen_height"></block>
    <block type="responsive_orientation"></block>
    <block type="responsive_is_touch"></block>
    <sep gap="32"></sep>
    
    <block type="responsive_if_mobile"></block>
    <block type="responsive_if_tablet"></block>
    <block type="responsive_if_desktop"></block>
    <sep gap="32"></sep>
    
    <block type="responsive_grid_columns"></block>
    <block type="responsive_spacing"></block>
    <block type="responsive_max_width"></block>
</xml>
`;

/* [PART 3] 블록 기능 정의 및 코드 생성 로직
  - Blockly 엔진에 블록의 모양(UI)과 해당 블록이 생성할 코드(Python/HTML)를 등록합니다.
*/
export const defineBlocks = () => {
  // 중복 등록 방지: 이미 블록이 정의되어 있다면 실행을 중단합니다.
  if (Blockly.Blocks['responsive_container']) return;

  /* A. 반응형 컨테이너 블록 (핵심 기능)
    - 특징: 브라우저 너비가 줄어들면 내부 콘텐츠가 화면 밖으로 나가지 않게 'width: 100%'를 적용합니다.
    - 코드: <div style="width:100%; max-width:설정값px;"> 형태로 생성됩니다.
  */
  Blockly.Blocks['responsive_container'] = {
    init() {
      this.appendDummyInput()
          .appendField("📱 반응형 상자")
          .appendField("최대 너비")
          .appendField(new Blockly.FieldTextInput("1200"), "MAX_WIDTH")
          .appendField("px");
      this.appendStatementInput('CONTENT').setCheck(null); // 내부에 다른 블록을 끼울 수 있는 공간
      this.setPreviousStatement(true);
      this.setNextStatement(true);
      this.setColour('#0091ea');
      this.setTooltip("브라우저가 좁아지면 자동으로 너비가 줄어드는 상자입니다.");
    }
  };

  pythonGenerator.forBlock['responsive_container'] = (block, gen) => {
    const maxWidth = block.getFieldValue('MAX_WIDTH');
    const content = gen.statementToCode(block, 'CONTENT');
    return `<div style="width:100%; max-width:${maxWidth}px; margin:0 auto; box-sizing:border-box;">\n${content}</div>\n`;
  };

  /* B. 화면 데이터 조회 블록 (단순 값 반환)
    - 화면의 너비, 높이, 현재 기기 타입 등을 'responsive' 객체에서 실시간으로 가져옵니다.
  */
  const data = [
    { id: 'responsive_screen_type', label: '📱 화면 크기', type: 'String', color: 210, py: '("모바일" if responsive["width"] < 768 else "태블릿" if responsive["width"] < 1024 else "데스크톱")' },
    { id: 'responsive_screen_width', label: '↔️ 화면 너비', type: 'Number', color: 210, py: 'responsive["width"]' },
    { id: 'responsive_screen_height', label: '↕️ 화면 높이', type: 'Number', color: 210, py: 'responsive["height"]' },
    { id: 'responsive_orientation', label: '🔄 화면 방향', type: 'String', color: 210, py: 'responsive["orientation"]' },
    { id: 'responsive_is_touch', label: '👆 터치 지원?', type: 'Boolean', color: 210, py: 'responsive["is_touch"]' }
  ];

  data.forEach(d => {
    Blockly.Blocks[d.id] = {
      init() {
        this.appendDummyInput().appendField(d.label);
        this.setOutput(true, d.type); // 데이터 값을 내보내는 둥근 모양의 블록
        this.setColour(d.color);
      }
    };
    pythonGenerator.forBlock[d.id] = () => [d.py, pythonGenerator.ORDER_ATOMIC];
  });

  /* C. 기기별 조건 실행 블록 (If 제어문)
    - 특정 화면 크기 구간에서만 블록 내부의 코드가 실행되도록 감싸는 역할을 합니다.
  */
  const ifBlocks = [
    { id: 'responsive_if_mobile', label: '📱 모바일일 때', cond: 'responsive["width"] < 768' },
    { id: 'responsive_if_tablet', label: '📱 태블릿일 때', cond: '768 <= responsive["width"] < 1024' },
    { id: 'responsive_if_desktop', label: '💻 데스크톱일 때', cond: 'responsive["width"] >= 1024' }
  ];

  ifBlocks.forEach(d => {
    Blockly.Blocks[d.id] = {
      init() {
        this.appendDummyInput().appendField(d.label);
        this.appendStatementInput('DO'); // 조건 충족 시 실행할 블록 입구
        this.setPreviousStatement(true);
        this.setNextStatement(true);
        this.setColour(20);
      }
    };
    pythonGenerator.forBlock[d.id] = (block, gen) => {
      const body = gen.statementToCode(block, 'DO');
      return `if ${d.cond}:\n${body || '    pass\n'}`;
    };
  });

  /* D. 가변 레이아웃 수치 블록 (자동 최적화)
    - 별도의 조건문을 직접 짜지 않아도, 블록 하나가 현재 화면 크기에 맞는 최적의 수치(그리드 칸 수 등)를 반환합니다.
  */
  const layoutValues = [
    { id: 'responsive_grid_columns', label: '📐 그리드 칼럼 수', v: [1, 2, 3] }, // 모바일 1칸, 태블릿 2칸, 데스크톱 3칸
    { id: 'responsive_spacing', label: '📏 여백(px)', v: [8, 16, 24] },
    { id: 'responsive_max_width', label: '📦 최대 너비(px)', v: [480, 768, 1200] }
  ];

  layoutValues.forEach(l => {
    Blockly.Blocks[l.id] = {
      init() {
        this.appendDummyInput().appendField(l.label);
        this.setOutput(true, 'Number');
        this.setColour(120);
      }
    };
    pythonGenerator.forBlock[l.id] = () => {
      // 3항 연산자를 중첩하여 기기별 최적값을 자동으로 선택하게 함
      const code = `(${l.v[0]} if responsive["width"] < 768 else ${l.v[1]} if responsive["width"] < 1024 else ${l.v[2]})`;
      return [code, pythonGenerator.ORDER_CONDITIONAL];
    };
  });
};
</script>