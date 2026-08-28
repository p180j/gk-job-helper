import { ApiError, get, post, put } from './http'
import type { ProfileForm, UserProfile } from '@/types/model'

/** 查询当前档案；档案不存在时抛 ApiError(code=40401) */
export function fetchProfile(): Promise<UserProfile | null> {
  return get<UserProfile | null>('/api/profile')
}

/** 判断档案是否不存在（40401） */
export function isProfileNotFound(error: unknown): boolean {
  return error instanceof ApiError && error.code === 40401
}

export function createProfile(form: ProfileForm): Promise<UserProfile> {
  return post<UserProfile>('/api/profile', form)
}

export function updateProfile(form: ProfileForm): Promise<UserProfile> {
  return put<UserProfile>('/api/profile', form)
}
