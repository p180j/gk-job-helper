package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.CareerProfile;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CareerProfileMapper {
    CareerProfile selectByProfileId(Long profileId);
    CareerProfile selectById(Long id);
    int insert(CareerProfile profile);
    int updateByProfileId(CareerProfile profile);
    int deleteAll();
}
