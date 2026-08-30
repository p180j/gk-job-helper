import { get, post, put } from './http'
import type { InterviewScoreImportResult, JobPreference, PageVO, RecommendationItem } from '@/types/model'

export function fetchPreferences(profileId: number) { return get<JobPreference>(`/api/preferences/${profileId}`) }
export function savePreferences(profileId: number, data: JobPreference) { return put<JobPreference>(`/api/preferences/${profileId}`, data) }
export function fetchRecommendations(params: { profileId: number; importId: number; priorityLevel?: string; page: number; size: number }) {
  return get<PageVO<RecommendationItem>>('/api/recommendations', { params })
}
export function importInterviewScores(file: File, importId: number) { const form = new FormData(); form.append('file', file); form.append('importId', String(importId)); return post<InterviewScoreImportResult>('/api/interview-scores/import', form) }
