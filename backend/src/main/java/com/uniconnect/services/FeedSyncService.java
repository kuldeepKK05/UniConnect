package com.uniconnect.services;

import com.uniconnect.cassandra.PostByLocationEntity;
import com.uniconnect.cassandra.PostByTrendingEntity;
import com.uniconnect.cassandra.PostByUserEntity;
import com.uniconnect.cassandra.repositories.PostByLocationRepository;
import com.uniconnect.cassandra.repositories.PostByTrendingRepository;
import com.uniconnect.cassandra.repositories.PostByUserRepository;
import com.uniconnect.models.PostEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Asynchronously syncs post writes from PostgreSQL → Cassandra.
 *
 * Called after every successful PG write.
 * If Cassandra is unavailable, failures are swallowed (PG is source of truth).
 */
@Service
public class FeedSyncService {

    private static final Logger log = LoggerFactory.getLogger(FeedSyncService.class);

    @Autowired(required = false) private PostByLocationRepository locationRepo;
    @Autowired(required = false) private PostByTrendingRepository trendingRepo;
    @Autowired(required = false) private PostByUserRepository     userRepo;

    // ── Sync new or updated post to all Cassandra tables ──────

    @Async
    public void syncPost(PostEntity post) {
        if (!isCassandraAvailable()) return;
        try {
            syncToLocation(post);
            syncToTrending(post);
            syncToUserPosted(post);
        } catch (Exception e) {
            log.warn("Cassandra sync failed for post {}: {}", post.getId(), e.getMessage());
        }
    }

    // ── Sync save action to posts_by_user ─────────────────────

    @Async
    public void syncSave(PostEntity post, UUID userId, boolean saved) {
        if (!isCassandraAvailable()) return;
        try {
            if (saved) {
                PostByUserEntity entity = buildUserEntity(post, userId, "SAVED");
                userRepo.save(entity);
            } else {
                userRepo.deleteByUserIdAndListTypeAndPostId(userId, "SAVED", post.getId());
            }
        } catch (Exception e) {
            log.warn("Cassandra save sync failed: {}", e.getMessage());
        }
    }

    // ── Sync trending score update ─────────────────────────────

    @Async
    public void syncTrendingScore(PostEntity post) {
        if (!isCassandraAvailable()) return;
        try {
            syncToTrending(post);
        } catch (Exception e) {
            log.warn("Cassandra trending sync failed for post {}: {}", post.getId(), e.getMessage());
        }
    }

    // ── Remove post from Cassandra (soft delete / deactivate) ─

    @Async
    public void removePost(PostEntity post) {
        if (!isCassandraAvailable()) return;
        try {
            locationRepo.deleteByCountryAndCityAndCreatedAtAndPostId(
                    post.getCountry(), post.getCity(), post.getCreatedAt(), post.getId());

            String bucket = monthBucket();
            trendingRepo.deleteByDateBucketAndTrendingScoreAndPostId(
                    bucket, post.getTrendingScore(), post.getId());
        } catch (Exception e) {
            log.warn("Cassandra remove failed for post {}: {}", post.getId(), e.getMessage());
        }
    }

    // ── Private builders ──────────────────────────────────────

    private void syncToLocation(PostEntity post) {
        PostByLocationEntity e = new PostByLocationEntity();
        e.setCountry(post.getCountry());
        e.setCity(post.getCity());
        e.setCreatedAt(post.getCreatedAt());
        e.setPostId(post.getId());
        e.setTitle(post.getTitle());
        e.setUniversityName(post.getUniversityName());
        e.setCategory(post.getCategory().name());
        e.setMode(post.getMode().name());
        e.setLatitude(post.getLatitude());
        e.setLongitude(post.getLongitude());
        e.setUpvoteCount(post.getUpvoteCount());
        e.setCommentCount(post.getCommentCount());
        e.setTrendingScore(post.getTrendingScore());
        e.setDeadline(post.getDeadline());
        e.setImageUrl(post.getImageUrl());
        locationRepo.save(e);

        // For virtual posts, also index under a "global" bucket
        if (post.isVirtual()) {
            e.setCountry("GLOBAL");
            e.setCity("VIRTUAL");
            locationRepo.save(e);
        }
    }

    private void syncToTrending(PostEntity post) {
        PostByTrendingEntity e = new PostByTrendingEntity();
        e.setDateBucket(monthBucket());
        e.setTrendingScore(post.getTrendingScore());
        e.setPostId(post.getId());
        e.setTitle(post.getTitle());
        e.setUniversityName(post.getUniversityName());
        e.setCategory(post.getCategory().name());
        e.setMode(post.getMode().name());
        e.setCity(post.getCity());
        e.setCountry(post.getCountry());
        e.setLatitude(post.getLatitude());
        e.setLongitude(post.getLongitude());
        e.setUpvoteCount(post.getUpvoteCount());
        e.setCommentCount(post.getCommentCount());
        e.setCreatedAt(post.getCreatedAt());
        e.setDeadline(post.getDeadline());
        e.setImageUrl(post.getImageUrl());
        trendingRepo.save(e);

        // Also index under "global" bucket
        e.setDateBucket("global");
        trendingRepo.save(e);
    }

    private void syncToUserPosted(PostEntity post) {
        if (post.getAuthor() == null) return;
        PostByUserEntity e = buildUserEntity(post, post.getAuthor().getId(), "POSTED");
        userRepo.save(e);
    }

    private PostByUserEntity buildUserEntity(PostEntity post, UUID userId, String listType) {
        PostByUserEntity e = new PostByUserEntity();
        e.setUserId(userId);
        e.setListType(listType);
        e.setCreatedAt(post.getCreatedAt());
        e.setPostId(post.getId());
        e.setTitle(post.getTitle());
        e.setUniversityName(post.getUniversityName());
        e.setCategory(post.getCategory().name());
        e.setMode(post.getMode().name());
        e.setCity(post.getCity());
        e.setCountry(post.getCountry());
        e.setImageUrl(post.getImageUrl());
        return e;
    }

    private String monthBucket() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private boolean isCassandraAvailable() {
        return locationRepo != null && trendingRepo != null && userRepo != null;
    }
}
