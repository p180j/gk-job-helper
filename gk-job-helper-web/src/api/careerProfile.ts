import { get, post, put } from './http'
import type { AiProviderConfig, CareerProfile, CareerProfileDraft, ResumeFile } from '@/types/model'

export function fetchCareerProfile(): Promise<CareerProfile | null> {
  return get<CareerProfile | null>('/api/career-profile')
}

export function parseResume(file: File, aiConfig: AiProviderConfig): Promise<CareerProfileDraft> {
  const form = new FormData()
  form.append('file', file)
  form.append('aiConfig', JSON.stringify(aiConfig))
  return post<CareerProfileDraft>('/api/career-profile/resume/draft', form, { timeout: 120000 })
}

export function fetchCurrentResume(): Promise<ResumeFile | null> { return get<ResumeFile | null>('/api/career-profile/resume') }

export const currentResumeContentUrl = '/api/career-profile/resume/content'

export function saveCareerProfile(draft: CareerProfileDraft): Promise<CareerProfile> {
  return put<CareerProfile>('/api/career-profile', draft)
}
