package cn.seecoder.campushelp.controller;

import cn.seecoder.campushelp.common.ApiResult;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                                                      @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(userService.updateProfile(userId, request));
    }

    /** Upload profile avatar image. Accepts multipart file, saves to disk, returns URL. */
    @PostMapping("/avatar")
    public ApiResult<String> uploadAvatar(Authentication auth,
                                           @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        return ApiResult.success(userService.updateAvatar(userId, file));
    }

    /** Change password. Validates the old password before applying the new one. */
    @PutMapping("/password")
    public ApiResult<Void> changePassword(Authentication auth,
                                           @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = (Long) auth.getPrincipal();
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ApiResult.success();
    }
}
