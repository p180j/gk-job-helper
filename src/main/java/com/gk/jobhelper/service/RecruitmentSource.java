package com.gk.jobhelper.service;
import com.gk.jobhelper.dto.RecruitmentNoticeCandidate;import java.util.List;
public interface RecruitmentSource {String getSourceCode();String getSourceName();List<RecruitmentNoticeCandidate> fetchLatest();}
