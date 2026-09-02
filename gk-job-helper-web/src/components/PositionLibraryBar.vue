<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { RecentImport } from '@/types/model'

const props = defineProps<{ active: 'all' | 'recommend' | 'favorites'; imports: RecentImport[]; importId?: number }>()
const emit = defineEmits<{ select: [value: number] }>()
const router = useRouter()
function changeTab(tab: string) {
  const path = tab === 'all' ? '/positions' : tab === 'recommend' ? '/recommend' : '/favorites'
  router.push({ path, query: props.importId ? { importId: String(props.importId) } : {} })
}
</script>
<template>
  <div class="page-card library-bar">
    <div class="library-title"><h1 class="page-title">岗位库</h1><el-button type="primary" @click="router.push('/import')">上传职位表</el-button></div>
    <div class="library-context"><span>当前职位表</span><el-select :model-value="importId" placeholder="请选择已导入职位表" @change="emit('select', Number($event))"><el-option v-for="item in imports" :key="item.importId" :label="`${item.fileName}${item.examYear ? `（${item.examYear}）` : ''}`" :value="item.importId" /></el-select></div>
    <el-tabs :model-value="active" @tab-change="changeTab"><el-tab-pane label="全部岗位" name="all" /><el-tab-pane label="为我优选" name="recommend" /><el-tab-pane label="我的收藏" name="favorites" /></el-tabs>
  </div>
</template>
<style scoped>.library-bar{margin-bottom:18px}.library-title,.library-context{display:flex;align-items:center;justify-content:space-between;gap:16px}.library-context{justify-content:flex-start;margin:18px 0 4px;font-weight:600}.library-context .el-select{width:min(720px,100%)}:deep(.el-tabs__header){margin-bottom:0}</style>
