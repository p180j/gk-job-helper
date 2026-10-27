package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Excel 正式导入服务：
 * - getMapping: 读取上传文件表头并给出字段映射建议（EXACT/ALIAS/UNKNOWN）
 * - confirm:   按用户确认（可人工修改）的映射重新读取 Excel，转换为标准岗位并落库
 *              已导入批次不允许覆盖，以保护已有匹配、收藏及派生数据
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
    private final ObjectMapper objectMapper;

    public JobImportService(ImportFileMapper importFileMapper,
                            JobPositionMapper jobPositionMapper,
                            ExcelRowReader excelRowReader,
                            JobPositionConverter jobPositionConverter,
                            FieldMappingService fieldMappingService, JobImportAsyncService jobImportAsyncService,
                            ImportProgressStore progressStore, ExcelPreviewParser excelPreviewParser,
                            ObjectMapper objectMapper) {
        this.importFileMapper = importFileMapper;
        this.jobPositionMapper = jobPositionMapper;
        this.excelRowReader = excelRowReader;
        this.jobPositionConverter = jobPositionConverter;
        this.fieldMappingService = fieldMappingService;
        this.jobImportAsyncService = jobImportAsyncService;
        this.progressStore = progressStore;
        this.excelPreviewParser = excelPreviewParser;
        this.objectMapper = objectMapper;
    }

    /**
     * 字段映射预览
     */
    public FieldMappingPreviewVO getMapping(Long importId, List<String> requestedSheetNames) {
        ImportFile record = requireImportFile(importId);
        File file = requireFile(record);
        List<String> sheetNames = normalizeSheetNames(requestedSheetNames, record.getSheetName());
        List<ExcelRawSheet> sheets = readSheets(file, sheetNames);
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
    public ImportResultVO confirm(Long importId, ImportConfirmRequest request) {
        ImportFile record = requireImportFile(importId);
        File file = requireFile(record);
        List<String> sheetNames = normalizeSheetNames(request.getSheetNames(), request.getSheetName());
        if (sheetNames.isEmpty()) sheetNames = Collections.singletonList(record.getSheetName());
        List<ExcelRawSheet> sheets = readSheets(file, sheetNames);
        for (ExcelRawSheet sheet : sheets) {
            validateMappings(sheet, fieldMappingService.mappingsForSheet(sheet, request));
        }

        if (jobPositionMapper.countByImportFileId(importId) > 0) {
            return confirmAlreadyImported(importId, request, sheets);
        }
        if (importFileMapper.tryStartImport(importId) != 1) {
            throw new BusinessException("当前导入批次正在执行或已有完成状态，请刷新后重试");
        }

        // 创建后台任务并立即返回，前端通过 progress 接口展示实时进度。
        int totalRows = 0;
        for (ExcelRawSheet sheet : sheets) totalRows += sheet.getRows().size();
        progressStore.start(importId, totalRows);
        try {
            jobImportAsyncService.execute(importId, request);
        } catch (RuntimeException e) {
            importFileMapper.updateImportResult(importId, "IMPORT_FAILED", 0, 0, 0, e.getMessage());
            throw e;
        }

        ImportResultVO vo = new ImportResultVO();
        vo.setImportId(importId);
        vo.setTotalRows(totalRows);
        vo.setSuccessRows(0);
        vo.setFailedRows(0);
        return vo;
    }

    private ImportResultVO confirmAlreadyImported(Long importId, ImportConfirmRequest request,
                                                  List<ExcelRawSheet> sheets) {
        Map<String, ObjectNode> existing = new HashMap<>();
        for (JobPosition position : jobPositionMapper.selectByImportFileId(importId)) {
            existing.put(sourceKey(position), comparable(position));
        }
        int totalRows = 0, successRows = 0, failedRows = 0;
        for (ExcelRawSheet sheet : sheets) {
            totalRows += sheet.getRows().size();
            ConversionResult conversion = jobPositionConverter.convert(sheet,
                    fieldMappingService.mappingsForSheet(sheet, request), importId);
            successRows += conversion.getPositions().size();
            failedRows += conversion.getFailures().size();
            for (JobPosition position : conversion.getPositions()) {
                if (!comparable(position).equals(existing.remove(sourceKey(position)))) {
                    throw new BusinessException("此批次映射或来源数据与已导入岗位不同，请重新上传为新批次，避免覆盖匹配或收藏记录");
                }
            }
        }
        if (!existing.isEmpty()) {
            throw new BusinessException("此批次所选 Sheet 与已导入岗位不同，请重新上传为新批次");
        }
        progressStore.update(importId, "IMPORTED", successRows, successRows, failedRows, null);
        ImportResultVO result = new ImportResultVO();
        result.setImportId(importId);
        result.setTotalRows(totalRows);
        result.setSuccessRows(successRows);
        result.setFailedRows(failedRows);
        return result;
    }

    private String sourceKey(JobPosition position) {
        return position.getSourceSheet() + "\u0000" + position.getSourceRow();
    }

    private ObjectNode comparable(JobPosition position) {
        ObjectNode value = objectMapper.valueToTree(position);
        value.remove("id");
        value.remove("createdAt");
        value.remove("updatedAt");
        return value;
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

    private List<ExcelSheetPreviewVO> toSheetPreviews(File file, String defaultSheetName) {
        List<ExcelSheetPreviewVO> previews = new ArrayList<>();
        for (ExcelPreviewResult sheet : excelPreviewParser.parseAll(file)) {
            ExcelSheetPreviewVO preview = new ExcelSheetPreviewVO();
            preview.setSheetName(sheet.getSheetName()); preview.setHeaders(sheet.getHeaders());
            preview.setTotalRows(sheet.getTotalRows());
            preview.setParseError(sheet.getParseError());
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
        if (mappings == null || mappings.isEmpty()) throw new BusinessException("Sheet “" + sheet.getSheetName() + "”没有可用字段映射");
        List<String> headers = sheet.getHeaders();
        java.util.Set<String> mapped = new java.util.HashSet<>();
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
            if (target != null && !target.trim().isEmpty()) mapped.add(target.trim());
        }
        if (!mapped.contains("positionCode") || !mapped.contains("positionName")
                || !mapped.contains("departmentName") && !mapped.contains("departmentCode")) {
            throw new BusinessException("Sheet “" + sheet.getSheetName() + "”缺少职位代码、职位名称或招录机关的可靠映射，请先确认表头");
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
