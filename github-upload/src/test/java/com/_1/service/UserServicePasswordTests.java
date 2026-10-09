package com._1.service;

import com._1.core.common.Roles;
import com._1.entity.User;
import com._1.repository.ClassEntityRepository;
import com._1.repository.UserRepository;
import com._1.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 密码只在 encodePassword / saveUser / resetPassword / changePassword 里加密，
 * save 原样保存；登录时角色补 ROLE_ 前缀，禁用账号不能登录。
 */
class UserServicePasswordTests {

    // 强度 4 只为让测试跑得快，生产用默认强度
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserServiceImpl service = new UserServiceImpl(userRepository, encoder, mock(ClassEntityRepository.class));

    @BeforeEach
    void setUp() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void saveUserStoresBcryptHash() {
        when(userRepository.findByUsername("t1")).thenReturn(Optional.empty());

        User saved = service.saveUser("t1", "张老师", "Secret123!", Roles.TEACHER);

        assertThat(saved.getPassword()).isNotEqualTo("Secret123!").startsWith("$2");
        assertThat(encoder.matches("Secret123!", saved.getPassword())).isTrue();
        assertThat(saved.getRole()).isEqualTo(Roles.TEACHER);
    }

    @Test
    void saveUserKeepsExistingAccountUntouched() {
        User existing = user("t1", "old-hash", Roles.TEACHER);
        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(existing));

        User result = service.saveUser("t1", "张老师", "Secret123!", Roles.ADMIN);

        assertThat(result).isSameAs(existing);
        assertThat(result.getPassword()).isEqualTo("old-hash");
        assertThat(result.getRole()).isEqualTo(Roles.TEACHER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void saveDoesNotEncodeAgain() {
        // 以前看到 "$2a$" 前缀就当成已加密，这里确认 save 不再猜测，也不会把哈希再加密一遍
        String hash = encoder.encode("Secret123!");
        User user = user("t1", hash, Roles.TEACHER);

        assertThat(service.save(user).getPassword()).isEqualTo(hash);
    }

    @Test
    void resetPasswordEncodesNewPassword() {
        User user = user("t1", encoder.encode("Old123!"), Roles.TEACHER);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        service.resetPassword(7L, "New123!");

        assertThat(encoder.matches("New123!", user.getPassword())).isTrue();
        assertThat(encoder.matches("Old123!", user.getPassword())).isFalse();
    }

    @Test
    void changePasswordRequiresCurrentPassword() {
        String oldHash = encoder.encode("Old123!");
        User user = user("t1", oldHash, Roles.TEACHER);
        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThat(service.changePassword("t1", "Wrong123!", "New123!")).isFalse();
        assertThat(user.getPassword()).isEqualTo(oldHash);
        assertThat(service.changePassword("nobody", "Old123!", "New123!")).isFalse();
        verify(userRepository, never()).save(any(User.class));

        assertThat(service.changePassword("t1", "Old123!", "New123!")).isTrue();
        assertThat(encoder.matches("New123!", user.getPassword())).isTrue();
    }

    @Test
    void loadUserByUsernameMapsRoleAndEnabledFlag() {
        User teacher = user("t1", "hash", Roles.TEACHER);
        User disabled = user("t2", "hash", Roles.ADMIN);
        disabled.setEnabled(false);
        when(userRepository.findByUsername("t1")).thenReturn(Optional.of(teacher));
        when(userRepository.findByUsername("t2")).thenReturn(Optional.of(disabled));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        UserDetails details = service.loadUserByUsername("t1");
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_TEACHER");
        assertThat(details.isEnabled()).isTrue();

        assertThat(service.loadUserByUsername("t2").isEnabled()).isFalse();
        assertThatThrownBy(() -> service.loadUserByUsername("nobody")).isInstanceOf(UsernameNotFoundException.class);
    }

    private static User user(String username, String password, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }
}
