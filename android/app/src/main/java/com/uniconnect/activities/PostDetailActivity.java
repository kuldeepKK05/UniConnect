package com.uniconnect.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.uniconnect.R;
import com.uniconnect.adapters.CommentAdapter;
import com.uniconnect.models.Comment;
import com.uniconnect.models.Post;
import com.uniconnect.utils.SessionManager;
import com.uniconnect.viewmodels.PostDetailViewModel;

import android.widget.ImageView;

public class PostDetailActivity extends AppCompatActivity {

    private PostDetailViewModel viewModel;
    private CommentAdapter commentAdapter;
    private SessionManager session;

    private ImageView ivBanner;
    private TextView tvTitle, tvUniversity, tvLocation, tvVenue, tvDistance;
    private TextView tvDeadline, tvPostedDate, tvUpvotes, tvDescription;
    private Chip chipCategory, chipMode;
    private ImageButton btnUpvote, btnSave, btnShare;
    private MaterialButton btnApply;
    private RecyclerView rvComments;
    private TextInputEditText etComment;
    private View btnPostComment;
    private View layoutReplyingTo;
    private TextView tvReplyingTo;

    private String postId;
    private String replyToCommentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        postId = getIntent().getStringExtra("postId");
        if (postId == null) { finish(); return; }

        session = SessionManager.getInstance(this);
        viewModel = new ViewModelProvider(this,
                new PostDetailViewModel.Factory(getApplication(), postId))
                .get(PostDetailViewModel.class);

        initViews();
        setupToolbar();
        setupComments();
        observeViewModel();

        viewModel.loadPost();
        viewModel.loadComments();
    }

    private void initViews() {
        ivBanner = findViewById(R.id.ivBanner);
        tvTitle = findViewById(R.id.tvTitle);
        tvUniversity = findViewById(R.id.tvUniversity);
        tvLocation = findViewById(R.id.tvLocation);
        tvVenue = findViewById(R.id.tvVenue);
        tvDistance = findViewById(R.id.tvDistance);
        tvDeadline = findViewById(R.id.tvDeadline);
        tvPostedDate = findViewById(R.id.tvPostedDate);
        tvUpvotes = findViewById(R.id.tvUpvotes);
        tvDescription = findViewById(R.id.tvDescription);
        chipCategory = findViewById(R.id.chipCategory);
        chipMode = findViewById(R.id.chipMode);
        btnUpvote = findViewById(R.id.btnUpvote);
        btnSave = findViewById(R.id.btnSave);
        btnShare = findViewById(R.id.btnShare);
        btnApply = findViewById(R.id.btnApply);
        rvComments = findViewById(R.id.rvComments);
        etComment = findViewById(R.id.etComment);
        btnPostComment = findViewById(R.id.btnPostComment);
        layoutReplyingTo = findViewById(R.id.layoutReplyingTo);
        tvReplyingTo = findViewById(R.id.tvReplyingTo);

        btnUpvote.setOnClickListener(v -> {
            if (!session.isLoggedIn()) { goToLogin(); return; }
            animateButton(btnUpvote);
            viewModel.toggleUpvote();
        });

        btnSave.setOnClickListener(v -> {
            if (!session.isLoggedIn()) { goToLogin(); return; }
            animateButton(btnSave);
            viewModel.toggleSave();
        });

        btnShare.setOnClickListener(v -> sharePost());

        btnApply.setOnClickListener(v -> viewModel.getPost().observe(this, post -> {
            if (post != null && post.getApplyLink() != null) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(post.getApplyLink())));
            }
        }));

        btnPostComment.setOnClickListener(v -> postComment());

        // Cancel reply
        View btnCancelReply = findViewById(R.id.btnCancelReply);
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> cancelReply());
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupComments() {
        commentAdapter = new CommentAdapter(comment -> {
            // Reply callback
            replyToCommentId = comment.getId();
            tvReplyingTo.setText("Replying to " + comment.getAuthorName());
            if (layoutReplyingTo != null) layoutReplyingTo.setVisibility(View.VISIBLE);
            etComment.requestFocus();
        }, comment -> {
            // Like callback
            if (!session.isLoggedIn()) { goToLogin(); return; }
            viewModel.toggleCommentLike(comment.getId());
        });

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        rvComments.setAdapter(commentAdapter);
        rvComments.setNestedScrollingEnabled(false);
    }

    private void observeViewModel() {
        viewModel.getPost().observe(this, this::bindPost);

        viewModel.getComments().observe(this, comments -> {
            if (comments != null) commentAdapter.setComments(comments);
        });

        viewModel.isLoading().observe(this, loading -> {
            // show/hide shimmer
        });

        viewModel.getError().observe(this, error -> {
            if (error != null) {
                Snackbar.make(btnApply, error, Snackbar.LENGTH_LONG).show();
            }
        });

        viewModel.getUpvoteState().observe(this, upvoted -> {
            btnUpvote.setImageResource(upvoted
                    ? R.drawable.ic_upvote_filled
                    : R.drawable.ic_upvote_outline);
        });

        viewModel.getSaveState().observe(this, saved -> {
            btnSave.setImageResource(saved
                    ? R.drawable.ic_bookmark_filled
                    : R.drawable.ic_bookmark_outline);
        });
    }

    private void bindPost(Post post) {
        if (post == null) return;

        tvTitle.setText(post.getTitle());
        tvUniversity.setText(post.getUniversityName());
        tvLocation.setText(post.getFormattedLocation());
        tvDistance.setText(post.getFormattedDistance());
        tvDescription.setText(post.getDescription());
        tvUpvotes.setText(post.getUpvoteCount() + " upvotes");
        tvPostedDate.setText("Posted: " + post.getPostedDate());

        if (post.getVenue() != null) {
            tvVenue.setText(post.getVenue());
            tvVenue.setVisibility(View.VISIBLE);
        }

        if (post.getDeadline() != null) {
            tvDeadline.setText("Deadline: " + post.getDeadline());
            tvDeadline.setVisibility(View.VISIBLE);
        }

        chipCategory.setText(formatCategory(post.getCategory()));
        chipMode.setText(formatMode(post.getMode()));

        btnUpvote.setImageResource(post.isUpvotedByUser()
                ? R.drawable.ic_upvote_filled
                : R.drawable.ic_upvote_outline);
        btnSave.setImageResource(post.isSavedByUser()
                ? R.drawable.ic_bookmark_filled
                : R.drawable.ic_bookmark_outline);

        if (post.getImageUrl() != null) {
            ivBanner.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.getImageUrl())
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .into(ivBanner);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(post.getCategory());
        }
    }

    private void postComment() {
        if (!session.isLoggedIn()) { goToLogin(); return; }
        String text = etComment.getText() != null ? etComment.getText().toString().trim() : "";
        if (text.isEmpty()) { etComment.setError("Write something first"); return; }

        viewModel.postComment(text, replyToCommentId);
        etComment.setText("");
        cancelReply();
    }

    private void cancelReply() {
        replyToCommentId = null;
        if (layoutReplyingTo != null) layoutReplyingTo.setVisibility(View.GONE);
    }

    private void sharePost() {
        viewModel.getPost().observe(this, post -> {
            if (post == null) return;
            String text = post.getTitle() + "\nby " + post.getUniversityName()
                    + "\n\nDiscover on UniConnect";
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(share, "Share via"));
        });
    }

    private void animateButton(View btn) {
        btn.animate().scaleX(0.85f).scaleY(0.85f).setDuration(100)
                .withEndAction(() -> btn.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                .start();
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    private String formatCategory(String cat) {
        if (cat == null) return "";
        switch (cat) {
            case "HACKATHON": return "Hackathon";
            case "INTERNSHIP": return "Internship";
            case "SUMMER_COURSE": return "Summer Course";
            default: return cat;
        }
    }

    private String formatMode(String mode) {
        if (mode == null) return "";
        switch (mode) {
            case "ON_GROUND": return "On-Ground";
            case "VIRTUAL": return "Virtual";
            case "WFH": return "Work From Home";
            default: return mode;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
