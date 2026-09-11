<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import BackNavigation from '@/components/BackNavigation.vue'
import { fetchRecruitmentPosition, fetchRecruitmentPositionMatch, matchRecruitmentPosition } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentPosition, RecruitmentMatchResult } from '@/types/model'
const route=useRoute(),id=Number(route.params.id),loading=ref(false),position=ref<RecruitmentPosition>(),match=ref<RecruitmentMatchResult|null>(null)
const backTarget=computed(()=>route.query.from==='library'?'/positions/recruitment':position.value?{name:'recruitment-position-list',params:{id:position.value.noticeId}}:'/recruitment')
const backText=computed(()=>route.query.from==='library'?'返回招聘岗位':'返回岗位列表')
async function load(){loading.value=true;try{position.value=await fetchRecruitmentPosition(id);match.value=await fetchRecruitmentPositionMatch(id)}catch(e){showError(e,'读取招聘岗位详情失败。')}finally{loading.value=false}}
async function analyze(){loading.value=true;try{match.value=await matchRecruitmentPosition(id)}catch(e){showError(e,'资格分析失败。')}finally{loading.value=false}}
function open(url:string|null){if(url)window.open(url,'_blank','noopener')}
function label(value:string){return value==='MATCH'?'可以报':value==='UNCERTAIN'?'待确认':'不符合'}
function tag(value:string){return value==='MATCH'?'success':value==='UNCERTAIN'?'warning':'danger'}
function typeName(value:string){return ({EDUCATION:'学历',DEGREE:'学位',MAJOR:'专业',AGE:'年龄',WORK_EXPERIENCE:'工作经验',OTHER:'其他条件'} as Record<string,string>)[value]||value}
onMounted(load)
</script>
<template>
  <section v-loading="loading">
    <BackNavigation floating :text="backText" :to="backTarget"/>
    <div v-if="position" class="page-card">
      <div class="title-line"><h1 class="page-title">{{position.positionName}}</h1><el-tag v-if="match" :type="tag(match.result)" effect="dark">{{label(match.result)}}</el-tag><el-tag v-else type="info">未分析</el-tag></div>
      <el-button v-if="!match" type="primary" @click="analyze">分析资格</el-button>
      <template v-if="match"><el-divider>资格匹配</el-divider><p class="match-summary">{{match.summary}}</p><el-descriptions :column="1" border><el-descriptions-item v-for="item in match.items" :key="item.id" :label="typeName(item.requirementType)"><el-tag :type="tag(item.result)" effect="plain">{{label(item.result)}}</el-tag><p>岗位要求：{{item.positionRequirement}}</p><p>我的情况：{{item.userEvidence || '未提供'}}</p><p>{{item.reason}}</p></el-descriptions-item></el-descriptions></template>
      <el-divider>岗位信息</el-divider>
      <el-descriptions :column="2" border><el-descriptions-item v-if="position.organizationName" label="招聘单位">{{position.organizationName}}</el-descriptions-item><el-descriptions-item v-if="position.departmentName" label="部门">{{position.departmentName}}</el-descriptions-item><el-descriptions-item v-if="position.positionCode" label="岗位代码">{{position.positionCode}}</el-descriptions-item><el-descriptions-item v-if="position.recruitCount != null" label="招聘人数">{{position.recruitCount}}</el-descriptions-item><el-descriptions-item v-if="position.workLocation" label="工作地点">{{position.workLocation}}</el-descriptions-item><el-descriptions-item v-if="position.educationRequirement" label="学历">{{position.educationRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.degreeRequirement" label="学位">{{position.degreeRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.majorRequirement" label="专业（含本科/研究生层级）" :span="2">{{position.majorRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.ageRequirement" label="年龄">{{position.ageRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.workYearsRequirement" label="工作经验">{{position.workYearsRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.responsibility" label="岗位职责" :span="2">{{position.responsibility}}</el-descriptions-item><el-descriptions-item v-if="position.otherRequirement" label="资格/职称/任职要求" :span="2">{{position.otherRequirement}}</el-descriptions-item><el-descriptions-item v-if="position.preferredRequirement" label="优先条件" :span="2">{{position.preferredRequirement}}</el-descriptions-item></el-descriptions>
      <el-divider>来源</el-divider><el-descriptions :column="2" border><el-descriptions-item label="公告标题">{{position.noticeTitle || `公告 #${position.noticeId}`}}</el-descriptions-item><el-descriptions-item label="来源">{{position.sourceAttachmentId ? '公告附件' : '公告正文'}}</el-descriptions-item><el-descriptions-item label="原始位置" :span="2">{{position.sourceSheet}} 第 {{position.sourceRow}} 行</el-descriptions-item></el-descriptions><el-button v-if="position.noticeUrl" class="source-button" type="primary" plain @click="open(position.noticeUrl)">查看官方原文</el-button><template v-if="position.rawRequirement"><el-divider>原始岗位条件</el-divider><pre>{{position.rawRequirement}}</pre></template>
    </div>
  </section>
</template>
<style scoped>.title-line{display:flex;gap:12px;align-items:center}.source-button{margin-top:16px}.match-summary{color:#606266}.el-descriptions p{margin:6px 0;color:#606266}pre{white-space:pre-wrap;line-height:1.7}</style>
