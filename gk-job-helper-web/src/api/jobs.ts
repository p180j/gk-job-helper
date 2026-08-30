import { get } from './http'
import type { HistoricalAnalysis, JobCompareItem, JobFeature, JobPosition } from '@/types/model'

export function fetchJob(jobId: number) {
  return get<JobPosition>(`/api/jobs/${jobId}`)
}

export function fetchJobFeature(jobId: number) { return get<JobFeature | null>(`/api/job-features/${jobId}`) }
export function fetchHistoricalAnalysis(jobId: number, examYear = 2026) { return get<HistoricalAnalysis>(`/api/jobs/${jobId}/historical-analysis`, { params: { examYear } }) }

export function compareJobs(profileId: number, jobIds: number[]) {
  return get<JobCompareItem[]>('/api/jobs/compare', { params: { profileId, jobIds: jobIds.join(',') } })
}
