package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uniconnect.R;
import com.uniconnect.models.User;
import com.uniconnect.utils.SessionManager;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        session = SessionManager.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Profile");
        }

        bindUser();
        setupActions();
    }

    private void bindUser() {
        User user = session.getUser();
        if (user == null) return;

        CircleImageView ivAvatar = findViewById(R.id.ivAvatar);
        TextView tvName          = findViewById(R.id.tvName);
        TextView tvEmail         = findViewById(R.id.tvEmail);
        TextView tvRole          = findViewById(R.id.tvRole);
        TextView tvUniversity    = findViewById(R.id.tvUniversity);
        TextView tvCity          = findViewById(R.id.tvCity);
        View     badgeVerified   = findViewById(R.id.badgeVerified);

        tvName.setText(user.getName());
        tvEmail.setText(user.getEmail());
        tvRole.setText(formatRole(user.getRole()));

        if (user.getUniversityName() != null && !user.getUniversityName().isEmpty()) {
            tvUniversity.setText(user.getUniversityName());
            tvUniversity.setVisibility(View.VISIBLE);
        }

        if (user.getCity() != null && !user.getCity().isEmpty()) {
            tvCity.setText(user.getCity());
            tvCity.setVisibility(View.VISIBLE);
        }

        if (badgeVerified != null) {
            badgeVerified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
        }

        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getAvatarUrl())
                    .placeholder(R.drawable.ic_default_avatar)
                    .circleCrop()
                    .into(ivAvatar);
        }
    }

    private void setupActions() {
        View btnSaved = findViewById(R.id.btnSaved);
        if (btnSaved != null) {
            btnSaved.setOnClickListener(v ->
                    startActivity(new Intent(this, SavedPostsActivity.class)));
        }

        View btnLocation = findViewById(R.id.btnChangeLocation);
        if (btnLocation != null) {
            btnLocation.setOnClickListener(v ->
                    startActivity(new Intent(this, LocationPickerActivity.class)));
        }

        // Difficulty spinner (per spec)
        android.widget.Spinner spinnerDifficulty = findViewById(R.id.spinnerDifficulty);
        if (spinnerDifficulty != null) {
            String[] opts = {"Easy", "Medium", "Hard", "Grand Master", "Magnus"};
            android.widget.ArrayAdapter<String> diffAdapter = new android.widget.ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_dropdown_item, opts);
            spinnerDifficulty.setAdapter(diffAdapter);

            // Restore saved preference
            String saved = session.getDifficulty();
            for (int i = 0; i < opts.length; i++) {
                if (opts[i].equals(saved)) { spinnerDifficulty.setSelection(i); break; }
            }

            spinnerDifficulty.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> p,
                                                   View v, int pos, long id) {
                            session.saveDifficulty(opts[pos]);
                        }
                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> p) {}
                    });
        }

        MaterialButton btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> confirmLogout());
        }
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out", (d, w) -> {
                    session.clearSession();
                    Intent i = new Intent(this, LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String formatRole(String role) {
        if (role == null) return "";
        switch (role) {
            case "ADMIN":      return "Administrator";
            case "UNIVERSITY": return "University Account";
            default:           return "Student";
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
