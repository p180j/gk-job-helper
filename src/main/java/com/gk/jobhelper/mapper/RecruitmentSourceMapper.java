package com.gk.jobhelper.mapper;
import com.gk.jobhelper.entity.RecruitmentSource;import org.apache.ibatis.annotations.Mapper;import org.apache.ibatis.annotations.Param;import java.time.LocalDateTime;import java.util.List;
@Mapper public interface RecruitmentSourceMapper {List<RecruitmentSource> selectEnabled();RecruitmentSource selectByCode(String sourceCode);int updateFetch(@Param("id")Long id,@Param("time")LocalDateTime time,@Param("status")String status);}
