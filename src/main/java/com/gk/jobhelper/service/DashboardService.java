package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.MatchResultStatRow;
import com.gk.jobhelper.dto.RecentImportVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 首页仪表数据服务：最近一次导入 + 当前档案匹配统计（Iteration 5 前端首页最小必要接口）
 */
@Service
public class DashboardService {

    private final ImportFileMapper importFileMapper;
    private final JobPositionMapper jobPositionMapper;
    private final JobMatchMapper jobMatchMapper;
    private final UserProfileService userProfileService;

    public DashboardService(ImportFileMapper importFileMapper,
                            JobPositionMapper jobPositionMapper,
                            JobMatchMapper jobMatchMapper,
                            UserProfileService userProfileService) {
        this.importFileMapper = importFileMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.jobMatchMapper = jobMatchMapper;
        this.userProfileService = userProfileService;
    }

    /** 最近一次导入记录及匹配统计；无导入记录时返回 null（前端显示空状态） */
    public RecentImportVO getRecentImport() {
        ImportFile latest = importFileMapper.selectLatest();
        if (latest == null) {
            return null;
        }

        return toImportVO(latest);
    }

    /** 全部导入/匹配记录，最新优先 */
    public PageVO<RecentImportVO> pageImports(int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        long total = importFileMapper.countAll();
        List<ImportFile> records = importFileMapper.selectPage((safePage - 1) * safeSize, safeSize);
        List<RecentImportVO> items = new java.util.ArrayList<>();
        for (ImportFile record : records) {
            items.add(toImportVO(record));
        }
        return new PageVO<>(total, safePage, safeSize, items);
    }

    private RecentImportVO toImportVO(ImportFile latest) {
        RecentImportVO vo = new RecentImportVO();
        vo.setImportId(latest.getId());
        vo.setFileName(latest.getOriginalName());
        vo.setSheetName(latest.getSheetName());
        vo.setTotalRows(latest.getTotalRows());
        vo.setStatus(latest.getStatus());
        vo.setCreatedAt(latest.getCreatedAt());
        vo.setJobCount(jobPositionMapper.countByImportFileId(latest.getId()));
        vo.setMatchStats(buildMatchStats(latest.getId()));
        return vo;
    }

    /** 当前档案在该批次的匹配统计；无档案时返回全 0 */
    private RecentImportVO.MatchStats buildMatchStats(Long importId) {
        UserProfile profile;
        try {
            profile = userProfileService.getProfile();
        } catch (BusinessException e) {
            if (e.getCode() == ApiResponse.CODE_PROFILE_NOT_FOUND) {
                return new RecentImportVO.MatchStats(0, 0, 0, 0);
            }
            throw e;
        }
        return sumStats(profile.getId(), importId);
    }

    private RecentImportVO.MatchStats sumStats(Long profileId, Long importId) {
        long match = 0;
        long uncertain = 0;
        long notMatch = 0;
        List<MatchResultStatRow> rows = jobMatchMapper.selectResultStats(profileId, importId);
        if (rows != null) {
            for (MatchResultStatRow row : rows) {
                long cnt = row.getCnt();
                switch (row.getMatchResult()) {
                    case "MATCH":
                        match = cnt;
                        break;
                    case "UNCERTAIN":
                        uncertain = cnt;
                        break;
                    case "NOT_MATCH":
                        notMatch = cnt;
                        break;
                    default:
                        break;
                }
            }
        }
        return new RecentImportVO.MatchStats(match + uncertain + notMatch, match, uncertain, notMatch);
    }
}
