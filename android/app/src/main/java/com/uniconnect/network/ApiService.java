package com.uniconnect.network;

import com.uniconnect.models.*;
import com.uniconnect.models.ApiResponses.*;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // ── AUTH ──────────────────────────────────────────────────
    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);
    @POST("auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);
    @POST("auth/google")
    Call<ApiResponse<AuthResponse>> googleLogin(@Body Map<String, String> body);
    @GET("auth/me")
    Call<ApiResponse<AuthResponse.UserDto>> getCurrentUser();
    @POST("auth/logout")
    Call<ApiResponse<Void>> logout();
    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refreshToken(@Body Map<String, String> body);

    // ── FEED ──────────────────────────────────────────────────
    @GET("feed")
    Call<ApiResponse<FeedResponse>> getFeed(
            @Query("sortBy") String sortBy, @Query("scope") String scope,
            @Query("category") String category, @Query("mode") String mode,
            @Query("radiusKm") int radiusKm, @Query("city") String city,
            @Query("university") String university, @Query("keyword") String keyword,
            @Query("userLat") double userLat, @Query("userLng") double userLng,
            @Query("cursor") String cursor, @Query("limit") int limit);

    // ── POSTS ─────────────────────────────────────────────────
    @GET("posts/{id}")
    Call<ApiResponse<Post>> getPost(@Path("id") String postId);
    @POST("posts")
    Call<ApiResponse<Post>> createPost(@Body Post post);
    @PUT("posts/{id}")
    Call<ApiResponse<Post>> updatePost(@Path("id") String postId, @Body Post post);
    @DELETE("posts/{id}")
    Call<ApiResponse<Void>> deletePost(@Path("id") String postId);

    // ── UPVOTE / SAVE ─────────────────────────────────────────
    @POST("posts/{id}/upvote")
    Call<ApiResponse<UpvoteResponse>> toggleUpvote(@Path("id") String postId);
    @POST("posts/{id}/save")
    Call<ApiResponse<SaveResponse>> toggleSave(@Path("id") String postId);
    @GET("users/saved")
    Call<ApiResponse<FeedResponse>> getSavedPosts(@Query("cursor") String cursor, @Query("limit") int limit);

    // ── COMMENTS ──────────────────────────────────────────────
    @GET("posts/{id}/comments")
    Call<ApiResponse<List<Comment>>> getComments(@Path("id") String postId, @Query("cursor") String cursor, @Query("limit") int limit);
    @POST("posts/{id}/comments")
    Call<ApiResponse<Comment>> addComment(@Path("id") String postId, @Body CommentRequest request);
    @POST("posts/comments/{id}/replies")
    Call<ApiResponse<Comment>> addReply(@Path("id") String commentId, @Body CommentRequest request);
    @POST("posts/comments/{id}/like")
    Call<ApiResponse<LikeResponse>> toggleCommentLike(@Path("id") String commentId);
    @DELETE("posts/comments/{id}")
    Call<ApiResponse<Void>> deleteComment(@Path("id") String commentId);

    // ── USERS ─────────────────────────────────────────────────
    @GET("users/{id}")
    Call<ApiResponse<AuthResponse.UserDto>> getUser(@Path("id") String userId);
    @PUT("users/{id}")
    Call<ApiResponse<AuthResponse.UserDto>> updateProfile(@Path("id") String userId, @Body UpdateProfileRequest req);

    // ── SEARCH ────────────────────────────────────────────────
    @GET("search")
    Call<ApiResponse<FeedResponse>> search(@Query("q") String keyword, @Query("category") String category, @Query("mode") String mode, @Query("city") String city, @Query("cursor") String cursor, @Query("limit") int limit);

    // ── GEO ───────────────────────────────────────────────────
    @GET("geo/cities")
    Call<ApiResponse<List<City>>> searchCities(@Query("q") String query);

    // ── ADMIN ─────────────────────────────────────────────────
    @GET("admin/pending")
    Call<ApiResponse<List<AuthResponse.UserDto>>> getPendingVerifications();
    @POST("admin/verify/{userId}")
    Call<ApiResponse<AuthResponse.UserDto>> verifyUniversity(@Path("userId") String userId);

    // ── Requests ──────────────────────────────────────────────
    class CommentRequest {
        public String content, parentId;
        public CommentRequest(String content, String parentId) { this.content=content; this.parentId=parentId; }
    }

    class UpdateProfileRequest {
        public String name, city, country, avatarUrl;
    }
}
