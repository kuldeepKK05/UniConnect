package com.uniconnect.services;

import com.uniconnect.controllers.PostController;
import com.uniconnect.models.*;
import com.uniconnect.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired private CommentRepository  commentRepo;
    @Autowired private PostRepository     postRepo;
    @Autowired private UserRepository     userRepo;
    @Autowired private CommentLikeRepository likeRepo;

    // ── Get threaded comments for a post ──────────────────────

    public List<PostController.CommentDto> getComments(String postId, String cursor, int limit) {
        UUID pid = UUID.fromString(postId);
        // Fetch only root comments; replies loaded via entity relationship
        List<CommentEntity> roots = commentRepo
                .findByPostIdAndParentIsNullAndActiveTrueOrderByCreatedAtAsc(pid);

        return roots.stream()
                .map(c -> toDto(c, null, 0))
                .collect(Collectors.toList());
    }

    // ── Add top-level comment ─────────────────────────────────

    @Transactional
    public PostController.CommentDto addComment(String postId, String content,
                                                String parentId, String authorEmail) {
        PostEntity post = postRepo.findByIdAndActiveTrue(UUID.fromString(postId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserEntity author = findUser(authorEmail);

        CommentEntity parent = null;
        if (parentId != null) {
            parent = commentRepo.findById(UUID.fromString(parentId)).orElse(null);
        }

        CommentEntity comment = CommentEntity.builder()
                .post(post)
                .author(author)
                .parent(parent)
                .content(content.trim())
                .build();
        comment = commentRepo.save(comment);

        // Increment post comment count
        post.setCommentCount(post.getCommentCount() + 1);
        postRepo.save(post);

        return toDto(comment, null, parent != null ? 1 : 0);
    }

    // ── Add reply ─────────────────────────────────────────────

    @Transactional
    public PostController.CommentDto addReply(String parentCommentId, String content,
                                              String authorEmail) {
        CommentEntity parent = commentRepo.findById(UUID.fromString(parentCommentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return addComment(parent.getPost().getId().toString(), content,
                parentCommentId, authorEmail);
    }

    // ── Toggle like on comment ────────────────────────────────

    @Transactional
    public Map<String, Object> toggleLike(String commentId, String userEmail) {
        CommentEntity comment = commentRepo.findById(UUID.fromString(commentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserEntity user = findUser(userEmail);

        boolean liked = likeRepo.existsByUserAndComment(user, comment);
        if (liked) {
            likeRepo.deleteByUserAndComment(user, comment);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            likeRepo.save(CommentLikeEntity.builder().user(user).comment(comment).build());
            comment.setLikeCount(comment.getLikeCount() + 1);
        }
        commentRepo.save(comment);

        return Map.of("liked", !liked, "newCount", comment.getLikeCount());
    }

    // ── Delete comment (soft) ─────────────────────────────────

    @Transactional
    public void deleteComment(String commentId, String userEmail) {
        CommentEntity comment = commentRepo.findById(UUID.fromString(commentId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserEntity user = findUser(userEmail);

        boolean isAuthor = comment.getAuthor().getEmail().equals(userEmail);
        boolean isAdmin  = user.getRole() == UserEntity.Role.ADMIN;
        if (!isAuthor && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        comment.setActive(false);
        commentRepo.save(comment);
    }

    // ── Helpers ───────────────────────────────────────────────

    private PostController.CommentDto toDto(CommentEntity c,
                                            String requestingUserId,
                                            int depth) {
        PostController.CommentDto dto = new PostController.CommentDto();
        dto.id              = c.getId().toString();
        dto.postId          = c.getPost().getId().toString();
        dto.parentId        = c.getParent() != null ? c.getParent().getId().toString() : null;
        dto.authorName      = c.getAuthor().getName();
        dto.authorAvatarUrl = c.getAuthor().getAvatarUrl();
        dto.authorId        = c.getAuthor().getId().toString();
        dto.content         = c.isActive() ? c.getContent() : "[deleted]";
        dto.likeCount       = c.getLikeCount();
        dto.isLikedByUser   = false; // simplified — extend with per-user query
        dto.createdAt       = c.getCreatedAt().toString();

        if (c.getReplies() != null && !c.getReplies().isEmpty()) {
            dto.replies = c.getReplies().stream()
                    .filter(CommentEntity::isActive)
                    .map(r -> toDto(r, requestingUserId, depth + 1))
                    .collect(Collectors.toList());
        }

        return dto;
    }

    private UserEntity findUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
