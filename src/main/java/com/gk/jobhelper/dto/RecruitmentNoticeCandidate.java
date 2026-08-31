package com.gk.jobhelper.dto;
import java.time.LocalDateTime;
public class RecruitmentNoticeCandidate {
 private final String title,noticeUrl;private final LocalDateTime publishDate;
 public RecruitmentNoticeCandidate(String title,String noticeUrl,LocalDateTime publishDate){this.title=title;this.noticeUrl=noticeUrl;this.publishDate=publishDate;}public String getTitle(){return title;}public String getNoticeUrl(){return noticeUrl;}public LocalDateTime getPublishDate(){return publishDate;}
}
