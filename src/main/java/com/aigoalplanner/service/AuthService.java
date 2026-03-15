package com.aigoalplanner.service;

import com.aigoalplanner.dto.AuthRequest;
import com.aigoalplanner.dto.AuthResponse;
import com.aigoalplanner.exception.GlobalExceptionHandler.DuplicateResourceException;
import com.aigoalplanner.model.User;
import com.aigoalplanner.repository.UserRepository;
import com.aigoalplanner.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // --------------------------------------------------------
    // SIGNUP
    // --------------------------------------------------------

    @Transactional
    public AuthResponse signup(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                "Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .currentSkills(request.getCurrentSkills())
                .experienceLevel(request.getExperienceLevel() != null
                        ? request.getExperienceLevel()
                        : User.ExperienceLevel.BEGINNER)
                .build();

        User saved = userRepository.save(user);
        log.info("New user signed up: id={} email={}", saved.getId(), saved.getEmail());

        String token = jwtTokenProvider.generateToken(saved.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .experienceLevel(saved.getExperienceLevel())
                .build();
    }

    // --------------------------------------------------------
    // LOGIN
    // --------------------------------------------------------

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        // Spring Security validates email + password via AuthenticationManager
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        String token = jwtTokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        log.info("User logged in: id={} email={}", user.getId(), user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .experienceLevel(user.getExperienceLevel())
                .build();
    }
}
