package com.gk.jobhelper.controller;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.dto.MatchPositionResultVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.service.JobFavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    private final JobFavoriteService favoriteService;

    public FavoriteController(JobFavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping("/{positionId}")
    public ApiResponse<Void> favorite(@PathVariable("positionId") Long positionId,
                                      @RequestParam("profileId") Long profileId) {
        favoriteService.favorite(profileId, positionId);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{positionId}")
    public ApiResponse<Void> unfavorite(@PathVariable("positionId") Long positionId,
                                        @RequestParam("profileId") Long profileId) {
        favoriteService.unfavorite(profileId, positionId);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<PageVO<MatchPositionResultVO>> page(
            @RequestParam("profileId") Long profileId,
            @RequestParam(value = "importId", required = false) Long importId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(favoriteService.page(profileId, importId, page, size));
    }
}
