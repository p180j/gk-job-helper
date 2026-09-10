package com.gk.jobhelper.dto;
import java.time.LocalDateTime;
import java.util.List;
public class RecruitmentPositionLibraryPageVO extends PageVO<RecruitmentPositionVO> {
 private long noticeCount; private LocalDateTime latestUpdatedAt;
 public RecruitmentPositionLibraryPageVO(long total,int page,int size,List<RecruitmentPositionVO> items,long noticeCount,LocalDateTime latestUpdatedAt){super(total,page,size,items);this.noticeCount=noticeCount;this.latestUpdatedAt=latestUpdatedAt;}
 public long getNoticeCount(){return noticeCount;} public void setNoticeCount(long v){noticeCount=v;} public LocalDateTime getLatestUpdatedAt(){return latestUpdatedAt;} public void setLatestUpdatedAt(LocalDateTime v){latestUpdatedAt=v;}
}
