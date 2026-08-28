import { del, get, post } from './http'
import type {
  ExcelPreview,
  FieldMappingPreview,
  ImportResult,
  MappingItem,
  ImportProgress,
  RecentImport,
  PageVO
} from '@/types/model'

/** 上传 Excel 职位表并返回预览（multipart，由 axios 自动携带 boundary） */
export function uploadExcel(file: File): Promise<ExcelPreview> {
  const formData = new FormData()
  formData.append('file', file)
  return post<ExcelPreview>('/api/import/upload', formData)
}

/** 字段映射建议预览 */
export function fetchMapping(importId: number): Promise<FieldMappingPreview> {
  return get<FieldMappingPreview>(`/api/import/${importId}/mapping`)
}

/** 按确认的映射正式导入岗位 */
export function confirmImport(importId: number, mappings: MappingItem[]): Promise<ImportResult> {
  return post<ImportResult>(`/api/import/${importId}/confirm`, { mappings })
}

export function fetchImportProgress(importId: number): Promise<ImportProgress> {
  return get<ImportProgress>(`/api/import/${importId}/progress`)
}

/** 首页最近分析卡片：最近一次导入 + 匹配统计；无记录返回 null */
export function fetchRecentImport(): Promise<RecentImport | null> {
  return get<RecentImport | null>('/api/import/recent')
}

/** 所有职位表/匹配记录（最新优先） */
export function fetchImports(page: number, size: number): Promise<PageVO<RecentImport>> {
  return get<PageVO<RecentImport>>('/api/import', { params: { page, size } })
}

/** 删除指定职位表及其岗位、匹配结果 */
export function deleteImport(importId: number): Promise<void> {
  return del<void>(`/api/import/${importId}`)
}
