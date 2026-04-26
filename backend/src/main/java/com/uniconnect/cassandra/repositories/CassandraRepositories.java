package com.uniconnect.cassandra.repositories;

import com.uniconnect.cassandra.PostByLocationEntity;
import com.uniconnect.cassandra.PostByTrendingEntity;
import com.uniconnect.cassandra.PostByUserEntity;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// posts_by_location
// ─────────────────────────────────────────────────────────────
@Repository
interface PostByLocationRepository
        extends CassandraRepository<PostByLocationEntity, Object> {

    List<PostByLocationEntity> findByCountryAndCityOrderByCreatedAtDesc(
            String country, String city, Pageable pageable);

    List<PostByLocationEntity> findByCountryAndCityAndCreatedAtBeforeOrderByCreatedAtDesc(
            String country, String city, Instant before, Pageable pageable);

    void deleteByCountryAndCityAndCreatedAtAndPostId(
            String country, String city, Instant createdAt, UUID postId);
}

// ─────────────────────────────────────────────────────────────
// posts_by_trending
// ─────────────────────────────────────────────────────────────
@Repository
interface PostByTrendingRepository
        extends CassandraRepository<PostByTrendingEntity, Object> {

    List<PostByTrendingEntity> findByDateBucketOrderByTrendingScoreDesc(
            String dateBucket, Pageable pageable);

    List<PostByTrendingEntity> findByDateBucketAndTrendingScoreLessThanOrderByTrendingScoreDesc(
            String dateBucket, double scoreCursor, Pageable pageable);

    void deleteByDateBucketAndTrendingScoreAndPostId(
            String dateBucket, double trendingScore, UUID postId);
}

// ─────────────────────────────────────────────────────────────
// posts_by_user
// ─────────────────────────────────────────────────────────────
@Repository
interface PostByUserRepository
        extends CassandraRepository<PostByUserEntity, Object> {

    List<PostByUserEntity> findByUserIdAndListTypeOrderByCreatedAtDesc(
            UUID userId, String listType, Pageable pageable);

    List<PostByUserEntity> findByUserIdAndListTypeAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID userId, String listType, Instant before, Pageable pageable);

    void deleteByUserIdAndListTypeAndPostId(UUID userId, String listType, UUID postId);
}
