package com.uniconnect.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// Comment
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "comments",
       indexes = {
           @Index(name = "idx_comments_post",   columnList = "post_id"),
           @Index(name = "idx_comments_parent", columnList = "parent_id"),
           @Index(name = "idx_comments_author", columnList = "author_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class CommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    // null = top-level, non-null = reply
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CommentEntity parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CommentEntity> replies = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;
}

// ─────────────────────────────────────────────────────────────
// Post Vote (upvote — one per user per post)
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "post_votes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}),
       indexes = {
           @Index(name = "idx_votes_post", columnList = "post_id"),
           @Index(name = "idx_votes_user", columnList = "user_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class PostVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @CreationTimestamp
    private Instant createdAt;
}

// ─────────────────────────────────────────────────────────────
// Comment Like
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "comment_likes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "comment_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class CommentLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommentEntity comment;

    @CreationTimestamp
    private Instant createdAt;
}

// ─────────────────────────────────────────────────────────────
// Saved Post (bookmark)
// ─────────────────────────────────────────────────────────────
@Entity
@Table(name = "saved_posts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "post_id"}),
       indexes = {
           @Index(name = "idx_saved_user", columnList = "user_id"),
           @Index(name = "idx_saved_post", columnList = "post_id")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
class SavedPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private PostEntity post;

    @CreationTimestamp
    private Instant savedAt;
}
