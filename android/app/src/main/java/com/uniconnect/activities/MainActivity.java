package com.uniconnect.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.uniconnect.R;
import com.uniconnect.adapters.PostAdapter;
import com.uniconnect.models.Post;
import com.uniconnect.utils.LocationHelper;
import com.uniconnect.utils.SessionManager;
import com.uniconnect.viewmodels.FeedViewModel;

public class MainActivity extends AppCompatActivity implements PostAdapter.OnPostInteractionListener {

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    private FeedViewModel viewModel;
    private PostAdapter adapter;
    private SessionManager session;
    private LocationHelper locationHelper;

    // Views
    private RecyclerView recyclerView;
    private View layoutLoading, layoutError, layoutEmpty;
    private ExtendedFloatingActionButton fabCreate;
    private ChipGroup chipGroupSort, chipGroupScope;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean granted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (granted) {
                    fetchAndSaveLocation();
                } else {
                    // Fall back to manual selection
                    if (!session.isLocationSet()) {
                        openLocationPicker();
                    } else {
                        viewModel.loadFeed();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        session = SessionManager.getInstance(this);
        locationHelper = new LocationHelper(this);
        viewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        initViews();
        setupRecyclerView();
        setupBottomNav();
        setupFilterChips();
        setupSwipeRefresh();
        setupFab();
        observeViewModel();
        requestLocationAndLoad();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutError = findViewById(R.id.layoutError);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        fabCreate = findViewById(R.id.fabCreate);
        chipGroupSort = findViewById(R.id.chipGroupSort);
        chipGroupScope = findViewById(R.id.chipGroupScope);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // Retry button
        View btnRetry = findViewById(R.id.btnRetry);
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> viewModel.refresh());
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        adapter = new PostAdapter(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemViewCacheSize(20);

        // Infinite scroll listener
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return; // Only load when scrolling down
                int totalItems = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= totalItems - 4 && !viewModel.isLoadingMore().getValue()) {
                    viewModel.loadMorePosts();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int state) {
                // Extend/shrink FAB based on scroll
                if (state == RecyclerView.SCROLL_STATE_IDLE) {
                    fabCreate.extend();
                } else {
                    fabCreate.shrink();
                }
            }
        });
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_feed) {
                // Already here
                return true;
            } else if (id == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (id == R.id.nav_saved) {
                startActivity(new Intent(this, SavedPostsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFilterChips() {
        // Sort chips
        chipGroupSort.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) viewModel.setSortBy(chip.getTag().toString());
        });

        // Scope chips
        chipGroupScope.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) viewModel.setScope(chip.getTag().toString());
        });

        // Filter button
        View btnFilter = findViewById(R.id.btnFilter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        }
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary, R.color.secondary);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refresh());
    }

    private void setupFab() {
        fabCreate.setVisibility(session.isAdmin() ? View.VISIBLE : View.GONE);
        fabCreate.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
    }

    private void observeViewModel() {
        viewModel.getPosts().observe(this, posts -> {
            adapter.setPosts(posts);
            layoutEmpty.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(posts.isEmpty() ? View.GONE : View.VISIBLE);
        });

        viewModel.isLoading().observe(this, loading -> {
            swipeRefresh.setRefreshing(loading);
            if (loading) {
                layoutLoading.setVisibility(View.VISIBLE);
                layoutError.setVisibility(View.GONE);
            } else {
                layoutLoading.setVisibility(View.GONE);
            }
        });

        viewModel.isLoadingMore().observe(this, loading -> {
            adapter.setLoadingMore(loading);
        });

        viewModel.isLastPage().observe(this, isLast -> {
            adapter.setLastPage(isLast);
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                swipeRefresh.setRefreshing(false);
                Snackbar.make(recyclerView, error, Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> viewModel.refresh())
                        .show();
            }
        });
    }

    private void requestLocationAndLoad() {
        if (session.isLocationSet()) {
            viewModel.loadFeed();
            return;
        }

        if (locationHelper.hasPermission()) {
            fetchAndSaveLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void fetchAndSaveLocation() {
        locationHelper.getCurrentLocation(new LocationHelper.LocationCallback2() {
            @Override
            public void onLocationReceived(double lat, double lng, String city, String country) {
                session.saveLocation(lat, lng, city);
                viewModel.updateUserLocation(lat, lng);
                Toast.makeText(MainActivity.this,
                        "Showing posts near " + city, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationFailed(String reason) {
                if (!session.isLocationSet()) {
                    openLocationPicker();
                } else {
                    viewModel.loadFeed();
                }
            }
        });
    }

    private void openLocationPicker() {
        startActivity(new Intent(this, LocationPickerActivity.class));
    }

    private void showFilterBottomSheet() {
        FilterBottomSheetFragment fragment = FilterBottomSheetFragment.newInstance();
        fragment.setOnFiltersApplied((category, mode, radius, city, university) -> {
            viewModel.setCategory(category);
            viewModel.setMode(mode);
            viewModel.setRadius(radius);
            viewModel.setCityFilter(city);
            viewModel.setUniversityFilter(university);
        });
        fragment.show(getSupportFragmentManager(), "filters");
    }

    // ---- PostAdapter.OnPostInteractionListener ----

    @Override
    public void onUpvoteClicked(Post post, int position) {
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        viewModel.toggleUpvote(post.getId());
    }

    @Override
    public void onSaveClicked(Post post, int position) {
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            return;
        }
        viewModel.toggleSave(post.getId());
    }

    @Override
    public void onPostClicked(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("postId", post.getId());
        startActivity(intent);
    }

    @Override
    public void onApplyClicked(Post post) {
        if (post.getApplyLink() != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse(post.getApplyLink()));
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationHelper.cleanup();
    }
}
