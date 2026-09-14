<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { createRecruitmentSource, deleteRecruitmentNotices, discoverRecruitment, fetchRecruitmentNotices, fetchRecruitmentSources, updateRecruitmentSourceEnabled, updateRecruitmentStatus } from '@/api/recruitment'
import { showError } from '@/api/http'
import type { RecruitmentNotice, RecruitmentNoticeStatus, RecruitmentSource } from '@/types/model'

type NoticeFilterStatus = RecruitmentNoticeStatus | 'UNREAD'

const router = useRouter()
const status = ref<NoticeFilterStatus | ''>('')
const keyword = ref('')
const days = ref(1)
const items = ref<RecruitmentNotice[]>([])
const selectedIds = ref<number[]>([])
const sources = ref<RecruitmentSource[]>([])
const configVisible = ref(false)
const configLoading = ref(false)
const sourceSaving = ref(false)
const sourceForm = reactive({ sourceName: '', listUrl: '' })
const total = ref(0)
const page = ref(1)
const loading = ref(false)
const pulling = ref(false)
const deleting = ref(false)
const labels: Record<RecruitmentNoticeStatus, string> = { NEW: '新发现', INTERESTED: '感兴趣', FOLLOWING: '正在看', IGNORED: '已忽略' }

onMounted(load)

async function load() {
  loading.value = true
  selectedIds.value = []
  try {
    const data = await fetchRecruitmentNotices({ status: status.value || undefined, keyword: keyword.value || undefined, page: page.value, pageSize: 20 })
    items.value = data.items
    total.value = data.total
  } catch (e) {
    showError(e, '读取招聘公告失败。')
  } finally {
    loading.value = false
  }
}

async function pull() {
  pulling.value = true
  try {
    const result = await discoverRecruitment(days.value)
    const range = days.value === 0 ? '当前首页' : `最近 ${days.value} 天`
    const summary = `拉取${range}完成：首页获取 ${result.fetchedCount} 条，时间范围跳过 ${result.filteredCount} 条，新增 ${result.newCount} 条，已存在 ${result.duplicateCount} 条。`
    result.failedCount > 0 ? ElMessage.warning(`${summary} ${result.failedCount} 个来源或公告处理失败。`) : ElMessage.success(summary)
    page.value = 1
    await load()
  } catch (e) {
    showError(e, '拉取招聘信息失败。')
  } finally {
    pulling.value = false
  }
}

async function openConfig() {
  configVisible.value = true
  configLoading.value = true
  try {
    sources.value = await fetchRecruitmentSources()
  } catch (e) {
    showError(e, '读取招聘配置失败。')
  } finally {
    configLoading.value = false
  }
}

async function addSource() {
  if (!sourceForm.sourceName.trim() || !sourceForm.listUrl.trim()) {
    ElMessage.warning('请填写网站名称和招聘公告列表地址。')
    return
  }
  sourceSaving.value = true
  try {
    const source = await createRecruitmentSource({ sourceName: sourceForm.sourceName.trim(), listUrl: sourceForm.listUrl.trim() })
    sources.value.push(source)
    sourceForm.sourceName = ''
    sourceForm.listUrl = ''
    ElMessage.success('招聘网站已添加并启用，下次拉取时将从该列表页识别公告。')
  } catch (e) {
    showError(e, '添加招聘网站失败。')
  } finally {
    sourceSaving.value = false
  }
}

async function toggleSource(source: RecruitmentSource, value: boolean) {
  try {
    await updateRecruitmentSourceEnabled(source.id, value)
    source.enabled = value
    ElMessage.success(value ? '已启用招聘网站' : '已停用招聘网站')
  } catch (e) {
    showError(e, '更新招聘网站失败。')
  }
}

async function setStatus(notice: RecruitmentNotice, nextStatus: RecruitmentNoticeStatus) {
  try {
    await updateRecruitmentStatus(notice.id, nextStatus)
    notice.userStatus = nextStatus
    ElMessage.success(`已标记为${labels[nextStatus]}`)
  } catch (e) {
    showError(e, '更新公告状态失败。')
  }
}

async function removeNotices(ids: number[]) {
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`将永久删除 ${ids.length} 篇公告，以及其已解析岗位、附件和匹配记录。此操作不可恢复。`, '确认删除', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
    deleting.value = true
    await deleteRecruitmentNotices(ids)
    ElMessage.success('已删除公告')
    if (items.value.length === ids.length && page.value > 1) page.value--
    await load()
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') showError(e, '删除公告失败。')
  } finally {
    deleting.value = false
  }
}

function noticeLabel(notice: RecruitmentNotice) { return notice.userStatus === 'NEW' ? (notice.viewedAt ? '已查阅' : '未查阅') : labels[notice.userStatus] }
function detail(notice: RecruitmentNotice) { router.push({ name: 'recruitment-notice-detail', params: { id: notice.id } }) }
function changeStatus(value: string | number) { status.value = (value || '') as NoticeFilterStatus | ''; page.value = 1; load() }
function search() { page.value = 1; load() }
function changePage(value: number) { page.value = value; load() }
</script>

<template>
  <section>
    <div class="page-card head">
      <div>
        <h1 class="page-title">招聘发现</h1>
        <p class="page-subtitle">从公开招聘网站发现最新公告，集中管理你的关注状态。</p>
      </div>
      <div class="pull-actions">
        <el-button plain @click="openConfig">招聘网站配置</el-button>
        <el-select v-model="days" aria-label="拉取范围">
          <el-option label="最近 1 天" :value="1" />
          <el-option label="最近 3 天" :value="3" />
          <el-option label="最近 7 天" :value="7" />
          <el-option label="当前首页全部" :value="0" />
        </el-select>
        <el-button type="primary" :loading="pulling" @click="pull">立即拉取</el-button>
      </div>
    </div>

    <div class="page-card toolbar">
      <el-tabs :model-value="status" @tab-change="changeStatus">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="未查阅" name="UNREAD" />
        <el-tab-pane label="感兴趣" name="INTERESTED" />
      </el-tabs>
      <el-input v-model="keyword" clearable placeholder="搜索公告标题" @keyup.enter="search">
        <template #append><el-button @click="search">搜索</el-button></template>
      </el-input>
    </div>

    <div v-loading="loading">
      <div v-if="items.length" class="batch-actions">
        <span>已选 {{ selectedIds.length }} 篇</span>
        <el-button type="danger" plain :disabled="!selectedIds.length" :loading="deleting" @click="removeNotices(selectedIds)">删除已选</el-button>
      </div>
      <el-empty v-if="!items.length && !loading" description="暂无招聘公告，点击“立即拉取”开始发现。" />
      <article v-for="notice in items" :key="notice.id" class="page-card notice">
        <el-checkbox v-model="selectedIds" :label="notice.id" :aria-label="`选择公告：${notice.title}`" />
        <div class="notice-main">
          <h2>{{ notice.title }}</h2>
          <p>{{ notice.sourceName }} · 发布时间：{{ notice.publishDate?.replace('T', ' ') || '未知' }} · <span :class="{ muted: notice.viewedAt }">{{ notice.viewedAt ? '已查看' : '未查看' }}</span></p>
        </div>
        <div class="actions">
          <el-tag>{{ noticeLabel(notice) }}</el-tag>
          <el-button @click="detail(notice)">详情</el-button>
          <el-button v-if="notice.userStatus !== 'INTERESTED'" @click="setStatus(notice, 'INTERESTED')">感兴趣</el-button>
          <el-button type="danger" text :loading="deleting" @click="removeNotices([notice.id])">删除</el-button>
        </div>
      </article>
      <div v-if="total" class="pager"><el-pagination background layout="total,prev,pager,next" :total="total" :page-size="20" :current-page="page" @current-change="changePage" /></div>
    </div>

    <el-dialog v-model="configVisible" title="招聘配置" width="620px">
      <p class="config-note">添加公开招聘网站后，系统会在下次拉取时读取该地址中的招聘公告链接；请填写包含招聘公告的列表页，而不是网站首页或单篇公告页。</p>
      <el-form class="source-form" label-position="top">
        <el-form-item label="网站名称">
          <el-input v-model="sourceForm.sourceName" maxlength="100" placeholder="例如：江西人事考试网" />
        </el-form-item>
        <el-form-item label="招聘公告列表地址">
          <el-input v-model="sourceForm.listUrl" placeholder="例如：https://example.gov.cn/recruitment" />
        </el-form-item>
        <div class="source-form-actions">
          <span>仅支持无需登录、无需验证码的公开网页。</span>
          <el-button type="primary" :loading="sourceSaving" @click="addSource">添加招聘网站</el-button>
        </div>
      </el-form>
      <el-divider>已接入网站</el-divider>
      <div v-loading="configLoading">
        <el-empty v-if="!sources.length && !configLoading" description="暂无已接入招聘网站" />
        <div v-for="source in sources" :key="source.id" class="source-row">
          <div>
            <strong>{{ source.sourceName }}</strong>
            <p>{{ source.listUrl }}<span v-if="source.lastFetchTime"> · 上次拉取：{{ source.lastFetchTime.replace('T', ' ') }}</span></p>
            <small v-if="source.lastFetchStatus">{{ source.lastFetchStatus }}</small>
          </div>
          <el-switch :model-value="source.enabled" active-text="启用" inactive-text="停用" @update:model-value="toggleSource(source, $event)" />
        </div>
      </div>
    </el-dialog>
  </section>
</template>

<style scoped>
.head,.pull-actions{display:flex;justify-content:space-between;align-items:center;gap:10px}.head{margin-bottom:16px}.pull-actions .el-select{width:150px}.toolbar{display:flex;align-items:center;gap:24px;margin-bottom:16px}.toolbar .el-tabs{flex:1}.toolbar .el-input{width:300px}.batch-actions{display:flex;align-items:center;justify-content:space-between;margin:0 0 10px;padding:0 4px;color:#606266;font-size:14px}.notice{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:12px}.notice-main{min-width:0;flex:1}.notice h2{font-size:17px;margin:0 0 10px}.notice p,.source-row p,.source-row small{margin:0;color:#606266;font-size:13px}.actions{display:flex;align-items:center;gap:8px;flex-wrap:wrap;justify-content:flex-end}.muted{color:#909399}.pager{text-align:right;margin:22px 0}.config-note{color:#606266;font-size:14px;margin-top:0;line-height:1.6}.source-form{padding:4px 0 10px}.source-form-actions{display:flex;align-items:center;justify-content:space-between;gap:12px;color:#909399;font-size:13px}.source-row{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:15px 0;border-top:1px solid #ebeef5}.source-row strong{display:block;margin-bottom:6px;word-break:break-all}@media(max-width:768px){.head,.toolbar{display:block}.pull-actions{margin-top:10px;flex-wrap:wrap}.toolbar .el-input{width:100%;margin-top:8px}.notice{align-items:flex-start}.actions{max-width:140px}.batch-actions{padding:0}.source-form-actions{align-items:flex-start;flex-direction:column}}
</style>
