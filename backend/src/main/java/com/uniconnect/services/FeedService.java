package com.uniconnect.services;

import com.uniconnect.controllers.FeedController;
import com.uniconnect.controllers.PostController;
import com.uniconnect.models.PostEntity;
import com.uniconnect.repositories.PostRepository;
import com.uniconnect.repositories.PostVoteRepository;
import com.uniconnect.repositories.SavedPostRepository;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

@Service
public class FeedService {

    @Autowired private PostRepository postRepo;
    @Autowired private PostVoteRepository voteRepo;
    @Autowired private SavedPostRepository savedRepo;

    @Value("${app.feed.default-page-size:15}")
    private int defaultPageSize;

    // ── Main feed query ───────────────────────────────────────

    public FeedController.FeedResponse getFeed(FeedQuery query) {
        int limit = query.getLimit() > 0 ? query.getLimit() : defaultPageSize;

        // Decode cursor → lastSeenId + lastSeenValue
        CursorData cursor = decodeCursor(query.getCursor());

        // Build the post list based on scope + sort
        List<PostEntity> posts = fetchPosts(query, cursor, limit + 1);

        boolean hasMore = posts.size() > limit;
        if (hasMore) posts = posts.subList(0, limit);

        // Compute distances + user-specific flags
        Set<UUID> upvotedIds = new HashSet<>();
        Set<UUID> savedIds   = new HashSet<>();

        if (query.getRequestingUserId() != null) {
            List<UUID> postIds = posts.stream()
                    .map(PostEntity::getId)
                    .collect(Collectors.toList());
            upvotedIds = new HashSet<>(voteRepo.findVotedPostIdsByUserEmail(
                    query.getRequestingUserId(), postIds));
            savedIds   = new HashSet<>(savedRepo.findSavedPostIdsByUserEmail(
                    query.getRequestingUserId(), postIds));
        }

        final Set<UUID> finalUpvotedIds = upvotedIds;
        final Set<UUID> finalSavedIds   = savedIds;

        List<PostController.PostDto> dtos = posts.stream().map(p -> {
            double dist = p.isVirtual() ? 0.0
                    : p.distanceTo(query.getUserLat(), query.getUserLng());
            return PostController.PostDto.from(p, dist,
                    finalUpvotedIds.contains(p.getId()),
                    finalSavedIds.contains(p.getId()));
        }).collect(Collectors.toList());

        // Build next cursor
        String nextCursor = null;
        if (hasMore && !posts.isEmpty()) {
            PostEntity last = posts.get(posts.size() - 1);
            nextCursor = encodeCursor(last, query.getSortBy());
        }

        FeedController.FeedResponse resp = new FeedController.FeedResponse();
        resp.posts      = dtos;
        resp.nextCursor = nextCursor;
        resp.hasMore    = hasMore;
        resp.totalCount = dtos.size();
        return resp;
    }

    // ── Fetch from DB based on scope and sort ─────────────────

    private List<PostEntity> fetchPosts(FeedQuery q, CursorData cursor, int limit) {
        Sort sort = buildSort(q.getSortBy());
        PageRequest page = PageRequest.of(0, limit, sort);

        // Virtual/WFH posts are always globally visible
        switch (q.getScope()) {
            case "NEAR_ME":
            case "NEARBY":
                double radius = q.getRadiusKm() > 0 ? q.getRadiusKm() : 50;
                return postRepo.findNearbyPosts(
                        q.getUserLat(), q.getUserLng(), radius,
                        q.getCategory(), q.getMode(), q.getKeyword(),
                        cursor != null ? cursor.getLastSeenCreatedAt() : null,
                        cursor != null ? cursor.getLastSeenId() : null,
                        page);

            case "SAME_CITY":
                return postRepo.findByCityPosts(
                        q.getCity() != null ? q.getCity() : "",
                        q.getCategory(), q.getMode(), q.getKeyword(),
                        cursor != null ? cursor.getLastSeenCreatedAt() : null,
                        page);

            case "GLOBAL":
            default:
                return postRepo.findGlobalPosts(
                        q.getCategory(), q.getMode(), q.getKeyword(),
                        q.getUniversity(),
                        cursor != null ? cursor.getLastSeenCreatedAt() : null,
                        cursor != null ? cursor.getLastSeenId() : null,
                        page);
        }
    }

    // ── Sort direction ────────────────────────────────────────

    private Sort buildSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        switch (sortBy) {
            case "TRENDING": return Sort.by(Sort.Direction.DESC, "trendingScore");
            case "NEAREST":  return Sort.by(Sort.Direction.ASC,  "id"); // Distance computed later
            default:         return Sort.by(Sort.Direction.DESC, "createdAt");
        }
    }

    // ── Cursor encoding / decoding ────────────────────────────

    private String encodeCursor(PostEntity last, String sortBy) {
        String raw = last.getId() + "|" + last.getCreatedAt().toEpochMilli()
                   + "|" + last.getTrendingScore();
        return Base64.getUrlEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private CursorData decodeCursor(String encoded) {
        if (encoded == null || encoded.isBlank()) return null;
        try {
            String raw   = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|");
            UUID id      = UUID.fromString(parts[0]);
            Instant ts   = Instant.ofEpochMilli(Long.parseLong(parts[1]));
            return new CursorData(id, ts);
        } catch (Exception e) {
            return null;
        }
    }

    @Data
    private static class CursorData {
        private final UUID    lastSeenId;
        private final Instant lastSeenCreatedAt;
    }

    // ── Query object ──────────────────────────────────────────

    @Data @Builder
    public static class FeedQuery {
        private String sortBy;
        private String scope;
        private String category;
        private String mode;
        private int    radiusKm;
        private String city;
        private String university;
        private String keyword;
        private double userLat;
        private double userLng;
        private String cursor;
        private int    limit;
        private String requestingUserId;
    }
}
