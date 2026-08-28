package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.entity.UserEducation;
import com.gk.jobhelper.mapper.UserEducationMapper;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 个人教育经历的最小 CRUD；档案仍保持系统仅一份的既有约束。 */
@RestController
@RequestMapping("/api/profile/educations")
public class UserEducationController {
    private final UserEducationMapper mapper;
    public UserEducationController(UserEducationMapper mapper) { this.mapper = mapper; }

    @GetMapping("/{profileId}")
    public ApiResponse<List<UserEducation>> list(@PathVariable Long profileId) { return ApiResponse.ok(mapper.selectByProfileId(profileId)); }

    @PostMapping
    public ApiResponse<UserEducation> create(@RequestBody UserEducation education) {
        validate(education); LocalDateTime now = LocalDateTime.now(); education.setCreatedAt(now); education.setUpdatedAt(now);
        if (Boolean.TRUE.equals(education.getHighest())) mapper.clearHighest(education.getProfileId());
        mapper.insert(education); return ApiResponse.ok(mapper.selectById(education.getId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserEducation> update(@PathVariable Long id, @RequestBody UserEducation education) {
        UserEducation existing = mapper.selectById(id);
        if (existing == null) throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "教育经历不存在: id=" + id);
        education.setId(id); education.setProfileId(existing.getProfileId()); validate(education); education.setUpdatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(education.getHighest())) mapper.clearHighest(education.getProfileId());
        mapper.update(education); return ApiResponse.ok(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) { mapper.deleteById(id); return ApiResponse.ok(null); }

    private void validate(UserEducation education) {
        if (education.getProfileId() == null || blank(education.getEducationLevel()))
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "profileId 与学历层次不能为空");
        if (education.getEnabled() == null) education.setEnabled(true);
        if (education.getHighest() == null) education.setHighest(false);
    }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
