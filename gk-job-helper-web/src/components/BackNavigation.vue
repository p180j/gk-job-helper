<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import type { RouteLocationRaw } from 'vue-router'
import { useRouter } from 'vue-router'
const props = defineProps<{ text: string; to: RouteLocationRaw }>()
const router = useRouter()
</script>
<template><button type="button" class="back-navigation" @click="router.push(props.to)"><el-icon><ArrowLeft /></el-icon><span class="back-navigation-text">{{ text }}</span></button></template>
<style scoped>.back-navigation{display:inline-flex;align-items:center;gap:6px;height:32px;padding:0;border:0;background:transparent;color:var(--el-text-color-secondary);font-size:14px;font-weight:400;cursor:pointer;transition:color .2s ease,transform .2s ease}.back-navigation:hover{color:var(--el-color-primary);transform:translateX(-2px)}.back-navigation :deep(.el-icon){font-size:16px}@media(max-width:768px){.back-navigation-text{font-size:0}.back-navigation-text::after{content:'返回';font-size:14px}}</style>
