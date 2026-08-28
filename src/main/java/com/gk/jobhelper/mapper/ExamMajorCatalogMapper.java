package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.ExamMajorCatalog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 考试与专业目录绑定表 Mapper
 */
@Mapper
public interface ExamMajorCatalogMapper {

    /** 按考试查询绑定（按 priority 升序） */
    List<ExamMajorCatalog> selectByExamId(Long examId);

    /** 插入绑定（测试 / 考试目录配置使用） */
    int insert(ExamMajorCatalog binding);

    /** 清空绑定表（测试使用） */
    int deleteAll();
}
