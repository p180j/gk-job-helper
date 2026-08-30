import type { MatchResultValue } from '@/types/model'

export const MATCH_STATUS: Record<MatchResultValue, { label: string; detailLabel: string; type: 'success' | 'warning' | 'danger'; icon: string }> = {
  MATCH: { label: '可以报', detailLabel: '符合', type: 'success', icon: '✓' },
  UNCERTAIN: { label: '待确认', detailLabel: '需要确认', type: 'warning', icon: '!' },
  NOT_MATCH: { label: '不符合', detailLabel: '不能报', type: 'danger', icon: '×' }
}

export function matchStatus(value: MatchResultValue) {
  return MATCH_STATUS[value] ?? MATCH_STATUS.UNCERTAIN
}

export const CONDITION_LABELS: Record<string, string> = {
  EDUCATION: '学历', AGE: '年龄', POLITICAL: '政治面貌', WORK_EXPERIENCE: '基层工作经历', MAJOR: '专业', REMARK: '备注条件'
}
