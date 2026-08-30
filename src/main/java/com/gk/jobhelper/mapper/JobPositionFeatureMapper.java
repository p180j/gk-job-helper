package com.gk.jobhelper.mapper;
import com.gk.jobhelper.entity.JobPositionFeature;
import org.apache.ibatis.annotations.Mapper; import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper public interface JobPositionFeatureMapper {
    int upsert(JobPositionFeature feature);
    JobPositionFeature selectByPositionId(Long positionId);
    List<JobPositionFeature> selectByPositionIds(@Param("ids") List<Long> ids);
    List<JobPositionFeature> selectBySubjectGroup(@Param("group") String group);
    void deleteAll();
}
