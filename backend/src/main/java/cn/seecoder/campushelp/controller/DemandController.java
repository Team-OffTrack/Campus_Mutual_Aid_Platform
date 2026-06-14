package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.CreateDemandRequest;
import cn.seecoder.campushelp.dto.DemandResponse;
import cn.seecoder.campushelp.dto.UpdateDemandRequest;
import cn.seecoder.campushelp.service.DemandService;
import cn.seecoder.campushelp.service.FavoriteService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final FavoriteService favoriteService;

    public DemandController(DemandService demandService, FavoriteService favoriteService) {
        this.demandService = demandService;
        this.favoriteService = favoriteService;
    }

    /** Publish a new demand. */
    @PostMapping
    public ApiResult<DemandResponse> publish(Authentication auth,
                                              @Valid @RequestBody CreateDemandRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.publish(userId, request));
    }

    /** Upload a demand image. Returns the server-relative URL to include in publish. */
    @PostMapping("/upload-image")
    public ApiResult<String> uploadImage(Authentication auth,
                                          @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.uploadImage(userId, file));
    }

    /** Paginated demand list with optional type, keyword filters and sort order. */
    @GetMapping
    public ApiResult<Page<DemandResponse>> list(
            Authentication auth,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.list(userId, pageNum, pageSize, type, keyword, sortBy));
    }

    /** List demands where the current user is publisher or acceptor. */
    @GetMapping("/my")
    public ApiResult<List<DemandResponse>> myOrders(Authentication auth,
                                                     @RequestParam(defaultValue = "publisher") String role) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.myOrders(userId, role));
    }

    /** Paginated list of the current user's favorited demands. */
    @GetMapping("/my/favorites")
    public ApiResult<Page<DemandResponse>> myFavorites(Authentication auth,
                                                        @RequestParam(defaultValue = "1") int pageNum,
                                                        @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(favoriteService.listFavorites(userId, pageNum, pageSize));
    }

    /** Get a single demand with full detail including acceptor info. */
    @GetMapping("/{demandId}")
    public ApiResult<DemandResponse> getById(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.getById(demandId, userId));
    }

    /** Add a demand to favorites. */
    @PostMapping("/{demandId}/favorite")
    public ApiResult<Void> favorite(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        favoriteService.favorite(userId, demandId);
        return ApiResult.success();
    }

    /** Remove a demand from favorites. */
    @DeleteMapping("/{demandId}/favorite")
    public ApiResult<Void> unfavorite(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        favoriteService.unfavorite(userId, demandId);
        return ApiResult.success();
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

    /** Update an OPEN demand's editable fields. Only the publisher may edit. */
    @PutMapping("/{demandId}")
    public ApiResult<DemandResponse> updateDemand(Authentication auth,
                                                   @PathVariable Long demandId,
                                                   @Valid @RequestBody UpdateDemandRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.updateDemand(demandId, userId, request));
    }

    /** Cancel an OPEN or IN_PROGRESS demand (publisher only). */
    @PutMapping("/{demandId}/cancel")
    public ApiResult<Void> cancel(Authentication auth, @PathVariable Long demandId) {
        Long userId = (Long) auth.getPrincipal();
        demandService.cancel(demandId, userId);
        return ApiResult.success();
    }

    /** List demands where the current user is a joined team member. */
    @GetMapping("/my/team")
    public ApiResult<List<DemandResponse>> myTeamOrders(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(demandService.myTeamOrders(userId));
    }
}
