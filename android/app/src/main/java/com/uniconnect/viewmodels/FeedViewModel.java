package com.uniconnect.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uniconnect.models.Post;
import com.uniconnect.repositories.PostRepository;
import com.uniconnect.utils.SessionManager;

import java.util.List;

public class FeedViewModel extends AndroidViewModel {

    private final PostRepository repository;
    private final SessionManager session;

    // Filter state (exposed for UI)
    private final MutableLiveData<String> sortBy = new MutableLiveData<>("LATEST");
    private final MutableLiveData<String> scope = new MutableLiveData<>("NEAR_ME");
    private final MutableLiveData<String> selectedCategory = new MutableLiveData<>(null);
    private final MutableLiveData<String> selectedMode = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> radiusKm = new MutableLiveData<>(50);
    private final MutableLiveData<String> cityFilter = new MutableLiveData<>(null);
    private final MutableLiveData<String> universityFilter = new MutableLiveData<>(null);
    private final MutableLiveData<String> keyword = new MutableLiveData<>(null);

    public FeedViewModel(@NonNull Application application) {
        super(application);
        repository = new PostRepository(application);
        session = SessionManager.getInstance(application);

        // Restore preferences
        sortBy.setValue(session.getSortPreference());
        scope.setValue(session.getScopePreference());
        radiusKm.setValue(session.getRadius());

        // Set saved location
        repository.setUserLocation(session.getUserLat(), session.getUserLng());
    }

    // ---- Exposed LiveData ----

    public LiveData<List<Post>> getPosts() { return repository.postsLiveData; }
    public LiveData<Boolean> isLoading() { return repository.loadingLiveData; }
    public LiveData<Boolean> isLoadingMore() { return repository.loadingMoreLiveData; }
    public LiveData<Boolean> isLastPage() { return repository.isLastPageLiveData; }
    public LiveData<String> getError() { return repository.errorLiveData; }
    public LiveData<String> getSortBy() { return sortBy; }
    public LiveData<String> getScope() { return scope; }

    // ---- Feed loading ----

    public void loadFeed() {
        syncFiltersToRepo();
        repository.loadFreshFeed();
    }

    public void loadMorePosts() {
        repository.loadMorePosts();
    }

    public void refresh() {
        loadFeed();
    }

    // ---- Filter updates ----

    public void setSortBy(String sort) {
        sortBy.setValue(sort);
        session.saveSortPreference(sort);
        loadFeed();
    }

    public void setScope(String newScope) {
        scope.setValue(newScope);
        session.saveScopePreference(newScope);
        loadFeed();
    }

    public void setCategory(String category) {
        selectedCategory.setValue(category);
        loadFeed();
    }

    public void setMode(String mode) {
        selectedMode.setValue(mode);
        loadFeed();
    }

    public void setRadius(int km) {
        radiusKm.setValue(km);
        session.saveRadius(km);
        loadFeed();
    }

    public void setCityFilter(String city) {
        cityFilter.setValue(city);
        loadFeed();
    }

    public void setUniversityFilter(String uni) {
        universityFilter.setValue(uni);
        loadFeed();
    }

    public void setKeyword(String kw) {
        keyword.setValue(kw);
        loadFeed();
    }

    public void clearFilters() {
        selectedCategory.setValue(null);
        selectedMode.setValue(null);
        cityFilter.setValue(null);
        universityFilter.setValue(null);
        keyword.setValue(null);
        loadFeed();
    }

    public void updateUserLocation(double lat, double lng) {
        repository.setUserLocation(lat, lng);
        loadFeed();
    }

    // ---- Upvote / Save ----

    public void toggleUpvote(String postId) {
        repository.toggleUpvote(postId, new PostRepository.UpvoteCallback() {
            @Override
            public void onResult(boolean upvoted, int newCount) {
                // LiveData auto-updates in repository
            }

            @Override
            public void onError(String message) {
                repository.errorLiveData.postValue(message);
            }
        });
    }

    public void toggleSave(String postId) {
        repository.toggleSave(postId, new PostRepository.SaveCallback() {
            @Override
            public void onResult(boolean saved) {
                // LiveData auto-updates
            }

            @Override
            public void onError(String message) {
                repository.errorLiveData.postValue(message);
            }
        });
    }

    // ---- Helpers ----

    private void syncFiltersToRepo() {
        repository.setFilters(
                valueOrDefault(sortBy.getValue(), "LATEST"),
                valueOrDefault(scope.getValue(), "NEAR_ME"),
                selectedCategory.getValue(),
                selectedMode.getValue(),
                radiusKm.getValue() != null ? radiusKm.getValue() : 50,
                cityFilter.getValue(),
                universityFilter.getValue(),
                keyword.getValue()
        );
    }

    private String valueOrDefault(String val, String def) {
        return val != null ? val : def;
    }

    public boolean isAdmin() {
        return session.isAdmin();
    }
}
