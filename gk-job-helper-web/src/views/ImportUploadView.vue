<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { uploadExcel } from '@/api/import'
import { showError } from '@/api/http'
import type { ExcelPreview } from '@/types/model'
const router = useRouter(); const preview = ref<ExcelPreview | null>(null); const uploading = ref(false)
const beforeUpload = async (file: File) => { if (!/\.(xls|xlsx)$/i.test(file.name)) { ElMessage.error('仅支持 .xls 或 .xlsx 文件。'); return false } uploading.value = true; try { preview.value = await uploadExcel(file); ElMessage.success('职位表预览成功。') } catch (error) { showError(error, '上传失败，请检查 Excel 文件格式。') } finally { uploading.value = false } return false }
</script>
<template><section class="page-card"><h1 class="page-title">上传职位表</h1><p class="page-subtitle">支持官方发布的 .xls、.xlsx 职位表；上传后可预览并确认字段映射。</p><el-upload drag action="#" :show-file-list="false" :disabled="uploading || !!preview" :before-upload="beforeUpload"><el-icon class="upload-icon"><UploadFilled /></el-icon><div class="el-upload__text">将 Excel 文件拖到此处，或 <em>点击选择文件</em></div><template #tip><div class="el-upload__tip">仅支持 .xls / .xlsx 格式</div></template></el-upload><div v-if="preview" class="preview"><el-descriptions title="文件预览" :column="4" border><el-descriptions-item label="文件名">{{ preview.fileName }}</el-descriptions-item><el-descriptions-item label="Sheet">{{ preview.sheetName }}</el-descriptions-item><el-descriptions-item label="总行数">{{ preview.totalRows }}</el-descriptions-item><el-descriptions-item label="表头数量">{{ preview.headers.length }}</el-descriptions-item></el-descriptions><el-table :data="preview.previewRows" max-height="390" border style="margin-top:20px"><el-table-column v-for="header in preview.headers" :key="header" :prop="header" :label="header" min-width="140" show-overflow-tooltip /></el-table><div class="actions"><el-button @click="preview = null">重新选择</el-button><el-button type="primary" @click="router.push(`/import/${preview.fileId}/mapping`)">下一步：确认字段映射</el-button></div></div></section></template>
<style scoped>.upload-icon { font-size: 56px; color:#409eff; margin-bottom:12px }.preview{margin-top:28px}.actions{margin-top:20px;display:flex;justify-content:flex-end;gap:12px}</style>
