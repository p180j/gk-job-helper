import { get, post, put } from './http'
import type { PageVO, RecruitmentDiscoveryResult, RecruitmentNotice, RecruitmentNoticeStatus, RecruitmentPosition, RecruitmentPositionExtractionResponse, RecruitmentPositionLibraryPage, RecruitmentMatchResult, RecruitmentPositionFilterOptions } from '@/types/model'
export function discoverRecruitment(days:number){return post<RecruitmentDiscoveryResult>('/api/recruitment/discovery',undefined,{params:{days}})}
export function fetchRecruitmentNotices(params:{status?:RecruitmentNoticeStatus|'UNREAD';keyword?:string;page:number;pageSize:number}){return get<PageVO<RecruitmentNotice>>('/api/recruitment/notices',{params})}
export function fetchRecruitmentNotice(id:number){return get<RecruitmentNotice>(`/api/recruitment/notices/${id}`)}
export function fetchRecruitmentNoticeDetail(id:number){return post<{noticeId:number;detailStatus:string;attachmentCount:number}>(`/api/recruitment/notices/${id}/fetch-detail`)}
export function extractRecruitmentPositions(id:number){return post<RecruitmentPositionExtractionResponse>(`/api/recruitment/notices/${id}/extract-positions`)}
export function extractRecruitmentBodyPositions(id:number){return post<RecruitmentPositionExtractionResponse>(`/api/recruitment/notices/${id}/extract-body-positions`)}
export function fetchRecruitmentPositions(id:number){return get<RecruitmentPosition[]>(`/api/recruitment/notices/${id}/positions`)}
export function fetchRecruitmentPosition(id:number){return get<RecruitmentPosition>(`/api/recruitment/positions/${id}`)}
export function fetchRecruitmentPositionLibrary(params:{keyword?:string;location?:string;organization?:string;education?:string;major?:string;noticeId?:number;matchStatus?:string;page:number;pageSize:number}){return get<RecruitmentPositionLibraryPage>('/api/recruitment/positions',{params})}
export function fetchRecruitmentPositionFilterOptions(){return get<RecruitmentPositionFilterOptions>('/api/recruitment/positions/filter-options')}
export function matchRecruitmentPosition(id:number){return post<RecruitmentMatchResult>(`/api/recruitment/positions/${id}/match`)}
export function fetchRecruitmentPositionMatch(id:number){return get<RecruitmentMatchResult | null>(`/api/recruitment/positions/${id}/match`)}
export function rebuildRecruitmentMatches(){return post<Record<string,number>>('/api/recruitment/matches/rebuild')}
export function viewRecruitmentNotice(id:number){return post<void>(`/api/recruitment/notices/${id}/view`)}
export function updateRecruitmentStatus(id:number,status:RecruitmentNoticeStatus){return put<void>(`/api/recruitment/notices/${id}/status`,{status})}
export function deleteRecruitmentNotices(ids:number[]){return post<void>('/api/recruitment/notices/batch-delete',ids)}
