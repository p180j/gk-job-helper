package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.MajorCatalog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业目录表 Mapper
 */
@Mapper
public interface MajorCatalogMapper {

    /** 全部目录（按 priority 升序） */
    List<MajorCatalog> selectAll();

    /** 按主键查询 */
    MajorCatalog selectById(Long id);

    /** 按稳定目录编码查询（离线官方目录初始化使用） */
    MajorCatalog selectByCode(@Param("catalogCode") String catalogCode);

    /** 已启用目录：按类型 + 学历层级过滤（层级为空时不过滤层级，MIXED 始终包含），按 priority 升序 */
    List<MajorCatalog> selectEnabledByTypeAndLevel(@Param("catalogType") String catalogType,
                                                   @Param("educationLevel") String educationLevel);

    /** 插入目录（测试 / 后续目录导入使用） */
    int insert(MajorCatalog catalog);

    /** 清空目录表（测试使用） */
    int deleteAll();
}
