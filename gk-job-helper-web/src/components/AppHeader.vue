<script setup lang="ts">
import { Document, HomeFilled, UserFilled } from '@element-plus/icons-vue'
</script>

<template>
  <header class="app-header">
    <router-link class="brand" to="/">公考智能选岗助手</router-link>
    <nav>
      <el-menu mode="horizontal" :ellipsis="false" router :default-active="$route.path">
        <el-menu-item index="/"><el-icon><HomeFilled /></el-icon>首页</el-menu-item>
        <el-menu-item index="/profile"><el-icon><UserFilled /></el-icon>我的档案</el-menu-item>
        <el-menu-item index="/import"><el-icon><Document /></el-icon>职位表</el-menu-item>
      </el-menu>
    </nav>
  </header>
</template>

<style scoped>
.app-header { height: 64px; background: #fff; border-bottom: 1px solid #ebeef5; display: flex; align-items: center; padding: 0 max(24px, calc((100% - 1200px) / 2)); gap: 38px; }
.brand { color: #1f5fae; font-size: 18px; font-weight: 700; text-decoration: none; white-space: nowrap; }
.el-menu { border-bottom: 0; }
</style>
