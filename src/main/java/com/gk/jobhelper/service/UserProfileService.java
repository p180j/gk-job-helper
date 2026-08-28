package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ProfileRequest;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 个人档案业务逻辑（Iteration 1 为单档案模式：系统中最多一条档案记录）
 */
@Service
public class UserProfileService {

    private final UserProfileMapper userProfileMapper;

    public UserProfileService(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * 查询个人档案（当前第一条）
     */
    public UserProfile getProfile() {
        UserProfile profile = userProfileMapper.selectFirstProfile();
        if (profile == null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND,
                    "个人档案尚未创建，请先调用 POST /api/profile 创建");
        }
        return profile;
    }

    /**
     * 创建个人档案，已存在则拒绝（使用 PUT 更新）
     */
    public UserProfile createProfile(ProfileRequest request) {
        if (userProfileMapper.selectFirstProfile() != null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_EXISTS,
                    "个人档案已存在，请使用 PUT /api/profile 更新");
        }
        UserProfile profile = new UserProfile();
        copyFields(request, profile);
        LocalDateTime now = LocalDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        userProfileMapper.insert(profile);
        return userProfileMapper.selectById(profile.getId());
    }

    /**
     * 更新个人档案，请求中为 null 的字段保持原值不变
     */
    public UserProfile updateProfile(ProfileRequest request) {
        UserProfile existing = userProfileMapper.selectFirstProfile();
        if (existing == null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND,
                    "个人档案不存在，无法更新，请先调用 POST /api/profile 创建");
        }
        copyFields(request, existing);
        existing.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.updateById(existing);
        return userProfileMapper.selectById(existing.getId());
    }

    private void copyFields(ProfileRequest request, UserProfile profile) {
        if (request.getName() != null) {
            profile.setName(request.getName());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBirthDate() != null) {
            profile.setBirthDate(request.getBirthDate());
        }
        if (request.getPoliticalStatus() != null) {
            profile.setPoliticalStatus(request.getPoliticalStatus());
        }
        if (request.getEducation() != null) {
            profile.setEducation(request.getEducation());
        }
        if (request.getDegree() != null) {
            profile.setDegree(request.getDegree());
        }
        if (request.getMajor() != null) {
            profile.setMajor(request.getMajor());
        }
        if (request.getMajorCode() != null) {
            profile.setMajorCode(request.getMajorCode());
        }
        if (request.getGraduationDate() != null) {
            profile.setGraduationDate(request.getGraduationDate());
        }
        if (request.getWorkYears() != null) {
            profile.setWorkYears(request.getWorkYears());
        }
        if (request.getFreshGraduateStatus() != null) {
            profile.setFreshGraduateStatus(request.getFreshGraduateStatus());
        }
        if (request.getHousehold() != null) {
            profile.setHousehold(request.getHousehold());
        }
        if (request.getStudentOrigin() != null) {
            profile.setStudentOrigin(request.getStudentOrigin());
        }
        if (request.getServiceProjectType() != null) {
            profile.setServiceProjectType(request.getServiceProjectType());
        }
        if (request.getVeteran() != null) {
            profile.setVeteran(request.getVeteran());
        }
        if (request.getCertificates() != null) {
            profile.setCertificates(request.getCertificates());
        }
        if (request.getTargetRegion() != null) {
            profile.setTargetRegion(request.getTargetRegion());
        }
        if (request.getNotes() != null) {
            profile.setNotes(request.getNotes());
        }
    }
}
