import { get, post, put } from './http'
import type { AiProviderConfig, CareerProfile, CareerProfileDraft } from '@/types/model'

export function fetchCareerProfile(): Promise<CareerProfile | null> {
  return get<CareerProfile | null>('/api/career-profile')
}

/** 简历仅用于当前请求生成草稿，后端不会保存原文件或文本。 */
export function parseResume(file: File, aiConfig: AiProviderConfig): Promise<CareerProfileDraft> {
  const form = new FormData()
  form.append('file', file)
  form.append('aiConfig', JSON.stringify(aiConfig))
  return post<CareerProfileDraft>('/api/career-profile/resume/draft', form, { timeout: 120000 })
}

export function saveCareerProfile(draft: CareerProfileDraft): Promise<CareerProfile> {
  return put<CareerProfile>('/api/career-profile', draft)
}
