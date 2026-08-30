<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { StarFilled } from '@element-plus/icons-vue'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { fetchFavorites, removeFavorite } from '@/api/favorites'
import { showError } from '@/api/http'
import { matchStatus } from '@/utils/matchStatus'
import type { MatchPositionResult } from '@/types/model'

const router = useRouter()
const profileId = ref<number>()
const items = ref<MatchPositionResult[]>([])
const total = ref(0)
const page = ref(1)
const size = 20
const loading = ref(false)
const selectedIds = ref<number[]>([])
const selectedCount = computed(() => selectedIds.value.length)

onMounted(async () => {
  try {
    const profile = await fetchProfile()
    if (!profile) return
    profileId.value = profile.id
    await load()
  } catch (error) {
    if (isProfileNotFound(error)) router.replace('/profile')
    else showError(error, '读取收藏岗位失败。')
  }
})

async function load() {
  if (!profileId.value) return
  loading.value = true
  try {
    const data = await fetchFavorites(profileId.value, page.value, size)
    items.value = data.items
    total.value = data.total
  } catch (error) { showError(error, '读取收藏岗位失败。') }
  finally { loading.value = false }
}

async function unfavorite(item: MatchPositionResult) {
  if (!profileId.value) return
  try {
    await removeFavorite(item.jobId, profileId.value)
    selectedIds.value = selectedIds.value.filter(id => id !== item.jobId)
    if (items.value.length === 1 && page.value > 1) page.value--
    await load()
    ElMessage.success('已取消收藏')
  } catch (error) { showError(error, '取消收藏失败。') }
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
  if (selectedCount.value < 2) return
  router.push({ path: '/compare', query: { jobIds: selectedIds.value.join(',') } })
}

function changePage(value: number) { page.value = value; load() }
</script>

<template>
  <section>
    <div class="page-card favorite-head"><h1 class="page-title">我的收藏</h1><p class="page-subtitle">收藏感兴趣的岗位，并选择 2—4 个进行横向对比。</p></div>
    <div v-loading="loading" class="favorite-list">
      <el-empty v-if="!items.length && !loading" description="暂无收藏岗位" />
      <article v-for="item in items" :key="item.jobId" class="favorite-card">
        <el-checkbox :model-value="selectedIds.includes(item.jobId)" :disabled="selectedCount >= 4 && !selectedIds.includes(item.jobId)" @change="onCompareChange(item.jobId, $event)" />
        <div class="favorite-main">
          <div class="title-line"><el-tag v-if="item.matchResult" :type="matchStatus(item.matchResult).type">{{ matchStatus(item.matchResult).label }}</el-tag><el-tag v-else type="info">尚未匹配</el-tag><h2>{{ item.positionName || '未命名岗位' }}</h2></div>
          <p>{{ item.departmentName || '-' }}<span v-if="item.organizationName"> · {{ item.organizationName }}</span></p>
          <div class="meta"><span>地区：{{ item.region || '-' }}</span><span>代码：{{ item.positionCode || '-' }}</span><span>招录：{{ item.recruitCount ?? '-' }}人</span><span>学历：{{ item.educationRequirement || '无要求' }}</span><span>专业：{{ item.majorRequirement || '无要求' }}</span></div>
        </div>
        <div class="actions"><el-button circle type="warning" title="取消收藏" @click="unfavorite(item)"><el-icon><StarFilled /></el-icon></el-button><el-button type="primary" plain @click="router.push({path:`/jobs/${item.jobId}`,query:{from:'favorites'}})">查看详情</el-button></div>
      </article>
      <div v-if="total" class="pager"><el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" :current-page="page" @current-change="changePage" /></div>
    </div>
    <div v-if="selectedCount" class="compare-bar"><span>已选择 <strong>{{ selectedCount }}</strong> 个岗位</span><el-button type="primary" :disabled="selectedCount < 2" @click="startCompare">开始对比</el-button></div>
  </section>
</template>

<style scoped>
.favorite-head{margin-bottom:18px}.favorite-list{min-height:260px;padding-bottom:64px}.favorite-card{display:flex;align-items:center;gap:16px;margin-bottom:14px;padding:20px 22px;border:1px solid #ebeef5;border-radius:10px;background:#fff}.favorite-main{min-width:0;flex:1}.title-line{display:flex;align-items:center;gap:10px}.title-line h2{margin:0;font-size:17px}.favorite-main p{margin:9px 0 14px;color:#606266}.meta{display:flex;flex-wrap:wrap;gap:12px 20px;color:#606266;font-size:13px}.actions{display:flex;align-items:center;white-space:nowrap}.pager{text-align:right;margin:24px 0}.compare-bar{position:fixed;z-index:20;left:50%;bottom:24px;transform:translateX(-50%);min-width:360px;padding:13px 18px;background:#303133;color:#fff;border-radius:10px;box-shadow:0 8px 26px rgba(0,0,0,.2);display:flex;align-items:center;justify-content:space-between;gap:24px}.compare-bar strong{color:#79bbff}
</style>
