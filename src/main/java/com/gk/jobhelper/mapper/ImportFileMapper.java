package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.ImportFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 导入文件记录表 Mapper
 */
@Mapper
public interface ImportFileMapper {

    /** 新增记录，写入后回填自增 id */
    int insert(ImportFile importFile);

    /** 按主键查询 */
    ImportFile selectById(Long id);

    /** 查询最近一条记录（测试使用） */
    ImportFile selectLatest();

    /** 导入记录分页查询（最新优先） */
    List<ImportFile> selectPage(@Param("offset") int offset, @Param("size") int size);

    long countAll();

    /** 更新导入状态（PREVIEWED / IMPORTED） */
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(Long id);

    /** 清空记录表（测试使用） */
    int deleteAll();
}
