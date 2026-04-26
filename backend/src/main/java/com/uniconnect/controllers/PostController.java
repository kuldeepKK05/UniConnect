package com.uniconnect.controllers;

import com.uniconnect.models.PostEntity;
import com.uniconnect.services.PostService;
import com.uniconnect.services.CommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired private PostService postService;
    @Autowired private CommentService commentService;

    // ── GET /posts/:id ────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> getPost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        String userId = principal != null ? principal.getUsername() : null;
        PostDto post = postService.getPost(id, userId);
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    // ── POST /posts (admin/university only) ───────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY')")
    public ResponseEntity<ApiResponse<PostDto>> createPost(
            @Valid @RequestBody CreatePostRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        PostDto post = postService.createPost(req, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(post));
    }

    // ── PUT /posts/:id ────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY')")
    public ResponseEntity<ApiResponse<PostDto>> updatePost(
            @PathVariable String id,
            @Valid @RequestBody CreatePostRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        PostDto post = postService.updatePost(id, req, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(post));
    }

    // ── DELETE /posts/:id ─────────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','UNIVERSITY')")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        postService.deletePost(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Post deleted"));
    }

    // ── POST /posts/:id/upvote ────────────────────────────────
    @PostMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleUpvote(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        var result = postService.toggleUpvote(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── POST /posts/:id/save ──────────────────────────────────
    @PostMapping("/{id}/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleSave(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        var result = postService.toggleSave(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── GET /posts/:id/comments ───────────────────────────────
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<CommentDto>>> getComments(
            @PathVariable String id,
            @RequestParam(required = false)  String cursor,
            @RequestParam(defaultValue = "30") int limit) {
        List<CommentDto> comments = commentService.getComments(id, cursor, limit);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    // ── POST /posts/:id/comments ──────────────────────────────
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<CommentDto>> addComment(
            @PathVariable String id,
            @Valid @RequestBody CommentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        CommentDto comment = commentService.addComment(id, req.getContent(),
                req.getParentId(), principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(comment));
    }

    // ── POST /comments/:id/replies ────────────────────────────
    @PostMapping("/comments/{id}/replies")
    public ResponseEntity<ApiResponse<CommentDto>> addReply(
            @PathVariable String id,
            @Valid @RequestBody CommentRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        CommentDto reply = commentService.addReply(id, req.getContent(), principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(reply));
    }

    // ── POST /comments/:id/like ───────────────────────────────
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<ApiResponse<Map<String, Object>>> likeComment(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        var result = commentService.toggleLike(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── DELETE /comments/:id ──────────────────────────────────
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails principal) {
        commentService.deleteComment(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }

    // ─────────────────────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────────────────────

    @Data
    public static class CreatePostRequest {
        @NotBlank @Size(max = 300) private String title;
        @NotBlank                  private String description;
        @NotBlank                  private String category;
        @NotBlank                  private String mode;
        @NotBlank @Size(max = 200) private String universityName;
        @NotBlank @Size(max = 100) private String city;
        @Size(max = 100)           private String state;
        @NotBlank @Size(max = 100) private String country;
        @Size(max = 200)           private String venue;
        private double latitude;
        private double longitude;
        @NotBlank                  private String applyLink;
        private String deadline;  // yyyy-MM-dd
        private String imageUrl;
    }

    @Data
    public static class CommentRequest {
        @NotBlank @Size(max = 2000) private String content;
        private String parentId;
    }

    public static class PostDto {
        public String  id;
        public String  title;
        public String  description;
        public String  category;
        public String  mode;
        public String  universityName;
        public String  city;
        public String  state;
        public String  country;
        public String  venue;
        public double  latitude;
        public double  longitude;
        public double  distanceKm;
        public String  deadline;
        public String  applyLink;
        public String  postedDate;
        public int     upvoteCount;
        public int     commentCount;
        public boolean isUpvotedByUser;
        public boolean isSavedByUser;
        public String  imageUrl;
        public String  postedBy;
        public double  trendingScore;

        public static PostDto from(PostEntity p, double distanceKm,
                                   boolean upvoted, boolean saved) {
            PostDto dto = new PostDto();
            dto.id             = p.getId().toString();
            dto.title          = p.getTitle();
            dto.description    = p.getDescription();
            dto.category       = p.getCategory().name();
            dto.mode           = p.getMode().name();
            dto.universityName = p.getUniversityName();
            dto.city           = p.getCity();
            dto.state          = p.getState();
            dto.country        = p.getCountry();
            dto.venue          = p.getVenue();
            dto.latitude       = p.getLatitude();
            dto.longitude      = p.getLongitude();
            dto.distanceKm     = distanceKm;
            dto.deadline       = p.getDeadline() != null ? p.getDeadline().toString() : null;
            dto.applyLink      = p.getApplyLink();
            dto.postedDate     = p.getCreatedAt().toString();
            dto.upvoteCount    = p.getUpvoteCount();
            dto.commentCount   = p.getCommentCount();
            dto.isUpvotedByUser = upvoted;
            dto.isSavedByUser  = saved;
            dto.imageUrl       = p.getImageUrl();
            dto.postedBy       = p.getAuthor() != null ? p.getAuthor().getName() : null;
            dto.trendingScore  = p.getTrendingScore();
            return dto;
        }
    }

    public static class CommentDto {
        public String id;
        public String postId;
        public String parentId;
        public String authorName;
        public String authorAvatarUrl;
        public String authorId;
        public String content;
        public int    likeCount;
        public boolean isLikedByUser;
        public String createdAt;
        public List<CommentDto> replies;
    }
}
