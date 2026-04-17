package com.estorilflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.estorilflow.dto.AuthRequest;
import com.estorilflow.dto.AuthResponse;
import com.estorilflow.exceptions.InvalidCredentialsException;
import com.estorilflow.config.security.AuthenticatedUserDetails;
import com.estorilflow.config.security.JwtTokenService;
import com.estorilflow.entity.Role;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtTokenService);
    }

    @Test
    void shouldAuthenticateAndReturnJwtMetadata() {
        AuthenticatedUserDetails principal = new AuthenticatedUserDetails(
                1L,
                "Admin User",
                "admin",
                "admin@estorilflow.com",
                "encoded-password",
                Role.ADMIN,
                true
        );

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );

        JwtTokenService.TokenDetails tokenDetails = new JwtTokenService.TokenDetails(
                "jwt-token",
                Instant.parse("2026-04-17T04:00:00Z"),
                Instant.parse("2026-04-17T06:00:00Z"),
                7200
        );

        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtTokenService.generateToken(principal)).thenReturn(tokenDetails);

        AuthResponse response = authService.authenticate(new AuthRequest("admin", "admin123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void shouldThrowInvalidCredentialsWhenAuthenticationFails() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(new AuthRequest("admin", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
