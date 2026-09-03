package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.ResumeFile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ResumeFileMapper {
    ResumeFile selectByProfileId(Long profileId);
    int insert(ResumeFile resumeFile);
    int updateByProfileId(ResumeFile resumeFile);
}
