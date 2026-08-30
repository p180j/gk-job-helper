<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { importInterviewScores } from '@/api/recommendations'
import { fetchImports } from '@/api/import'
import { showError } from '@/api/http'
import type { RecentImport } from '@/types/model'

const route=useRoute(),router=useRouter()
const importId=Number(route.query.importId)||0
const routeYear=Number(route.query.examYear)||undefined
const currentImport=ref<RecentImport>()
const file=ref<File>(),loading=ref(false)
const examYear=computed(()=>currentImport.value?.examYear||routeYear)

onMounted(async()=>{
  if(!importId){ElMessage.warning('请先在为我优选中选择当前岗位表');await router.replace('/recommend');return}
  try{
    const records=await fetchImports(1,100)
    currentImport.value=records.items.find(item=>item.importId===importId)
    if(!currentImport.value)throw new Error('当前岗位表不存在或不在最近记录中')
  }catch(e){showError(e,'读取当前岗位表失败。')}
})

function choose(e:Event){file.value=(e.target as HTMLInputElement).files?.[0]}
async function submit(){
  if(!file.value){ElMessage.warning('请选择进面名单 Excel');return}
  loading.value=true
  try{
    const result=await importInterviewScores(file.value,importId)
    ElMessage.success(`导入完成：新增 ${result.insertedCount}，覆盖 ${result.updatedCount}，未关联 ${result.unlinkedPositionCount}`)
    await router.push({path:'/recommend',query:{importId:String(importId)}})
  }catch(e){showError(e,'进面名单导入失败。')}finally{loading.value=false}
}
</script>

<template><section><div class="page-card"><h1 class="page-title">导入进面名单</h1><p class="page-subtitle">本次数据归属当前岗位表对应年度；按年度和职位代码全局保存，同年同职位代码会更新。</p><el-alert v-if="currentImport" type="info" :closable="false" show-icon><template #title>当前岗位表：{{ currentImport.fileName }}；考试年度：{{ examYear }}。导入后返回该岗位表的“为我优选”。</template></el-alert><div class="upload-row"><input type="file" accept=".xls,.xlsx" @change="choose"/><el-button type="primary" :loading="loading" :disabled="!currentImport" @click="submit">开始导入</el-button></div></div></section></template>

<style scoped>.el-alert{margin:18px 0}.upload-row{display:flex;gap:16px;align-items:center}</style>
