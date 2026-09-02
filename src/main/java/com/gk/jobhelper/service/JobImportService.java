package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import com.gk.jobhelper.dto.ExcelSheetPreviewVO;
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
import java.util.Collections;
import java.util.LinkedHashSet;
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
    private final ExcelPreviewParser excelPreviewParser;

    public JobImportService(ImportFileMapper importFileMapper,
                            JobPositionMapper jobPositionMapper,
                            ExcelRowReader excelRowReader,
                            JobPositionConverter jobPositionConverter,
                            FieldMappingService fieldMappingService, JobImportAsyncService jobImportAsyncService,
                            ImportProgressStore progressStore, ExcelPreviewParser excelPreviewParser) {
        this.importFileMapper = importFileMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.excelRowReader = excelRowReader;
        this.jobPositionConverter = jobPositionConverter;
        this.fieldMappingService = fieldMappingService;
        this.jobImportAsyncService = jobImportAsyncService;
        this.progressStore = progressStore;
        this.excelPreviewParser = excelPreviewParser;
    }

    /**
     * 字段映射预览
     */
    public FieldMappingPreviewVO getMapping(Long importId, List<String> requestedSheetNames) {
        ImportFile record = requireImportFile(importId);
        File file = requireFile(record);
        List<String> sheetNames = normalizeSheetNames(requestedSheetNames, record.getSheetName());
        List<ExcelRawSheet> sheets = readSheets(file, sheetNames);
        validateSameHeaders(sheets);
        ExcelRawSheet sheet = sheets.get(0);

        FieldMappingPreviewVO vo = new FieldMappingPreviewVO();
        vo.setImportId(importId);
        vo.setSheetName(sheet.getSheetName());
        vo.setSheets(toSheetPreviews(file, record.getSheetName()));
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

        List<String> sheetNames = normalizeSheetNames(request.getSheetNames(), request.getSheetName());
        if (sheetNames.isEmpty()) sheetNames = Collections.singletonList(record.getSheetName());
        List<ExcelRawSheet> sheets = readSheets(file, sheetNames);
        validateSameHeaders(sheets);

        for (ExcelRawSheet sheet : sheets) validateMappings(sheet, request.getMappings());

        // 创建后台任务并立即返回，前端通过 progress 接口展示实时进度。
        int totalRows = 0;
        for (ExcelRawSheet sheet : sheets) totalRows += sheet.getRows().size();
        progressStore.start(importId, totalRows);
        jobImportAsyncService.execute(importId, request);

        ImportResultVO vo = new ImportResultVO();
        vo.setImportId(importId);
        vo.setTotalRows(totalRows);
        vo.setSuccessRows(0);
        vo.setFailedRows(0);
        return vo;
    }

    private List<String> normalizeSheetNames(List<String> sheetNames, String fallback) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (sheetNames != null) for (String name : sheetNames) if (name != null && !name.trim().isEmpty()) names.add(name.trim());
        if (names.isEmpty() && fallback != null && !fallback.trim().isEmpty()) names.add(fallback.trim());
        return new ArrayList<>(names);
    }

    private List<ExcelRawSheet> readSheets(File file, List<String> sheetNames) {
        List<ExcelRawSheet> sheets = new ArrayList<>();
        for (String sheetName : sheetNames) sheets.add(excelRowReader.read(file, sheetName));
        if (sheets.isEmpty()) throw new BusinessException("请至少选择一个职位 Sheet");
        return sheets;
    }

    private void validateSameHeaders(List<ExcelRawSheet> sheets) {
        List<String> expected = sheets.get(0).getHeaders();
        for (int i = 1; i < sheets.size(); i++) {
            List<String> headers = sheets.get(i).getHeaders();
            if (expected.size() != headers.size()) throw new BusinessException("所选 Sheet 表头不一致，请分别导入: " + sheets.get(i).getSheetName());
            for (int column = 0; column < expected.size(); column++) {
                if (!com.gk.jobhelper.constant.PositionStandardField.normalize(expected.get(column))
                        .equals(com.gk.jobhelper.constant.PositionStandardField.normalize(headers.get(column)))) {
                    throw new BusinessException("所选 Sheet 表头不一致，请分别导入: " + sheets.get(i).getSheetName());
                }
            }
        }
    }

    private List<ExcelSheetPreviewVO> toSheetPreviews(File file, String defaultSheetName) {
        List<ExcelSheetPreviewVO> previews = new ArrayList<>();
        for (ExcelPreviewResult sheet : excelPreviewParser.parseAll(file)) {
            ExcelSheetPreviewVO preview = new ExcelSheetPreviewVO();
            preview.setSheetName(sheet.getSheetName()); preview.setHeaders(sheet.getHeaders());
            preview.setTotalRows(sheet.getTotalRows());
            preview.setSuggestedForImport(isPositionLike(sheet));
            previews.add(preview);
        }
        return previews;
    }

    private boolean isPositionLike(ExcelPreviewResult sheet) {
        int recognized = 0;
        boolean hasPositionName = false;
        for (String header : sheet.getHeaders()) {
            String suggested = fieldMappingService.suggest(header).getSuggestedField();
            if (suggested != null) recognized++;
            if ("positionName".equals(suggested)) hasPositionName = true;
        }
        return hasPositionName && recognized >= 2 && sheet.getTotalRows() > 0;
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
