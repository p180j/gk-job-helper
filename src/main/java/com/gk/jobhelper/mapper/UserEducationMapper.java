package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.UserEducation;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface UserEducationMapper {
    List<UserEducation> selectEnabledByProfileId(Long profileId);
    List<UserEducation> selectByProfileId(Long profileId);
    UserEducation selectById(Long id);
    int insert(UserEducation education);
    int update(UserEducation education);
    int deleteById(Long id);
    int clearHighest(Long profileId);
}
