package com.estorilflow.api;

import com.estorilflow.dto.AuthRequest;
import com.estorilflow.dto.AuthResponse;
import com.estorilflow.dto.AuthenticatedUserResponse;
import com.estorilflow.service.AuthService;
import com.estorilflow.config.security.AuthenticatedUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me(
            @AuthenticationPrincipal AuthenticatedUserDetails authenticatedUser
    ) {
        return ResponseEntity.ok(authService.currentUser(authenticatedUser));
    }
}
