package com.uniconnect.controllers;

import com.uniconnect.models.UserEntity;
import com.uniconnect.repositories.PostRepository;
import com.uniconnect.repositories.SavedPostRepository;
import com.uniconnect.repositories.UserRepository;
import com.uniconnect.services.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// ─────────────────────────────────────────────────────────────
// AdminController
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
class AdminController {

    @Autowired private UserRepository userRepo;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<AuthController.UserDto>>> getPending() {
        List<AuthController.UserDto> users = userRepo.findPendingUniversities()
                .stream().map(AuthController.UserDto::from).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PostMapping("/verify/{userId}")
    public ResponseEntity<ApiResponse<AuthController.UserDto>> verify(@PathVariable String userId) {
        UserEntity user = userRepo.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setVerified(true);
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(AuthController.UserDto.from(user),
                "University verified"));
    }

    @PostMapping("/deactivate/{userId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String userId) {
        UserEntity user = userRepo.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setActive(false);
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated"));
    }
}

// ─────────────────────────────────────────────────────────────
// UserController
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/users")
class UserController {

    @Autowired private UserRepository      userRepo;
    @Autowired private SavedPostRepository savedRepo;
    @Autowired private FeedService         feedService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthController.UserDto>> getUser(@PathVariable String id) {
        UserEntity user = userRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(ApiResponse.success(AuthController.UserDto.from(user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AuthController.UserDto>> updateProfile(
            @PathVariable String id,
            @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal UserDetails principal) {

        UserEntity user = userRepo.findById(UUID.fromString(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!user.getEmail().equals(principal.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (req.getName()    != null) user.setName(req.getName());
        if (req.getCity()    != null) user.setCity(req.getCity());
        if (req.getCountry() != null) user.setCountry(req.getCountry());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());

        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.success(AuthController.UserDto.from(user)));
    }

    @GetMapping("/saved")
    public ResponseEntity<ApiResponse<FeedController.FeedResponse>> getSavedPosts(
            @RequestParam(required = false)  String cursor,
            @RequestParam(defaultValue = "15") int limit,
            @AuthenticationPrincipal UserDetails principal) {

        var posts = savedRepo.findSavedPostsByUserEmail(
                principal.getUsername(), PageRequest.of(0, limit));

        var dtos = posts.stream()
                .map(p -> PostController.PostDto.from(p, 0.0, false, true))
                .collect(Collectors.toList());

        FeedController.FeedResponse resp = new FeedController.FeedResponse();
        resp.posts    = dtos;
        resp.hasMore  = dtos.size() == limit;
        resp.totalCount = dtos.size();
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @lombok.Data
    static class UpdateProfileRequest {
        private String name;
        private String city;
        private String country;
        private String avatarUrl;
    }
}

// ─────────────────────────────────────────────────────────────
// SearchController
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/search")
class SearchController {

    @Autowired private FeedService feedService;

    @GetMapping
    public ResponseEntity<ApiResponse<FeedController.FeedResponse>> search(
            @RequestParam(required = false)  String q,
            @RequestParam(required = false)  String category,
            @RequestParam(required = false)  String mode,
            @RequestParam(required = false)  String city,
            @RequestParam(required = false)  String cursor,
            @RequestParam(defaultValue = "15") int limit,
            @AuthenticationPrincipal UserDetails principal) {

        String userId = principal != null ? principal.getUsername() : null;

        FeedService.FeedQuery query = FeedService.FeedQuery.builder()
                .sortBy("LATEST")
                .scope("GLOBAL")
                .keyword(q)
                .category(category)
                .mode(mode)
                .city(city)
                .cursor(cursor)
                .limit(limit)
                .requestingUserId(userId)
                .build();

        FeedController.FeedResponse resp = feedService.getFeed(query);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}

// ─────────────────────────────────────────────────────────────
// GeoController
// ─────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/geo")
class GeoController {

    @GetMapping("/cities")
    public ResponseEntity<ApiResponse<List<CityDto>>> searchCities(
            @RequestParam(defaultValue = "") String q) {
        // In production: query a geocoding DB or call Nominatim/Google Places API
        // Here we return a stub for illustration
        List<CityDto> cities = List.of(
            new CityDto("Mumbai",    "Maharashtra", "India",    19.076, 72.877),
            new CityDto("Delhi",     "Delhi",       "India",    28.644, 77.216),
            new CityDto("Bangalore", "Karnataka",   "India",    12.972, 77.594),
            new CityDto("Chennai",   "Tamil Nadu",  "India",    13.083, 80.270),
            new CityDto("Hyderabad", "Telangana",   "India",    17.385, 78.487),
            new CityDto("New York",  "NY",          "USA",      40.713,-74.006),
            new CityDto("London",    "England",     "UK",       51.509, -0.118),
            new CityDto("Singapore", "-",           "Singapore",1.352, 103.820)
        ).stream()
         .filter(c -> q.isBlank() || c.name.toLowerCase().contains(q.toLowerCase()))
         .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(cities));
    }

    record CityDto(String name, String state, String country, double latitude, double longitude) {}
}
