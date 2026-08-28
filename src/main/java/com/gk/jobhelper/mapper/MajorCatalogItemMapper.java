package com.gk.jobhelper.mapper;

import com.gk.jobhelper.dto.MajorCatalogItemVO;
import com.gk.jobhelper.dto.MajorSearchItemVO;
import com.gk.jobhelper.entity.MajorCatalogItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业目录节点表 Mapper
 */
@Mapper
public interface MajorCatalogItemMapper {

    /** 按主键查询 */
    MajorCatalogItem selectById(Long id);

    /** 目录内按代码精确查询（标准化代码） */
    MajorCatalogItem selectByCatalogAndCode(@Param("catalogId") Long catalogId,
                                            @Param("majorCode") String majorCode);

    /** 目录内按标准化名称集合查询（含"去专业后缀"比较值，可能多条） */
    List<MajorCatalogItem> selectByCatalogAndNames(@Param("catalogId") Long catalogId,
                                                   @Param("names") List<String> names);

    /** 目录内按别名标准化值集合查询（JOIN major_alias，返回命中的目录节点） */
    List<MajorCatalogItem> selectByCatalogAndAliasNames(@Param("catalogId") Long catalogId,
                                                        @Param("names") List<String> names);

    /** 目录内按父节点查询直接子节点（按 sort_no, id 升序） */
    List<MajorCatalogItem> selectChildren(@Param("catalogId") Long catalogId,
                                          @Param("parentId") Long parentId);

    /**
     * 目录节点分页查询（keyword 模糊名称/精确代码，majorCode 精确，majorName 模糊），
     * 附带父节点名称，供目录管理接口使用。
     */
    List<MajorCatalogItemVO> selectItemsByCatalogId(@Param("catalogId") Long catalogId,
                                                    @Param("keyword") String keyword,
                                                    @Param("majorCode") String majorCode,
                                                    @Param("majorName") String majorName,
                                                    @Param("offset") int offset,
                                                    @Param("size") int size);

    /** 节点分页查询计数 */
    long countItemsByCatalogId(@Param("catalogId") Long catalogId,
                               @Param("keyword") String keyword,
                               @Param("majorCode") String majorCode,
                               @Param("majorName") String majorName);

    /**
     * 跨目录专业检索：代码精确 / 名称模糊 / 别名精确，仅启用目录，
     * 返回目录 + 节点 + 父级信息，按目录优先级排序，限制返回条数。
     */
    List<MajorSearchItemVO> searchItems(@Param("keyword") String keyword,
                                        @Param("normalizedKeyword") String normalizedKeyword,
                                        @Param("limit") int limit);

    /** 插入节点（测试 / 后续目录导入使用） */
    int insert(MajorCatalogItem item);

    long countByCatalogId(Long catalogId);

    int deleteByCatalogId(Long catalogId);

    /** 清空节点表（测试使用） */
    int deleteAll();
}
