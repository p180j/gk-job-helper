import { get, post } from './http'
import type { MatchPositionResult, MatchResult, MatchResultValue, MatchSummary, PageVO } from '@/types/model'

export function executeMatch(profileId: number, importId: number, referenceDate: string) {
  return post<MatchSummary>('/api/match/execute', { profileId, importId, referenceDate })
}

export function fetchMatchResults(params: { profileId: number; importId: number; result?: MatchResultValue; page: number; size: number }) {
  return get<PageVO<MatchPositionResult>>('/api/match/result', { params })
}

export function fetchJobMatch(jobId: number, profileId: number) {
  return get<MatchResult>(`/api/jobs/${jobId}/match`, { params: { profileId } })
}
