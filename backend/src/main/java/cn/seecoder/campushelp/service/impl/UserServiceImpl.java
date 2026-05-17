package cn.seecoder.campushelp.service.impl;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.common.ResultCode;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.PrivacyProfile;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.mapper.PrivacyProfileMapper;
import cn.seecoder.campushelp.mapper.UserAccountMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import cn.seecoder.campushelp.security.JwtTokenProvider;
import cn.seecoder.campushelp.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PrivacyProfileMapper privacyProfileMapper;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public UserServiceImpl(UserMapper userMapper,
                           PrivacyProfileMapper privacyProfileMapper,
                           UserAccountMapper userAccountMapper,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userMapper = userMapper;
        this.privacyProfileMapper = privacyProfileMapper;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // Guard against duplicate student IDs — student_id is the unique campus credential
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStudentId, request.getStudentId())) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "该学号已被注册");
        }

        User user = new User();
        user.setStudentId(request.getStudentId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        // Every new user gets a privacy profile and a points account (1:1 composition)
        PrivacyProfile profile = new PrivacyProfile();
        profile.setUserId(user.getUserId());
        privacyProfileMapper.insert(profile);

        UserAccount account = new UserAccount();
        account.setUserId(user.getUserId());
        userAccountMapper.insert(account);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentId, request.getStudentId()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "学号或密码错误");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被封禁");
        }

        // Stateless JWT token — server doesn't store session, token carries identity
        String token = jwtTokenProvider.generateToken(
                user.getUserId(), user.getStudentId(), user.getRole());
        return new LoginResponse(token, user.getUserId(), user.getName(), user.getRole());
    }

    @Override
    public UserInfoResponse getProfile(Long userId) {
        User user = findUserOrFail(userId);
        PrivacyProfile profile = privacyProfileMapper.selectOne(
                new LambdaQueryWrapper<PrivacyProfile>().eq(PrivacyProfile::getUserId, userId));
        UserAccount account = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccount>().eq(UserAccount::getUserId, userId));
        return UserInfoResponse.from(user, profile, account);
    }

    @Override
    @Transactional
    public UserInfoResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserOrFail(userId);

        // Only update fields that were actually provided (partial update)
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        // name and avatar are on the same row — update once to avoid duplicate SQL
        if (request.getName() != null || request.getAvatar() != null) {
            userMapper.updateById(user);
        }

        if (request.getIsAnonymous() != null || request.getMaskName() != null) {
            PrivacyProfile profile = privacyProfileMapper.selectOne(
                    new LambdaQueryWrapper<PrivacyProfile>().eq(PrivacyProfile::getUserId, userId));
            if (request.getIsAnonymous() != null) {
                profile.setIsAnonymous(request.getIsAnonymous() ? 1 : 0);
            }
            if (request.getMaskName() != null) {
                profile.setMaskName(request.getMaskName());
            }
            privacyProfileMapper.updateById(profile);
        }

        return getProfile(userId);
    }

    @Override
    public Page<UserInfoResponse> listUsers(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getStudentId, keyword).or().like(User::getName, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);

        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<User> users = page.getRecords();

        // Batch-load profiles & accounts to avoid N+1 queries
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        if (userIds.isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        Map<Long, PrivacyProfile> profileMap = privacyProfileMapper.selectList(
                        new LambdaQueryWrapper<PrivacyProfile>().in(PrivacyProfile::getUserId, userIds))
                .stream().collect(Collectors.toMap(PrivacyProfile::getUserId, p -> p));
        Map<Long, UserAccount> accountMap = userAccountMapper.selectList(
                        new LambdaQueryWrapper<UserAccount>().in(UserAccount::getUserId, userIds))
                .stream().collect(Collectors.toMap(UserAccount::getUserId, a -> a));

        Page<UserInfoResponse> resultPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        resultPage.setRecords(users.stream()
                .map(u -> UserInfoResponse.from(u, profileMap.get(u.getUserId()), accountMap.get(u.getUserId())))
                .toList());
        return resultPage;
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findUserOrFail(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status, Long operatorId) {
        if (userId.equals(operatorId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能封禁自己");
        }
        User user = findUserOrFail(userId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    private User findUserOrFail(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return user;
    }
}
