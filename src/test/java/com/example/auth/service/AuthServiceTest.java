package com.example.auth.service;

import com.example.auth.dto.AuthRequest;
import com.example.auth.entity.User;
import com.example.auth.exception.DuplicateEmailException;
import com.example.auth.exception.InvalidCredentialsException;
import com.example.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerStoresBcryptHashNotPlainText() {
        authService.register(new AuthRequest("test@example.com", "mypassword"));

        User user = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("mypassword").startsWith("$2");
        assertThat(passwordEncoder.matches("mypassword", user.getPassword())).isTrue();
    }

    @Test
    void registerNormalizesEmail() {
        authService.register(new AuthRequest("  Test@Example.COM ", "mypassword"));

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void registerDuplicateEmailIgnoringCaseThrows() {
        authService.register(new AuthRequest("test@example.com", "mypassword"));

        assertThatThrownBy(() -> authService.register(new AuthRequest("TEST@example.com", "other123")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void loginWithCorrectCredentialsSucceeds() {
        authService.register(new AuthRequest("test@example.com", "mypassword"));

        assertThatCode(() -> authService.login(new AuthRequest("test@example.com", "mypassword")))
                .doesNotThrowAnyException();
    }

    @Test
    void loginWithWrongPasswordThrows() {
        authService.register(new AuthRequest("test@example.com", "mypassword"));

        assertThatThrownBy(() -> authService.login(new AuthRequest("test@example.com", "wrongpass")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginWithUnknownEmailThrows() {
        assertThatThrownBy(() -> authService.login(new AuthRequest("nobody@example.com", "mypassword")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
