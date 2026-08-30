<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchHistoricalAnalysis, fetchJob, fetchJobFeature } from '@/api/jobs'
import { fetchJobMatch } from '@/api/match'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { showError } from '@/api/http'
import { CONDITION_LABELS, matchStatus } from '@/utils/matchStatus'
import type { HistoricalAnalysis, JobFeature, JobPosition, MatchItem, MatchResult } from '@/types/model'

interface DetailItem {
  label: string
  value: string | number | null | undefined
  emptyText: string
  requirement?: boolean
  span?: number
}

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const job = ref<JobPosition>()
const match = ref<MatchResult>()
const feature = ref<JobFeature | null>()
const historical = ref<HistoricalAnalysis>()
const loading = ref(true)
const returnSource = computed(() => String(route.query.from || ''))

const rawFields = computed<Record<string, unknown>>(() => {
  if (!job.value?.rawData) return {}
  try {
    return JSON.parse(job.value.rawData) as Record<string, unknown>
  } catch {
    return {}
  }
})

const contactPhone = computed(() => {
  const fields = rawFields.value
  const value = fields['联系电话'] ?? fields['咨询电话'] ?? fields['联系电话（政策咨询）']
  return value == null ? '' : String(value).trim()
})

const info = computed<DetailItem[]>(() => {
  if (!job.value) return []
  const current = job.value
  const region = [current.province, current.city, current.district].filter(Boolean).join(' ')
  return [
    { label: '招录单位', value: current.departmentName, emptyText: '-' },
    ...(hasValue(current.organizationName)
      ? [{ label: '用人单位', value: current.organizationName, emptyText: '-' }]
      : []),
    { label: '联系电话', value: contactPhone.value, emptyText: '-' },
    { label: '岗位名称', value: current.positionName, emptyText: '-' },
    { label: '岗位代码', value: current.positionCode, emptyText: '-' },
    ...(hasValue(region) ? [{ label: '地区', value: region, emptyText: '-' }] : []),
    { label: '招录人数', value: current.recruitCount == null ? null : `${current.recruitCount} 人`, emptyText: '-' },
    { label: '考试科目', value: feature.value?.examSubjects?.join(' + '), emptyText: '暂未识别', span: 2 },
    { label: '学历', value: current.educationRequirement, emptyText: '无要求', requirement: true },
    { label: '学位', value: current.degreeRequirement, emptyText: '无要求', requirement: true },
    { label: '专业', value: current.majorRequirement, emptyText: '无要求', requirement: true, span: 2 },
    { label: '年龄', value: current.ageRequirement, emptyText: '无要求', requirement: true },
    { label: '政治面貌', value: current.politicalRequirement, emptyText: '无要求', requirement: true },
    { label: '基层工作经历', value: current.workYearRequirement, emptyText: '无要求', requirement: true },
    { label: '应届要求', value: current.freshGraduateRequirement, emptyText: '无要求', requirement: true },
    { label: '户籍', value: current.householdRequirement, emptyText: '无要求', requirement: true },
    { label: '服务基层项目', value: current.serviceProjectRequirement, emptyText: '无要求', requirement: true },
    { label: '资格证书', value: current.certificateRequirement, emptyText: '无要求', requirement: true },
    { label: '性别', value: current.genderRequirement, emptyText: '无要求', requirement: true },
    { label: '职位描述', value: current.positionDescription, emptyText: '未提供', span: 2 },
  ]
})

onMounted(async () => {
  try {
    const profile = await fetchProfile()
    if (!profile) return
    ;[job.value, match.value, feature.value, historical.value] = await Promise.all([fetchJob(id), fetchJobMatch(id, profile.id), fetchJobFeature(id), fetchHistoricalAnalysis(id, 2026)])
  } catch (error) {
    if (isProfileNotFound(error)) router.replace('/profile')
    else showError(error, '读取岗位详情失败。')
  } finally {
    loading.value = false
  }
})

function backToResults() {
  if (returnSource.value === 'recommend') {
    router.push({ path: '/recommend', query: route.query.importId ? { importId: String(route.query.importId) } : {} })
    return
  }
  if (returnSource.value === 'favorites') {
    router.push('/favorites')
    return
  }
  const importId = String(route.query.importId || '')
  if (importId) {
    router.push({
      path: `/results/${importId}`,
      query: route.query.result ? { result: String(route.query.result) } : {},
    })
    return
  }
  router.push('/')
}

function hasValue(value: unknown) {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function displayValue(item: DetailItem) {
  return hasValue(item.value) ? String(item.value).trim() : item.emptyText
}

function hasNoRequirement(item: MatchItem) {
  return !hasValue(item.requirementValue)
}

function itemStatusLabel(item: MatchItem) {
  return hasNoRequirement(item) ? '无要求' : matchStatus(item.result).detailLabel
}

function itemStatusType(item: MatchItem) {
  return hasNoRequirement(item) ? 'success' : matchStatus(item.result).type
}

function userValueText(item: MatchItem) {
  return hasNoRequirement(item) ? '无需核验' : item.userValue || '未填写'
}

function requirementText(item: MatchItem) {
  return hasNoRequirement(item) ? '无要求' : item.requirementValue
}

function evidenceText(item: MatchItem) {
  const evidence = item.evidence
  if (!evidence) return ''
  const catalog = evidence.catalogName ? `判断依据：${evidence.catalogName}` : ''
  const relation = evidence.majorName && evidence.parentName
    ? `${evidence.majorName}${evidence.majorCode ? `（${evidence.majorCode}）` : ''} 属于 ${evidence.parentName}${evidence.parentCode ? `（${evidence.parentCode}）` : ''}`
    : ''
  return [catalog, relation].filter(Boolean).join('；')
}

function relativeText(value: string | null | undefined) {
  return ({ LOWER: '较低', LOWER_MIDDLE: '中低', UPPER_MIDDLE: '中高', HIGHER: '较高' } as Record<string, string>)[value || ''] || '暂无可靠分析'
}
</script>

<template>
  <section v-loading="loading">
    <div v-if="job" class="page-card">
      <button type="button" class="back-nav" @click="backToResults">
        <span class="back-nav-icon"><el-icon><ArrowLeft /></el-icon></span>
        <span>返回</span>
      </button>

      <div class="title-row">
        <div>
          <h1 class="page-title">{{ job.positionName || '岗位详情' }}</h1>
          <p class="page-subtitle">{{ job.departmentName || '-' }} · 岗位代码：{{ job.positionCode || '-' }}</p>
        </div>
        <el-tag v-if="match" size="large" :type="matchStatus(match.result).type">
          {{ matchStatus(match.result).label }}
        </el-tag>
      </div>

      <el-descriptions :column="2" border :label-width="116" class="job-descriptions">
        <el-descriptions-item
          v-for="item in info"
          :key="item.label"
          :label="item.label"
          :span="item.span || 1"
        >
          <span
            class="description-value"
            :class="{ 'no-requirement': item.requirement && !hasValue(item.value) }"
          >{{ displayValue(item) }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="remark-block" :class="{ empty: !hasValue(job.remark) }">
        <div class="remark-title">备注</div>
        <div class="remark-content">{{ hasValue(job.remark) ? job.remark : '暂无备注' }}</div>
      </div>

      <div class="interview-reference">
        <h2>进面参考</h2>
        <template v-if="historical?.available">
          <div class="reference-badges"><el-tag v-if="historical.minInterviewScore != null" type="warning" size="large">{{ historical.examYear }}最低进面{{ historical.minInterviewScore }}</el-tag><el-tag type="primary" size="large">同类平均分 {{ historical.sampleAverageScore }}</el-tag><el-tag v-if="historical.percentile != null" size="large">同类进面位置{{ relativeText(historical.relativeLevel) }}</el-tag></div>
          <p>{{ historical.comparisonDescription }}；比较层级 {{ historical.comparisonLevel || '-' }}；可信度 {{ historical.confidence }}。</p>
          <el-alert v-if="!historical.reliable" :closable="false" type="info" title="同类岗位样本不足，当前结果仅展示，不参与为我优选排序。" />
        </template>
        <el-empty v-else :image-size="64" description="暂无可靠进面数据" />
      </div>
    </div>

    <div v-if="match" class="page-card match-panel">
      <h2 class="page-title">
        为什么我{{ match.result === 'MATCH' ? '能报' : match.result === 'NOT_MATCH' ? '不能报' : '需要确认' }}
      </h2>
      <p class="page-subtitle">
        综合判断：<el-tag :type="matchStatus(match.result).type">{{ matchStatus(match.result).detailLabel }}</el-tag>
      </p>

      <div v-for="item in match.items" :key="item.conditionType" class="match-item">
        <div class="item-title">
          <strong>{{ CONDITION_LABELS[item.conditionType] || item.conditionType }}</strong>
          <el-tag :type="itemStatusType(item)">{{ itemStatusLabel(item) }}</el-tag>
        </div>
        <div class="match-copy">
          <p><span>你的条件</span>{{ userValueText(item) }}</p>
          <p><span>岗位要求</span>{{ requirementText(item) }}</p>
          <p><span>判断说明</span>{{ item.reason || '暂无说明' }}</p>
        </div>
        <p v-if="evidenceText(item)" class="evidence">{{ evidenceText(item) }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.back-nav {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #606266;
  font: inherit;
  cursor: pointer;
  transition: color 0.2s ease;
}

.back-nav-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f2f6fc;
  color: #409eff;
  transition: background 0.2s ease, transform 0.2s ease;
}

.back-nav:hover {
  color: #409eff;
}

.back-nav:hover .back-nav-icon {
  background: #e8f3ff;
  transform: translateX(-2px);
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.job-descriptions {
  margin-top: 4px;
}

.job-descriptions :deep(.el-descriptions__label) {
  box-sizing: border-box;
  width: 116px;
  min-width: 116px;
  white-space: nowrap;
}

.job-descriptions :deep(.el-descriptions__content) {
  min-width: 220px;
}

.description-value {
  white-space: pre-wrap;
  line-height: 1.7;
}

.no-requirement {
  color: #67c23a;
  font-weight: 500;
}

.remark-block {
  display: grid;
  grid-template-columns: 72px 1fr;
  gap: 16px;
  margin-top: 18px;
  padding: 16px 18px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
}

.remark-block.empty {
  border-color: #ebeef5;
  background: #fafafa;
}

.remark-title {
  color: #b88230;
  font-weight: 600;
}

.remark-block.empty .remark-title,
.remark-block.empty .remark-content {
  color: #909399;
}

.remark-content {
  color: #7a4f01;
  line-height: 1.7;
  white-space: pre-wrap;
}

.match-panel {
  margin-top: 20px;
}
.interview-reference{margin-top:18px;padding:18px;border:1px solid #ebeef5;border-radius:8px}.interview-reference h2{font-size:16px;margin:0 0 12px}.interview-reference p{color:#606266;font-size:13px}.reference-badges{display:flex;gap:10px;flex-wrap:wrap}

.match-item {
  padding: 20px 0;
  border-bottom: 1px solid #ebeef5;
}

.match-item:last-child {
  border-bottom: 0;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.match-copy {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.match-copy p {
  display: grid;
  grid-template-columns: 88px 1fr;
  gap: 12px;
  margin: 0;
  color: #606266;
  line-height: 1.65;
}

.match-copy p span {
  color: #909399;
}

.evidence {
  margin: 10px 0 0;
  color: #409eff;
}
</style>
