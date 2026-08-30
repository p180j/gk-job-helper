<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { deleteImport, fetchImports } from '@/api/import'
import { showError } from '@/api/http'
import { fetchMatchProgress, type MatchProgress } from '@/api/match'
import type { RecentImport, UserProfile } from '@/types/model'

const router = useRouter()
const route = useRoute()
const profile = ref<UserProfile | null>(null)
const records = ref<RecentImport[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(true)
const matchProgress = ref<MatchProgress | null>(null)
let progressTimer: number | undefined
const profileSummary = computed(() => profile.value ? `${profile.value.education ?? '学历未填'} · ${profile.value.major ?? '专业未填'} · ${profile.value.birthDate ? `${new Date().getFullYear() - new Date(profile.value.birthDate).getFullYear()}岁` : '年龄未填'}` : '')
const matchingImportId = computed(() => {
  const importId = Number(route.query.matching)
  return Number.isFinite(importId) && importId > 0 ? importId : null
})
const matching = computed(() => matchingImportId.value !== null
  && matchProgress.value?.status !== 'COMPLETED'
  && matchProgress.value?.status !== 'FAILED')
const matchingRecord = computed(() => records.value.find(record => record.importId === matchingImportId.value))

onMounted(async () => {
  try { profile.value = await fetchProfile() } catch (error) { if (!isProfileNotFound(error)) showError(error, '读取个人档案失败。') }
  await loadRecords()
  const importId = Number(route.query.matching)
  if (importId && profile.value) startProgressPolling(importId, profile.value.id)
  loading.value = false
})

onBeforeUnmount(() => window.clearInterval(progressTimer))

async function loadRecords() {
  try { const data = await fetchImports(page.value, pageSize); records.value = data.items; total.value = data.total } catch (error) { showError(error, '读取匹配记录失败。') }
}

async function removeRecord(record: RecentImport) {
  if (isMatchingRecord(record)) return
  try {
    await ElMessageBox.confirm(
      h('div', { class: 'delete-confirm-content' }, [
        h('p', { class: 'delete-confirm-question' }, '确定删除这条匹配记录吗？'),
        h('div', { class: 'delete-confirm-file' }, record.fileName),
        h('p', { class: 'delete-confirm-tip' }, '该职位表、岗位和匹配结果将一并删除，删除后无法恢复。'),
      ]),
      '删除匹配记录',
      {
        type: 'warning',
        center: true,
        customClass: 'delete-confirm-dialog',
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'delete-confirm-button',
        closeOnClickModal: false,
        closeOnPressEscape: true,
      },
    )
  } catch {
    return
  }
  try { await deleteImport(record.importId); if (records.value.length === 1 && page.value > 1) page.value--; await loadRecords() } catch (error) { showError(error, '删除匹配记录失败。') }
}

function openRecord(record: RecentImport) {
  if (isStartButtonDisabled(record)) return
  if (record.status !== 'IMPORTED') { router.push(`/import/${record.importId}/mapping`); return }
  router.push(record.matchStats.total ? `/results/${record.importId}` : `/import/${record.importId}/execute`)
}

function isStartButton(record: RecentImport) {
  return record.status === 'IMPORTED' && !record.matchStats.total
}

function isStartButtonDisabled(record: RecentImport) {
  return matching.value && isStartButton(record)
}

function isMatchingRecord(record: RecentImport) {
  return matching.value && record.importId === matchingImportId.value
}

function rowClassName({ row }: { row: RecentImport }) {
  return isMatchingRecord(row) ? 'matching-row' : ''
}

function changePage(value: number) {
  page.value = value
  loadRecords()
}

function startProgressPolling(importId: number, profileId: number) {
  const refresh = async () => {
    try {
      matchProgress.value = await fetchMatchProgress(profileId, importId)
      if (!matchProgress.value || matchProgress.value.status === 'FEATURE_BUILDING' || matchProgress.value.status === 'MATCHING') return
      window.clearInterval(progressTimer)
      await loadRecords()
    } catch (error) { showError(error, '读取匹配进度失败。'); window.clearInterval(progressTimer) }
  }
  refresh()
  progressTimer = window.setInterval(refresh, 800)
}

function openProgressResult() {
  const importId = matchingImportId.value
  if (importId) router.push(`/results/${importId}`)
}
</script>

<template>
  <section v-loading="loading">
    <div class="hero">
      <div><h1>公考智能选岗助手</h1><p>上传官方职位表，自动筛出你能报的岗位。</p></div>
      <el-button type="primary" size="large" :icon="UploadFilled" @click="router.push('/import')">上传职位表</el-button>
    </div>
    <div class="page-card profile-card"><h2 class="page-title">我的报考档案</h2><template v-if="profile"><p class="profile-main">{{ profileSummary }}</p><p class="muted">{{ profile.politicalStatus ?? '政治面貌未填' }} · {{ profile.workYears ?? 0 }}年基层工作经历</p><el-button @click="router.push('/profile')">编辑档案</el-button></template><el-empty v-else description="请先完善报考档案" :image-size="72"><el-button type="primary" @click="router.push('/profile')">立即创建</el-button></el-empty></div>
    <div class="page-card recent-card">
      <h2 class="page-title">我的匹配记录</h2>
      <p class="page-subtitle">可查看、继续导入或删除指定职位表记录。</p>
      <div v-if="matchProgress" class="progress-panel">
        <template v-if="matchProgress.status === 'FEATURE_BUILDING'">
          <div class="progress-title">
            <strong>正在构建岗位特征：{{ matchingRecord?.fileName || `记录 #${matchingImportId}` }}</strong>
            <el-tag type="warning" effect="dark">准备匹配</el-tag>
          </div>
          <span class="progress-meta">正在识别考试科目和专业限制，完成后自动开始资格匹配。</span>
          <el-progress :percentage="100" :indeterminate="true" />
        </template>
        <template v-else-if="matchProgress.status === 'MATCHING'">
          <div class="progress-title">
            <strong>正在匹配：{{ matchingRecord?.fileName || `记录 #${matchingImportId}` }}</strong>
            <el-tag type="primary" effect="dark">匹配中</el-tag>
          </div>
          <span class="progress-meta">
            {{ matchingRecord?.createdAt ? `上传于 ${matchingRecord.createdAt.replace('T', ' ')} · ` : '' }}
            {{ matchProgress.processed }} / {{ matchProgress.total || '...' }} 个岗位
          </span>
          <el-progress :percentage="matchProgress.total ? Math.floor(matchProgress.processed * 100 / matchProgress.total) : 0" :indeterminate="!matchProgress.total" />
        </template>
        <template v-else-if="matchProgress.status === 'COMPLETED'">
          <strong>智能匹配完成：{{ matchingRecord?.fileName || `记录 #${matchingImportId}` }}</strong>
          <span>已处理 {{ matchProgress.processed }} 个岗位</span>
          <el-button type="primary" link @click="openProgressResult">查看结果</el-button>
        </template>
        <template v-else>
          <strong>匹配任务失败：{{ matchingRecord?.fileName || `记录 #${matchingImportId}` }}</strong>
          <span>{{ matchProgress.errorMessage || '请稍后重试。' }}</span>
        </template>
      </div>
      <template v-if="records.length">
        <el-table :data="records" border :row-class-name="rowClassName">
          <el-table-column label="职位表" min-width="330" show-overflow-tooltip>
            <template #default="{ row }">
              <span>{{ row.fileName }}</span>
              <el-tag v-if="isMatchingRecord(row)" class="row-matching-tag" size="small" type="primary">{{ matchProgress?.status === 'FEATURE_BUILDING' ? '构建特征' : '正在匹配' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="jobCount" label="岗位数量" width="100" />
          <el-table-column label="上传时间" width="180"><template #default="{ row }">{{ row.createdAt?.replace('T', ' ') || '-' }}</template></el-table-column>
          <el-table-column label="匹配统计" min-width="220"><template #default="{ row }"><span class="match-count success">可以报 {{ row.matchStats.match }}</span><span class="match-count warning">待确认 {{ row.matchStats.uncertain }}</span><span class="match-count danger">不符合 {{ row.matchStats.notMatch }}</span></template></el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="isMatchingRecord(row)" type="primary">{{ matchProgress?.status === 'FEATURE_BUILDING' ? '准备匹配' : '匹配中' }}</el-tag>
              <el-tag v-else :type="row.status === 'IMPORTED' ? 'success' : 'warning'">{{ row.status === 'IMPORTED' ? '已导入' : '待确认映射' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link :disabled="isStartButtonDisabled(row)" @click="openRecord(row)">
                {{ isMatchingRecord(row) ? (matchProgress?.status === 'FEATURE_BUILDING' ? '构建特征...' : '匹配中...') : row.status !== 'IMPORTED' ? '继续导入' : row.matchStats.total ? '查看结果' : '开始匹配' }}
              </el-button>
              <el-button type="danger" link :disabled="isMatchingRecord(row)" @click="removeRecord(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pager" v-if="total > pageSize"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" @current-change="changePage" /></div>
      </template>
      <el-empty v-else description="暂无职位表分析记录" />
    </div>
  </section>
</template>

<style scoped>
.hero { background: #fff; border-radius: 8px; padding: 30px 34px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; border-left: 4px solid #409eff; }.hero h1 { margin: 0 0 8px; font-size: 26px; }.hero p { margin: 0; color: #606266; }.profile-card { min-height: 175px; }.profile-main { font-size: 18px; margin: 28px 0 8px; }.recent-card { margin-top: 20px; }.progress-panel { margin: 20px 0 14px; padding: 16px 18px; border: 1px solid #a0cfff; border-radius: 8px; background: #ecf5ff; }.progress-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }.progress-meta { display: block; margin-bottom: 8px; color: #606266; }.row-matching-tag { margin-left: 10px; }.match-count { margin-right: 12px; white-space: nowrap; }.success { color: #67c23a; }.warning { color: #e6a23c; }.danger { color: #f56c6c; }.pager { margin-top: 20px; text-align: right; }
:deep(.el-table .matching-row > td.el-table__cell) { background: #ecf5ff !important; }
:global(.delete-confirm-dialog) { width: 430px; max-width: calc(100vw - 32px); padding: 22px 24px 18px; border-radius: 12px; }
:global(.delete-confirm-dialog .el-message-box__title) { font-size: 18px; font-weight: 600; color: #303133; }
:global(.delete-confirm-dialog .el-message-box__message) { width: 100%; }
:global(.delete-confirm-content) { width: 100%; text-align: left; }
:global(.delete-confirm-question) { margin: 0 0 12px; font-size: 15px; color: #303133; }
:global(.delete-confirm-file) { padding: 10px 12px; border-radius: 6px; background: #f5f7fa; color: #606266; line-height: 1.5; word-break: break-all; }
:global(.delete-confirm-tip) { margin: 12px 0 0; color: #f56c6c; font-size: 13px; }
:global(.delete-confirm-dialog .delete-confirm-button) { border-color: #f56c6c; background: #f56c6c; }
:global(.delete-confirm-dialog .delete-confirm-button:hover) { border-color: #f78989; background: #f78989; }
</style>
