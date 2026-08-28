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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-main {
  flex: 1;
  width: 1200px;
  max-width: calc(100% - 48px);
  margin: 24px auto 0;
}

.app-footer {
  text-align: center;
  color: #909399;
  font-size: 12px;
  padding: 24px 0;
}

@media (max-width: 1366px) {
  .app-main {
    width: 1280px;
    max-width: calc(100% - 32px);
  }
}
</style>
