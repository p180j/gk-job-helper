<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmImport, fetchImportProgress, fetchMapping } from '@/api/import'
import { showError } from '@/api/http'
import { STANDARD_FIELDS } from '@/utils/fields'
import type { FieldMappingPreview, HeaderSuggestion, ImportProgress, ImportResult, MappingItem } from '@/types/model'

const route = useRoute(); const router = useRouter(); const importId = Number(route.params.id)
const data = ref<FieldMappingPreview | null>(null)
const sheetMappings = ref<Record<string, MappingItem[]>>({})
const sheetHeaders = ref<Record<string, HeaderSuggestion[]>>({})
const activeSheet = ref('')
const mappings = computed(() => sheetMappings.value[activeSheet.value] ?? [])
const headers = computed(() => sheetHeaders.value[activeSheet.value] ?? [])
const selectedSheetNames = ref<string[]>([]); const result = ref<ImportResult | null>(null); const progress = ref<ImportProgress | null>(null); const loading = ref(false)
const mappedNames = computed(() => new Set(mappings.value.map(item => item.targetField).filter(Boolean)))

async function loadMapping(sheetNames?: string[]) {
  const next = await fetchMapping(importId, sheetNames)
  data.value = next
  activeSheet.value = next.sheetName
  sheetHeaders.value[next.sheetName] = next.headers
  sheetMappings.value[next.sheetName] = next.headers.map(item => ({ sourceField: item.sourceField, targetField: item.suggestedField }))
}

onMounted(async () => {
  try {
    await loadMapping()
    selectedSheetNames.value = data.value?.sheets.filter(item => item.suggestedForImport).map(item => item.sheetName) ?? []
    if (!selectedSheetNames.value.length && data.value?.sheetName) selectedSheetNames.value = [data.value.sheetName]
    await refreshSheets()
  } catch (error) { showError(error, '读取字段映射失败。') }
})

async function refreshSheets() {
  if (!selectedSheetNames.value.length) { ElMessage.warning('请至少选择一个职位 Sheet。'); return }
  try {
    for (const sheetName of selectedSheetNames.value) {
      if (sheetMappings.value[sheetName]) continue
      const preview = await fetchMapping(importId, [sheetName])
      sheetHeaders.value[sheetName] = preview.headers
      sheetMappings.value[sheetName] = preview.headers.map(item => ({ sourceField: item.sourceField, targetField: item.suggestedField }))
    }
    if (!selectedSheetNames.value.includes(activeSheet.value)) activeSheet.value = selectedSheetNames.value[0]
  } catch (error) { showError(error, '读取所选 Sheet 的字段映射失败。') }
}
function optionsFor(item: MappingItem) { return STANDARD_FIELDS.filter(([key]) => key === item.targetField || !mappedNames.value.has(key)) }
async function submit() {
  if (!selectedSheetNames.value.length) { ElMessage.warning('请至少选择一个职位 Sheet。'); return }
  for (const sheetName of selectedSheetNames.value) {
    const targets = new Set(sheetMappings.value[sheetName]?.map(item => item.targetField))
    if (!targets.has('positionName') || !targets.has('positionCode') || !targets.has('departmentName') && !targets.has('departmentCode')) {
      ElMessage.warning(`${sheetName} 缺少职位名称、代码或招录机关映射。`); return
    }
  }
  loading.value = true
  try { await confirmImport(importId, mappings.value, selectedSheetNames.value, sheetMappings.value); await pollProgress() } catch (error) { showError(error, '职位表导入失败。') } finally { loading.value = false }
}
async function pollProgress() { for (;;) { progress.value = await fetchImportProgress(importId); if (progress.value.status === 'IMPORTED') { result.value = { importId, totalRows: progress.value.totalRows, successRows: progress.value.successRows, failedRows: progress.value.failedRows, failedItems: [] }; ElMessage.success('岗位导入完成。'); return }; if (progress.value.status === 'IMPORT_FAILED') throw new Error(progress.value.errorMessage || '后台导入失败'); await new Promise(resolve => window.setTimeout(resolve, 800)) } }
</script>
<template>
  <section class="page-card"><h1 class="page-title">选择职位 Tab 并确认字段映射</h1><p class="page-subtitle">可合并导入不同表头的多个职位 Tab；每条岗位会保留自己的来源 Tab 和行号。</p>
    <div v-if="data" class="sheet-picker"><div class="picker-title">选择要导入的 Sheet</div><el-checkbox-group v-model="selectedSheetNames" @change="refreshSheets"><el-checkbox v-for="sheet in data.sheets" :key="sheet.sheetName" :label="sheet.sheetName" border>{{ sheet.sheetName }}（{{ sheet.totalRows }} 行）<el-tag v-if="sheet.suggestedForImport" size="small" type="success">建议</el-tag><el-tag v-else-if="sheet.parseError" size="small" type="warning" :title="sheet.parseError">需核对表头</el-tag></el-checkbox></el-checkbox-group></div>
    <div v-if="selectedSheetNames.length" class="sheet-mapping-tabs"><span>当前编辑：</span><el-radio-group v-model="activeSheet" size="small"><el-radio-button v-for="name in selectedSheetNames" :key="name" :label="name">{{ name }}</el-radio-button></el-radio-group></div>
    <p class="page-subtitle">逐个确认所选 Sheet 的字段映射；职位名称、职位代码和招录机关为关键字段。</p>
    <el-table v-if="data" :data="headers" border><el-table-column type="index" width="58" label="#" /><el-table-column label="Excel 原始字段" min-width="260"><template #default="{ row }"><span>{{ row.sourceField }}</span><el-tag v-if="row.sourceField === '招考职位' || mappings.find(item => item.sourceField === row.sourceField)?.targetField === 'positionName'" size="small" type="danger" effect="plain" class="key-tag">关键</el-tag></template></el-table-column><el-table-column label="识别状态" width="140"><template #default="{ row }"><el-tag :type="row.confidence === 'EXACT' ? 'success' : row.confidence === 'ALIAS' ? 'primary' : 'warning'">{{ row.confidence === 'EXACT' ? '精确识别' : row.confidence === 'ALIAS' ? '同义词识别' : '未识别' }}</el-tag></template></el-table-column><el-table-column label="系统标准字段" min-width="280"><template #default="{ $index }"><el-select v-model="mappings[$index].targetField" clearable placeholder="不导入该字段"><el-option label="不导入该字段" value="" /><el-option v-for="[key, label] in optionsFor(mappings[$index])" :key="key" :label="label" :value="key" /></el-select></template></el-table-column></el-table>
    <div v-if="!result" class="actions"><el-button type="primary" :loading="loading" @click="submit">确认合并导入职位</el-button></div>
    <div v-if="progress && progress.status === 'IMPORTING'" class="progress"><el-progress :percentage="progress.totalRows ? Math.floor(progress.processedRows * 100 / progress.totalRows) : 0" /><p>正在解析岗位：{{ progress.processedRows }} / {{ progress.totalRows }}</p></div>
    <div v-if="result" class="result"><el-alert v-if="progress?.errorMessage" type="warning" :closable="false" show-icon :title="progress.errorMessage" /><el-result icon="success" title="职位导入完成" :sub-title="`共 ${result.totalRows} 行，成功 ${result.successRows} 行，失败 ${result.failedRows} 行`"><template #extra><el-button type="primary" @click="router.push(`/import/${importId}/execute`)">开始智能匹配</el-button></template></el-result></div>
  </section>
</template>
<style scoped>.key-tag{margin-left:8px}.actions{margin-top:22px;text-align:right}.progress,.result{margin-top:18px}.sheet-picker{margin:18px 0;padding:16px;border:1px solid #dcdfe6;border-radius:6px}.sheet-mapping-tabs{display:flex;align-items:center;gap:12px;margin:0 0 14px;flex-wrap:wrap}.picker-title{margin-bottom:10px;font-weight:600}.el-checkbox{margin:0 10px 10px 0}.el-tag{margin-left:6px}</style>
