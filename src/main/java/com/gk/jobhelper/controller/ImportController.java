package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.ExcelPreviewVO;
import com.gk.jobhelper.dto.FieldMappingPreviewVO;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.dto.ImportResultVO;
import com.gk.jobhelper.dto.RecentImportVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.service.DashboardService;
import com.gk.jobhelper.service.ExcelImportService;
import com.gk.jobhelper.service.JobImportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

/**
 * Excel 导入接口
 * POST /api/import/upload         上传 .xls / .xlsx 文件并返回预览（不写岗位表）
 * GET  /api/import/{id}/mapping   字段映射建议预览（EXACT/ALIAS/UNKNOWN）
 * POST /api/import/{id}/confirm   按确认映射正式导入岗位
 */
@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ExcelImportService excelImportService;
    private final JobImportService jobImportService;
    private final DashboardService dashboardService;

    public ImportController(ExcelImportService excelImportService, JobImportService jobImportService,
                            DashboardService dashboardService) {
        this.excelImportService = excelImportService;
        this.jobImportService = jobImportService;
        this.dashboardService = dashboardService;
    }

    /** 首页"最近分析"卡片：最近一次导入记录 + 当前档案匹配统计；无记录时 data 为 null */
    @GetMapping("/recent")
    public ApiResponse<RecentImportVO> recent() {
        return ApiResponse.ok(dashboardService.getRecentImport());
    }

    /** 我的全部职位表/匹配记录（最新优先） */
    @GetMapping
    public ApiResponse<PageVO<RecentImportVO>> page(@RequestParam(value = "page", defaultValue = "1") int page,
                                                     @RequestParam(value = "size", defaultValue = "10") int size) {
        return ApiResponse.ok(dashboardService.pageImports(page, size));
    }

    /** 删除指定职位表及其导入岗位、匹配结果 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        excelImportService.deleteImport(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/upload")
    public ApiResponse<ExcelPreviewVO> upload(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(excelImportService.upload(file));
    }

    @GetMapping("/{id}/mapping")
    public ApiResponse<FieldMappingPreviewVO> getMapping(@PathVariable("id") Long id) {
        return ApiResponse.ok(jobImportService.getMapping(id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ImportResultVO> confirm(@PathVariable("id") Long id,
                                               @Valid @RequestBody ImportConfirmRequest request) {
        return ApiResponse.ok(jobImportService.confirm(id, request));
    }
}
