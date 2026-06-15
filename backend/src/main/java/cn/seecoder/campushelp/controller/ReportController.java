package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.CreateReportRequest;
import cn.seecoder.campushelp.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * User-facing report endpoints. All routes require authentication.
 */
@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Submit a report against a demand, user, or message. */
    @PostMapping("/reports")
    public ApiResult<Void> createReport(Authentication auth,
                                         @Valid @RequestBody CreateReportRequest request) {
        Long reporterId = (Long) auth.getPrincipal();
        reportService.createReport(reporterId, request);
        return ApiResult.success();
    }
}
