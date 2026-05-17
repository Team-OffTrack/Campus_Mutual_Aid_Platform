package cn.seecoder.campushelp.service;

import cn.seecoder.campushelp.common.BusinessException;
import cn.seecoder.campushelp.dto.*;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // Clean up before each test so tests are independent
        userMapper.delete(new LambdaQueryWrapper<>());
    }

    @Test
    @DisplayName("Register creates user, profile, and account in one transaction")
    void register_shouldCreateUserProfileAndAccount() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");

        userService.register(req);

        // Verify user was persisted
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "2024001"));
        assertNotNull(user);
        assertEquals("张三", user.getName());
        assertEquals("USER", user.getRole());
        assertEquals(1, user.getStatus());
    }

    @Test
    @DisplayName("Register with duplicate student ID throws conflict")
    void register_duplicateStudentId_shouldThrow() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");

        userService.register(req);

        RegisterRequest dupReq = new RegisterRequest();
        dupReq.setStudentId("2024001");
        dupReq.setPassword("pass456");
        dupReq.setName("李四");

        assertThrows(BusinessException.class, () -> userService.register(dupReq));
    }

    @Test
    @DisplayName("Login with correct credentials returns token")
    void login_correctCredentials_shouldReturnToken() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");
        userService.register(req);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("2024001");
        loginReq.setPassword("pass123");

        LoginResponse rsp = userService.login(loginReq);
        assertNotNull(rsp.getToken());
        assertEquals("张三", rsp.getName());
        assertEquals("USER", rsp.getRole());
    }

    @Test
    @DisplayName("Login with wrong password throws unauthorized")
    void login_wrongPassword_shouldThrow() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");
        userService.register(req);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("2024001");
        loginReq.setPassword("wrongpass");

        assertThrows(BusinessException.class, () -> userService.login(loginReq));
    }

    @Test
    @DisplayName("Get profile returns full user info including points")
    void getProfile_shouldReturnFullInfo() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "2024001"));

        UserInfoResponse profile = userService.getProfile(user.getUserId());
        assertEquals("张三", profile.getName());
        assertEquals(0, profile.getAvailablePoints());
        assertEquals(5.0, profile.getReputationScore());
    }

    @Test
    @DisplayName("Update profile changes name and privacy settings")
    void updateProfile_shouldUpdateFields() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001");
        req.setPassword("pass123");
        req.setName("张三");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "2024001"));

        UpdateProfileRequest update = new UpdateProfileRequest();
        update.setName("张三丰");
        update.setIsAnonymous(true);
        update.setMaskName("匿名侠");

        UserInfoResponse rsp = userService.updateProfile(user.getUserId(), update);
        assertEquals("张三丰", rsp.getName());
        assertTrue(rsp.getIsAnonymous());
        assertEquals("匿名侠", rsp.getMaskName());
    }

    @Test
    @DisplayName("List users with keyword search")
    void listUsers_withKeyword_shouldFilter() {
        for (int i = 1; i <= 3; i++) {
            RegisterRequest req = new RegisterRequest();
            req.setStudentId("202400" + i);
            req.setPassword("pass123");
            req.setName("用户" + i);
            userService.register(req);
        }

        var page = userService.listUsers(1, 10, "用户1");
        assertEquals(1, page.getTotal());
        assertEquals("用户1", page.getRecords().get(0).getName());
    }

    @Test
    @DisplayName("Admin cannot ban themselves")
    void updateUserStatus_selfBan_shouldThrow() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("admin001");
        req.setPassword("pass123");
        req.setName("管理员");
        userService.register(req);

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "admin001"));

        assertThrows(BusinessException.class,
                () -> userService.updateUserStatus(user.getUserId(), 0, user.getUserId()));
    }

    @Test
    @DisplayName("Admin can ban other users")
    void updateUserStatus_banOther_shouldSucceed() {
        // Register two users: one operator, one target
        RegisterRequest r1 = new RegisterRequest();
        r1.setStudentId("op001"); r1.setPassword("pass123"); r1.setName("操作员");
        userService.register(r1);
        RegisterRequest r2 = new RegisterRequest();
        r2.setStudentId("target001"); r2.setPassword("pass123"); r2.setName("目标用户");
        userService.register(r2);

        User operator = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "op001"));
        User target = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "target001"));

        userService.updateUserStatus(target.getUserId(), 0, operator.getUserId());
        User updated = userMapper.selectById(target.getUserId());
        assertEquals(0, updated.getStatus());
    }

    @Test
    @DisplayName("Change password with correct old password succeeds and new password works for login")
    void changePassword_correctOldPassword_shouldUpdateAndAllowLogin() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001"); req.setPassword("pass123"); req.setName("张三");
        userService.register(req);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "2024001"));
        userService.changePassword(user.getUserId(), "pass123", "newpass456");

        LoginRequest loginReq = new LoginRequest();
        loginReq.setStudentId("2024001"); loginReq.setPassword("newpass456");
        assertNotNull(userService.login(loginReq).getToken());
    }

    @Test
    @DisplayName("Change password with wrong old password throws")
    void changePassword_wrongOldPassword_shouldThrow() {
        RegisterRequest req = new RegisterRequest();
        req.setStudentId("2024001"); req.setPassword("pass123"); req.setName("张三");
        userService.register(req);
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getStudentId, "2024001"));
        assertThrows(BusinessException.class,
                () -> userService.changePassword(user.getUserId(), "wrong", "newpass"));
    }
}
