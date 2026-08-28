package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.MajorAlias;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业别名表 Mapper
 */
@Mapper
public interface MajorAliasMapper {

    /** 目录内按标准化别名集合查询 */
    List<MajorAlias> selectByCatalogAndNames(@Param("catalogId") Long catalogId,
                                             @Param("names") List<String> names);

    /** 插入别名（测试 / 人工维护使用） */
    int insert(MajorAlias alias);

    int deleteByCatalogId(Long catalogId);

    /** 清空别名表（测试使用） */
    int deleteAll();
}
