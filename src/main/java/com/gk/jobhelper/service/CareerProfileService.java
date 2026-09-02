package com.gk.jobhelper.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.CareerEducationVO;
import com.gk.jobhelper.dto.CareerProfileRequest;
import com.gk.jobhelper.dto.CareerProfileVO;
import com.gk.jobhelper.dto.CareerProjectVO;
import com.gk.jobhelper.dto.CareerWorkVO;
import com.gk.jobhelper.entity.CareerProfile;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.CareerProfileMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 仅负责用户确认后的职业画像读写，不参与 AI 或招聘岗位匹配。
 * 未来资料来源优先级固定为：用户确认的基础档案 > 用户确认的职业画像 > AI 简历草稿。
 * AI 草稿只存在于请求响应中，绝不会自动覆盖任一已确认资料。
 */
@Service
public class CareerProfileService {
    private final CareerProfileMapper careerProfileMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;

    public CareerProfileService(CareerProfileMapper careerProfileMapper, UserProfileMapper userProfileMapper, ObjectMapper objectMapper) {
        this.careerProfileMapper = careerProfileMapper;
        this.userProfileMapper = userProfileMapper;
        this.objectMapper = objectMapper;
    }

    public CareerProfileVO getCurrent() {
        UserProfile profile = userProfileMapper.selectFirstProfile();
        if (profile == null) return null;
        CareerProfile careerProfile = careerProfileMapper.selectByProfileId(profile.getId());
        return careerProfile == null ? null : toVO(careerProfile);
    }

    @Transactional
    public CareerProfileVO save(CareerProfileRequest request) {
        UserProfile profile = userProfileMapper.selectFirstProfile();
        if (profile == null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND, "请先保存我的档案，再确认职业画像。");
        }
        CareerProfile entity = careerProfileMapper.selectByProfileId(profile.getId());
        boolean creating = entity == null;
        if (creating) {
            entity = new CareerProfile();
            entity.setProfileId(profile.getId());
            entity.setCreatedAt(LocalDateTime.now());
        }
        entity.setCurrentPosition(request.getCurrentPosition());
        entity.setTotalWorkYears(request.getTotalWorkYears());
        entity.setCareerDirections(write(request.getCareerDirections()));
        entity.setIndustries(write(request.getIndustries()));
        entity.setEducationExperiences(write(request.getEducationExperiences()));
        entity.setWorkExperiences(write(request.getWorkExperiences()));
        entity.setProjectExperiences(write(request.getProjectExperiences()));
        entity.setSkills(write(request.getSkills()));
        entity.setCertificates(write(request.getCertificates()));
        entity.setUpdatedAt(LocalDateTime.now());
        if (creating) careerProfileMapper.insert(entity); else careerProfileMapper.updateByProfileId(entity);
        return toVO(careerProfileMapper.selectByProfileId(profile.getId()));
    }

    private CareerProfileVO toVO(CareerProfile entity) {
        CareerProfileVO vo = new CareerProfileVO();
        vo.setId(entity.getId());
        vo.setProfileId(entity.getProfileId());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCurrentPosition(entity.getCurrentPosition());
        vo.setTotalWorkYears(entity.getTotalWorkYears());
        vo.setCareerDirections(read(entity.getCareerDirections(), new TypeReference<List<String>>() { }));
        vo.setIndustries(read(entity.getIndustries(), new TypeReference<List<String>>() { }));
        vo.setEducationExperiences(read(entity.getEducationExperiences(), new TypeReference<List<CareerEducationVO>>() { }));
        vo.setWorkExperiences(read(entity.getWorkExperiences(), new TypeReference<List<CareerWorkVO>>() { }));
        vo.setProjectExperiences(read(entity.getProjectExperiences(), new TypeReference<List<CareerProjectVO>>() { }));
        vo.setSkills(read(entity.getSkills(), new TypeReference<List<String>>() { }));
        vo.setCertificates(read(entity.getCertificates(), new TypeReference<List<String>>() { }));
        return vo;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? new ArrayList<Object>() : value);
        } catch (Exception e) {
            throw new IllegalStateException("职业画像序列化失败", e);
        }
    }

    private <T> List<T> read(String value, TypeReference<List<T>> type) {
        if (value == null || value.trim().isEmpty()) return new ArrayList<T>();
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            throw new IllegalStateException("职业画像数据格式异常", e);
        }
    }
}
