package com.gk.jobhelper.service;
import com.gk.jobhelper.dto.RecruitmentNoticeVO;
public interface RecruitmentDetailFetcher {boolean supports(RecruitmentNoticeVO notice);RecruitmentDetailFetchResult fetch(RecruitmentNoticeVO notice);}
