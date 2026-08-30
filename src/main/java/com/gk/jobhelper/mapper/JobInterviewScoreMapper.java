package com.gk.jobhelper.mapper;
import com.gk.jobhelper.entity.JobInterviewScore;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
import java.util.List;
import com.gk.jobhelper.dto.HistoricalSampleRow;
@Mapper public interface JobInterviewScoreMapper {
    int upsert(JobInterviewScore score);
    JobInterviewScore selectByYearAndCode(@Param("examYear") Integer year,@Param("positionCode") String code);
    List<JobInterviewScore> selectByYearAndCodes(@Param("examYear") Integer year,@Param("codes") List<String> codes);
    List<JobInterviewScore> selectByYear(@Param("examYear") Integer year);
    List<HistoricalSampleRow> selectAnalysisSamples();
    long countLinkedCodes(@Param("examYear") Integer year);
    void deleteAll();
}
