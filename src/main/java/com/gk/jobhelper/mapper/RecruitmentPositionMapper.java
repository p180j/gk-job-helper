package com.gk.jobhelper.mapper;
import com.gk.jobhelper.dto.RecruitmentPositionVO;import com.gk.jobhelper.entity.*;import java.util.*;import org.apache.ibatis.annotations.*;
@Mapper public interface RecruitmentPositionMapper {int deleteByAttachmentId(Long attachmentId);int insertBatch(@Param("list")List<RecruitmentPosition> positions);List<RecruitmentPositionVO> selectByNoticeId(Long noticeId);RecruitmentPositionVO selectById(Long id);List<RecruitmentRequirement> selectRequirementsByPositionId(Long positionId);int insertRequirements(@Param("list")List<RecruitmentRequirement> requirements);}
