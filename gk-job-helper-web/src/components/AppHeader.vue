<script setup lang="ts">
import { Document, HomeFilled, MagicStick, StarFilled, UserFilled } from '@element-plus/icons-vue'
</script>

<template>
  <header class="app-header">
    <router-link class="brand" to="/">公考智能选岗助手</router-link>
    <nav>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path">
        <el-menu-item index="/"><el-icon><HomeFilled /></el-icon>首页</el-menu-item>
        <el-menu-item index="/profile"><el-icon><UserFilled /></el-icon>我的档案</el-menu-item>
        <el-menu-item index="/import"><el-icon><Document /></el-icon>职位表</el-menu-item>
        <el-menu-item index="/favorites"><el-icon><StarFilled /></el-icon>我的收藏</el-menu-item>
        <el-menu-item index="/recommend"><el-icon><MagicStick /></el-icon>为我优选</el-menu-item>
      </el-menu>
    </nav>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  box-sizing: border-box;
  height: 64px;
  background: rgba(247, 251, 255, 0.94);
  border-bottom: 1px solid rgba(180, 208, 237, 0.92);
  box-shadow: 0 5px 22px rgba(38, 91, 148, 0.1);
  backdrop-filter: blur(16px) saturate(135%);
  display: flex;
  align-items: center;
  padding: 0 max(24px, calc((100% - 1200px) / 2));
  gap: 38px;
}
.brand {
  color: #1f5fae;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: .2px;
  text-decoration: none;
  white-space: nowrap;
}
.el-menu { border-bottom: 0; background: transparent; }
.el-menu-item { background: transparent !important; }
</style>
