package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.service.DemandService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Demand publishing, discovery, and order-flow endpoints.
 * <p>
 * All routes require authentication. The principal (userId) is injected from the JWT via Spring Security.
 * "my" routes must be declared before path-variable routes to avoid ambiguity.
 */
@RestController
@RequestMapping("/api/v1/demands")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    /** Publish a new demand. */
    @PostMapping
    public ApiResult<DemandResponse> publish(Authentication auth,
                                              @Valid @RequestBody CreateDemandRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.publish(userId, request));
    }

    /** Paginated demand list with optional type, keyword filters and sort order. */
    @GetMapping
    public ApiResult<Page<DemandResponse>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return ApiResult.success(demandService.list(pageNum, pageSize, type, keyword, sortBy));
    }

    /** List demands where the current user is publisher or acceptor. */
    @GetMapping("/my")
    public ApiResult<List<DemandResponse>> myOrders(Authentication auth,
                                                     @RequestParam(defaultValue = "publisher") String role) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.myOrders(userId, role));
    }

    /** Get a single demand with full detail including acceptor info. */
    @GetMapping("/{demandId}")
    public ApiResult<DemandResponse> getById(@PathVariable Long demandId) {
        return ApiResult.success(demandService.getById(demandId));
    }

    /** Accept an OPEN demand. Cannot accept your own. */
    @PutMapping("/{demandId}/accept")
    public ApiResult<DemandResponse> accept(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.accept(demandId, userId));
    }

    /** Mark an IN_PROGRESS demand as COMPLETED (publisher only). */
    @PutMapping("/{demandId}/complete")
    public ApiResult<DemandResponse> complete(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.complete(demandId, userId));
    }

    /** Cancel an OPEN or IN_PROGRESS demand (publisher only). */
    @PutMapping("/{demandId}/cancel")
    public ApiResult<Void> cancel(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        demandService.cancel(demandId, userId);
        return ApiResult.success();
    }
}
