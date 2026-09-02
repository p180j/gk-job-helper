import { get, post, put } from './http'
import type { PageVO, RecruitmentDiscoveryResult, RecruitmentNotice, RecruitmentNoticeStatus, RecruitmentPosition, RecruitmentPositionExtractionResponse } from '@/types/model'
export function discoverRecruitment(){return post<RecruitmentDiscoveryResult>('/api/recruitment/discovery')}
export function fetchRecruitmentNotices(params:{status?:RecruitmentNoticeStatus;keyword?:string;page:number;pageSize:number}){return get<PageVO<RecruitmentNotice>>('/api/recruitment/notices',{params})}
export function fetchRecruitmentNotice(id:number){return get<RecruitmentNotice>(`/api/recruitment/notices/${id}`)}
export function fetchRecruitmentNoticeDetail(id:number){return post<{noticeId:number;detailStatus:string;attachmentCount:number}>(`/api/recruitment/notices/${id}/fetch-detail`)}
export function extractRecruitmentPositions(id:number){return post<RecruitmentPositionExtractionResponse>(`/api/recruitment/notices/${id}/extract-positions`)}
export function fetchRecruitmentPositions(id:number){return get<RecruitmentPosition[]>(`/api/recruitment/notices/${id}/positions`)}
export function fetchRecruitmentPosition(id:number){return get<RecruitmentPosition>(`/api/recruitment/positions/${id}`)}
export function viewRecruitmentNotice(id:number){return post<void>(`/api/recruitment/notices/${id}/view`)}
export function updateRecruitmentStatus(id:number,status:RecruitmentNoticeStatus){return put<void>(`/api/recruitment/notices/${id}/status`,{status})}
