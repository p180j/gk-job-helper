import { get, post, put } from './http'
import type { PageVO, RecruitmentDiscoveryResult, RecruitmentNotice, RecruitmentNoticeStatus } from '@/types/model'
export function discoverRecruitment(){return post<RecruitmentDiscoveryResult>('/api/recruitment/discovery')}
export function fetchRecruitmentNotices(params:{status?:RecruitmentNoticeStatus;keyword?:string;page:number;pageSize:number}){return get<PageVO<RecruitmentNotice>>('/api/recruitment/notices',{params})}
export function viewRecruitmentNotice(id:number){return post<void>(`/api/recruitment/notices/${id}/view`)}
export function updateRecruitmentStatus(id:number,status:RecruitmentNoticeStatus){return put<void>(`/api/recruitment/notices/${id}/status`,{status})}
