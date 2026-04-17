package com.estorilflow.service;

import com.estorilflow.dto.AuthRequest;
import com.estorilflow.dto.AuthResponse;
import com.estorilflow.dto.AuthenticatedUserResponse;
import com.estorilflow.exceptions.InvalidCredentialsException;
import com.estorilflow.config.security.AuthenticatedUserDetails;
import com.estorilflow.config.security.JwtTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenService jwtTokenService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
    }

    public AuthResponse authenticate(AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
            );

            AuthenticatedUserDetails user = (AuthenticatedUserDetails) authentication.getPrincipal();
            JwtTokenService.TokenDetails tokenDetails = jwtTokenService.generateToken(user);

            return new AuthResponse(
                    tokenDetails.token(),
                    "Bearer",
                    tokenDetails.expiresIn(),
                    tokenDetails.issuedAt(),
                    tokenDetails.expiresAt(),
                    toUserResponse(user)
            );
        } catch (BadCredentialsException | AuthenticationServiceException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    public AuthenticatedUserResponse currentUser(AuthenticatedUserDetails user) {
        return toUserResponse(user);
    }

    private AuthenticatedUserResponse toUserResponse(AuthenticatedUserDetails user) {
        return new AuthenticatedUserResponse(
                user.id(),
                user.name(),
                user.getUsername(),
                user.email(),
                user.role()
        );
    }
}
