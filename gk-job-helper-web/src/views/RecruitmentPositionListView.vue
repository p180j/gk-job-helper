<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute,useRouter } from 'vue-router'
import { fetchRecruitmentPositions } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentPosition } from '@/types/model'
const route=useRoute(),router=useRouter(),id=Number(route.params.id),loading=ref(false),rows=ref<RecruitmentPosition[]>([])
async function load(){loading.value=true;try{rows.value=await fetchRecruitmentPositions(id)}catch(e){showError(e,'读取招聘岗位失败。')}finally{loading.value=false}}
onMounted(load)
</script>
<template><section v-loading="loading"><el-button text @click="router.push({name:'recruitment-notice-detail',params:{id}})">← 返回公告详情</el-button><div class="page-card"><h1 class="page-title">招聘岗位</h1><p class="meta">共 {{rows.length}} 个已解析岗位</p><el-empty v-if="!rows.length&&!loading" description="尚未解析到招聘岗位"/><el-table v-else :data="rows" @row-click="(row:RecruitmentPosition)=>router.push({name:'recruitment-position-detail',params:{id:row.id}})" style="cursor:pointer"><el-table-column prop="positionName" label="岗位名称" min-width="180"/><el-table-column prop="organizationName" label="招聘单位" min-width="180"/><el-table-column prop="departmentName" label="部门" min-width="140"/><el-table-column prop="recruitCount" label="人数" width="80"/><el-table-column prop="educationRequirement" label="学历要求" min-width="130"/><el-table-column prop="majorRequirement" label="专业要求" min-width="200"/><el-table-column prop="workLocation" label="工作地点" min-width="120"/></el-table></div></section></template>
<style scoped>.meta{color:#606266;font-size:14px}</style>
