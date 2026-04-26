package com.uniconnect.services;

import com.uniconnect.models.PostEntity;
import com.uniconnect.repositories.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Periodically recalculates trending scores for active posts.
 * Uses a Hacker News-style gravity algorithm:
 *   score = upvotes / (ageHours + 2) ^ 1.8
 *
 * Runs every 15 minutes.
 */
@Component
public class TrendingScoreScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrendingScoreScheduler.class);
    private static final double GRAVITY = 1.8;

    @Autowired
    private PostRepository postRepo;

    /**
     * Recalculate trending score for all active posts
     * created in the last 30 days (older posts decay to near-zero naturally).
     */
    @Scheduled(fixedDelay = 15 * 60 * 1000)   // every 15 min
    @Transactional
    public void recalculateTrendingScores() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);

        // Fetch in pages to avoid OOM on large datasets
        int page = 0;
        int pageSize = 200;
        int updated = 0;

        List<PostEntity> batch;
        do {
            batch = postRepo.findGlobalPosts(
                    null, null, null, null,
                    cutoff,
                    null,
                    PageRequest.of(page, pageSize));

            for (PostEntity post : batch) {
                double ageHours = (System.currentTimeMillis() -
                        post.getCreatedAt().toEpochMilli()) / 3_600_000.0;
                double score = post.getUpvoteCount() / Math.pow(ageHours + 2, GRAVITY);
                post.setTrendingScore(score);
            }

            if (!batch.isEmpty()) {
                postRepo.saveAll(batch);
                updated += batch.size();
            }

            page++;
        } while (batch.size() == pageSize);

        if (updated > 0) {
            log.info("Recalculated trending scores for {} posts", updated);
        }
    }
}
