package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecruitmentNoticeVO;
import com.gk.jobhelper.entity.RecruitmentAttachment;
import com.gk.jobhelper.mapper.RecruitmentAttachmentMapper;
import com.gk.jobhelper.mapper.RecruitmentNoticeMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class RecruitmentAutoProcessService {
    private final RecruitmentDetailService details; private final RecruitmentPositionService positions; private final RecruitmentQualificationMatchService matches;
    private final RecruitmentNoticeService notices; private final RecruitmentNoticeMapper noticeMapper;
    private final RecruitmentAttachmentMapper attachments;
    public RecruitmentAutoProcessService(RecruitmentDetailService details, RecruitmentPositionService positions, RecruitmentQualificationMatchService matches,
            RecruitmentNoticeService notices, RecruitmentNoticeMapper noticeMapper, RecruitmentAttachmentMapper attachments) {
        this.details = details; this.positions = positions; this.matches = matches; this.notices = notices; this.noticeMapper = noticeMapper; this.attachments = attachments;
    }
    @Async("recruitmentProcessor")
    public void processNoticeAsync(Long noticeId) { processNotice(noticeId); }
    public void processNotice(Long noticeId) {
        RecruitmentNoticeVO notice = notices.detail(noticeId);
        if ("COMPLETED".equals(notice.getProcessStatus())) return;
        noticeMapper.updateProcess(noticeId, "PROCESSING", "DETAIL", null, null, 0, null, LocalDateTime.now());
        try {
            details.fetch(noticeId);
            notice = notices.detail(noticeId);
            List<RecruitmentAttachment> files = attachments.selectPositionDataByNoticeId(noticeId);
            noticeMapper.updateProcess(noticeId, "PROCESSING", "SOURCE_DETECT", null, null, 0, null, LocalDateTime.now());
            int count;
            if (!files.isEmpty()) count = positions.extract(noticeId).getPositionCount();
            else if (notice.getBodyPositionHint() != null) count = positions.extractBody(noticeId).getPositionCount();
            else { manual(noticeId, "NO_VALID_POSITION", "未发现可自动结构化的岗位来源"); return; }
            if (count > 0) { matchPositions(noticeId); noticeMapper.updateProcess(noticeId, "COMPLETED", "DONE", null, null, count, LocalDateTime.now(), LocalDateTime.now()); }
            else manual(noticeId, "NO_VALID_POSITION", "未识别到有效岗位");
        } catch (Exception exception) {
            String code = manualCode(noticeId, exception.getMessage());
            if ("PARSE_FAILED".equals(code)) noticeMapper.updateProcess(noticeId, "FAILED", "POSITION_EXTRACT", code, safe(exception.getMessage()), 0, null, LocalDateTime.now());
            else manual(noticeId, code, safe(exception.getMessage()));
        }
    }
    private void manual(Long id, String code, String reason) { noticeMapper.updateProcess(id, "NEED_MANUAL", "POSITION_EXTRACT", code, safe(reason), 0, null, LocalDateTime.now()); }
    private String manualCode(Long id, String message) {
        for (RecruitmentAttachment file : attachments.selectPositionDataByNoticeId(id)) {
            if ("OCR_REQUIRED".equals(file.getParseStatus())) return "OCR_REQUIRED";
            if ("NO_VALID_POSITION".equals(file.getParseStatus())) return "NO_VALID_POSITION";
            if ("UNSUPPORTED_FILE_TYPE".equals(file.getParseStatus())) return "PARSE_FAILED";
        }
        String text = message == null ? "" : message;
        if (text.contains("安全验证")) return "EXTERNAL_SOURCE_ACCESS_RESTRICTED";
        if (text.contains("二维码")) return "QR_MANUAL";
        if (text.contains("下载")) return "DOWNLOAD_FAILED";
        return "PARSE_FAILED";
    }
    private String safe(String value) { if (value == null || value.trim().isEmpty()) return "自动处理失败"; return value.length() > 500 ? value.substring(0, 500) : value; }
    private void matchPositions(Long noticeId) { try { matches.matchNotice(noticeId); } catch (Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("招聘岗位自动资格匹配失败，noticeId={}", noticeId, e); } }
}
