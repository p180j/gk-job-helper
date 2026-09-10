package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.*;
import java.util.*;
import org.apache.ibatis.annotations.*;

@Mapper public interface RecruitmentMatchMapper {
 RecruitmentMatchResult selectByProfileAndPosition(@Param("profileId")Long profileId,@Param("positionId")Long positionId);
 List<RecruitmentMatchResult> selectByProfileAndPositionIds(@Param("profileId")Long profileId,@Param("positionIds")List<Long> positionIds);
 int upsert(@Param("record")RecruitmentMatchResult record);
 int deleteItems(@Param("matchResultId")Long matchResultId);
 int insertItems(@Param("items")List<RecruitmentMatchItem> items);
 List<RecruitmentMatchItem> selectItems(@Param("matchResultId")Long matchResultId);
 long countByProfileAndResult(@Param("profileId")Long profileId,@Param("result")String result);
}
