package com.aigoalplanner.controller;

import com.aigoalplanner.dto.AuthRequest;
import com.aigoalplanner.dto.AuthResponse;
import com.aigoalplanner.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    /**
     * POST /api/auth/signup
     * Creates a new user account and returns a JWT token.
     *
     * Request body:
     * {
     *   "name": "Shiva",
     *   "email": "shiva@example.com",
     *   "password": "secret123",
     *   "currentSkills": "Java, DSA",
     *   "experienceLevel": "BEGINNER"
     * }
     */
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody AuthRequest request) {
        log.info("POST /api/auth/signup — email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    /**
     * POST /api/auth/login
     * Validates credentials and returns a JWT token.
     *
     * Request body:
     * {
     *   "email": "shiva@example.com",
     *   "password": "secret123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("POST /api/auth/login — email={}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }
}
