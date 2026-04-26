package com.uniconnect.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.Comment;
import com.uniconnect.models.Post;
import com.uniconnect.models.ApiResponses.*;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private final String postId;

    private final MutableLiveData<Post> post = new MutableLiveData<>();
    private final MutableLiveData<List<Comment>> comments = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> upvoteState = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saveState = new MutableLiveData<>(false);

    public PostDetailViewModel(@NonNull Application application, String postId) {
        super(application);
        this.apiService = RetrofitClient.getInstance(application).getApiService();
        this.postId = postId;
    }

    public LiveData<Post> getPost() { return post; }
    public LiveData<List<Comment>> getComments() { return comments; }
    public LiveData<Boolean> isLoading() { return isLoading; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getUpvoteState() { return upvoteState; }
    public LiveData<Boolean> getSaveState() { return saveState; }

    public void loadPost() {
        isLoading.setValue(true);
        apiService.getPost(postId).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call, Response<ApiResponse<Post>> response) {
                isLoading.setValue(false);
                if (response.isSuccessful() && response.body() != null) {
                    Post data = response.body().getData();
                    post.setValue(data);
                    if (data != null) {
                        upvoteState.setValue(data.isUpvotedByUser());
                        saveState.setValue(data.isSavedByUser());
                    }
                } else error.setValue("Failed to load post");
            }
            @Override public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                isLoading.setValue(false);
                error.setValue(t.getMessage());
            }
        });
    }

    public void loadComments() {
        apiService.getComments(postId, null, 50).enqueue(new Callback<ApiResponse<List<Comment>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Comment>>> call, Response<ApiResponse<List<Comment>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    comments.setValue(response.body().getData());
                }
            }
            @Override public void onFailure(Call<ApiResponse<List<Comment>>> call, Throwable t) {}
        });
    }

    public void toggleUpvote() {
        apiService.toggleUpvote(postId).enqueue(new Callback<ApiResponse<UpvoteResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UpvoteResponse>> call, Response<ApiResponse<UpvoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpvoteResponse data = response.body().getData();
                    if (data != null) {
                        upvoteState.setValue(data.upvoted);
                        Post p = post.getValue();
                        if (p != null) {
                            p.setUpvotedByUser(data.upvoted);
                            p.setUpvoteCount(data.newCount);
                            post.setValue(p);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<ApiResponse<UpvoteResponse>> call, Throwable t) {}
        });
    }

    public void toggleSave() {
        apiService.toggleSave(postId).enqueue(new Callback<ApiResponse<SaveResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SaveResponse>> call, Response<ApiResponse<SaveResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SaveResponse data = response.body().getData();
                    if (data != null) {
                        saveState.setValue(data.saved);
                        Post p = post.getValue();
                        if (p != null) {
                            p.setSavedByUser(data.saved);
                            post.setValue(p);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<ApiResponse<SaveResponse>> call, Throwable t) {}
        });
    }

    public void postComment(String text, String parentId) {
        ApiService.CommentRequest req = new ApiService.CommentRequest(text, parentId);
        Call<ApiResponse<Comment>> call = (parentId != null)
                ? apiService.addReply(parentId, req)
                : apiService.addComment(postId, req);

        call.enqueue(new Callback<ApiResponse<Comment>>() {
            @Override
            public void onResponse(Call<ApiResponse<Comment>> c, Response<ApiResponse<Comment>> response) {
                if (response.isSuccessful()) loadComments();
            }
            @Override public void onFailure(Call<ApiResponse<Comment>> c, Throwable t) {}
        });
    }

    public void toggleCommentLike(String commentId) {
        apiService.toggleCommentLike(commentId).enqueue(new Callback<ApiResponse<LikeResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<LikeResponse>> call, Response<ApiResponse<LikeResponse>> response) {
                if (response.isSuccessful()) loadComments();
            }
            @Override public void onFailure(Call<ApiResponse<LikeResponse>> call, Throwable t) {}
        });
    }

    public static class Factory implements ViewModelProvider.Factory {
        private final Application application;
        private final String postId;

        public Factory(Application application, String postId) {
            this.application = application;
            this.postId = postId;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new PostDetailViewModel(application, postId);
        }
    }
}
