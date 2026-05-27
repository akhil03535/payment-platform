package com.paymentplatform.service;

import com.paymentplatform.dto.request.LoginRequest;
import com.paymentplatform.dto.request.RegisterRequest;
import com.paymentplatform.dto.response.AuthResponse;
import com.paymentplatform.entity.User;
import com.paymentplatform.exception.InvalidPaymentException;
import com.paymentplatform.repository.UserRepository;
import com.paymentplatform.security.jwt.JwtUtils;
import com.paymentplatform.service.impl.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtils jwtUtils;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$hashed")
                .firstName("Test")
                .lastName("User")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        registerRequest = new RegisterRequest(
                "testuser", "test@example.com",
                "Test@1234", "Test", "User"
        );

        loginRequest = new LoginRequest("testuser", "Test@1234");
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void shouldRegisterNewUser() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("$2a$12$hashed");
        when(jwtUtils.generateAccessToken(any())).thenReturn("access.token");
        when(jwtUtils.generateRefreshToken(any())).thenReturn("refresh.token");
        when(jwtUtils.getRefreshTokenExpiry()).thenReturn(604800000L);
        when(jwtUtils.getAccessTokenExpiry()).thenReturn(900000L);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh.token");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getUsername()).isEqualTo("testuser");

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder, atLeastOnce()).encode(any());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowWhenUsernameExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowWhenEmailExists() {
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities()));
        when(jwtUtils.generateAccessToken(testUser)).thenReturn("access.token");
        when(jwtUtils.generateRefreshToken(testUser)).thenReturn("refresh.token");
        when(jwtUtils.getRefreshTokenExpiry()).thenReturn(604800000L);
        when(jwtUtils.getAccessTokenExpiry()).thenReturn(900000L);
        when(passwordEncoder.encode("refresh.token")).thenReturn("hashed.refresh");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(authenticationManager).authenticate(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialsException for wrong password")
    void shouldThrowForWrongPassword() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("Should logout user and clear refresh token")
    void shouldLogoutUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.logout("testuser");

        verify(userRepository).save(argThat(u -> u.getRefreshToken() == null));
    }

    @Test
    @DisplayName("Should throw exception when refresh token is blank")
    void shouldThrowWhenRefreshTokenBlank() {
        assertThatThrownBy(() -> authService.refreshToken(""))
                .isInstanceOf(InvalidPaymentException.class)
                .hasMessageContaining("Refresh token is required");
    }
}
