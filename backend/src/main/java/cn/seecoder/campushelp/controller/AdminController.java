package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.ReportResponse;
import cn.seecoder.campushelp.dto.ResolveReportRequest;
import cn.seecoder.campushelp.dto.UserInfoResponse;
import cn.seecoder.campushelp.service.ReportService;
import cn.seecoder.campushelp.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only REST endpoints.
 * <p>
 * Access to all routes in this controller requires ROLE_ADMIN, enforced by
 * {@link cn.seecoder.campushelp.config.SecurityConfig}. No additional role-check
 * boilerplate is needed in the methods themselves.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserService userService;
    private final ReportService reportService;

    public AdminController(UserService userService, ReportService reportService) {
        this.userService = userService;
        this.reportService = reportService;
    }

    /** Paginated user list with optional keyword search. */
    @GetMapping("/users")
    public ApiResult<Page<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    /** Ban (status=0) or unban (status=1) a user. Operator cannot target themselves. */
    @PutMapping("/users/{userId}/status")
    public ApiResult<Void> updateUserStatus(Authentication auth,
                                            @PathVariable Long userId,
                                            @RequestBody Map<String, Integer> body) {
        Long operatorId = (Long) auth.getPrincipal();
        userService.updateUserStatus(userId, body.get("status"), operatorId);
        return ApiResult.success();
    }

    /** Paginated report list with optional status filter. */
    @GetMapping("/reports")
    public ApiResult<Page<ReportResponse>> listReports(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        return ApiResult.success(reportService.listReports(pageNum, pageSize, status));
    }

    /** Resolve a report (mark RESOLVED or DISMISSED). */
    @PutMapping("/reports/{id}/resolve")
    public ApiResult<Void> resolveReport(Authentication auth,
                                          @PathVariable Long id,
                                          @Valid @RequestBody ResolveReportRequest request) {
        Long adminId = (Long) auth.getPrincipal();
        reportService.resolveReport(id, adminId, request);
        return ApiResult.success();
    }
}
