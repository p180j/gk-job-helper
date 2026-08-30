package com.gk.jobhelper.controller;
import com.gk.jobhelper.common.ApiResponse; import com.gk.jobhelper.dto.InterviewScoreImportResult; import com.gk.jobhelper.entity.JobInterviewScore; import com.gk.jobhelper.mapper.JobInterviewScoreMapper; import com.gk.jobhelper.service.ImportContextService; import com.gk.jobhelper.service.InterviewScoreImportService;
import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/interview-scores") public class InterviewScoreController {
 private final InterviewScoreImportService service; private final JobInterviewScoreMapper mapper; private final ImportContextService importContextService;
 public InterviewScoreController(InterviewScoreImportService s,JobInterviewScoreMapper m,ImportContextService context){service=s;mapper=m;importContextService=context;}
 @PostMapping("/import") public ApiResponse<InterviewScoreImportResult> upload(@RequestParam("importId")Long importId,@RequestParam("file")MultipartFile file){int year=importContextService.requireExamYear(importId);return ApiResponse.ok(service.importFile(year,importId,file));}
 @GetMapping("/job/{positionCode}") public ApiResponse<JobInterviewScore> get(@PathVariable String positionCode,@RequestParam("examYear")Integer year){return ApiResponse.ok(mapper.selectByYearAndCode(year,positionCode));}
}
