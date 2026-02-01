<template>
  <div class="chat-modal" v-if="friend">
    <div class="chat-header">
      <span>{{ friend.nickname }}님과 대화</span>
      <button @click="$emit('close')">X</button>
    </div>
    <div class="chat-content" ref="msgBox">
      <div
        v-for="(msg, i) in messages"
        :key="i"
        :class="['msg', msg.senderId === myId ? 'me' : 'other']"
      >
        <p>{{ msg.content }}</p>
      </div>
    </div>
    <div class="chat-footer">
      <input
        v-model="input"
        @keyup.enter="send"
        placeholder="메시지를 입력하세요..."
      />
    </div>
  </div>
</template>

<script>
import { chatService } from '@/services/chatService'; // 아까 말한 서비스 파일 필요

export default {
  props: ['friend', 'myId'],
  data() {
    return { input: '', messages: [], roomId: '', sub: null };
  },
  mounted() {
    this.roomId = [this.myId, this.friend.id].sort().join('_');
    this.sub = chatService.subscribe(this.roomId, (msg) => {
      this.messages.push(msg);
      this.$nextTick(() => {
        this.$refs.msgBox.scrollTop = this.$refs.msgBox.scrollHeight;
      });
    });
  },
  methods: {
    send() {
      if (!this.input.trim()) return;
      chatService.send(this.roomId, this.myId, this.input);
      this.input = '';
    },
  },
  beforeUnmount() {
    if (this.sub) this.sub.unsubscribe();
  },
};
</script>
