package com.uniconnect.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.uniconnect.controllers.AuthController;
import com.uniconnect.models.UserEntity;
import com.uniconnect.repositories.UserRepository;
import com.uniconnect.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
public class AuthService {

    @Autowired private UserRepository       userRepo;
    @Autowired private JwtUtils             jwtUtils;
    @Autowired private PasswordEncoder      passwordEncoder;
    @Autowired private AuthenticationManager authManager;

    @Value("${app.google.client-id}")
    private String googleClientId;

    @Value("${app.verified-domains}")
    private List<String> verifiedDomains;

    // ── Login ─────────────────────────────────────────────────

    public AuthController.AuthResponse login(String email, String password) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password));
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        UserEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        return buildAuthResponse(user);
    }

    // ── Register ──────────────────────────────────────────────

    @Transactional
    public AuthController.AuthResponse register(String name, String email, String password,
                                                String roleStr, String universityName) {
        if (userRepo.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        UserEntity.Role role;
        try {
            role = UserEntity.Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            role = UserEntity.Role.STUDENT;
        }

        // Auto-verify known university domains
        boolean autoVerified = role == UserEntity.Role.STUDENT
                || UserEntity.isDomainVerified(email, verifiedDomains);

        UserEntity user = UserEntity.builder()
                .name(name)
                .email(email.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .universityName(universityName)
                .verified(autoVerified)
                .active(true)
                .build();

        user = userRepo.save(user);

        if (role == UserEntity.Role.UNIVERSITY && !autoVerified) {
            // Return a limited response indicating pending verification
            AuthController.AuthResponse resp = new AuthController.AuthResponse();
            resp.setUser(AuthController.UserDto.from(user));
            return resp; // no token until verified
        }

        return buildAuthResponse(user);
    }

    // ── Google OAuth ──────────────────────────────────────────

    @Transactional
    public AuthController.AuthResponse googleLogin(String idTokenStr) {
        GoogleIdToken.Payload payload = verifyGoogleToken(idTokenStr);

        String email  = payload.getEmail();
        String name   = (String) payload.get("name");
        String avatar = (String) payload.get("picture");

        UserEntity user = userRepo.findByEmail(email).orElseGet(() -> {
            UserEntity newUser = UserEntity.builder()
                    .email(email)
                    .name(name != null ? name : email)
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(UserEntity.Role.STUDENT)
                    .avatarUrl(avatar)
                    .verified(true)
                    .active(true)
                    .build();
            return userRepo.save(newUser);
        });

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
        }

        // Update avatar if changed
        if (avatar != null && !avatar.equals(user.getAvatarUrl())) {
            user.setAvatarUrl(avatar);
            userRepo.save(user);
        }

        return buildAuthResponse(user);
    }

    // ── Refresh token ─────────────────────────────────────────

    public AuthController.AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        String userId = jwtUtils.getUserIdFromToken(refreshToken);
        UserEntity user = userRepo.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return buildAuthResponse(user);
    }

    // ── Lookup ────────────────────────────────────────────────

    public UserEntity getUserByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // ── Helpers ───────────────────────────────────────────────

    private AuthController.AuthResponse buildAuthResponse(UserEntity user) {
        String token        = jwtUtils.generateToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtils.generateRefreshToken(user.getId().toString());

        AuthController.AuthResponse resp = new AuthController.AuthResponse();
        resp.setToken(token);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(86400000L);
        resp.setUser(AuthController.UserDto.from(user));
        return resp;
    }

    private GoogleIdToken.Payload verifyGoogleToken(String idTokenStr) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
            GoogleIdToken idToken = verifier.verify(idTokenStr);
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid Google token");
            }
            return idToken.getPayload();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google token verification failed: " + e.getMessage());
        }
    }
}
