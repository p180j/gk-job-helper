package com.gk.jobhelper.mapper;

import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.MatchResultQuery;
import com.gk.jobhelper.dto.MatchResultStatRow;
import com.gk.jobhelper.entity.JobMatch;
import com.gk.jobhelper.entity.JobMatchItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位匹配结果 Mapper
 */
@Mapper
public interface JobMatchMapper {

    /** 按档案+导入批次分组统计各匹配结果数量（首页最近分析卡片用） */
    List<MatchResultStatRow> selectResultStats(@Param("profileId") Long profileId,
                                               @Param("importFileId") Long importFileId);

    /** 查询档案+岗位的最新匹配记录（一个组合仅保留一条） */
    JobMatch selectByProfileAndPosition(@Param("profileId") Long profileId,
                                        @Param("jobPositionId") Long jobPositionId);

    int insert(JobMatch record);

    /** 覆盖更新匹配结果（幂等重匹配） */
    int updateMatch(JobMatch record);

    List<JobMatchItem> selectItemsByMatchId(@Param("jobMatchId") Long jobMatchId);

    int deleteItemsByMatchId(@Param("jobMatchId") Long jobMatchId);

    /** 删除指定导入批次的匹配明细与汇总（删除导入记录前调用） */
    int deleteItemsByImportFileId(@Param("importFileId") Long importFileId);

    int deleteByImportFileId(@Param("importFileId") Long importFileId);

    int insertItems(@Param("items") List<JobMatchItem> items);

    long countResultPage(MatchResultQuery query);

    /** 分页查询匹配结果（关联岗位基本信息） */
    List<MatchPositionResultVO> selectResultPage(MatchResultQuery query);

    /** 清库（测试用） */
    void deleteAllItems();

    void deleteAll();
}
