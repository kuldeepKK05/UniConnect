package com.uniconnect.controllers;

import com.uniconnect.services.FeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feed")
public class FeedController {

    @Autowired private FeedService feedService;

    /**
     * GET /feed
     *
     * Query params:
     *   sortBy     – LATEST | TRENDING | NEAREST
     *   scope      – NEAR_ME | SAME_CITY | NEARBY | GLOBAL
     *   category   – HACKATHON | INTERNSHIP | SUMMER_COURSE (optional)
     *   mode       – ON_GROUND | VIRTUAL | WFH (optional)
     *   radiusKm   – int (default 50)
     *   city       – string (optional)
     *   university – string (optional)
     *   keyword    – string (optional, full-text search)
     *   userLat    – double (required for geo scopes)
     *   userLng    – double (required for geo scopes)
     *   cursor     – string (opaque cursor for pagination)
     *   limit      – int (default 15, max 50)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<FeedResponse>> getFeed(
            @RequestParam(defaultValue = "LATEST")  String sortBy,
            @RequestParam(defaultValue = "NEAR_ME") String scope,
            @RequestParam(required = false)          String category,
            @RequestParam(required = false)          String mode,
            @RequestParam(defaultValue = "50")       int    radiusKm,
            @RequestParam(required = false)          String city,
            @RequestParam(required = false)          String university,
            @RequestParam(required = false)          String keyword,
            @RequestParam(defaultValue = "0")        double userLat,
            @RequestParam(defaultValue = "0")        double userLng,
            @RequestParam(required = false)          String cursor,
            @RequestParam(defaultValue = "15")       int    limit,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String userId = principal != null ? principal.getUsername() : null;

        FeedService.FeedQuery query = FeedService.FeedQuery.builder()
                .sortBy(sortBy)
                .scope(scope)
                .category(category)
                .mode(mode)
                .radiusKm(Math.min(radiusKm, 500))
                .city(city)
                .university(university)
                .keyword(keyword)
                .userLat(userLat)
                .userLng(userLng)
                .cursor(cursor)
                .limit(Math.min(limit, 50))
                .requestingUserId(userId)
                .build();

        FeedResponse response = feedService.getFeed(query);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Response DTO ──────────────────────────────────────────

    public static class FeedResponse {
        public List<PostController.PostDto> posts;
        public String nextCursor;
        public boolean hasMore;
        public int totalCount;
    }
}
