package com.gk.jobhelper.service;
public interface RecruitmentPositionExtractor {boolean supports(String fileType);RecruitmentPositionExtractionResult extract(DownloadedAttachment attachment,Long noticeId);}
