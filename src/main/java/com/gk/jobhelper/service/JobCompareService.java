package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.JobCompareVO;
import com.gk.jobhelper.dto.MatchResultVO;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class JobCompareService {
    private final JobPositionMapper positionMapper;
    private final JobMatchService matchService;

    public JobCompareService(JobPositionMapper positionMapper, JobMatchService matchService) {
        this.positionMapper = positionMapper;
        this.matchService = matchService;
    }

    public List<JobCompareVO> compare(Long profileId, List<Long> requestedIds) {
        if (profileId == null) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "profileId 不能为空");
        }
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (requestedIds != null) uniqueIds.addAll(requestedIds);
        uniqueIds.remove(null);
        if (uniqueIds.size() < 2) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "岗位对比至少选择 2 个岗位");
        }
        if (uniqueIds.size() > 4) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "岗位对比最多选择 4 个岗位");
        }

        List<JobCompareVO> result = new ArrayList<>();
        for (Long jobId : uniqueIds) {
            JobPosition position = positionMapper.selectById(jobId);
            if (position == null) {
                throw new BusinessException(ApiResponse.CODE_JOB_NOT_FOUND, "岗位不存在: " + jobId);
            }
            MatchResultVO match = matchService.getMatchDetail(jobId, profileId);
            result.add(toVO(position, match));
        }
        return result;
    }

    private JobCompareVO toVO(JobPosition p, MatchResultVO match) {
        JobCompareVO vo = new JobCompareVO();
        vo.setJobId(p.getId());
        vo.setRegion(join(p.getProvince(), p.getCity(), p.getDistrict()));
        vo.setDepartmentName(p.getDepartmentName());
        vo.setOrganizationName(p.getOrganizationName());
        vo.setPositionName(p.getPositionName());
        vo.setPositionCode(p.getPositionCode());
        vo.setRecruitCount(p.getRecruitCount());
        vo.setEducationRequirement(p.getEducationRequirement());
        vo.setMajorRequirement(p.getMajorRequirement());
        vo.setAgeRequirement(p.getAgeRequirement());
        vo.setPoliticalRequirement(p.getPoliticalRequirement());
        vo.setWorkYearRequirement(p.getWorkYearRequirement());
        vo.setFreshGraduateRequirement(p.getFreshGraduateRequirement());
        vo.setOtherRestrictions(join(p.getHouseholdRequirement(), p.getServiceProjectRequirement(),
                p.getCertificateRequirement(), p.getGenderRequirement(), p.getRemark()));
        vo.setOverallStatus(match.getResult());
        vo.setMatchItems(match.getItems());
        return vo;
    }

    private String join(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (result.length() > 0) result.append("；");
            result.append(value.trim());
        }
        return result.length() == 0 ? null : result.toString();
    }
}
