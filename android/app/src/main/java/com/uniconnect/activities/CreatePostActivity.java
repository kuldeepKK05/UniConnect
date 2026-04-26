package com.uniconnect.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.uniconnect.R;
import com.uniconnect.models.Post;
import com.uniconnect.models.ApiResponse;
import com.uniconnect.network.ApiService;
import com.uniconnect.network.RetrofitClient;
import com.uniconnect.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreatePostActivity extends AppCompatActivity {

    private TextInputLayout tilTitle, tilDescription, tilUniversity, tilVenue,
            tilApplyLink, tilCity, tilState, tilCountry;
    private TextInputEditText etTitle, etDescription, etUniversity, etVenue,
            etApplyLink, etCity, etState, etCountry, etDeadline;
    private AutoCompleteTextView spinnerCategory, spinnerMode;
    private MaterialButton btnPublish, btnPickDeadline;
    private View loadingOverlay;

    private ApiService apiService;
    private SessionManager session;
    private String selectedDeadline = null;
    private double selectedLat = 0, selectedLng = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        session = SessionManager.getInstance(this);
        apiService = RetrofitClient.getInstance(this).getApiService();

        // Only admins allowed
        if (!session.isAdmin()) { finish(); return; }

        initViews();
        setupDropdowns();
        prefillUniversity();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Opportunity");
        }

        tilTitle       = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        tilUniversity  = findViewById(R.id.tilUniversity);
        tilVenue       = findViewById(R.id.tilVenue);
        tilApplyLink   = findViewById(R.id.tilApplyLink);
        tilCity        = findViewById(R.id.tilCity);
        tilState       = findViewById(R.id.tilState);
        tilCountry     = findViewById(R.id.tilCountry);

        etTitle        = findViewById(R.id.etTitle);
        etDescription  = findViewById(R.id.etDescription);
        etUniversity   = findViewById(R.id.etUniversity);
        etVenue        = findViewById(R.id.etVenue);
        etApplyLink    = findViewById(R.id.etApplyLink);
        etCity         = findViewById(R.id.etCity);
        etState        = findViewById(R.id.etState);
        etCountry      = findViewById(R.id.etCountry);
        etDeadline     = findViewById(R.id.etDeadline);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerMode     = findViewById(R.id.spinnerMode);
        btnPublish      = findViewById(R.id.btnPublish);
        btnPickDeadline = findViewById(R.id.btnPickDeadline);
        loadingOverlay  = findViewById(R.id.loadingOverlay);

        btnPublish.setOnClickListener(v -> attemptPublish());
        btnPickDeadline.setOnClickListener(v -> showDatePicker());

        // Location picker button
        View btnPickLocation = findViewById(R.id.btnPickLocation);
        if (btnPickLocation != null) {
            btnPickLocation.setOnClickListener(v -> {
                // Use device location as shortcut
                if (session.isLocationSet()) {
                    etCity.setText(session.getUserCity());
                    selectedLat = session.getUserLat();
                    selectedLng = session.getUserLng();
                    Toast.makeText(this, "Using your current location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void setupDropdowns() {
        String[] categories = {"Hackathon", "Internship", "Summer Course"};
        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, categories));
        spinnerCategory.setText("Hackathon", false);

        String[] modes = {"On-Ground", "Virtual", "Work From Home"};
        spinnerMode.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, modes));
        spinnerMode.setText("On-Ground", false);
    }

    private void prefillUniversity() {
        com.uniconnect.models.User user = session.getUser();
        if (user != null && user.getUniversityName() != null) {
            etUniversity.setText(user.getUniversityName());
        }
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Deadline")
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            selectedDeadline = sdf.format(new Date(selection));
            etDeadline.setText(selectedDeadline);
        });
        picker.show(getSupportFragmentManager(), "deadline_picker");
    }

    private void attemptPublish() {
        if (!validateForm()) return;

        setLoading(true);

        Post post = new Post();
        post.setTitle(get(etTitle));
        post.setDescription(get(etDescription));
        post.setUniversityName(get(etUniversity));
        post.setVenue(get(etVenue));
        post.setApplyLink(get(etApplyLink));
        post.setCity(get(etCity));
        post.setState(get(etState));
        post.setCountry(get(etCountry));
        post.setDeadline(selectedDeadline);
        post.setLatitude(selectedLat);
        post.setLongitude(selectedLng);
        post.setCategory(mapCategory(spinnerCategory.getText().toString()));
        post.setMode(mapMode(spinnerMode.getText().toString()));

        apiService.createPost(post).enqueue(new Callback<ApiResponse<Post>>() {
            @Override
            public void onResponse(Call<ApiResponse<Post>> call,
                                   Response<ApiResponse<Post>> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(CreatePostActivity.this,
                            "Opportunity published!", Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 403) {
                    Snackbar.make(btnPublish, "Your account is not yet verified",
                            Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(btnPublish, "Failed to publish", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Post>> call, Throwable t) {
                setLoading(false);
                Snackbar.make(btnPublish, "Network error: " + t.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateForm() {
        clearErrors();
        boolean valid = true;
        if (TextUtils.isEmpty(get(etTitle)))       { tilTitle.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(get(etDescription))) { tilDescription.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(get(etCity)))        { tilCity.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(get(etCountry)))     { tilCountry.setError("Required"); valid = false; }
        if (TextUtils.isEmpty(get(etApplyLink)))   { tilApplyLink.setError("Required"); valid = false; }
        return valid;
    }

    private void clearErrors() {
        tilTitle.setError(null); tilDescription.setError(null);
        tilCity.setError(null);  tilCountry.setError(null);
        tilApplyLink.setError(null);
    }

    private void setLoading(boolean l) {
        loadingOverlay.setVisibility(l ? View.VISIBLE : View.GONE);
        btnPublish.setEnabled(!l);
    }

    private String get(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String mapCategory(String display) {
        switch (display) {
            case "Internship":    return "INTERNSHIP";
            case "Summer Course": return "SUMMER_COURSE";
            default:              return "HACKATHON";
        }
    }

    private String mapMode(String display) {
        switch (display) {
            case "Virtual":       return "VIRTUAL";
            case "Work From Home":return "WFH";
            default:              return "ON_GROUND";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
