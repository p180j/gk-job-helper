<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BackNavigation from '@/components/BackNavigation.vue'
import { extractRecruitmentPositions, fetchRecruitmentNotice, fetchRecruitmentNoticeDetail } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentNotice } from '@/types/model'

const route = useRoute(), router = useRouter(), notice = ref<RecruitmentNotice>()
const loading = ref(false), fetching = ref(false), extracting = ref(false), id = Number(route.params.id)
const typeLabels: Record<string, string> = { POSITION_DATA: '岗位数据', APPLICATION_FORM: '报名材料', COMMITMENT: '承诺材料', GUIDE: '说明材料', QR_ATTACHMENT_HINT: '二维码附件提示', OTHER: '其他附件' }
const detailReady = computed(() => notice.value?.detailStatus === 'FETCHED')
async function load(){ loading.value=true;try{notice.value=await fetchRecruitmentNotice(id)}catch(e){showError(e,'读取公告详情失败。')}finally{loading.value=false} }
async function fetchDetail(){fetching.value=true;try{await fetchRecruitmentNoticeDetail(id);ElMessage.success('公告详情已获取');await load()}catch(e){showError(e,'获取公告详情失败。')}finally{fetching.value=false}}
async function extract(){extracting.value=true;try{const result=await extractRecruitmentPositions(id);ElMessage.success(`解析完成：${result.positionCount} 个岗位`);await load()}catch(e){showError(e,'招聘岗位解析失败。');await load()}finally{extracting.value=false}}
function open(url:string){window.open(url,'_blank','noopener')}
onMounted(load)
</script>
<template>
  <section v-loading="loading">
    <BackNavigation text="返回招聘发现" to="/recruitment" />
    <div v-if="notice" class="page-card">
      <h1 class="page-title">{{ notice.title }}</h1>
      <p class="meta">{{ notice.sourceName }} · 发布时间：{{ notice.publishDate?.replace('T',' ') || '未知' }}</p>
      <p class="meta">详情状态：{{ notice.detailStatus || 'DISCOVERED' }}</p>
      <div v-if="!detailReady" class="empty"><p>公告详情尚未获取</p><el-button type="primary" :loading="fetching" @click="fetchDetail">获取公告详情</el-button></div>
      <template v-else>
        <el-divider>公告正文</el-divider><div v-if="notice.bodyHtml" class="body" v-html="notice.bodyHtml"/><pre v-else class="body-text">{{ notice.bodyText }}</pre>
        <el-divider>附件与招聘岗位</el-divider>
        <el-empty v-if="!notice.attachments?.length" description="该公告没有识别到附件"/>
        <div v-else class="attachments"><div v-for="file in notice.attachments" :key="file.id" class="attachment"><div><strong>{{ file.fileName }}</strong><p>{{ typeLabels[file.attachmentType] || '其他附件' }} · {{ file.fileType }}<span v-if="file.attachmentType==='POSITION_DATA'"> · {{ file.parseStatus || 'UNPARSED' }} · 已解析 {{ file.positionCount || 0 }} 个岗位</span></p></div><div class="attachment-actions"><el-button v-if="file.attachmentType==='POSITION_DATA'" type="primary" :loading="extracting" @click="extract">解析招聘岗位</el-button><el-button v-if="file.attachmentType==='POSITION_DATA'&&file.positionCount" @click="router.push({name:'recruitment-position-list',params:{id}})">查看岗位</el-button><el-button @click="open(file.fileUrl)">打开附件</el-button></div></div></div>
      </template>
      <el-divider/><div class="bottom-actions"><BackNavigation text="返回招聘发现" to="/recruitment"/><el-button type="primary" plain @click="open(notice.noticeUrl)">查看官方原文</el-button></div>
    </div>
  </section>
</template>
<style scoped>.meta{color:#606266;font-size:14px}.empty{text-align:center;padding:50px 0}.body{line-height:1.8;word-break:break-word}.body :deep(table){max-width:100%;border-collapse:collapse}.body :deep(td),.body :deep(th){border:1px solid #dcdfe6;padding:8px}.body-text{white-space:pre-wrap;line-height:1.8}.attachments{display:grid;gap:10px}.attachment{border:1px solid #ebeef5;border-radius:6px;padding:14px;display:flex;justify-content:space-between;align-items:center;gap:16px}.attachment p{margin:6px 0 0;color:#606266;font-size:13px}.attachment-actions,.bottom-actions{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.bottom-actions{gap:12px}</style>
