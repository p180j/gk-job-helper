package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.dto.ExcelSheetPreviewVO;
import com.gk.jobhelper.config.UploadProperties;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import com.gk.jobhelper.dto.ExcelPreviewVO;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobMatchMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Excel 上传业务：
 * 1. 校验文件后缀（仅 .xls / .xlsx）
 * 2. 原始文件保存至本地 uploads 目录
 * 3. 解析表头与数据行生成预览（不写岗位表）
 * 4. 记录 import_file（状态 PREVIEWED），返回带 fileId 的预览数据
 */
@Service
public class ExcelImportService {

    private static final int MAX_RECENT_IMPORTS = 5;
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList(".xls", ".xlsx"));

    private final ExcelPreviewParser excelPreviewParser;
    private final ImportFileMapper importFileMapper;
    private final UploadProperties uploadProperties;
    private final JobPositionMapper jobPositionMapper;
    private final JobMatchMapper jobMatchMapper;
    private final FieldMappingService fieldMappingService;
    private final ExcelSheetAiAssistant excelSheetAiAssistant;

    public ExcelImportService(ExcelPreviewParser excelPreviewParser,
                              ImportFileMapper importFileMapper,
                              UploadProperties uploadProperties,
                              JobPositionMapper jobPositionMapper,
                              JobMatchMapper jobMatchMapper, FieldMappingService fieldMappingService,
                              ExcelSheetAiAssistant excelSheetAiAssistant) {
        this.excelPreviewParser = excelPreviewParser;
        this.importFileMapper = importFileMapper;
        this.uploadProperties = uploadProperties;
        this.jobPositionMapper = jobPositionMapper;
        this.jobMatchMapper = jobMatchMapper;
        this.fieldMappingService = fieldMappingService;
        this.excelSheetAiAssistant = excelSheetAiAssistant;
    }

    public ExcelPreviewVO upload(MultipartFile file, AiProviderConfig aiConfig) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new BusinessException("无法获取上传文件名");
        }
        String extension = extensionOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 .xls / .xlsx 格式的 Excel 文件");
        }

        // 1. 保存原始文件到本地 uploads 目录
        Path storedFile = saveFile(file, extension);

        // 2. 解析预览（解析失败则删除已保存文件）
        List<ExcelPreviewResult> results;
        try {
            results = excelPreviewParser.parseAll(storedFile.toFile());
        } catch (RuntimeException e) {
            deleteQuietly(storedFile);
            throw e;
        }

        List<String> aiSuggestedSheets = excelSheetAiAssistant.suggestPositionSheets(aiConfig, results);
        List<ExcelSheetPreviewVO> sheets = toSheets(results, aiSuggestedSheets);
        ExcelPreviewResult primary = choosePrimary(results, sheets);

        // 3. 记录导入文件元信息（默认 Sheet 供旧流程兼容，后续可选择多个 Sheet）
        ImportFile record = new ImportFile();
        record.setOriginalName(fileNameOnly(originalName));
        record.setStoredName(storedFile.getFileName().toString());
        record.setStoredPath(storedFile.toString());
        record.setFileSize(file.getSize());
        record.setFileType(extension);
        record.setSheetName(primary.getSheetName());
        record.setHeaders(String.join(",", primary.getHeaders()));
        record.setTotalRows(primary.getTotalRows());
        record.setExamYear(ImportExamYearResolver.resolve(fileNameOnly(originalName)));
        record.setStatus("PREVIEWED");
        record.setCreatedAt(LocalDateTime.now());
        importFileMapper.insert(record);

        return toVO(record, primary, sheets);
    }

    /** 删除一条导入记录及其岗位、匹配结果和已保存的原始文件。 */
    @Transactional
    public void deleteImport(Long importId) {
        ImportFile record = importFileMapper.selectById(importId);
        if (record == null) {
            throw new BusinessException(ApiResponse.CODE_IMPORT_NOT_FOUND, "导入记录不存在: id=" + importId);
        }
        jobMatchMapper.deleteItemsByImportFileId(importId);
        jobMatchMapper.deleteByImportFileId(importId);
        jobPositionMapper.deleteByImportFileId(importId);
        importFileMapper.deleteById(importId);
        deleteQuietly(Paths.get(record.getStoredPath()));
    }

    /** 保留最新上传的五条导入记录，清理更早记录及其关联岗位、匹配结果和原文件。 */
    @Transactional
    public void retainMostRecentImports() {
        long count = importFileMapper.countAll();
        if (count <= MAX_RECENT_IMPORTS) {
            return;
        }
        int excess = (int) Math.min(Integer.MAX_VALUE, count - MAX_RECENT_IMPORTS);
        for (ImportFile record : importFileMapper.selectPage(MAX_RECENT_IMPORTS, excess)) {
            deleteImport(record.getId());
        }
    }

    public ImportFile getImportFile(Long importId) {
        ImportFile record = importFileMapper.selectById(importId);
        if (record == null) {
            throw new BusinessException(ApiResponse.CODE_IMPORT_NOT_FOUND, "导入记录不存在: id=" + importId);
        }
        return record;
    }

    private Path saveFile(MultipartFile file, String extension) {
        try {
            Path uploadDir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String storedName = System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        } catch (IOException e) {
            throw new BusinessException("保存上传文件失败: " + e.getMessage());
        }
    }

    private ExcelPreviewVO toVO(ImportFile record, ExcelPreviewResult result, List<ExcelSheetPreviewVO> sheets) {
        ExcelPreviewVO vo = new ExcelPreviewVO();
        vo.setFileId(record.getId());
        vo.setFileName(record.getOriginalName());
        vo.setFileType(record.getFileType());
        vo.setSheetName(result.getSheetName());
        vo.setHeaders(result.getHeaders());
        vo.setTotalRows(result.getTotalRows());
        List<String> headers = result.getHeaders();
        List<Map<String, String>> previewRows = new ArrayList<>();
        for (Map<Integer, String> rawRow : result.getPreviewRows()) {
            Map<String, String> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                rowMap.put(headers.get(i), rawRow.get(i) == null ? "" : rawRow.get(i));
            }
            // 超出表头长度的列以"列N"保留，避免丢失原始数据
            for (Map.Entry<Integer, String> entry : rawRow.entrySet()) {
                if (entry.getKey() >= headers.size()) {
                    rowMap.put("列" + (entry.getKey() + 1),
                            entry.getValue() == null ? "" : entry.getValue());
                }
            }
            previewRows.add(rowMap);
        }
        vo.setPreviewRows(previewRows);
        vo.setSheets(sheets);
        return vo;
    }

    private List<ExcelSheetPreviewVO> toSheets(List<ExcelPreviewResult> results, List<String> aiSuggestedSheets) {
        List<ExcelSheetPreviewVO> sheets = new ArrayList<>();
        for (ExcelPreviewResult result : results) {
            ExcelSheetPreviewVO sheet = new ExcelSheetPreviewVO();
            sheet.setSheetName(result.getSheetName());
            sheet.setHeaders(result.getHeaders());
            sheet.setTotalRows(result.getTotalRows());
            sheet.setSuggestedForImport(aiSuggestedSheets.contains(result.getSheetName()) || isPositionLike(result));
            List<Map<String, String>> previewRows = new ArrayList<>();
            for (Map<Integer, String> rawRow : result.getPreviewRows()) {
                previewRows.add(toRowMap(result.getHeaders(), rawRow));
            }
            sheet.setPreviewRows(previewRows);
            sheets.add(sheet);
        }
        return sheets;
    }

    private ExcelPreviewResult choosePrimary(List<ExcelPreviewResult> results, List<ExcelSheetPreviewVO> sheets) {
        for (int i = 0; i < sheets.size(); i++) {
            if (sheets.get(i).isSuggestedForImport()) return results.get(i);
        }
        return results.get(0);
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

    private Map<String, String> toRowMap(List<String> headers, Map<Integer, String> rawRow) {
        Map<String, String> rowMap = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) rowMap.put(headers.get(i), rawRow.get(i) == null ? "" : rawRow.get(i));
        return rowMap;
    }

    private String extensionOf(String name) {
        int index = name.lastIndexOf('.');
        if (index < 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index).toLowerCase(Locale.ROOT);
    }

    private String fileNameOnly(String name) {
        // 去除可能的路径部分，防止路径注入
        String cleaned = name.replace('\\', '/');
        int index = cleaned.lastIndexOf('/');
        return index < 0 ? cleaned : cleaned.substring(index + 1);
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignore) {
            // 忽略清理失败
        }
    }
}
