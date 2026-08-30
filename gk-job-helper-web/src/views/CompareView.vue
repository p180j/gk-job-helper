<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { compareJobs } from '@/api/jobs'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { showError } from '@/api/http'
import { CONDITION_LABELS, matchStatus } from '@/utils/matchStatus'
import type { JobCompareItem, MatchItem } from '@/types/model'

interface CompareRow { key: string; label: string; value: (job: JobCompareItem) => string; matchType?: string }
const route = useRoute()
const router = useRouter()
const jobs = ref<JobCompareItem[]>([])
const loading = ref(true)

const rows: CompareRow[] = [
  { key: 'region', label: '地区', value: job => job.region || '-' },
  { key: 'organization', label: '招录单位', value: job => [job.departmentName, job.organizationName].filter(Boolean).join(' · ') || '-' },
  { key: 'position', label: '岗位名称', value: job => job.positionName || '-' },
  { key: 'code', label: '岗位代码', value: job => job.positionCode || '-' },
  { key: 'count', label: '招录人数', value: job => job.recruitCount == null ? '-' : `${job.recruitCount}人` },
  { key: 'education', label: '学历要求', value: job => job.educationRequirement || '无要求' },
  { key: 'major', label: '专业要求', value: job => job.majorRequirement || '无要求' },
  { key: 'age', label: '年龄要求', value: job => job.ageRequirement || '无要求' },
  { key: 'political', label: '政治面貌', value: job => job.politicalRequirement || '无要求' },
  { key: 'work', label: '基层工作经历', value: job => job.workYearRequirement || '无要求' },
  { key: 'fresh', label: '应届要求', value: job => job.freshGraduateRequirement || '无要求' },
  { key: 'other', label: '其他限制条件', value: job => job.otherRestrictions || '无要求' },
]

const conditionTypes = computed(() => {
  const result: string[] = []
  jobs.value.forEach(job => job.matchItems.forEach(item => { if (!result.includes(item.conditionType)) result.push(item.conditionType) }))
  return result
})

onMounted(async () => {
  try {
    const profile = await fetchProfile()
    if (!profile) return
    const jobIds = String(route.query.jobIds || '').split(',').map(Number).filter(id => Number.isFinite(id) && id > 0)
    jobs.value = await compareJobs(profile.id, jobIds)
  } catch (error) {
    if (isProfileNotFound(error)) router.replace('/profile')
    else showError(error, '读取岗位对比失败。')
  } finally { loading.value = false }
})

function isDifferent(row: CompareRow) { return new Set(jobs.value.map(job => row.value(job))).size > 1 }
function condition(job: JobCompareItem, type: string): MatchItem | undefined { return job.matchItems.find(item => item.conditionType === type) }
function conditionDifferent(type: string) { return new Set(jobs.value.map(job => condition(job, type)?.result || '-')).size > 1 }
</script>

<template>
  <section v-loading="loading" class="page-card compare-page">
    <div class="compare-head"><div><h1 class="page-title">岗位对比</h1><p class="page-subtitle">差异项已用浅黄色背景突出显示。</p></div><el-button @click="router.back()">返回</el-button></div>
    <el-empty v-if="!jobs.length && !loading" description="请选择 2—4 个岗位进行对比" />
    <div v-else class="table-wrap">
      <table class="compare-table">
        <thead><tr><th class="label-cell">对比项目</th><th v-for="job in jobs" :key="job.jobId"><strong>{{ job.positionName || '未命名岗位' }}</strong><small>{{ job.positionCode || '-' }}</small></th></tr></thead>
        <tbody>
          <tr v-for="row in rows" :key="row.key" :class="{ different: isDifferent(row) }"><th class="label-cell">{{ row.label }}</th><td v-for="job in jobs" :key="job.jobId">{{ row.value(job) }}</td></tr>
          <tr v-for="type in conditionTypes" :key="type" :class="{ different: conditionDifferent(type) }">
            <th class="label-cell">{{ CONDITION_LABELS[type] || type }}匹配</th>
            <td v-for="job in jobs" :key="job.jobId"><template v-if="condition(job,type)"><el-tag :type="matchStatus(condition(job,type)!.result).type">{{ matchStatus(condition(job,type)!.result).detailLabel }}</el-tag><p class="reason">{{ condition(job,type)!.reason || '-' }}</p></template><span v-else>-</span></td>
          </tr>
          <tr class="overall" :class="{ different: new Set(jobs.map(job => job.overallStatus)).size > 1 }"><th class="label-cell">综合结果</th><td v-for="job in jobs" :key="job.jobId"><el-tag size="large" :type="matchStatus(job.overallStatus).type">{{ matchStatus(job.overallStatus).label }}</el-tag></td></tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<style scoped>
.compare-page{min-height:360px}.compare-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:20px}.table-wrap{overflow:auto;border:1px solid #dcdfe6;border-radius:8px}.compare-table{width:100%;min-width:900px;border-collapse:collapse;table-layout:fixed}.compare-table th,.compare-table td{padding:14px 16px;border-right:1px solid #ebeef5;border-bottom:1px solid #ebeef5;vertical-align:top;text-align:left;line-height:1.55;word-break:break-word}.compare-table thead th{background:#f5f7fa;text-align:center}.compare-table thead strong,.compare-table thead small{display:block}.compare-table thead small{margin-top:5px;color:#909399;font-weight:400}.label-cell{width:150px;background:#fafafa;font-weight:600}.different th,.different td{background:#fdf6ec}.overall th,.overall td{padding-top:18px;padding-bottom:18px}.reason{margin:8px 0 0;color:#606266;font-size:12px}
</style>
