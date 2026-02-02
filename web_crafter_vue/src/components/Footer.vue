<script setup>
import { Blocks, Mail } from 'lucide-vue-next';

const currentYear = new Date().getFullYear();

// ✅ 여기 이메일만 실제 값으로 바꿔 넣으면 됨
const members = [
  { name: '장지윤', email: 'gbjy8347@gmail.com' },
  { name: '김영민', email: 'ymyj0212k@gmail.com' },
  { name: '김민재', email: 'rlaalswo4377@gmail.com' },
];
</script>

<template>
  <footer class="footer">
    <div class="footer-container">
      <!-- 상단: 브랜드 + 설명 -->
      <div class="footer-top">
        <div class="brand">
          <span class="brand-icon"><Blocks :size="28" /></span>
          <span class="brand-text">Web Crafter</span>
        </div>
        <p class="brand-desc">
          블록코딩 기술을 활용하여 나만의 웹 사이트를 손쉽게 제작하고 관리하세요.
        </p>
      </div>

      <!-- ✅ 팀 연락처만 -->
      <div class="contact-card">
        <h4 class="contact-title">
          <Mail :size="18" />
          <span>Contact</span>
        </h4>

        <ul class="contact-list">
          <li v-for="m in members" :key="m.name" class="contact-item">
            <span class="name">{{ m.name }}</span>
            <span class="colon">:</span>
            <a class="email" :href="`mailto:${m.email}`">{{ m.email }}</a>
          </li>
        </ul>
      </div>

      <!-- 하단 -->
      <div class="footer-bottom">
        <p>&copy; {{ currentYear }} Web Crafter. All rights reserved.</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
/* ✅ 전체 높이 줄임: padding, margin-top 축소 */
.footer {
  position: relative;
  background: linear-gradient(180deg, rgba(15, 15, 30, 0.5) 0%, #0f0f1e 100%);
  border-top: 1px solid rgba(0, 212, 255, 0.1);
  padding: 2.2rem 2rem 1.2rem;  /* ⬅️ 줄임 */
  margin-top: 2.2rem;          /* ⬅️ 줄임 */
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* ✅ 상단 간격 축소 */
.footer-top {
  margin-bottom: 1rem; /* ⬅️ 줄임 */
}

.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;           /* ⬅️ 줄임 */
  font-size: 1.35rem;    /* ⬅️ 살짝 줄임 */
  font-weight: 800;
  margin-bottom: 0.45rem; /* ⬅️ 줄임 */
}

.brand-icon {
  display: flex;
  align-items: center;
  color: #00d4ff;
}

.brand-text {
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-desc {
  color: #a0a0a0;
  font-size: 0.9rem;     /* ⬅️ 줄임 */
  line-height: 1.45;     /* ⬅️ 줄임 */
  margin: 0;
}

/* ✅ 연락처 카드: padding/라운드/타이틀 간격 축소 */
.contact-card {
  border: 1px solid rgba(0, 212, 255, 0.12);
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;         /* ⬅️ 살짝 줄임 */
  padding: 0.9rem 1rem;        /* ⬅️ 줄임 */
}

.contact-title {
  display: flex;
  align-items: center;
  gap: 0.45rem;               /* ⬅️ 줄임 */
  color: #e0e0e0;
  font-size: 0.9rem;          /* ⬅️ 줄임 */
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 0.9px;
  margin: 0 0 0.6rem 0;       /* ⬅️ 줄임 */
}

.contact-title :deep(svg) {
  color: #00d4ff;
}

/* ✅ 리스트 간격 줄임 */
.contact-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 0.45rem;               /* ⬅️ 줄임 */
}

.contact-item {
  display: flex;
  align-items: baseline;
  gap: 0.45rem;               /* ⬅️ 줄임 */
  flex-wrap: wrap;
  color: #a0a0a0;
  font-size: 0.9rem;          /* ⬅️ 줄임 */
  line-height: 1.35;          /* ⬅️ 줄임 */
}

.name {
  color: #e8eef6;
  font-weight: 700;
}

.colon {
  color: #606060;
}

.email {
  color: #00d4ff;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s ease;
}

.email:hover {
  color: #00ffff;
  border-bottom-color: rgba(0, 255, 255, 0.6);
}

/* ✅ 하단 영역 높이 줄임 */
.footer-bottom {
  margin-top: 0.9rem;         /* ⬅️ 줄임 */
  padding-top: 0.75rem;       /* ⬅️ 줄임 */
  border-top: 1px solid rgba(0, 212, 255, 0.05);
  color: #808080;
  font-size: 0.85rem;         /* ⬅️ 줄임 */
}

/* ✅ 반응형에서도 padding 더 줄임 */
@media (max-width: 768px) {
  .footer {
    padding: 1.8rem 1rem 1rem; /* ⬅️ 줄임 */
    margin-top: 1.6rem;        /* ⬅️ 줄임 */
  }
  .contact-card {
    padding: 0.85rem 0.9rem;   /* ⬅️ 줄임 */
  }
}
</style>