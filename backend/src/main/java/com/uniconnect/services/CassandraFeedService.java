package com.uniconnect.services;

import com.uniconnect.cassandra.PostByLocationEntity;
import com.uniconnect.cassandra.PostByTrendingEntity;
import com.uniconnect.cassandra.PostByUserEntity;
import com.uniconnect.cassandra.repositories.PostByLocationRepository;
import com.uniconnect.cassandra.repositories.PostByTrendingRepository;
import com.uniconnect.cassandra.repositories.PostByUserRepository;
import com.uniconnect.controllers.PostController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reads feed data from Cassandra for performance-critical paths.
 * Falls back to PostgreSQL (FeedService) on any Cassandra failure.
 *
 * Write strategy: FeedSyncService writes to Cassandra asynchronously
 * after every PostgreSQL write (see FeedSyncService.java).
 */
@Service
public class CassandraFeedService {

    private static final Logger log = LoggerFactory.getLogger(CassandraFeedService.class);

    @Autowired(required = false) private PostByLocationRepository locationRepo;
    @Autowired(required = false) private PostByTrendingRepository trendingRepo;
    @Autowired(required = false) private PostByUserRepository     userRepo;

    // ── Nearby posts from Cassandra (location bucket) ─────────

    public List<PostController.PostDto> getNearbyFromCassandra(
            String country, String city, int limit, Instant cursor) {
        if (locationRepo == null) return new ArrayList<>();
        try {
            List<PostByLocationEntity> rows = cursor == null
                    ? locationRepo.findByCountryAndCityOrderByCreatedAtDesc(
                            country, city,
                            org.springframework.data.domain.PageRequest.of(0, limit))
                    : locationRepo.findByCountryAndCityAndCreatedAtBeforeOrderByCreatedAtDesc(
                            country, city, cursor,
                            org.springframework.data.domain.PageRequest.of(0, limit));

            return rows.stream().map(this::locationToDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Cassandra location read failed, will fallback to PG: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── Trending posts from Cassandra ─────────────────────────

    public List<PostController.PostDto> getTrendingFromCassandra(
            String dateBucket, int limit, Double scoreCursor) {
        if (trendingRepo == null) return new ArrayList<>();
        try {
            List<PostByTrendingEntity> rows = scoreCursor == null
                    ? trendingRepo.findByDateBucketOrderByTrendingScoreDesc(
                            dateBucket,
                            org.springframework.data.domain.PageRequest.of(0, limit))
                    : trendingRepo.findByDateBucketAndTrendingScoreLessThanOrderByTrendingScoreDesc(
                            dateBucket, scoreCursor,
                            org.springframework.data.domain.PageRequest.of(0, limit));

            return rows.stream().map(this::trendingToDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Cassandra trending read failed, will fallback to PG: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── User-specific lists (saved / posted) ──────────────────

    public List<PostController.PostDto> getUserListFromCassandra(
            UUID userId, String listType, int limit, Instant cursor) {
        if (userRepo == null) return new ArrayList<>();
        try {
            List<PostByUserEntity> rows = cursor == null
                    ? userRepo.findByUserIdAndListTypeOrderByCreatedAtDesc(
                            userId, listType,
                            org.springframework.data.domain.PageRequest.of(0, limit))
                    : userRepo.findByUserIdAndListTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
                            userId, listType, cursor,
                            org.springframework.data.domain.PageRequest.of(0, limit));

            return rows.stream().map(this::userPostToDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Cassandra user list read failed, will fallback to PG: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ── DTO converters ────────────────────────────────────────

    private PostController.PostDto locationToDto(PostByLocationEntity e) {
        PostController.PostDto dto = new PostController.PostDto();
        dto.id             = e.getPostId().toString();
        dto.title          = e.getTitle();
        dto.universityName = e.getUniversityName();
        dto.category       = e.getCategory();
        dto.mode           = e.getMode();
        dto.city           = e.getCity();
        dto.country        = e.getCountry();
        dto.latitude       = e.getLatitude();
        dto.longitude      = e.getLongitude();
        dto.upvoteCount    = e.getUpvoteCount();
        dto.commentCount   = e.getCommentCount();
        dto.trendingScore  = e.getTrendingScore();
        dto.postedDate     = e.getCreatedAt() != null ? e.getCreatedAt().toString() : null;
        dto.imageUrl       = e.getImageUrl();
        dto.deadline       = e.getDeadline() != null ? e.getDeadline().toString() : null;
        return dto;
    }

    private PostController.PostDto trendingToDto(PostByTrendingEntity e) {
        PostController.PostDto dto = new PostController.PostDto();
        dto.id             = e.getPostId().toString();
        dto.title          = e.getTitle();
        dto.universityName = e.getUniversityName();
        dto.category       = e.getCategory();
        dto.mode           = e.getMode();
        dto.city           = e.getCity();
        dto.country        = e.getCountry();
        dto.latitude       = e.getLatitude();
        dto.longitude      = e.getLongitude();
        dto.upvoteCount    = e.getUpvoteCount();
        dto.commentCount   = e.getCommentCount();
        dto.trendingScore  = e.getTrendingScore();
        dto.postedDate     = e.getCreatedAt() != null ? e.getCreatedAt().toString() : null;
        dto.imageUrl       = e.getImageUrl();
        dto.deadline       = e.getDeadline() != null ? e.getDeadline().toString() : null;
        return dto;
    }

    private PostController.PostDto userPostToDto(PostByUserEntity e) {
        PostController.PostDto dto = new PostController.PostDto();
        dto.id             = e.getPostId().toString();
        dto.title          = e.getTitle();
        dto.universityName = e.getUniversityName();
        dto.category       = e.getCategory();
        dto.mode           = e.getMode();
        dto.city           = e.getCity();
        dto.country        = e.getCountry();
        dto.imageUrl       = e.getImageUrl();
        dto.postedDate     = e.getCreatedAt() != null ? e.getCreatedAt().toString() : null;
        return dto;
    }

    // ── Health check ──────────────────────────────────────────

    public boolean isCassandraAvailable() {
        return locationRepo != null;
    }
}
