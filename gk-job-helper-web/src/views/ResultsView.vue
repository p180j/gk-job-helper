<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { fetchMatchRegions, fetchMatchResults } from '@/api/match'
import { addFavorite, removeFavorite } from '@/api/favorites'
import { showError } from '@/api/http'
import { matchStatus } from '@/utils/matchStatus'
import type { MatchPositionResult, MatchResultValue } from '@/types/model'

type StatusFilter = 'ALL' | MatchResultValue
type RecruitFilter = 'ALL' | '1' | '2' | '3_PLUS'

const route = useRoute()
const router = useRouter()
const importId = Number(route.params.importId)
const profileId = ref<number>()
const total = ref(0)
const items = ref<MatchPositionResult[]>([])
const regions = ref<string[]>([])
const page = ref(1)
const size = 10
const loading = ref(false)
const selectedIds = ref<number[]>([])
const favoriteLoading = ref<number[]>([])
const initialStatus = String(route.query.result || 'MATCH') as StatusFilter

const filters = reactive({
  status: (['ALL', 'MATCH', 'UNCERTAIN', 'NOT_MATCH'].includes(initialStatus) ? initialStatus : 'MATCH') as StatusFilter,
  region: '',
  organizationKeyword: '',
  positionKeyword: '',
  recruitCount: 'ALL' as RecruitFilter,
  educationKeyword: '',
  majorKeyword: '',
})

const selectedCount = computed(() => selectedIds.value.length)

onMounted(async () => {
  try {
    const profile = await fetchProfile()
    if (!profile) return
    profileId.value = profile.id
    ;[regions.value] = await Promise.all([fetchMatchRegions(profile.id, importId), load()])
  } catch (error) {
    if (isProfileNotFound(error)) router.replace('/profile')
    else showError(error, '读取岗位匹配结果失败。')
  }
})

async function load() {
  if (!profileId.value) return
  loading.value = true
  try {
    const recruit = recruitRange(filters.recruitCount)
    const data = await fetchMatchResults({
      profileId: profileId.value, importId,
      status: filters.status === 'ALL' ? undefined : filters.status,
      region: filters.region || undefined,
      organizationKeyword: filters.organizationKeyword || undefined,
      positionKeyword: filters.positionKeyword || undefined,
      recruitCountMin: recruit.min, recruitCountMax: recruit.max,
      educationKeyword: filters.educationKeyword || undefined,
      majorKeyword: filters.majorKeyword || undefined,
      page: page.value, size,
    })
    total.value = data.total
    items.value = data.items
  } catch (error) {
    showError(error, '读取匹配结果失败。')
  } finally {
    loading.value = false
  }
}

function recruitRange(value: RecruitFilter) {
  if (value === '1') return { min: 1, max: 1 }
  if (value === '2') return { min: 2, max: 2 }
  if (value === '3_PLUS') return { min: 3, max: undefined }
  return { min: undefined, max: undefined }
}

function applyFilters() {
  page.value = 1
  router.replace({ query: filters.status === 'ALL' ? {} : { result: filters.status } })
  load()
}

function resetFilters() {
  Object.assign(filters, { status: 'ALL', region: '', organizationKeyword: '', positionKeyword: '', recruitCount: 'ALL', educationKeyword: '', majorKeyword: '' })
  applyFilters()
}

function changePage(value: number) { page.value = value; load() }

function openDetail(item: MatchPositionResult) {
  router.push({ path: `/jobs/${item.jobId}`, query: { importId: String(importId), ...(filters.status === 'ALL' ? {} : { result: filters.status }) } })
}

async function toggleFavorite(item: MatchPositionResult) {
  if (!profileId.value || favoriteLoading.value.includes(item.jobId)) return
  favoriteLoading.value.push(item.jobId)
  try {
    if (item.favorite) await removeFavorite(item.jobId, profileId.value)
    else await addFavorite(item.jobId, profileId.value)
    item.favorite = !item.favorite
    ElMessage.success(item.favorite ? '已收藏岗位' : '已取消收藏')
  } catch (error) {
    showError(error, '更新收藏状态失败。')
  } finally {
    favoriteLoading.value = favoriteLoading.value.filter(id => id !== item.jobId)
  }
}

function toggleCompare(jobId: number, checked: boolean) {
  if (checked) {
    if (selectedIds.value.length >= 4) { ElMessage.warning('最多同时对比 4 个岗位。'); return }
    if (!selectedIds.value.includes(jobId)) selectedIds.value.push(jobId)
  } else selectedIds.value = selectedIds.value.filter(id => id !== jobId)
}

function onCompareChange(jobId: number, value: string | number | boolean) {
  toggleCompare(jobId, Boolean(value))
}

function startCompare() {
  if (selectedIds.value.length < 2) { ElMessage.warning('请至少选择 2 个岗位。'); return }
  router.push({ path: '/compare', query: { jobIds: selectedIds.value.join(',') } })
}
function subjects(item: MatchPositionResult) { try { return item.examSubjectsJson ? (JSON.parse(item.examSubjectsJson) as string[]) : [] } catch { return [] } }
</script>

<template>
  <section>
    <div class="page-card result-head">
      <h1 class="page-title">岗位匹配结果</h1>
      <p class="page-subtitle">筛选由后端分页执行；匹配结论仅供报考前核验。</p>
      <el-form class="filter-form" label-position="top" @submit.prevent="applyFilters">
        <div class="filter-grid">
          <el-form-item label="匹配状态"><el-select v-model="filters.status"><el-option label="全部" value="ALL" /><el-option label="可以报" value="MATCH" /><el-option label="待确认" value="UNCERTAIN" /><el-option label="不符合" value="NOT_MATCH" /></el-select></el-form-item>
          <el-form-item label="地区"><el-select v-model="filters.region" clearable filterable placeholder="全部地区"><el-option label="全部" value="" /><el-option v-for="region in regions" :key="region" :label="region" :value="region" /></el-select></el-form-item>
          <el-form-item label="招录单位"><el-input v-model="filters.organizationKeyword" clearable placeholder="单位关键字" /></el-form-item>
          <el-form-item label="岗位名称"><el-input v-model="filters.positionKeyword" clearable placeholder="岗位关键字" /></el-form-item>
          <el-form-item label="招录人数"><el-select v-model="filters.recruitCount"><el-option label="全部" value="ALL" /><el-option label="1人" value="1" /><el-option label="2人" value="2" /><el-option label="3人及以上" value="3_PLUS" /></el-select></el-form-item>
          <el-form-item label="学历要求"><el-input v-model="filters.educationKeyword" clearable placeholder="如：本科" /></el-form-item>
          <el-form-item label="专业要求"><el-input v-model="filters.majorKeyword" clearable placeholder="如：计算机类" /></el-form-item>
        </div>
        <div class="filter-actions"><el-button @click="resetFilters">重置</el-button><el-button type="primary" @click="applyFilters">查询</el-button></div>
      </el-form>
    </div>

    <div v-loading="loading" class="jobs">
      <el-empty v-if="!items.length && !loading" description="当前筛选条件下没有岗位。" />
      <article v-for="item in items" :key="item.jobId" class="job-card">
        <el-checkbox class="compare-check" :model-value="selectedIds.includes(item.jobId)" :disabled="selectedCount >= 4 && !selectedIds.includes(item.jobId)" @change="onCompareChange(item.jobId, $event)" />
        <div class="job-main">
          <div class="job-title-line"><el-tag v-if="item.matchResult" :type="matchStatus(item.matchResult).type">{{ matchStatus(item.matchResult).label }}</el-tag><h2>{{ item.positionName || '未命名岗位' }}</h2></div>
          <p>{{ item.departmentName || '招录单位未填' }} <span v-if="item.organizationName">· {{ item.organizationName }}</span></p>
          <div class="job-meta"><span>岗位代码：{{ item.positionCode || '-' }}</span><span>地区：{{ item.region || '-' }}</span><span>招录：{{ item.recruitCount ?? '-' }}人</span><span>学历：{{ item.educationRequirement || '无要求' }}</span><span>专业：{{ item.majorRequirement || '无要求' }}</span></div>
          <div class="reference-line"><span>考试科目：{{ subjects(item).length ? subjects(item).join(' + ') : '暂未识别' }}</span><el-tag v-if="item.minInterviewScore != null" type="warning" effect="plain">2026最低进面{{ item.minInterviewScore }}</el-tag></div>
        </div>
        <div class="job-actions"><el-button circle :loading="favoriteLoading.includes(item.jobId)" :type="item.favorite ? 'warning' : 'default'" :title="item.favorite ? '取消收藏' : '收藏岗位'" @click="toggleFavorite(item)"><el-icon><StarFilled v-if="item.favorite" /><Star v-else /></el-icon></el-button><el-button type="primary" plain @click="openDetail(item)">查看详情</el-button></div>
      </article>
      <div v-if="total" class="pager"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="changePage" /></div>
    </div>

    <div v-if="selectedCount" class="compare-bar"><span>已选择 <strong>{{ selectedCount }}</strong> 个岗位（最多4个）</span><el-button type="primary" :disabled="selectedCount < 2" @click="startCompare">开始对比</el-button></div>
  </section>
</template>

<style scoped>
.result-head{margin-bottom:18px}.filter-form{margin-top:20px;padding-top:18px;border-top:1px solid #ebeef5}.filter-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:0 18px}.filter-actions{text-align:right}.jobs{min-height:260px;padding-bottom:64px}.job-card{position:relative;background:#fff;border-radius:10px;padding:20px 22px;margin-bottom:14px;display:flex;align-items:center;gap:16px;border:1px solid #ebeef5;transition:.2s}.job-card:hover{border-color:#a0cfff;box-shadow:0 5px 16px rgba(64,158,255,.08)}.compare-check{align-self:flex-start;margin-top:3px}.job-main{min-width:0;flex:1}.job-title-line{display:flex;align-items:center;gap:10px}.job-main h2{font-size:17px;margin:0}.job-main p{margin:9px 0 14px;color:#606266}.job-meta{display:flex;gap:12px 20px;flex-wrap:wrap;font-size:13px;color:#606266}.job-actions{display:flex;align-items:center;white-space:nowrap}.pager{text-align:right;margin:24px 0}.compare-bar{position:fixed;z-index:20;left:50%;bottom:24px;transform:translateX(-50%);min-width:380px;padding:13px 18px;background:#303133;color:#fff;border-radius:10px;box-shadow:0 8px 26px rgba(0,0,0,.2);display:flex;align-items:center;justify-content:space-between;gap:24px}.compare-bar strong{color:#79bbff}.el-form-item{margin-bottom:14px}@media(max-width:900px){.filter-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.job-card{align-items:flex-start}.job-actions{flex-direction:column;gap:8px}.compare-bar{min-width:calc(100vw - 32px)}}
.reference-line{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:12px;color:#606266;font-size:13px}
</style>
