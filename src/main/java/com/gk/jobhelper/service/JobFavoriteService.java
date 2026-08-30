package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.FavoriteQuery;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.JobFavorite;
import com.gk.jobhelper.mapper.JobFavoriteMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class JobFavoriteService {
    private static final int MAX_PAGE_SIZE = 100;

    private final JobFavoriteMapper favoriteMapper;
    private final UserProfileMapper profileMapper;
    private final JobPositionMapper positionMapper;

    public JobFavoriteService(JobFavoriteMapper favoriteMapper, UserProfileMapper profileMapper,
                              JobPositionMapper positionMapper) {
        this.favoriteMapper = favoriteMapper;
        this.profileMapper = profileMapper;
        this.positionMapper = positionMapper;
    }

    /** INSERT IGNORE + 唯一约束保证重复收藏幂等。 */
    public void favorite(Long profileId, Long positionId) {
        requireProfile(profileId);
        requirePosition(positionId);
        JobFavorite favorite = new JobFavorite();
        favorite.setProfileId(profileId);
        favorite.setPositionId(positionId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insertIgnore(favorite);
    }

    /** 取消未收藏岗位同样按成功处理，保持幂等。 */
    public void unfavorite(Long profileId, Long positionId) {
        requireProfile(profileId);
        requirePosition(positionId);
        favoriteMapper.delete(profileId, positionId);
    }

    public PageVO<MatchPositionResultVO> page(Long profileId, int page, int size) {
        requireProfile(profileId);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        FavoriteQuery query = new FavoriteQuery();
        query.setProfileId(profileId);
        query.setOffset((safePage - 1) * safeSize);
        query.setSize(safeSize);
        long total = favoriteMapper.countPage(query);
        List<MatchPositionResultVO> items = favoriteMapper.selectPage(query);
        return new PageVO<>(total, safePage, safeSize, items);
    }

    private void requireProfile(Long profileId) {
        if (profileId == null || profileMapper.selectById(profileId) == null) {
            throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND, "档案不存在: " + profileId);
        }
    }

    private void requirePosition(Long positionId) {
        if (positionId == null || positionMapper.selectById(positionId) == null) {
            throw new BusinessException(ApiResponse.CODE_JOB_NOT_FOUND, "岗位不存在: " + positionId);
        }
    }
}
