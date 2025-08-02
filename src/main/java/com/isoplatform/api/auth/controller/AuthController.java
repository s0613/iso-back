package com.isoplatform.api.auth.controller;


import com.isoplatform.api.auth.request.AuthRequest;
import com.isoplatform.api.auth.response.AuthResponse;
import com.isoplatform.api.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse.SignupResponse> signup(
            @Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.signup(request));
    }

}