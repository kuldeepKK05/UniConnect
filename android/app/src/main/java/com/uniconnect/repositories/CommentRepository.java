package com.uniconnect.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.Comment;
import com.uniconnect.models.ApiResponses.*;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentRepository {

    private final ApiService apiService;

    public final MutableLiveData<List<Comment>> commentsLiveData  = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean>       loadingLiveData   = new MutableLiveData<>(false);
    public final MutableLiveData<String>        errorLiveData     = new MutableLiveData<>();
    public final MutableLiveData<Comment>       newCommentLiveData = new MutableLiveData<>();

    public CommentRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    // ── Load all comments for a post ──────────────────────────

    public void loadComments(String postId) {
        loadingLiveData.setValue(true);
        apiService.getComments(postId, null, 50)
                .enqueue(new Callback<ApiResponse<List<Comment>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<Comment>>> call,
                                           Response<ApiResponse<List<Comment>>> response) {
                        loadingLiveData.postValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            commentsLiveData.postValue(response.body().getData());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {
                        loadingLiveData.postValue(false);
                        // Silent fail for comments – post detail still usable
                    }
                });
    }

    // ── Post a new comment or reply ───────────────────────────

    public void postComment(String postId, String content, String parentId) {
        ApiService.CommentRequest req = new ApiService.CommentRequest(content, parentId);

        Call<ApiResponse<Comment>> call = (parentId != null && !parentId.isEmpty())
                ? apiService.addReply(parentId, req)
                : apiService.addComment(postId, req);

        call.enqueue(new Callback<ApiResponse<Comment>>() {
            @Override
            public void onResponse(Call<ApiResponse<Comment>> c,
                                   Response<ApiResponse<Comment>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    newCommentLiveData.postValue(response.body().getData());
                    // Refresh full list to get updated thread structure
                    loadComments(postId);
                } else {
                    errorLiveData.postValue("Failed to post comment");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Comment>> c, Throwable t) {
                errorLiveData.postValue("Network error: " + t.getMessage());
            }
        });
    }

    // ── Toggle like on a comment ──────────────────────────────

    public void toggleLike(String commentId, String postId) {
        apiService.toggleCommentLike(commentId)
                .enqueue(new Callback<ApiResponse<LikeResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<LikeResponse>> call,
                                           Response<ApiResponse<LikeResponse>> response) {
                        if (response.isSuccessful()) {
                            // Refresh to reflect updated like count
                            loadComments(postId);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<LikeResponse>> call,
                                          Throwable t) {
                        errorLiveData.postValue("Failed to like comment");
                    }
                });
    }

    // ── Delete a comment ──────────────────────────────────────

    public void deleteComment(String commentId, String postId) {
        apiService.deleteComment(commentId)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful()) {
                            loadComments(postId);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        errorLiveData.postValue("Failed to delete comment");
                    }
                });
    }
}
