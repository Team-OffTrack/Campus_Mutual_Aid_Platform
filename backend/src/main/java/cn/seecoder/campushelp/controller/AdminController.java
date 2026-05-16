package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.UserInfoResponse;
import cn.seecoder.campushelp.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    /** Paginated user list with optional keyword search. */
    @GetMapping("/users")
    public ApiResult<Page<UserInfoResponse>> listUsers(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return ApiResult.success(userService.listUsers(pageNum, pageSize, keyword));
    }

    /** Ban (status=0) or unban (status=1) a user. */
    @PutMapping("/users/{userId}/status")
    public ApiResult<Void> updateUserStatus(@PathVariable Long userId,
                                            @RequestBody Map<String, Integer> body) {
        userService.updateUserStatus(userId, body.get("status"));
        return ApiResult.success();
    }
}
