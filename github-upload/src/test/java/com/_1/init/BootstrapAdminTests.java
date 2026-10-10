package com._1.init;

import com._1.core.common.Roles;
import com._1.entity.User;
import com._1.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * dev 环境同时配置了 APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD 时，
 * .env 里的管理员和演示账号都要创建（以前演示管理员先建好，配置的管理员被跳过）。
 */
@SpringBootTest(properties = {
        // 单独的内存库，演示数据不影响其他测试
        "spring.datasource.url=jdbc:h2:mem:bootstrapdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "app.bootstrap-admin.username=boot.admin",
        "app.bootstrap-admin.password=BootAdmin123!"
})
@ActiveProfiles({"test", "dev"})
class BootstrapAdminTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void createsConfiguredAdminAlongsideDemoAccounts() {
        User admin = userRepository.findByUsername("boot.admin").orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Roles.ADMIN);
        assertThat(passwordEncoder.matches("BootAdmin123!", admin.getPassword())).isTrue();

        assertThat(userRepository.findByUsername("demo.admin")).isPresent();
        assertThat(userRepository.findByUsername("demo.teacher")).isPresent();
    }
}
