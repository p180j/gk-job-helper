package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.FieldMappingPreviewVO;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.dto.ImportResultVO;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 正式导入服务：
 * - getMapping: 读取上传文件表头并给出字段映射建议（EXACT/ALIAS/UNKNOWN）
 * - confirm:   按用户确认（可人工修改）的映射重新读取 Excel，转换为标准岗位并落库
 *              重复 confirm 会先删除该文件旧数据再导入（幂等，支持调整映射后重导）
 */
@Service
public class JobImportService {

    /** 批量插入每批大小 */
    private static final int BATCH_SIZE = 200;

    private final ImportFileMapper importFileMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ExcelRowReader excelRowReader;
    private final JobPositionConverter jobPositionConverter;
    private final FieldMappingService fieldMappingService;
    private final JobImportAsyncService jobImportAsyncService;
    private final ImportProgressStore progressStore;

    public JobImportService(ImportFileMapper importFileMapper,
                            JobPositionMapper jobPositionMapper,
                            ExcelRowReader excelRowReader,
                            JobPositionConverter jobPositionConverter,
                            FieldMappingService fieldMappingService, JobImportAsyncService jobImportAsyncService,
                            ImportProgressStore progressStore) {
        this.importFileMapper = importFileMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.excelRowReader = excelRowReader;
        this.jobPositionConverter = jobPositionConverter;
        this.fieldMappingService = fieldMappingService;
        this.jobImportAsyncService = jobImportAsyncService;
        this.progressStore = progressStore;
    }

    /**
     * 字段映射预览
     */
    public FieldMappingPreviewVO getMapping(Long importId) {
        ImportFile record = requireImportFile(importId);
        File file = requireFile(record);
        ExcelRawSheet sheet = excelRowReader.read(file, record.getSheetName());

        FieldMappingPreviewVO vo = new FieldMappingPreviewVO();
        vo.setImportId(importId);
        vo.setSheetName(sheet.getSheetName());
        vo.setHeaders(fieldMappingService.suggestAll(sheet.getHeaders()));
        return vo;
    }

    /**
     * 正式导入
     */
    @Transactional
    public ImportResultVO confirm(Long importId, ImportConfirmRequest request) {
        ImportFile record = requireImportFile(importId);
        File file = requireFile(record);

        // 确定读取的 Sheet：请求指定优先，否则用上传时记录的 Sheet
        String sheetName = request.getSheetName() == null || request.getSheetName().trim().isEmpty()
                ? record.getSheetName() : request.getSheetName().trim();
        ExcelRawSheet sheet = excelRowReader.read(file, sheetName);

        // 校验用户映射：sourceField 必须存在于表头，targetField 必须为合法标准字段或空
        validateMappings(sheet, request.getMappings());

        // 创建后台任务并立即返回，前端通过 progress 接口展示实时进度。
        progressStore.start(importId, record.getTotalRows() == null ? 0 : record.getTotalRows());
        jobImportAsyncService.execute(importId, request);

        ImportResultVO vo = new ImportResultVO();
        vo.setImportId(importId);
        vo.setTotalRows(record.getTotalRows() == null ? 0 : record.getTotalRows());
        vo.setSuccessRows(0);
        vo.setFailedRows(0);
        return vo;
    }

    private void validateMappings(ExcelRawSheet sheet, List<ImportConfirmRequest.MappingItem> mappings) {
        List<String> headers = sheet.getHeaders();
        for (ImportConfirmRequest.MappingItem mapping : mappings) {
            String source = mapping.getSourceField() == null ? "" : mapping.getSourceField().trim();
            if (source.isEmpty()) {
                throw new BusinessException("映射的 sourceField 不能为空");
            }
            if (fieldMappingService.findColumnIndex(headers, source) < 0) {
                throw new BusinessException("表头中不存在字段: " + source);
            }
            String target = mapping.getTargetField();
            if (target != null && !target.trim().isEmpty()
                    && !fieldMappingService.isKnownTargetField(target.trim())) {
                throw new BusinessException("非法的目标字段: " + target
                        + "，必须为标准字段英文名或留空表示不导入");
            }
        }
    }

    private ImportFile requireImportFile(Long importId) {
        ImportFile record = importFileMapper.selectById(importId);
        if (record == null) {
            throw new BusinessException(ApiResponse.CODE_IMPORT_NOT_FOUND, "导入记录不存在: " + importId);
        }
        return record;
    }

    private File requireFile(ImportFile record) {
        File file = new File(record.getStoredPath());
        if (!file.exists() || !file.isFile()) {
            throw new BusinessException("上传文件已不存在: " + record.getStoredPath());
        }
        return file;
    }

    /** 仅用于测试清库 */
    public List<JobPosition> listByImportFileId(Long importId) {
        return importId == null ? new ArrayList<>() : jobPositionMapper.selectByImportFileId(importId);
    }
}
