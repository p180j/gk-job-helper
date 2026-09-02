package com.gk.jobhelper.service;
import java.time.LocalDate;import java.util.*;
public class RecruitmentDetailFetchResult {public final String title,bodyHtml,bodyText;public final LocalDate publishDate;public final List<RecruitmentAttachmentDraft> attachments;public RecruitmentDetailFetchResult(String title,LocalDate date,String html,String text,List<RecruitmentAttachmentDraft> files){this.title=title;publishDate=date;bodyHtml=html;bodyText=text;attachments=files;}}
