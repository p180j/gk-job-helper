<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { deleteImport, fetchImports } from '@/api/import'
import { showError } from '@/api/http'
import type { RecentImport, UserProfile } from '@/types/model'

const router = useRouter()
const profile = ref<UserProfile | null>(null)
const records = ref<RecentImport[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(true)
const profileSummary = computed(() => profile.value ? `${profile.value.education ?? '学历未填'} · ${profile.value.major ?? '专业未填'} · ${profile.value.birthDate ? `${new Date().getFullYear() - new Date(profile.value.birthDate).getFullYear()}岁` : '年龄未填'}` : '')

onMounted(async () => {
  try { profile.value = await fetchProfile() } catch (error) { if (!isProfileNotFound(error)) showError(error, '读取个人档案失败。') }
  await loadRecords()
  loading.value = false
})

async function loadRecords() {
  try { const data = await fetchImports(page.value, pageSize); records.value = data.items; total.value = data.total } catch (error) { showError(error, '读取匹配记录失败。') }
}

async function removeRecord(record: RecentImport) {
  if (!window.confirm(`确认删除“${record.fileName}”吗？该职位表、岗位和匹配结果将一并删除。`)) return
  try { await deleteImport(record.importId); if (records.value.length === 1 && page.value > 1) page.value--; await loadRecords() } catch (error) { showError(error, '删除匹配记录失败。') }
}

function openRecord(record: RecentImport) {
  if (record.status !== 'IMPORTED') { router.push(`/import/${record.importId}/mapping`); return }
  router.push(record.matchStats.total ? `/results/${record.importId}` : `/import/${record.importId}/execute`)
}

function changePage(value: number) {
  page.value = value
  loadRecords()
}
</script>

<template>
  <section v-loading="loading">
    <div class="hero">
      <div><h1>公考智能选岗助手</h1><p>上传官方职位表，自动筛出你能报的岗位。</p></div>
      <el-button type="primary" size="large" :icon="UploadFilled" @click="router.push('/import')">上传职位表</el-button>
    </div>
    <div class="page-card profile-card"><h2 class="page-title">我的报考档案</h2><template v-if="profile"><p class="profile-main">{{ profileSummary }}</p><p class="muted">{{ profile.politicalStatus ?? '政治面貌未填' }} · {{ profile.workYears ?? 0 }}年基层工作经历</p><el-button @click="router.push('/profile')">编辑档案</el-button></template><el-empty v-else description="请先完善报考档案" :image-size="72"><el-button type="primary" @click="router.push('/profile')">立即创建</el-button></el-empty></div>
    <div class="page-card recent-card"><h2 class="page-title">我的匹配记录</h2><p class="page-subtitle">可查看、继续导入或删除指定职位表记录。</p><template v-if="records.length"><el-table :data="records" border><el-table-column prop="fileName" label="职位表" min-width="330" show-overflow-tooltip /><el-table-column prop="jobCount" label="岗位数量" width="100" /><el-table-column label="上传时间" width="180"><template #default="{ row }">{{ row.createdAt?.replace('T', ' ') || '-' }}</template></el-table-column><el-table-column label="匹配统计" min-width="220"><template #default="{ row }"><span class="match-count success">可以报 {{ row.matchStats.match }}</span><span class="match-count warning">待确认 {{ row.matchStats.uncertain }}</span><span class="match-count danger">不符合 {{ row.matchStats.notMatch }}</span></template></el-table-column><el-table-column label="状态" width="120"><template #default="{ row }"><el-tag :type="row.status === 'IMPORTED' ? 'success' : 'warning'">{{ row.status === 'IMPORTED' ? '已导入' : '待确认映射' }}</el-tag></template></el-table-column><el-table-column label="操作" width="190" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openRecord(row)">{{ row.status !== 'IMPORTED' ? '继续导入' : row.matchStats.total ? '查看结果' : '开始匹配' }}</el-button><el-button type="danger" link @click="removeRecord(row)">删除</el-button></template></el-table-column></el-table><div class="pager" v-if="total > pageSize"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="page" @current-change="changePage" /></div></template><el-empty v-else description="暂无职位表分析记录" /></div>
  </section>
</template>

<style scoped>
.hero { background: #fff; border-radius: 8px; padding: 30px 34px; margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; border-left: 4px solid #409eff; }.hero h1 { margin: 0 0 8px; font-size: 26px; }.hero p { margin: 0; color: #606266; }.profile-card { min-height: 175px; }.profile-main { font-size: 18px; margin: 28px 0 8px; }.recent-card { margin-top: 20px; }.match-count { margin-right: 12px; white-space: nowrap; }.success { color: #67c23a; }.warning { color: #e6a23c; }.danger { color: #f56c6c; }.pager { margin-top: 20px; text-align: right; }
</style>
