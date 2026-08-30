package com.gk.jobhelper.mapper;

import com.gk.jobhelper.dto.FavoriteQuery;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.entity.JobFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface JobFavoriteMapper {
    int insertIgnore(JobFavorite favorite);
    int delete(@Param("profileId") Long profileId, @Param("positionId") Long positionId);
    boolean exists(@Param("profileId") Long profileId, @Param("positionId") Long positionId);
    List<Long> selectPositionIds(@Param("profileId") Long profileId, @Param("positionIds") List<Long> positionIds);
    long countPage(FavoriteQuery query);
    List<MatchPositionResultVO> selectPage(FavoriteQuery query);
    void deleteAll();
}
