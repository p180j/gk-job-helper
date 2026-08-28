package com.gk.jobhelper.mapper;

import com.gk.jobhelper.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户档案表 Mapper
 */
@Mapper
public interface UserProfileMapper {

    /** 新增档案，写入后回填自增 id */
    int insert(UserProfile profile);

    /** 按主键更新（动态 SET，仅更新非空字段） */
    int updateById(UserProfile profile);

    /** 按主键查询 */
    UserProfile selectById(Long id);

    /** 查询第一条档案（Iteration 1 单档案模式） */
    UserProfile selectFirstProfile();

    /** 清空档案表（测试使用） */
    int deleteAll();
}
