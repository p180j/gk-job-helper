package com.gk.jobhelper.mapper;

import com.gk.jobhelper.dto.JobPositionQuery;
import com.gk.jobhelper.entity.JobPosition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 岗位表 Mapper
 */
@Mapper
public interface JobPositionMapper {

    /** 批量插入（每批控制在数百行） */
    int insertBatch(List<JobPosition> positions);

    /** 按主键查询（含 raw_data） */
    JobPosition selectById(Long id);

    /** 按导入文件查询（按来源行号排序，含 raw_data） */
    List<JobPosition> selectByImportFileId(Long importFileId);

    /** 指定导入批次的岗位总数（批量匹配用） */
    long countByImportFileId(Long importFileId);

    /** 分页加载指定导入批次岗位（不含 raw_data 大字段，批量匹配用） */
    List<JobPosition> selectPageByImportFileId(@Param("importFileId") Long importFileId,
                                               @Param("offset") int offset,
                                               @Param("size") int size);

    /** 分页条件查询（列表不含 raw_data） */
    List<JobPosition> selectByCondition(JobPositionQuery query);

    /** 条件计数 */
    long countByCondition(JobPositionQuery query);

    /** 删除某次导入的全部岗位（重复导入前清理） */
    int deleteByImportFileId(Long importFileId);

    /** 清空岗位表（测试使用） */
    int deleteAll();
}
