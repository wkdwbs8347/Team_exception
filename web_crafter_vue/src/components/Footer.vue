<script setup>
const currentYear = new Date().getFullYear()

const footerLinks = {
  product: [
    { label: 'Features', href: '#' },
    { label: 'Pricing', href: '#' },
    { label: 'Security', href: '#' },
    { label: 'Roadmap', href: '#' }
  ],
  company: [
    { label: 'About', href: '#' },
    { label: 'Blog', href: '#' },
    { label: 'Careers', href: '#' },
    { label: 'Contact', href: '#' }
  ],
  resources: [
    { label: 'Documentation', href: '#' },
    { label: 'API Reference', href: '#' },
    { label: 'Support', href: '#' },
    { label: 'Community', href: '#' }
  ]
}

const socialLinks = [
  { icon: '𝕏', label: 'Twitter', href: '#' },
  { icon: 'f', label: 'Facebook', href: '#' },
  { icon: 'in', label: 'LinkedIn', href: '#' },
  { icon: '▶', label: 'YouTube', href: '#' }
]
</script>

<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-content">
        <div class="footer-section">
          <h3 class="footer-brand">
            <span class="brand-icon">✨</span>
            StyleHub
          </h3>
          <p class="brand-description">
            Transform your digital presence with cutting-edge design and technology.
          </p>
          <div class="social-links">
            <a v-for="social in socialLinks" :key="social.label" :href="social.href" class="social-link" :title="social.label">
              {{ social.icon }}
            </a>
          </div>
        </div>

        <div class="footer-section">
          <h4 class="section-title">Product</h4>
          <ul class="link-list">
            <li v-for="link in footerLinks.product" :key="link.label">
              <a :href="link.href">{{ link.label }}</a>
            </li>
          </ul>
        </div>

        <div class="footer-section">
          <h4 class="section-title">Company</h4>
          <ul class="link-list">
            <li v-for="link in footerLinks.company" :key="link.label">
              <a :href="link.href">{{ link.label }}</a>
            </li>
          </ul>
        </div>

        <div class="footer-section">
          <h4 class="section-title">Resources</h4>
          <ul class="link-list">
            <li v-for="link in footerLinks.resources" :key="link.label">
              <a :href="link.href">{{ link.label }}</a>
            </li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <div class="footer-bottom-left">
          <p>&copy; {{ currentYear }} StyleHub. All rights reserved.</p>
        </div>
        <div class="footer-bottom-right">
          <a href="#" class="footer-link">Privacy Policy</a>
          <span class="divider">•</span>
          <a href="#" class="footer-link">Terms of Service</a>
          <span class="divider">•</span>
          <a href="#" class="footer-link">Cookie Policy</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  position: relative;
  background: linear-gradient(180deg, rgba(15, 15, 30, 0.5) 0%, #0f0f1e 100%);
  border-top: 1px solid rgba(0, 212, 255, 0.1);
  padding: 4rem 2rem 2rem;
  margin-top: 4rem;
}

.footer-container {
  max-width: 1200px;
  margin: 0 auto;
}

.footer-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
}

.footer-section {
  display: flex;
  flex-direction: column;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 1rem;
  background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-icon {
  font-size: 2rem;
}

.brand-description {
  color: #a0a0a0;
  font-size: 0.95rem;
  line-height: 1.6;
  margin-bottom: 1.5rem;
}

.social-links {
  display: flex;
  gap: 1rem;
}

.social-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: rgba(0, 212, 255, 0.1);
  border: 1px solid rgba(0, 212, 255, 0.2);
  border-radius: 50%;
  color: #00d4ff;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.social-link:hover {
  background: rgba(0, 212, 255, 0.2);
  border-color: rgba(0, 212, 255, 0.4);
  transform: translateY(-3px);
}

.section-title {
  font-size: 1rem;
  font-weight: 700;
  color: #e0e0e0;
  margin-bottom: 1rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.link-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.link-list a {
  color: #a0a0a0;
  text-decoration: none;
  transition: all 0.3s ease;
  position: relative;
  padding-bottom: 0.25rem;
}

.link-list a::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 1px;
  background: #00d4ff;
  transition: width 0.3s ease;
}

.link-list a:hover {
  color: #00d4ff;
}

.link-list a:hover::after {
  width: 100%;
}

.footer-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 2rem;
  border-top: 1px solid rgba(0, 212, 255, 0.05);
  flex-wrap: wrap;
  gap: 1rem;
}

.footer-bottom-left,
.footer-bottom-right {
  color: #808080;
  font-size: 0.9rem;
}

.footer-link {
  color: #00d4ff;
  text-decoration: none;
  transition: all 0.3s ease;
}

.footer-link:hover {
  color: #00ffff;
}

.divider {
  color: #606060;
  margin: 0 0.75rem;
}

@media (max-width: 768px) {
  .footer {
    padding: 3rem 1rem 1.5rem;
  }

  .footer-content {
    grid-template-columns: 1fr;
    gap: 1.5rem;
  }

  .footer-bottom {
    flex-direction: column;
    text-align: center;
  }

  .footer-bottom-right {
    display: flex;
    justify-content: center;
    gap: 0;
  }
}
</style>
