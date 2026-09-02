import { del, get, post } from './http'
import type { MatchPositionResult, PageVO } from '@/types/model'

export function addFavorite(positionId: number, profileId: number) {
  return post<void>(`/api/favorites/${positionId}`, undefined, { params: { profileId } })
}

export function removeFavorite(positionId: number, profileId: number) {
  return del<void>(`/api/favorites/${positionId}`, { params: { profileId } })
}

export function fetchFavorites(profileId: number, importId: number, page: number, size: number) {
  return get<PageVO<MatchPositionResult>>('/api/favorites', { params: { profileId, importId, page, size } })
}
