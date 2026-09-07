<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import BackNavigation from '@/components/BackNavigation.vue'
import { extractRecruitmentBodyPositions, extractRecruitmentPositions, fetchRecruitmentNotice, fetchRecruitmentNoticeDetail } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentNotice } from '@/types/model'

const route = useRoute(), router = useRouter(), notice = ref<RecruitmentNotice>()
const loading = ref(false), fetching = ref(false), extracting = ref(false), id = Number(route.params.id)
const typeLabels: Record<string, string> = { POSITION_DATA: '岗位数据', APPLICATION_FORM: '报名材料', COMMITMENT: '承诺材料', GUIDE: '说明材料', QR_ATTACHMENT_HINT: '二维码附件提示', OTHER: '其他附件' }
const detailReady = computed(() => notice.value?.detailStatus === 'FETCHED')
const independentAttachments = computed(() => notice.value?.attachments?.filter(file => file.attachmentType !== 'QR_ATTACHMENT_HINT') ?? [])
const hasQrHint = computed(() => notice.value?.attachments?.some(file => file.attachmentType === 'QR_ATTACHMENT_HINT'))
const bodyHtml = computed(() => {
  const doc = new DOMParser().parseFromString(notice.value?.bodyHtml ?? '', 'text/html')
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
  doc.querySelectorAll('img').forEach(image => {
    const source = image.getAttribute('src') ?? ''
    if (source.startsWith(`/api/recruitment/notices/${id}/image?`)) image.src = base + source
    else image.removeAttribute('src')
  })
  return doc.body.innerHTML
})
function imageFailed(event: Event) {
  const image = event.target
  if (!(image instanceof HTMLImageElement)) return
  const hint = document.createElement('span')
  hint.className = 'image-fallback'
  hint.textContent = '图片暂时无法加载，可查看官方原文'
  image.replaceWith(hint)
}
async function load(){ loading.value=true;try{notice.value=await fetchRecruitmentNotice(id)}catch(e){showError(e,'读取公告详情失败。')}finally{loading.value=false} }
async function fetchDetail(){fetching.value=true;try{await fetchRecruitmentNoticeDetail(id);ElMessage.success('公告详情已获取');await load()}catch(e){showError(e,'获取公告详情失败。')}finally{fetching.value=false}}
async function extract(){extracting.value=true;try{const result=await extractRecruitmentPositions(id);ElMessage.success(`解析完成：${result.positionCount} 个岗位`);await load()}catch(e){showError(e,'招聘岗位解析失败。');await load()}finally{extracting.value=false}}
async function extractBody(){extracting.value=true;try{const result=await extractRecruitmentBodyPositions(id);ElMessage.success(`已从正文识别 ${result.positionCount} 个岗位`);router.push({name:'recruitment-position-list',params:{id}})}catch(e){showError(e,'正文岗位解析失败。')}finally{extracting.value=false}}
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
        <el-divider>公告正文</el-divider><div v-if="notice.bodyHtml" class="body" @error.capture="imageFailed" v-html="bodyHtml"/><pre v-else class="body-text">{{ notice.bodyText }}</pre>
        <div v-if="notice.bodyPositionHint === 'BODY_POSITION_CANDIDATE'" class="body-position-actions"><span class="meta">正文可能包含结构化岗位信息。</span><el-button type="primary" :loading="extracting" @click="extractBody">解析正文岗位</el-button></div>
        <el-divider>附件与招聘岗位</el-divider>
        <el-empty v-if="!independentAttachments.length" description="该公告未发现独立附件，岗位信息可能位于公告正文中。"/>
        <p v-if="hasQrHint" class="meta">该公告包含二维码入口，请扫码查看相关内容。</p>
        <div v-if="independentAttachments.length" class="attachments"><div v-for="file in independentAttachments" :key="file.id" class="attachment"><div><strong>{{ file.fileName }}</strong><p>{{ typeLabels[file.attachmentType] || '其他附件' }} · {{ file.fileType }}<span v-if="file.attachmentType==='POSITION_DATA'"> · {{ file.parseStatus || 'UNPARSED' }} · 已解析 {{ file.positionCount || 0 }} 个岗位</span></p></div><div class="attachment-actions"><el-button v-if="file.attachmentType==='POSITION_DATA'" type="primary" :loading="extracting" @click="extract">解析招聘岗位</el-button><el-button v-if="file.attachmentType==='POSITION_DATA'&&file.positionCount" @click="router.push({name:'recruitment-position-list',params:{id}})">查看岗位</el-button><el-button @click="open(file.fileUrl)">打开附件</el-button></div></div></div>
      </template>
      <el-divider/><div class="bottom-actions"><BackNavigation text="返回招聘发现" to="/recruitment"/><el-button type="primary" plain @click="open(notice.noticeUrl)">查看官方原文</el-button></div>
    </div>
  </section>
</template>
<style scoped>
.body :deep(img){max-width:100%;height:auto}
.body-position-actions{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:16px 0;flex-wrap:wrap}
.body :deep(.image-fallback){display:block;padding:12px;color:#606266;background:#f5f7fa}
</style>
<style scoped>.meta{color:#606266;font-size:14px}.empty{text-align:center;padding:50px 0}.body{line-height:1.8;word-break:break-word}.body :deep(table){max-width:100%;border-collapse:collapse}.body :deep(td),.body :deep(th){border:1px solid #dcdfe6;padding:8px}.body-text{white-space:pre-wrap;line-height:1.8}.attachments{display:grid;gap:10px}.attachment{border:1px solid #ebeef5;border-radius:6px;padding:14px;display:flex;justify-content:space-between;align-items:center;gap:16px}.attachment p{margin:6px 0 0;color:#606266;font-size:13px}.attachment-actions,.bottom-actions{display:flex;align-items:center;gap:8px;flex-wrap:wrap}.bottom-actions{gap:12px}</style>
