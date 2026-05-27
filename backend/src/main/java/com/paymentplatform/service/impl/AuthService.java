package com.paymentplatform.service.impl;

import com.paymentplatform.dto.request.LoginRequest;
import com.paymentplatform.dto.request.RegisterRequest;
import com.paymentplatform.dto.response.AuthResponse;
import com.paymentplatform.dto.response.UserResponse;
import com.paymentplatform.entity.User;
import com.paymentplatform.exception.InvalidPaymentException;
import com.paymentplatform.repository.UserRepository;
import com.paymentplatform.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new InvalidPaymentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new InvalidPaymentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .role(User.UserRole.USER)
            .enabled(true)
            .accountLocked(false)
            .build();

        String refreshToken = jwtUtils.generateRefreshToken(user);
        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        user.setRefreshTokenExpiry(ZonedDateTime.now().plusSeconds(
            jwtUtils.getRefreshTokenExpiry() / 1000
        ));

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        String accessToken = jwtUtils.generateAccessToken(user);

        return AuthResponse.of(
            accessToken,
            refreshToken,
            jwtUtils.getAccessTokenExpiry(),
            mapToUserResponse(user)
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(),
                request.getPassword()
            )
        );

        User user = (User) authentication.getPrincipal();
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        user.setRefreshToken(passwordEncoder.encode(refreshToken));
        user.setRefreshTokenExpiry(ZonedDateTime.now().plusSeconds(
            jwtUtils.getRefreshTokenExpiry() / 1000
        ));
        userRepository.save(user);

        log.info("User logged in: {}", user.getUsername());

        return AuthResponse.of(
            accessToken,
            refreshToken,
            jwtUtils.getAccessTokenExpiry(),
            mapToUserResponse(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidPaymentException("Refresh token is required");
        }

        String username = jwtUtils.extractUsername(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidPaymentException("Invalid refresh token"));

        if (user.getRefreshTokenExpiry() == null || ZonedDateTime.now().isAfter(user.getRefreshTokenExpiry())) {
            throw new InvalidPaymentException("Refresh token expired");
        }

        String newAccessToken = jwtUtils.generateAccessToken(user);
        String newRefreshToken = jwtUtils.generateRefreshToken(user);

        user.setRefreshToken(passwordEncoder.encode(newRefreshToken));
        user.setRefreshTokenExpiry(ZonedDateTime.now().plusSeconds(
            jwtUtils.getRefreshTokenExpiry() / 1000
        ));
        userRepository.save(user);

        return AuthResponse.of(
            newAccessToken,
            newRefreshToken,
            jwtUtils.getAccessTokenExpiry(),
            mapToUserResponse(user)
        );
    }

    @Transactional
    public void logout(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            log.info("User logged out: {}", username);
        });
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .enabled(user.isEnabled())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
