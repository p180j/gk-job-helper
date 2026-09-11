package com.gk.jobhelper.mapper;
import com.gk.jobhelper.entity.CareerFact;import java.util.*;import org.apache.ibatis.annotations.*;
@Mapper public interface CareerFactMapper {List<CareerFact> selectByProfileId(Long profileId);CareerFact selectByProfileAndKey(@Param("profileId")Long profileId,@Param("factType")String factType,@Param("factKey")String factKey);CareerFact selectById(Long id);int upsert(CareerFact fact);int deleteByIdAndProfile(@Param("id")Long id,@Param("profileId")Long profileId);}
