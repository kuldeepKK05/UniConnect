package com.uniconnect.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.AuthResponse;
import com.uniconnect.models.LoginRequest;
import com.uniconnect.models.RegisterRequest;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;
import com.uniconnect.utils.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends AndroidViewModel {

    private final ApiService apiService;
    private final SessionManager session;

    private final MutableLiveData<ApiResponse<AuthResponse>> authResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        apiService = RetrofitClient.getInstance(application).getApiService();
        session = SessionManager.getInstance(application);
    }

    public LiveData<ApiResponse<AuthResponse>> getAuthResult() { return authResult; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void login(String email, String password) {
        isLoading.setValue(true);
        apiService.login(new LoginRequest(email, password))
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                           Response<ApiResponse<AuthResponse>> resp) {
                        isLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) {
                            AuthResponse body = resp.body().getData();
                            if (body != null && body.getToken() != null) {
                                session.saveToken(body.getToken());
                                if (body.getUser() != null) {
                                    session.saveUser(body.getUser().toUser());
                                }
                            }
                        }
                        authResult.setValue(resp.body());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        isLoading.setValue(false);
                        authResult.setValue(null);
                    }
                });
    }

    public void register(RegisterRequest req) {
        isLoading.setValue(true);
        apiService.register(req)
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                           Response<ApiResponse<AuthResponse>> resp) {
                        isLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) {
                            AuthResponse body = resp.body().getData();
                            if (body != null && body.getToken() != null) {
                                session.saveToken(body.getToken());
                                if (body.getUser() != null) {
                                    session.saveUser(body.getUser().toUser());
                                }
                            }
                        }
                        authResult.setValue(resp.body());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        isLoading.setValue(false);
                        authResult.setValue(null);
                    }
                });
    }

    public void googleLogin(Map<String, String> body) {
        isLoading.setValue(true);
        apiService.googleLogin(body)
                .enqueue(new Callback<ApiResponse<AuthResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AuthResponse>> call,
                                           Response<ApiResponse<AuthResponse>> resp) {
                        isLoading.setValue(false);
                        if (resp.isSuccessful() && resp.body() != null) {
                            AuthResponse data = resp.body().getData();
                            if (data != null && data.getToken() != null) {
                                session.saveToken(data.getToken());
                                if (data.getUser() != null) {
                                    session.saveUser(data.getUser().toUser());
                                }
                            }
                        }
                        authResult.setValue(resp.body());
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                        isLoading.setValue(false);
                        authResult.setValue(null);
                    }
                });
    }
}
