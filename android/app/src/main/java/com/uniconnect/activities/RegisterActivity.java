package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.uniconnect.R;
import com.uniconnect.models.ApiResponse;
import com.uniconnect.models.AuthResponse;
import com.uniconnect.models.RegisterRequest;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;
import com.uniconnect.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilUniversity;
    private TextInputEditText etName, etEmail, etPassword, etUniversity;
    private AutoCompleteTextView spinnerRole;
    private View btnRegister, tvLogin, loadingOverlay;

    private ApiService apiService;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        session = SessionManager.getInstance(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        initViews();
        setupSpinner();
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilUniversity = findViewById(R.id.tilUniversity);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUniversity = findViewById(R.id.etUniversity);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        btnRegister.setOnClickListener(v -> attemptRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void setupSpinner() {
        String[] roles = {"STUDENT", "ALUMNI"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, roles);
        spinnerRole.setAdapter(adapter);
    }

    private void attemptRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String university = etUniversity.getText().toString().trim();
        String role = spinnerRole.getText().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        RegisterRequest req = new RegisterRequest(name, email, password, role, university);
        apiService.register(req).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse data = response.body().getData();
                    if (data != null) {
                        session.saveToken(data.getToken());
                        if (data.getUser() != null) session.saveUser(data.getUser().toUser());
                        
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finishAffinity();
                    }
                } else {
                    Snackbar.make(btnRegister, "Registration failed", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                setLoading(false);
                Snackbar.make(btnRegister, "Error: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean l) {
        loadingOverlay.setVisibility(l ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!l);
    }
}
