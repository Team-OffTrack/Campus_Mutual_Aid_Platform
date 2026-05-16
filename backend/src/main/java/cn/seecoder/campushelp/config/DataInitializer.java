package cn.seecoder.campushelp.config;

import cn.seecoder.campushelp.entity.PrivacyProfile;
import cn.seecoder.campushelp.entity.User;
import cn.seecoder.campushelp.entity.UserAccount;
import cn.seecoder.campushelp.mapper.PrivacyProfileMapper;
import cn.seecoder.campushelp.mapper.UserAccountMapper;
import cn.seecoder.campushelp.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seeds the database with a default admin account on first startup.
 * Only runs when no ADMIN user exists yet.
 */
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public ApplicationRunner initAdminUser(UserMapper userMapper,
                                           PrivacyProfileMapper profileMapper,
                                           UserAccountMapper accountMapper,
                                           PasswordEncoder passwordEncoder) {
        return args -> {
            if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")) == 0) {
                User admin = new User();
                admin.setStudentId("admin");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setName("系统管理员");
                admin.setRole("ADMIN");
                admin.setStatus(1);
                userMapper.insert(admin);

                // Composition: admin also needs privacy profile and account
                PrivacyProfile profile = new PrivacyProfile();
                profile.setUserId(admin.getUserId());
                profileMapper.insert(profile);

                UserAccount account = new UserAccount();
                account.setUserId(admin.getUserId());
                accountMapper.insert(account);

                log.info("Default admin user created: admin / admin123");
            }
        };
    }
}
