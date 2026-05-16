package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Public and authenticated user endpoints.
 * <p>
 * Registration and login are unauthenticated (permitAll in SecurityConfig).
 * Profile reads and writes require a valid JWT — the principal (userId) is
 * extracted from the Spring Security {@link Authentication} object injected by
 * {@link cn.seecoder.campushelp.security.JwtAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResult.success();
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success(userService.login(request));
    }

    @GetMapping("/profile")
    public ApiResult<UserInfoResponse> getProfile(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResult<UserInfoResponse> updateProfile(Authentication auth,
                                                      @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(userService.updateProfile(userId, request));
    }
}
