import { get, post } from './http'
import type { MatchPositionResult, MatchResult, MatchResultFilters, MatchSummary, PageVO } from '@/types/model'

export function executeMatch(profileId: number, importId: number, referenceDate: string) {
  return post<MatchSummary>('/api/match/execute', { profileId, importId, referenceDate })
}

export interface MatchProgress extends MatchSummary {
  status: 'FEATURE_BUILDING' | 'MATCHING' | 'COMPLETED' | 'FAILED'
  processed: number
  errorMessage?: string
}

export function executeMatchAsync(profileId: number, importId: number, referenceDate: string) {
  return post<MatchProgress>('/api/match/execute-async', { profileId, importId, referenceDate })
}

export function fetchMatchProgress(profileId: number, importId: number) {
  return get<MatchProgress | null>('/api/match/progress', { params: { profileId, importId } })
}

export function fetchMatchResults(params: MatchResultFilters) {
  return get<PageVO<MatchPositionResult>>('/api/match/result', { params })
}

export function fetchMatchRegions(profileId: number, importId: number) {
  return get<string[]>('/api/match/regions', { params: { profileId, importId } })
}

export function fetchJobMatch(jobId: number, profileId: number) {
  return get<MatchResult>(`/api/jobs/${jobId}/match`, { params: { profileId } })
}
