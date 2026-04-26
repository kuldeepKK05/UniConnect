package com.uniconnect.repositories;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.Post;
import com.uniconnect.models.FeedResponse;
import com.uniconnect.models.ApiResponses.*;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostRepository {

    private final ApiService apiService;

    public final MutableLiveData<List<Post>> postsLiveData = new MutableLiveData<>(new ArrayList<>());
    public final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> loadingMoreLiveData = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> isLastPageLiveData = new MutableLiveData<>(false);
    public final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private String currentCursor = null;
    private String sortBy, scope, category, mode, city, university, keyword;
    private int radiusKm = 50;
    private double userLat = 0, userLng = 0;

    public interface UpvoteCallback { void onResult(boolean upvoted, int newCount); void onError(String message); }
    public interface SaveCallback   { void onResult(boolean saved); void onError(String message); }

    public PostRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
    }

    public void setUserLocation(double lat, double lng) {
        this.userLat = lat;
        this.userLng = lng;
    }

    public void setFilters(String sortBy, String scope, String category, String mode, 
                           int radiusKm, String city, String university, String keyword) {
        this.sortBy = sortBy;
        this.scope = scope;
        this.category = category;
        this.mode = mode;
        this.radiusKm = radiusKm;
        this.city = city;
        this.university = university;
        this.keyword = keyword;
    }

    public void loadFreshFeed() {
        currentCursor = null;
        isLastPageLiveData.setValue(false);
        loadingLiveData.setValue(true);
        fetchFeed(false);
    }

    public void loadMorePosts() {
        if (Boolean.TRUE.equals(isLastPageLiveData.getValue()) || 
            Boolean.TRUE.equals(loadingMoreLiveData.getValue())) return;
        
        loadingMoreLiveData.setValue(true);
        fetchFeed(true);
    }

    private void fetchFeed(boolean isLoadMore) {
        apiService.getFeed(sortBy, scope, category, mode, radiusKm, city, university, keyword, 
                           userLat, userLng, currentCursor, 15)
                .enqueue(new Callback<ApiResponse<FeedResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<FeedResponse>> call, Response<ApiResponse<FeedResponse>> response) {
                        if (isLoadMore) loadingMoreLiveData.postValue(false);
                        else loadingLiveData.postValue(false);

                        if (response.isSuccessful() && response.body() != null) {
                            FeedResponse feed = response.body().getData();
                            if (feed != null) {
                                List<Post> newPosts = feed.getPosts();
                                List<Post> currentList = isLoadMore ? postsLiveData.getValue() : new ArrayList<>();
                                if (currentList == null) currentList = new ArrayList<>();
                                
                                if (newPosts != null) currentList.addAll(newPosts);
                                postsLiveData.postValue(currentList);
                                
                                currentCursor = feed.getNextCursor();
                                isLastPageLiveData.postValue(!feed.isHasMore());
                            }
                        } else {
                            errorLiveData.postValue("Failed to load feed");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<FeedResponse>> call, Throwable t) {
                        if (isLoadMore) loadingMoreLiveData.postValue(false);
                        else loadingLiveData.postValue(false);
                        errorLiveData.postValue(t.getMessage());
                    }
                });
    }

    public void toggleUpvote(String postId, UpvoteCallback callback) {
        apiService.toggleUpvote(postId).enqueue(new Callback<ApiResponse<UpvoteResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UpvoteResponse>> call, Response<ApiResponse<UpvoteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    UpvoteResponse data = response.body().getData();
                    if (data != null) {
                        updatePostLocally(postId, data.upvoted, data.newCount, null);
                        callback.onResult(data.upvoted, data.newCount);
                    }
                } else callback.onError("Error toggling upvote");
            }
            @Override public void onFailure(Call<ApiResponse<UpvoteResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void toggleSave(String postId, SaveCallback callback) {
        apiService.toggleSave(postId).enqueue(new Callback<ApiResponse<SaveResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SaveResponse>> call, Response<ApiResponse<SaveResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SaveResponse data = response.body().getData();
                    if (data != null) {
                        updatePostLocally(postId, null, null, data.saved);
                        callback.onResult(data.saved);
                    }
                } else callback.onError("Error toggling save");
            }
            @Override public void onFailure(Call<ApiResponse<SaveResponse>> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private void updatePostLocally(String postId, Boolean upvoted, Integer count, Boolean saved) {
        List<Post> list = postsLiveData.getValue();
        if (list == null) return;
        for (Post p : list) {
            if (p.getId().equals(postId)) {
                if (upvoted != null) p.setUpvotedByUser(upvoted);
                if (count != null) p.setUpvoteCount(count);
                if (saved != null) p.setSavedByUser(saved);
                postsLiveData.postValue(list);
                break;
            }
        }
    }
}
