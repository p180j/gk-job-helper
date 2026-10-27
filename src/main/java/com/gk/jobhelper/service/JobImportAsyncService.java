package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.dto.ImportFailedItem;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/** 单线程后台岗位导入，避免长 Excel 导入阻塞 HTTP 请求线程。 */
@Service
public class JobImportAsyncService {
    private static final int BATCH_SIZE = 200;
    private final ImportFileMapper importFileMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ExcelRowReader excelRowReader;
    private final JobPositionConverter jobPositionConverter;
    private final ImportProgressStore progressStore;
    private final FieldMappingService fieldMappingService;
    private final TransactionTemplate transactionTemplate;

    public JobImportAsyncService(ImportFileMapper importFileMapper, JobPositionMapper jobPositionMapper,
                                 ExcelRowReader excelRowReader, JobPositionConverter jobPositionConverter, ImportProgressStore progressStore,
                                 FieldMappingService fieldMappingService,
                                 TransactionTemplate transactionTemplate) {
        this.importFileMapper = importFileMapper; this.jobPositionMapper = jobPositionMapper;
        this.excelRowReader = excelRowReader; this.jobPositionConverter = jobPositionConverter;
        this.progressStore = progressStore;
        this.fieldMappingService = fieldMappingService;
        this.transactionTemplate = transactionTemplate;
    }

    @Async("importExecutor")
    public void execute(Long importId, ImportConfirmRequest request) {
        try {
            ImportFile record = importFileMapper.selectById(importId);
            if (record == null) return;
            List<String> sheetNames = new ArrayList<>(new LinkedHashSet<>(request.getSheetNames()));
            if (sheetNames.isEmpty()) sheetNames.add(request.getSheetName() == null || request.getSheetName().trim().isEmpty()
                    ? record.getSheetName() : request.getSheetName().trim());
            List<JobPosition> positions = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            int failedRows = 0;
            for (String sheetName : sheetNames) {
                ExcelRawSheet sheet = excelRowReader.read(new File(record.getStoredPath()), sheetName);
                ConversionResult conversion = jobPositionConverter.convert(sheet,
                        fieldMappingService.mappingsForSheet(sheet, request), importId);
                positions.addAll(conversion.getPositions());
                failedRows += conversion.getFailures().size();
                for (ImportFailedItem failure : conversion.getFailures()) {
                    if (failures.size() < 10) failures.add(sheetName + " 第" + failure.getRow() + "行：" + failure.getReason());
                }
            }
            final int rejectedRows = failedRows;
            final String rowErrors = failures.isEmpty() ? null : truncate(String.join("；", failures));
            transactionTemplate.execute(status -> {
                if (jobPositionMapper.countByImportFileId(importId) > 0) {
                    throw new IllegalStateException("此批次已有岗位数据，请重新上传文件创建新批次，避免覆盖匹配或收藏记录");
                }
                for (int start = 0; start < positions.size(); start += BATCH_SIZE) {
                    int end = Math.min(start + BATCH_SIZE, positions.size());
                    jobPositionMapper.insertBatch(positions.subList(start, end));
                    progressStore.update(importId, "IMPORTING", end, end, rejectedRows, null);
                }
                importFileMapper.updateImportResult(importId, "IMPORTED", positions.size(),
                        positions.size(), rejectedRows, rowErrors);
                return null;
            });
            progressStore.update(importId, "IMPORTED", positions.size(), positions.size(), failedRows, rowErrors);
        } catch (Exception e) {
            String reason = truncate(e.getMessage() == null ? "导入失败" : e.getMessage());
            importFileMapper.updateImportResult(importId, "IMPORT_FAILED", 0, 0, 0, reason);
            progressStore.update(importId, "IMPORT_FAILED", 0, 0, 0, reason);
        }
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
