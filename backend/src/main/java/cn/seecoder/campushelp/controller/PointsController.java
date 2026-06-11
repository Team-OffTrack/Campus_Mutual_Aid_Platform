package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.DailyCheckinResponse;
import cn.seecoder.campushelp.dto.DailyCheckinStatus;
import cn.seecoder.campushelp.entity.PointsTransaction;
import cn.seecoder.campushelp.service.PointsService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/points")
public class PointsController {

    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    /** Daily check-in. */
    @PostMapping("/checkin")
    public ApiResult<DailyCheckinResponse> checkin(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(pointsService.checkin(userId));
    }

    /** Get today's check-in status. */
    @GetMapping("/checkin/status")
    public ApiResult<DailyCheckinStatus> getCheckinStatus(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(pointsService.getCheckinStatus(userId));
    }

    /** Paginated transaction history. */
    @GetMapping("/transactions")
    public ApiResult<Page<PointsTransaction>> getTransactions(
            Authentication auth,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String type) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(pointsService.getTransactions(userId, pageNum, pageSize, type));
    }
}
