package com.uniconnect.repositories;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.AuthResponse;
import com.uniconnect.models.User;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;
import com.uniconnect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {

    private final ApiService     apiService;
    private final SessionManager session;

    public final MutableLiveData<User>    userLiveData    = new MutableLiveData<>();
    public final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    public final MutableLiveData<String>  errorLiveData   = new MutableLiveData<>();
    public final MutableLiveData<Boolean> updateSuccess   = new MutableLiveData<>();

    public UserRepository(Context context) {
        apiService = RetrofitClient.getInstance(context).getApiService();
        session    = SessionManager.getInstance(context);
    }

    // ── Get current user profile from API ─────────────────────

    public void refreshCurrentUser() {
        loadingLiveData.setValue(true);
        apiService.getCurrentUser()
                .enqueue(new Callback<ApiResponse<AuthResponse.UserDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse.UserDto>> call,
                                           Response<ApiResponse<AuthResponse.UserDto>> response) {
                        loadingLiveData.postValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            User user = response.body().getData().toUser();
                            session.saveUser(user);
                            userLiveData.postValue(user);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse.UserDto>> call,
                                          Throwable t) {
                        loadingLiveData.postValue(false);
                        // Fall back to cached session user
                        User cached = session.getUser();
                        if (cached != null) userLiveData.postValue(cached);
                    }
                });
    }

    // ── Get user by ID ────────────────────────────────────────

    public void getUserById(String userId) {
        loadingLiveData.setValue(true);
        apiService.getUser(userId)
                .enqueue(new Callback<ApiResponse<AuthResponse.UserDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse.UserDto>> call,
                                           Response<ApiResponse<AuthResponse.UserDto>> response) {
                        loadingLiveData.postValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            userLiveData.postValue(response.body().getData().toUser());
                        } else {
                            errorLiveData.postValue("User not found");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse.UserDto>> call,
                                          Throwable t) {
                        loadingLiveData.postValue(false);
                        errorLiveData.postValue("Network error: " + t.getMessage());
                    }
                });
    }

    // ── Update profile ────────────────────────────────────────

    public void updateProfile(String userId, String name, String city,
                               String country, String avatarUrl) {
        loadingLiveData.setValue(true);

        ApiService.UpdateProfileRequest req = new ApiService.UpdateProfileRequest();
        req.name      = name;
        req.city      = city;
        req.country   = country;
        req.avatarUrl = avatarUrl;

        apiService.updateProfile(userId, req)
                .enqueue(new Callback<ApiResponse<AuthResponse.UserDto>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse.UserDto>> call,
                                           Response<ApiResponse<AuthResponse.UserDto>> response) {
                        loadingLiveData.postValue(false);
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            User updated = response.body().getData().toUser();
                            session.saveUser(updated);
                            userLiveData.postValue(updated);
                            updateSuccess.postValue(true);
                        } else {
                            errorLiveData.postValue("Failed to update profile");
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse.UserDto>> call,
                                          Throwable t) {
                        loadingLiveData.postValue(false);
                        errorLiveData.postValue("Network error: " + t.getMessage());
                    }
                });
    }

    // ── Return cached user immediately ────────────────────────

    public User getCachedUser() {
        return session.getUser();
    }
}
