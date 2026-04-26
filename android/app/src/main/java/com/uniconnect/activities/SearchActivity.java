package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.uniconnect.R;
import com.uniconnect.adapters.PostAdapter;
import com.uniconnect.models.Post;
import com.uniconnect.viewmodels.FeedViewModel;

public class SearchActivity extends AppCompatActivity
        implements PostAdapter.OnPostInteractionListener {

    private FeedViewModel viewModel;
    private PostAdapter   adapter;

    private TextInputEditText etSearch;
    private RecyclerView      recyclerView;
    private View              layoutEmpty, layoutLoading;
    private ChipGroup         chipGroupCategory;
    private TextView          tvResultCount;

    // Debounce search
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;
    private static final int DEBOUNCE_MS = 400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        initViews();
        setupRecyclerView();
        setupSearch();
        setupCategoryChips();
        observeViewModel();

        // Auto-focus search box
        etSearch.requestFocus();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        etSearch       = findViewById(R.id.etSearch);
        recyclerView   = findViewById(R.id.recyclerView);
        layoutEmpty    = findViewById(R.id.layoutEmpty);
        layoutLoading  = findViewById(R.id.layoutLoading);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        tvResultCount  = findViewById(R.id.tvResultCount);
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new PostAdapter(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        // Infinite scroll
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int total   = layoutManager.getItemCount();
                int visible = layoutManager.findLastVisibleItemPosition();
                if (visible >= total - 4
                        && Boolean.FALSE.equals(viewModel.isLoadingMore().getValue())) {
                    viewModel.loadMorePosts();
                }
            }
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
                debounceRunnable = () -> {
                    String q = s.toString().trim();
                    viewModel.setKeyword(q.isEmpty() ? null : q);
                };
                debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_MS);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Clear button
        View btnClear = findViewById(R.id.btnClear);
        if (btnClear != null) {
            btnClear.setOnClickListener(v -> etSearch.setText(""));
        }
    }

    private void setupCategoryChips() {
        if (chipGroupCategory == null) return;
        chipGroupCategory.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) {
                viewModel.setCategory(null);
                return;
            }
            Chip chip = group.findViewById(ids.get(0));
            if (chip != null) viewModel.setCategory(chip.getTag().toString());
        });
    }

    private void observeViewModel() {
        viewModel.getPosts().observe(this, posts -> {
            adapter.setPosts(posts);
            boolean empty = posts.isEmpty();
            layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);

            if (tvResultCount != null) {
                String q = etSearch.getText() != null
                        ? etSearch.getText().toString().trim() : "";
                if (!q.isEmpty()) {
                    tvResultCount.setText(posts.size() + " result" +
                            (posts.size() == 1 ? "" : "s") + " for \"" + q + "\"");
                    tvResultCount.setVisibility(View.VISIBLE);
                } else {
                    tvResultCount.setVisibility(View.GONE);
                }
            }
        });

        viewModel.isLoading().observe(this, loading -> {
            layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.isLoadingMore().observe(this, loading ->
                adapter.setLoadingMore(loading));

        viewModel.isLastPage().observe(this, last ->
                adapter.setLastPage(last));
    }

    // ---- PostAdapter callbacks ----

    @Override
    public void onPostClicked(Post post) {
        Intent i = new Intent(this, PostDetailActivity.class);
        i.putExtra("postId", post.getId());
        startActivity(i);
    }

    @Override
    public void onUpvoteClicked(Post post, int position) {
        viewModel.toggleUpvote(post.getId());
    }

    @Override
    public void onSaveClicked(Post post, int position) {
        viewModel.toggleSave(post.getId());
    }

    @Override
    public void onApplyClicked(Post post) {
        if (post.getApplyLink() != null) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse(post.getApplyLink())));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { onBackPressed(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (debounceRunnable != null) debounceHandler.removeCallbacks(debounceRunnable);
    }
}
