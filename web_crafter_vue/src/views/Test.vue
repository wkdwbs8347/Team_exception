<template>
  <div>
    <h1>Spring Boot 연결 테스트</h1>
    <button @click="checkHello">서버 요청</button>
    <p>응답: {{ message }}</p>
  </div>
</template>

<script>
import api from '../api/axios'; // 작성하신 axios 인스턴스 import

export default {
  data() {
    return {
      message: ''
    };
  },
  methods: {
    async checkHello() {
      try {
        const response = await api.get('/hello'); // baseURL이 http://localhost:8080/api로 이미 설정되어 있음
        this.message = response.data;
      } catch (error) {
        console.error(error);
        this.message = '서버 요청 실패';
      }
    }
  }
};
</script>