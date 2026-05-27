package com.paymentplatform.service;

import com.paymentplatform.entity.User;
import com.paymentplatform.security.jwt.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtils Unit Tests")
class JwtUtilsTest {

    private JwtUtils jwtUtils;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
            "ThisIsAVeryLongSecretKeyForJWTTokenGenerationThatIsAtLeast512BitsLong12345");
        ReflectionTestUtils.setField(jwtUtils, "accessTokenExpiry", 900000L);
        ReflectionTestUtils.setField(jwtUtils, "refreshTokenExpiry", 604800000L);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should generate a valid access token")
    void shouldGenerateAccessToken() {
        String token = jwtUtils.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
    }

    @Test
    @DisplayName("Should extract username from token")
    void shouldExtractUsername() {
        String token = jwtUtils.generateAccessToken(testUser);
        String username = jwtUtils.extractUsername(token);

        assertThat(username).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should validate a valid token")
    void shouldValidateValidToken() {
        String token = jwtUtils.generateAccessToken(testUser);
        boolean valid = jwtUtils.isTokenValid(token, testUser);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for wrong user")
    void shouldRejectTokenForWrongUser() {
        String token = jwtUtils.generateAccessToken(testUser);

        User otherUser = User.builder()
                .id(UUID.randomUUID())
                .username("otheruser")
                .email("other@example.com")
                .role(User.UserRole.USER)
                .enabled(true)
                .build();

        boolean valid = jwtUtils.isTokenValid(token, otherUser);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Should identify ACCESS token type")
    void shouldIdentifyAccessTokenType() {
        String token = jwtUtils.generateAccessToken(testUser);
        String type = jwtUtils.extractTokenType(token);

        assertThat(type).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("Should identify REFRESH token type")
    void shouldIdentifyRefreshTokenType() {
        String token = jwtUtils.generateRefreshToken(testUser);
        String type = jwtUtils.extractTokenType(token);

        assertThat(type).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("Should generate different tokens for different calls")
    void shouldGenerateUniqueTokens() {
        String token1 = jwtUtils.generateRefreshToken(testUser);
        String token2 = jwtUtils.generateRefreshToken(testUser);

        // Refresh tokens include a UUID jti so they should differ
        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    @DisplayName("Should return true for expired token check on invalid token")
    void shouldReturnTrueForInvalidToken() {
        boolean expired = jwtUtils.isTokenExpired("not.a.valid.jwt");
        assertThat(expired).isTrue();
    }
}
