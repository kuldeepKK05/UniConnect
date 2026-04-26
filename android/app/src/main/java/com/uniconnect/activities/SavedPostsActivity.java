package com.uniconnect.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.uniconnect.R;
import com.uniconnect.adapters.PostAdapter;
import com.uniconnect.models.Post;
import com.uniconnect.viewmodels.FeedViewModel;

public class SavedPostsActivity extends AppCompatActivity
        implements PostAdapter.OnPostInteractionListener {

    private FeedViewModel viewModel;
    private PostAdapter   adapter;
    private RecyclerView  recyclerView;
    private View          layoutEmpty, layoutLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_posts);

        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Saved Opportunities");
        }

        recyclerView  = findViewById(R.id.recyclerView);
        layoutEmpty   = findViewById(R.id.layoutEmpty);
        layoutLoading = findViewById(R.id.layoutLoading);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        adapter = new PostAdapter(this);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                int total = lm.getItemCount();
                int last  = lm.findLastVisibleItemPosition();
                if (last >= total - 4
                        && Boolean.FALSE.equals(viewModel.isLoadingMore().getValue())) {
                    viewModel.loadMorePosts();
                }
            }
        });

        viewModel.getPosts().observe(this, posts -> {
            adapter.setPosts(posts);
            layoutEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(posts.isEmpty() ? View.GONE   : View.VISIBLE);
        });

        viewModel.isLoading().observe(this, loading ->
                layoutLoading.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.isLoadingMore().observe(this, loading ->
                adapter.setLoadingMore(loading));

        viewModel.isLastPage().observe(this, last ->
                adapter.setLastPage(last));

        // Set scope to SAVED
        viewModel.setScope("SAVED");
        viewModel.loadFeed();
    }

    @Override public void onPostClicked(Post post) {
        Intent i = new Intent(this, PostDetailActivity.class);
        i.putExtra("postId", post.getId());
        startActivity(i);
    }

    @Override public void onUpvoteClicked(Post post, int position) {
        viewModel.toggleUpvote(post.getId());
    }

    @Override public void onSaveClicked(Post post, int position) {
        viewModel.toggleSave(post.getId());
        Snackbar.make(recyclerView, "Removed from saved", Snackbar.LENGTH_SHORT).show();
    }

    @Override public void onApplyClicked(Post post) {
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
}
