package com.uniconnect.controllers;

import com.uniconnect.models.UserEntity;
import com.uniconnect.security.JwtUtils;
import com.uniconnect.services.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private JwtUtils jwtUtils;

    // ── POST /auth/login ──────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        AuthResponse response = authService.login(req.getEmail(), req.getPassword());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── POST /auth/register ───────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        AuthResponse response = authService.register(
            req.getName(), req.getEmail(), req.getPassword(),
            req.getRole(), req.getUniversityName()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── POST /auth/google ─────────────────────────────────────
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("idToken is required"));
        }
        AuthResponse response = authService.googleLogin(idToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── GET /auth/me ──────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> me(@AuthenticationPrincipal UserDetails principal) {
        UserEntity user = authService.getUserByEmail(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(UserDto.from(user)));
    }

    // ── POST /auth/logout ─────────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // JWT is stateless; client discards token
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }

    // ── POST /auth/refresh ────────────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── DTOs ─────────────────────────────────────────────────

    @Data
    public static class LoginRequest {
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 6) private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Size(min = 2, max = 100) private String name;
        @Email @NotBlank private String email;
        @NotBlank @Size(min = 8, max = 72) private String password;
        @NotBlank private String role;   // STUDENT | UNIVERSITY
        private String universityName;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String refreshToken;
        private long expiresIn;
        private UserDto user;
    }

    @Data
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String role;
        private String universityName;
        private String avatarUrl;
        private boolean verified;
        private String city;
        private String country;

        public static UserDto from(UserEntity u) {
            UserDto dto = new UserDto();
            dto.id             = u.getId().toString();
            dto.email          = u.getEmail();
            dto.name           = u.getName();
            dto.role           = u.getRole().name();
            dto.universityName = u.getUniversityName();
            dto.avatarUrl      = u.getAvatarUrl();
            dto.verified       = u.isVerified();
            dto.city           = u.getCity();
            dto.country        = u.getCountry();
            return dto;
        }
    }
}
