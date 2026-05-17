package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.dto.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * Core user-management facade.
 * <p>
 * Exposes registration, login (JWT issuance), profile CRUD, and administrative
 * user listing / status toggling. The implementation enforces the invariant that
 * every user has exactly one {@code PrivacyProfile} and one {@code UserAccount}.
 */
public interface UserService {

    /** Creates a User row plus the associated PrivacyProfile and UserAccount (transactional). */
    void register(RegisterRequest request);

    /** Authenticates by studentId + password and returns a signed JWT. */
    LoginResponse login(LoginRequest request);

    /** Returns the full user profile including privacy settings and account balance. */
    UserInfoResponse getProfile(Long userId);

    /** Partially updates user fields (name, avatar, privacy toggles). */
    UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** Paginated search across users (admin-only). Keyword matches studentId or name. */
    Page<UserInfoResponse> listUsers(int pageNum, int pageSize, String keyword);

    /** Toggles a user between active (1) and banned (0) — admin-only. Operator cannot target themselves. */
    void updateUserStatus(Long userId, Integer status, Long operatorId);

    /** Change password for the current user. Validates old password before updating. */
    void changePassword(Long userId, String oldPassword, String newPassword);
}
