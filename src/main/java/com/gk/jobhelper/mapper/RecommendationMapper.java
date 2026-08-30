package com.gk.jobhelper.mapper;
import com.gk.jobhelper.dto.RecommendationCandidateRow;import org.apache.ibatis.annotations.Mapper;import org.apache.ibatis.annotations.Param;import java.util.List;
@Mapper public interface RecommendationMapper {List<RecommendationCandidateRow> selectMatchCandidates(@Param("profileId")Long profileId,@Param("importId")Long importId);}
