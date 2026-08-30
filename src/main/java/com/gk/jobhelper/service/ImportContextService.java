package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import org.springframework.stereotype.Service;

/** 为优选、进面导入等功能提供统一的岗位表上下文。 */
@Service
public class ImportContextService {
    private final ImportFileMapper importFileMapper;

    public ImportContextService(ImportFileMapper importFileMapper) {
        this.importFileMapper = importFileMapper;
    }

    public ImportFile requireImport(Long importId) {
        if (importId == null) throw new BusinessException("请选择当前岗位表");
        ImportFile record = importFileMapper.selectById(importId);
        if (record == null) {
            throw new BusinessException(ApiResponse.CODE_IMPORT_NOT_FOUND, "岗位表不存在: id=" + importId);
        }
        if (!"IMPORTED".equals(record.getStatus())) {
            throw new BusinessException("当前岗位表尚未完成导入");
        }
        return record;
    }

    public int requireExamYear(Long importId) {
        ImportFile record = requireImport(importId);
        Integer year = ImportExamYearResolver.resolve(record);
        if (year == null) {
            throw new BusinessException("无法从当前岗位表识别考试年度，请重新上传名称包含四位年度的职位表");
        }
        return year;
    }
}
