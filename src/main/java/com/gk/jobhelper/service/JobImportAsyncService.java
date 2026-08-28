package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

/** 单线程后台岗位导入，避免长 Excel 导入阻塞 HTTP 请求线程。 */
@Service
public class JobImportAsyncService {
    private static final int BATCH_SIZE = 200;
    private final ImportFileMapper importFileMapper;
    private final JobPositionMapper jobPositionMapper;
    private final ExcelRowReader excelRowReader;
    private final JobPositionConverter jobPositionConverter;
    private final ImportProgressStore progressStore;

    public JobImportAsyncService(ImportFileMapper importFileMapper, JobPositionMapper jobPositionMapper,
                                 ExcelRowReader excelRowReader, JobPositionConverter jobPositionConverter, ImportProgressStore progressStore) {
        this.importFileMapper = importFileMapper; this.jobPositionMapper = jobPositionMapper;
        this.excelRowReader = excelRowReader; this.jobPositionConverter = jobPositionConverter;
        this.progressStore = progressStore;
    }

    @Async("importExecutor")
    public void execute(Long importId, ImportConfirmRequest request) {
        try {
            ImportFile record = importFileMapper.selectById(importId);
            if (record == null) return;
            ExcelRawSheet sheet = excelRowReader.read(new File(record.getStoredPath()), record.getSheetName());
            ConversionResult conversion = jobPositionConverter.convert(sheet, request.getMappings(), importId);
            List<JobPosition> positions = conversion.getPositions();
            jobPositionMapper.deleteByImportFileId(importId);
            int processed = 0;
            for (int start = 0; start < positions.size(); start += BATCH_SIZE) {
                int end = Math.min(start + BATCH_SIZE, positions.size());
                jobPositionMapper.insertBatch(positions.subList(start, end));
                processed = end;
                progressStore.update(importId, "IMPORTING", processed, processed, conversion.getFailures().size(), null);
            }
            importFileMapper.updateStatus(importId, "IMPORTED");
            progressStore.update(importId, "IMPORTED", positions.size(), positions.size(), conversion.getFailures().size(), null);
        } catch (Exception e) {
            ImportFile current = progressStore.get(importId);
            int processed = current == null || current.getProcessedRows() == null ? 0 : current.getProcessedRows();
            progressStore.update(importId, "IMPORT_FAILED", processed, processed, 0, e.getMessage() == null ? "导入失败" : e.getMessage());
        }
    }
}
