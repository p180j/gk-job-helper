<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchProfile, isProfileNotFound } from '@/api/profile'
import { executeMatchAsync } from '@/api/match'
import { showError } from '@/api/http'
import { matchStatus } from '@/utils/matchStatus'
import type { MatchSummary } from '@/types/model'
const route = useRoute(); const router = useRouter(); const importId = Number(route.params.id); const loading = ref(true); const summary = ref<MatchSummary | null>(null)
const statuses = ['MATCH', 'UNCERTAIN', 'NOT_MATCH'] as const
onMounted(async () => { try { const profile = await fetchProfile(); if (!profile) return; await executeMatchAsync(profile.id, importId, new Date().toISOString().slice(0, 10)); router.replace({ path: '/', query: { matching: String(importId) } }) } catch (error) { if (isProfileNotFound(error)) router.replace('/profile'); else showError(error, '岗位匹配失败，请稍后重试。'); loading.value = false } })
function open(result?: 'MATCH' | 'UNCERTAIN' | 'NOT_MATCH') { router.push({ path: `/results/${importId}`, query: result ? { result } : {} }) }
</script>
<template><section class="page-card execute" v-loading="loading"><template v-if="summary"><el-result icon="success" title="智能匹配已完成" :sub-title="`已分析 ${summary.total} 个岗位`" /><el-row :gutter="20"><el-col v-for="key in statuses" :key="key" :span="8"><button class="stat-card" :class="key.toLowerCase()" @click="open(key)"><el-tag :type="matchStatus(key).type">{{ matchStatus(key).label }}</el-tag><strong>{{ key === 'MATCH' ? summary.match : key === 'UNCERTAIN' ? summary.uncertain : summary.notMatch }}</strong><span>{{ key }}</span></button></el-col></el-row><div class="actions"><el-button type="primary" @click="open()">查看全部岗位结果</el-button></div><el-alert v-if="summary.failedCount" type="warning" :title="`${summary.failedCount} 个岗位匹配失败，可重试后再次查看。`" show-icon /></template><p v-else-if="!loading" class="muted">暂无可展示的匹配结果。</p></section></template>
<style scoped>.execute{min-height:340px}.stat-card{width:100%;padding:24px;background:#fff;border:1px solid #ebeef5;border-radius:8px;cursor:pointer;display:flex;flex-direction:column;gap:10px;align-items:center}.stat-card:hover{border-color:#409eff}.stat-card strong{font-size:34px}.match strong{color:#67c23a}.uncertain strong{color:#e6a23c}.not_match strong{color:#f56c6c}.stat-card span{color:#909399}.actions{text-align:center;margin:28px 0}</style>
