<script>
import * as Blockly from 'blockly';
import { javascriptGenerator } from 'blockly/javascript';

// ===== Content 카테고리 메타데이터 / 툴박스 UI를 구성하는 곳에서 이 값을 참조 =====
export const category = {
  label: '콘텐츠',
  color: '#00c853',
  icon: '🧩',
};

// ===== Content 툴박스 XML / 툴박스에 어떤 블록들을 보여줄지 =====
export const toolbox = `
<xml>
  <block type="content_heading"></block>
  <block type="content_button"></block>
  <block type="content_text"></block>
  <block type="content_image"></block>
  <block type="content_link"></block>
</xml>
`;

// ===== Content 블록 정의 및 코드 생성기 =====
export const defineBlocks = () => {
  // 공통 로직: 안전한 이름 생성 및 좌표 데이터 추출 함수
  const getBlockMeta = (block, defaultName) => {
    const nameInput = block.getFieldValue('NAME') || defaultName;
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const displayId = `${safeName}_${block.id.substring(0, 3)}`;

    let x = 0,
      y = 0;
    if (block.data) {
      try {
        const coords = JSON.parse(block.data);
        x = coords.x || 0;
        y = coords.y || 0;
      } catch (e) {
        console.error('좌표 파싱 실패', e);
      }
    }
    return { displayId, x, y, safeName };
  };

  // 핵심: "부모 레이아웃 안에 들어갔는지" 판별
  const isInsideLayout = (block) => {
    const parent = block.getSurroundParent?.();
    return (
      !!parent &&
      typeof parent.type === 'string' &&
      parent.type.startsWith('layout_')
    );
  };

  // layout 안/밖에 따라 style을 자동으로 바꿔주는 헬퍼
  // - layout 밖: absolute + translate (캔버스 배치)
  // - layout 안: 흐름 레이아웃 (absolute 제거)
  const getStyle = (block, x, y, baseStyle = '') => {
    const inside = isInsideLayout(block);

    if (inside) {
      // 부모 안에서는 "문서 흐름"을 따르게
      // (margin은 기본 0 유지, spacing은 CSS/스타일 블록에서 확장 가능)
      return `${baseStyle} margin:0; position: relative;`;
    }

    // 부모 밖에서는 기존처럼 캔버스 배치
    return `${baseStyle} position: absolute; transform: translate(${x}px, ${y}px); margin:0;`;
  };

  // ----- [1] 블록의 '모양' 정의 -----

  // 헤딩 블록 모양
  if (!Blockly.Blocks['content_heading']) {
    Blockly.Blocks['content_heading'] = {
      init() {
        this.appendDummyInput().appendField('🔤 제목');

        this.appendDummyInput()
          .appendField('이름:')
          .appendField(new Blockly.FieldTextInput('제목'), 'NAME');

        this.appendDummyInput()
          .appendField('레벨:')
          .appendField(
            new Blockly.FieldDropdown([
              ['H1', 'h1'],
              ['H2', 'h2'],
              ['H3', 'h3'],
              ['H4', 'h4'],
              ['H5', 'h5'],
              ['H6', 'h6'],
            ]),
            'LEVEL'
          );

        this.appendDummyInput()
          .appendField('텍스트:')
          .appendField(new Blockly.FieldTextInput('제목을 입력하세요'), 'TEXT');

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#00c853');
      },
    };
  }

  // 버튼 블록 모양
  if (!Blockly.Blocks['content_button']) {
    Blockly.Blocks['content_button'] = {
      init() {
        this.appendDummyInput().appendField('🆗 버튼');
        this.appendDummyInput()
          .appendField('이름:')
          .appendField(new Blockly.FieldTextInput('버튼'), 'NAME');
        this.appendDummyInput()
          .appendField('내용:')
          .appendField(new Blockly.FieldTextInput('클릭'), 'LABEL');

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#00c853');
      },
    };
  }

  // 텍스트 블록 모양
  if (!Blockly.Blocks['content_text']) {
    Blockly.Blocks['content_text'] = {
      init() {
        this.appendDummyInput().appendField('📝 텍스트');
        this.appendDummyInput()
          .appendField('이름:')
          .appendField(new Blockly.FieldTextInput('텍스트_요소'), 'NAME');
        this.appendDummyInput()
          .appendField('내용:')
          .appendField(new Blockly.FieldTextInput('내용을 입력하세요'), 'TEXT');

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#00c853');
      },
    };
  }

  // 이미지 블록 모양
  if (!Blockly.Blocks['content_image']) {
    Blockly.Blocks['content_image'] = {
      init() {
        this.appendDummyInput().appendField('🖼️ 이미지');
        this.appendDummyInput()
          .appendField('이름:')
          .appendField(new Blockly.FieldTextInput('이미지_요소'), 'NAME');
        this.appendDummyInput()
          .appendField('SRC:')
          .appendField(
            new Blockly.FieldTextInput('https://via.placeholder.com/150'),
            'SRC'
          );

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#00c853');
      },
    };
  }

  // 링크(a) 블록 모양
  if (!Blockly.Blocks['content_link']) {
    Blockly.Blocks['content_link'] = {
      init() {
        this.appendDummyInput().appendField('🔗 링크');

        this.appendDummyInput()
          .appendField('이름:')
          .appendField(new Blockly.FieldTextInput('링크'), 'NAME');

        this.appendDummyInput()
          .appendField('텍스트:')
          .appendField(new Blockly.FieldTextInput('링크 텍스트'), 'TEXT');

        this.appendDummyInput()
          .appendField('주소:')
          .appendField(
            new Blockly.FieldTextInput('https://example.com'),
            'HREF'
          );

        this.setPreviousStatement(true, 'ELEMENT');
        this.setNextStatement(true, 'ELEMENT');
        this.setColour('#00c853');
      },
    };
  }

  // ----- [2] 블록이 생성할 'HTML 코드' 정의 -----

  // 제목
  javascriptGenerator.forBlock['content_heading'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '제목';
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const { x, y } = getBlockMeta(block, '제목');

    const level = block.getFieldValue('LEVEL') || 'h2';
    const text = block.getFieldValue('TEXT') || '';

    const style = getStyle(block, x, y, ''); // baseStyle 없으면 ''로
    return `<${level} class="${safeName}"
data-block-id="${block.id}"
data-x="${x}"
data-y="${y}"
style="${style}">${text}</${level}>\n`;
  };

  // 버튼
  javascriptGenerator.forBlock['content_button'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '버튼';
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const { x, y } = getBlockMeta(block, '버튼');
    const label = block.getFieldValue('LABEL') || '';

    // layout 안에서는 버튼이 inline-block으로 흐름에 자연스럽게 들어가게
    const baseStyle = 'color:inherit; display:inline-block;';
    const style = getStyle(block, x, y, baseStyle);

    return `<button class="${safeName}"
data-block-id="${block.id}"
data-x="${x}"
data-y="${y}"
style="${style}">${label}</button>\n`;
  };

  // 텍스트
  javascriptGenerator.forBlock['content_text'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '텍스트';
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const { x, y } = getBlockMeta(block, '텍스트');
    const text = block.getFieldValue('TEXT') || '';

    const style = getStyle(block, x, y, '');
    return `<span class="${safeName}"
data-block-id="${block.id}"
data-x="${x}"
data-y="${y}"
style="${style}">${text}</span>\n`;
  };

  // 이미지
  javascriptGenerator.forBlock['content_image'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '이미지';
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const { x, y } = getBlockMeta(block, '이미지');

    const imgSrc =
      block.getFieldValue('SRC') || 'https://via.placeholder.com/150';

    // layout 안에서는 이미지가 줄바꿈되며 들어가게 (block)
    const baseStyle = 'display:block; max-width:100%; height:auto;';
    const style = getStyle(block, x, y, baseStyle);

    return `<img class="${safeName}"
src="${imgSrc}"
data-block-id="${block.id}"
data-x="${x}"
data-y="${y}"
style="${style}" />\n`;
  };

  // 링크(a)
  javascriptGenerator.forBlock['content_link'] = (block) => {
    const nameInput = block.getFieldValue('NAME') || '링크';
    const safeName = nameInput
      .replace(/\s+/g, '_')
      .replace(/[^a-zA-Z0-9_가-힣]/g, '');

    const { x, y } = getBlockMeta(block, '링크');

    const text = block.getFieldValue('TEXT') || '';
    let href = (block.getFieldValue('HREF') || '#').trim();

    // 내부 경로로 보이는 값은 차단(이동은 action_navigate로만)
    const looksInternal =
      href.startsWith('/') ||
      /^wc:\/\//i.test(href) ||
      /^page:/i.test(href) ||
      href.startsWith('#');

    if (looksInternal) {
      href = 'https://example.com'; // fallback
      // 사용자에게 안내하고 싶으면, 링크 텍스트에 표시하거나 콘솔에 남겨도 됨
    }

    // http(s) 없으면 외부링크로 간주하고 https:// 보정
    if (href && href !== '#' && !/^https?:\/\//i.test(href)) {
      href = 'https://' + href;
    }

    // 항상 새 탭 (외부 링크 전용)
    const targetAttr = ' target="_blank" rel="noopener noreferrer"';

    const baseStyle =
      'color:#1a73e8; text-decoration:underline; cursor:pointer;';
    const style = getStyle(block, x, y, baseStyle);

    return `<a class="${safeName}"
href="${href}"${targetAttr}
data-block-id="${block.id}"
data-x="${x}"
data-y="${y}"
style="${style}">${text}</a>\n`;
  };
};
</script>
