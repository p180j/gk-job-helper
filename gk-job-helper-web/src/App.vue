<script setup lang="ts">
import AppHeader from '@/components/AppHeader.vue'
</script>

<template>
  <div class="app-shell">
    <AppHeader />
    <main class="app-main">
      <router-view />
    </main>
    <footer class="app-footer">
      公考智能选岗助手 · 本工具匹配结论仅供参考，最终以招录机关审核口径为准
    </footer>
  </div>
</template>

<style scoped>
.app-shell {
  position: relative;
  isolation: isolate;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-shell::before,
.app-shell::after {
  position: fixed;
  z-index: -1;
  content: '';
  pointer-events: none;
  border-radius: 50%;
  filter: blur(2px);
}

.app-shell::before {
  top: 62px;
  left: -190px;
  width: 620px;
  height: 620px;
  background: radial-gradient(circle, rgba(64, 158, 255, 0.17), transparent 68%);
}

.app-shell::after {
  right: -210px;
  bottom: -80px;
  width: 650px;
  height: 650px;
  background: radial-gradient(circle, rgba(83, 168, 255, 0.14), transparent 70%);
}

.app-main {
  flex: 1;
  width: 1200px;
  max-width: calc(100% - 48px);
  margin: 28px auto 0;
}

.app-footer {
  text-align: center;
  color: #8a97a8;
  font-size: 12px;
  padding: 30px 20px 26px;
}

@media (max-width: 1366px) {
  .app-main {
    width: 1280px;
    max-width: calc(100% - 32px);
  }
}
</style>
