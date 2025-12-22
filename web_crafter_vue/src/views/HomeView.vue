<template>
  <div id="app">
    <h1>나의 파이썬 웹 빌더</h1>
    <div class="controls">
      <button @click="generatePython">파이썬 코드로 변환</button>
    </div>
    <div style="display: flex; justify-content: center; gap: 20px;">
      <div id="blocklyDiv" style="height: 480px; width: 600px;"></div>
      <pre id="pythonCode" style="height: 480px; width: 300px; background: #222; color: #fff; text-align: left; padding: 10px;">{{ pythonResult }}</pre>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import * as Blockly from 'blockly';
import { pythonGenerator } from 'blockly/python'; // 파이썬 제너레이터 임포트

const pythonResult = ref('# 코드가 여기에 표시됩니다.');
let workspace = null;

onMounted(() => {
  const toolbox = {
    'kind': 'flyoutToolbox',
    'contents': [
      { 'kind': 'block', 'type': 'controls_if' },
      { 'kind': 'block', 'type': 'logic_compare' },
      { 'kind': 'block', 'type': 'math_number' },
      { 'kind': 'block', 'type': 'text' },
      { 'kind': 'block', 'type': 'text_print' } // 출력 블록 추가
    ]
  };

  workspace = Blockly.inject('blocklyDiv', {
    toolbox: toolbox,
    scrollbars: true
  });
});

const generatePython = () => {
  // 블록을 파이썬 코드로 변환
  const code = pythonGenerator.workspaceToCode(workspace);
  pythonResult.value = code || '# 블록을 연결해 주세요!';
};
</script>

<style>
.controls { margin-bottom: 10px; }
button { padding: 10px 20px; font-weight: bold; cursor: pointer; }
#blocklyDiv { border: 1px solid #ccc; }
</style>