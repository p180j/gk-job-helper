package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.MajorCatalogItemVO;
import com.gk.jobhelper.dto.MajorSearchItemVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.service.MajorCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 专业目录查询接口（Iteration 4，仅后端基础接口，不含管理后台页面）
 * GET /api/major/catalogs                   全部专业目录（按 priority 升序）
 * GET /api/major/catalogs/{catalogId}/items 目录节点分页查询（keyword/majorCode/majorName 过滤）
 * GET /api/major/search                     跨目录专业检索（返回目录、代码、名称、父级）
 */
@RestController
@RequestMapping("/api/major")
public class MajorCatalogController {

    private final MajorCatalogService majorCatalogService;

    public MajorCatalogController(MajorCatalogService majorCatalogService) {
        this.majorCatalogService = majorCatalogService;
    }

    @GetMapping("/catalogs")
    public ApiResponse<List<MajorCatalog>> catalogs() {
        return ApiResponse.ok(majorCatalogService.listCatalogs());
    }

    @GetMapping("/catalogs/{catalogId}/items")
    public ApiResponse<PageVO<MajorCatalogItemVO>> items(
            @PathVariable("catalogId") Long catalogId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "majorCode", required = false) String majorCode,
            @RequestParam(value = "majorName", required = false) String majorName,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(majorCatalogService.pageItems(catalogId, keyword, majorCode,
                majorName, page, size));
    }

    @GetMapping("/search")
    public ApiResponse<List<MajorSearchItemVO>> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return ApiResponse.ok(majorCatalogService.search(keyword, limit));
    }
}
