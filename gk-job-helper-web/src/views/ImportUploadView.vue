<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadExcel } from '@/api/import'
import { loadAiProviderConfig } from '@/utils/aiConfig'
import { showError } from '@/api/http'
import type { ExcelPreview } from '@/types/model'
const router = useRouter(); const preview = ref<ExcelPreview | null>(null); const uploading = ref(false)
const beforeUpload = async (file: File) => { if (!/\.(xls|xlsx)$/i.test(file.name)) { ElMessage.error('仅支持 .xls 或 .xlsx 文件。'); return false } uploading.value = true; try { preview.value = await uploadExcel(file, loadAiProviderConfig()); ElMessage.success('职位表预览成功。') } catch (error) { showError(error, '上传失败，请检查 Excel 文件格式。') } finally { uploading.value = false } return false }
</script>
<template><section class="page-card"><h1 class="page-title">上传职位表</h1><p class="page-subtitle">支持官方发布的 .xls、.xlsx 职位表；可合并导入多个同表头职位 Tab。</p><el-upload drag action="#" :show-file-list="false" :disabled="uploading || !!preview" :before-upload="beforeUpload"><el-icon class="upload-icon"><UploadFilled /></el-icon><div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击选择文件</em></div><template #tip><div class="el-upload__tip">仅支持 .xls / .xlsx 格式</div></template></el-upload><div v-if="preview" class="preview"><el-descriptions title="文件预览" :column="4" border><el-descriptions-item label="文件名">{{ preview.fileName }}</el-descriptions-item><el-descriptions-item label="默认 Sheet">{{ preview.sheetName }}</el-descriptions-item><el-descriptions-item label="默认行数">{{ preview.totalRows }}</el-descriptions-item><el-descriptions-item label="Tab 数量">{{ preview.sheets.length }}</el-descriptions-item></el-descriptions><el-alert type="info" :closable="false" show-icon style="margin-top:16px" title="下一步可勾选多个职位 Tab；说明、目录等页面不会默认勾选。"/><el-table :data="preview.sheets" max-height="320" border style="margin-top:16px"><el-table-column prop="sheetName" label="Sheet"/><el-table-column prop="totalRows" label="数据行数" width="110"/><el-table-column label="系统建议" width="120"><template #default="{ row }"><el-tag v-if="row.suggestedForImport" type="success">建议导入</el-tag><span v-else>-</span></template></el-table-column></el-table><div class="actions"><el-button @click="preview = null">重新选择</el-button><el-button type="primary" @click="router.push(`/import/${preview.fileId}/mapping`)">下一步：选择 Tab 并确认映射</el-button></div></div></section></template>
<style scoped>.upload-icon { font-size: 56px; color:#409eff; margin-bottom:12px }.preview{margin-top:28px}.actions{margin-top:20px;display:flex;justify-content:flex-end;gap:12px}</style>
