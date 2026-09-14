package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.RecruitmentDetailFetchResponse;
import com.gk.jobhelper.dto.RecruitmentNoticeVO;
import com.gk.jobhelper.entity.RecruitmentAttachment;
import com.gk.jobhelper.mapper.RecruitmentAttachmentMapper;
import com.gk.jobhelper.mapper.RecruitmentNoticeMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruitmentDetailService {
    private final RecruitmentNoticeService notices;
    private final RecruitmentNoticeMapper noticeMapper;
    private final RecruitmentAttachmentMapper attachmentMapper;
    private final List<RecruitmentDetailFetcher> fetchers;

    public RecruitmentDetailService(RecruitmentNoticeService notices, RecruitmentNoticeMapper noticeMapper,
            RecruitmentAttachmentMapper attachmentMapper, List<RecruitmentDetailFetcher> fetchers) {
        this.notices = notices;
        this.noticeMapper = noticeMapper;
        this.attachmentMapper = attachmentMapper;
        this.fetchers = fetchers;
    }

    public RecruitmentDetailFetchResponse fetch(Long id) {
        RecruitmentNoticeVO notice = notices.detail(id);
        RecruitmentDetailFetcher fetcher = fetchers.stream().filter(item -> item.supports(notice)).findFirst()
                .orElseThrow(() -> new BusinessException(ApiResponse.CODE_BAD_REQUEST, "该公告来源暂不支持获取详情"));
        try {
            return save(id, fetcher.fetch(notice));
        } catch (Exception e) {
            noticeMapper.updateDetailFailure(id, "FAILED", safe(e.getMessage()), LocalDateTime.now());
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "公告详情获取失败：" + safe(e.getMessage()));
        }
    }

    @Transactional
    public RecruitmentDetailFetchResponse save(Long id, RecruitmentDetailFetchResult result) {
        LocalDateTime now = LocalDateTime.now();
        noticeMapper.updateDetail(id, "FETCHED", result.bodyHtml, result.bodyText, now, now);
        saveAttachments(id, result.attachments, now);
        return new RecruitmentDetailFetchResponse(id, "FETCHED", result.attachments.size());
    }

    @Transactional
    public void saveAttachments(Long noticeId, List<RecruitmentAttachmentDraft> drafts) {
        saveAttachments(noticeId, drafts, LocalDateTime.now());
    }

    private void saveAttachments(Long noticeId, List<RecruitmentAttachmentDraft> drafts, LocalDateTime now) {
        for (RecruitmentAttachmentDraft draft : drafts) {
            RecruitmentAttachment attachment = new RecruitmentAttachment();
            attachment.setNoticeId(noticeId);
            attachment.setFileName(draft.fileName);
            attachment.setFileUrl(draft.fileUrl);
            attachment.setDedupeKey(key(draft.fileUrl, draft.fileName));
            attachment.setFileType(draft.fileType);
            attachment.setAttachmentType(draft.attachmentType);
            attachment.setSourceText(draft.sourceText);
            attachment.setCreatedAt(now);
            attachment.setUpdatedAt(now);
            attachmentMapper.upsert(attachment);
        }
    }

    private String key(String url, String name) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest((url.trim() + "\n" + name.trim()).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : bytes) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("附件去重键生成失败", e);
        }
    }

    private String safe(String message) {
        if (message == null || message.trim().isEmpty()) return "公告页面暂时无法访问，请稍后重试。";
        return message.length() > 200 ? message.substring(0, 200) : message;
    }
}
