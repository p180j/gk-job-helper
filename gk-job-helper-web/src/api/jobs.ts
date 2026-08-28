import { get } from './http'
import type { JobPosition } from '@/types/model'

export function fetchJob(jobId: number) {
  return get<JobPosition>(`/api/jobs/${jobId}`)
}
