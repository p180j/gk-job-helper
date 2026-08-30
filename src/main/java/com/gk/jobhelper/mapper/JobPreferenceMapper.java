package com.gk.jobhelper.mapper;
import com.gk.jobhelper.entity.JobPreference;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface JobPreferenceMapper {
    JobPreference selectByProfileId(Long profileId);
    int upsert(JobPreference preference);
    void deleteAll();
}
