<script setup lang="ts">
import { onMounted,ref } from 'vue'
import { useRoute,useRouter } from 'vue-router'
import { fetchRecruitmentPosition } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentPosition } from '@/types/model'
const route=useRoute(),router=useRouter(),id=Number(route.params.id),loading=ref(false),position=ref<RecruitmentPosition>()
async function load(){loading.value=true;try{position.value=await fetchRecruitmentPosition(id)}catch(e){showError(e,'读取招聘岗位详情失败。')}finally{loading.value=false}}onMounted(load)
</script>
<template><section v-loading="loading"><el-button text @click="router.push({name:'recruitment-position-list',params:{id:position?.noticeId}})">← 返回岗位列表</el-button><div v-if="position" class="page-card"><h1 class="page-title">{{position.positionName}}</h1><el-descriptions :column="2" border><el-descriptions-item label="招聘单位">{{position.organizationName||'-'}}</el-descriptions-item><el-descriptions-item label="部门">{{position.departmentName||'-'}}</el-descriptions-item><el-descriptions-item label="岗位代码">{{position.positionCode||'-'}}</el-descriptions-item><el-descriptions-item label="招聘人数">{{position.recruitCount??'-'}}</el-descriptions-item><el-descriptions-item label="工作地点">{{position.workLocation||'-'}}</el-descriptions-item><el-descriptions-item label="学历">{{position.educationRequirement||'-'}}</el-descriptions-item><el-descriptions-item label="专业" :span="2">{{position.majorRequirement||'-'}}</el-descriptions-item><el-descriptions-item label="岗位职责" :span="2">{{position.responsibility||'-'}}</el-descriptions-item><el-descriptions-item label="其他要求" :span="2">{{position.otherRequirement||'-'}}</el-descriptions-item><el-descriptions-item label="来源" :span="2">{{position.sourceSheet}} 第 {{position.sourceRow}} 行</el-descriptions-item></el-descriptions><el-divider>原始要求</el-divider><pre>{{position.rawRequirement||'-'}}</pre></div></section></template>
<style scoped>pre{white-space:pre-wrap;line-height:1.7}</style>
