package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.uniconnect.R;
import com.uniconnect.utils.SessionManager;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fullscreen splash
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        // Animate logo
        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView  tvName = findViewById(R.id.tvAppName);
        TextView  tvTag  = findViewById(R.id.tvTagline);

        animateEntry(ivLogo, 0);
        animateEntry(tvName, 200);
        animateEntry(tvTag,  400);

        SessionManager session = SessionManager.getInstance(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (session.isLoggedIn()) {
                intent = new Intent(this, MainActivity.class);
            } else {
                intent = new Intent(this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }, 1800);
    }

    private void animateEntry(android.view.View view, long delayMs) {
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(600);
        fade.setStartOffset(delayMs);
        fade.setFillAfter(true);

        ScaleAnimation scale = new ScaleAnimation(
                0.7f, 1f, 0.7f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);
        scale.setStartOffset(delayMs);
        scale.setFillAfter(true);

        AnimationSet set = new AnimationSet(false);
        set.addAnimation(fade);
        set.addAnimation(scale);

        view.startAnimation(set);
    }
}
