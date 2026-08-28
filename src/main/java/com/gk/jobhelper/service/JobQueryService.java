package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.JobPositionQuery;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 岗位查询服务
 */
@Service
public class JobQueryService {

    private static final int MAX_PAGE_SIZE = 200;

    private final JobPositionMapper jobPositionMapper;

    public JobQueryService(JobPositionMapper jobPositionMapper) {
        this.jobPositionMapper = jobPositionMapper;
    }

    /**
     * 分页查询岗位（列表不含 rawData 大字段）
     */
    public PageVO<JobPosition> page(int page, int size, String keyword, String departmentName,
                                    String organizationName, String educationRequirement,
                                    String majorRequirement, String province, String city) {
        int safePage = Math.max(page, 1);
        int safeSize = size <= 0 ? 10 : Math.min(size, MAX_PAGE_SIZE);

        JobPositionQuery query = new JobPositionQuery();
        query.setKeyword(likeValue(keyword));
        query.setDepartmentName(likeValue(departmentName));
        query.setOrganizationName(likeValue(organizationName));
        query.setEducationRequirement(likeValue(educationRequirement));
        query.setMajorRequirement(likeValue(majorRequirement));
        query.setProvince(likeValue(province));
        query.setCity(likeValue(city));
        query.setOffset((safePage - 1) * safeSize);
        query.setSize(safeSize);

        long total = jobPositionMapper.countByCondition(query);
        List<JobPosition> items = jobPositionMapper.selectByCondition(query);
        return new PageVO<>(total, safePage, safeSize, items);
    }

    /**
     * 岗位详情（含 rawData）
     */
    public JobPosition getJob(Long id) {
        JobPosition position = jobPositionMapper.selectById(id);
        if (position == null) {
            throw new BusinessException(ApiResponse.CODE_JOB_NOT_FOUND, "岗位不存在: " + id);
        }
        return position;
    }

    /** 空白返回 null；非空包装为 %value% 形式的 LIKE 条件 */
    private String likeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "%" + value.trim() + "%";
    }
}
