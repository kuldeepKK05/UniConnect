package com.uniconnect.services;

import com.uniconnect.controllers.PostController;
import com.uniconnect.models.*;
import com.uniconnect.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@Service
public class PostService {

    @Autowired private PostRepository      postRepo;
    @Autowired private UserRepository      userRepo;
    @Autowired private PostVoteRepository  voteRepo;
    @Autowired private SavedPostRepository savedRepo;

    // ── Get single post ───────────────────────────────────────

    public PostController.PostDto getPost(String postId, String requestingUserEmail) {
        PostEntity post = findActivePost(postId);

        boolean upvoted = false, saved = false;
        if (requestingUserEmail != null) {
            UserEntity user = userRepo.findByEmail(requestingUserEmail).orElse(null);
            if (user != null) {
                upvoted = voteRepo.existsByUserAndPost(user, post);
                saved   = savedRepo.existsByUserAndPost(user, post);
            }
        }

        return PostController.PostDto.from(post, 0.0, upvoted, saved);
    }

    // ── Create post ───────────────────────────────────────────

    @Transactional
    public PostController.PostDto createPost(PostController.CreatePostRequest req,
                                             String authorEmail) {
        UserEntity author = userRepo.findByEmail(authorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!author.isVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your account is not yet verified");
        }

        PostEntity post = PostEntity.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .category(PostEntity.Category.valueOf(req.getCategory()))
                .mode(PostEntity.Mode.valueOf(req.getMode()))
                .universityName(req.getUniversityName())
                .city(req.getCity())
                .state(req.getState())
                .country(req.getCountry())
                .venue(req.getVenue())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .applyLink(req.getApplyLink())
                .deadline(req.getDeadline() != null ? LocalDate.parse(req.getDeadline()) : null)
                .imageUrl(req.getImageUrl())
                .author(author)
                .build();

        post = postRepo.save(post);
        return PostController.PostDto.from(post, 0.0, false, false);
    }

    // ── Update post ───────────────────────────────────────────

    @Transactional
    public PostController.PostDto updatePost(String postId,
                                             PostController.CreatePostRequest req,
                                             String editorEmail) {
        PostEntity post = findActivePost(postId);
        UserEntity editor = userRepo.findByEmail(editorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Only original author or ADMIN can edit
        if (!post.getAuthor().getEmail().equals(editorEmail)
                && editor.getRole() != UserEntity.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        post.setTitle(req.getTitle());
        post.setDescription(req.getDescription());
        post.setCategory(PostEntity.Category.valueOf(req.getCategory()));
        post.setMode(PostEntity.Mode.valueOf(req.getMode()));
        post.setVenue(req.getVenue());
        post.setApplyLink(req.getApplyLink());
        post.setDeadline(req.getDeadline() != null ? LocalDate.parse(req.getDeadline()) : null);
        post.setImageUrl(req.getImageUrl());
        post.setCity(req.getCity());
        post.setState(req.getState());
        post.setCountry(req.getCountry());
        post.setLatitude(req.getLatitude());
        post.setLongitude(req.getLongitude());

        post = postRepo.save(post);
        return PostController.PostDto.from(post, 0.0, false, false);
    }

    // ── Delete post (soft) ────────────────────────────────────

    @Transactional
    public void deletePost(String postId, String editorEmail) {
        PostEntity post = findActivePost(postId);
        UserEntity editor = userRepo.findByEmail(editorEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        if (!post.getAuthor().getEmail().equals(editorEmail)
                && editor.getRole() != UserEntity.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        post.setActive(false);
        postRepo.save(post);
    }

    // ── Toggle upvote ─────────────────────────────────────────

    @Transactional
    public Map<String, Object> toggleUpvote(String postId, String userEmail) {
        PostEntity post = findActivePost(postId);
        UserEntity user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean alreadyVoted = voteRepo.existsByUserAndPost(user, post);
        if (alreadyVoted) {
            voteRepo.deleteByUserAndPost(user, post);
            post.setUpvoteCount(Math.max(0, post.getUpvoteCount() - 1));
        } else {
            PostVoteEntity vote = PostVoteEntity.builder().user(user).post(post).build();
            voteRepo.save(vote);
            post.setUpvoteCount(post.getUpvoteCount() + 1);
        }

        recalculateTrendingScore(post);
        postRepo.save(post);

        return Map.of("upvoted", !alreadyVoted, "newCount", post.getUpvoteCount());
    }

    // ── Toggle save ───────────────────────────────────────────

    @Transactional
    public Map<String, Object> toggleSave(String postId, String userEmail) {
        PostEntity post = findActivePost(postId);
        UserEntity user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        boolean alreadySaved = savedRepo.existsByUserAndPost(user, post);
        if (alreadySaved) {
            savedRepo.deleteByUserAndPost(user, post);
        } else {
            savedRepo.save(SavedPostEntity.builder().user(user).post(post).build());
        }

        return Map.of("saved", !alreadySaved);
    }

    // ── Trending score (Wilson score-inspired) ────────────────

    private void recalculateTrendingScore(PostEntity post) {
        double ageHours = (System.currentTimeMillis() - post.getCreatedAt().toEpochMilli())
                / 3_600_000.0;
        double gravity  = 1.8;
        double score    = post.getUpvoteCount() / Math.pow(ageHours + 2, gravity);
        post.setTrendingScore(score);
    }

    // ── Helpers ───────────────────────────────────────────────

    private PostEntity findActivePost(String postId) {
        return postRepo.findByIdAndActiveTrue(UUID.fromString(postId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }
}
