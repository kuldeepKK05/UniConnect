package com.uniconnect.utils;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.uniconnect.R;
import com.uniconnect.activities.MainActivity;
import com.uniconnect.adapters.PostAdapter;
import com.uniconnect.models.Post;
import com.uniconnect.viewmodels.FeedViewModel;

import java.util.List;

/**
 * Wires GestureHelper swipe callbacks into MainActivity feed actions.
 *
 * Usage – call from MainActivity.setupRecyclerView():
 *
 *   SwipeGestureWiring.attach(this, recyclerView, adapter, viewModel);
 */
public final class SwipeGestureWiring {

    private SwipeGestureWiring() {}

    public static void attach(
            android.app.Activity activity,
            androidx.recyclerview.widget.RecyclerView recyclerView,
            PostAdapter adapter,
            FeedViewModel viewModel) {

        GestureHelper gesture = new GestureHelper(activity, new GestureHelper.SwipeListener() {

            @Override
            public void onSwipeRight(int position) {
                // Swipe right → Save / Unsave
                Post post = getPost(adapter, position);
                if (post == null) return;

                boolean wasSaved = post.isSavedByUser();
                viewModel.toggleSave(post.getId());

                Snackbar.make(recyclerView,
                        wasSaved ? "Removed from saved" : "Saved! ✓",
                        Snackbar.LENGTH_SHORT)
                        .setAction(wasSaved ? null : "View Saved", v ->
                                activity.startActivity(new android.content.Intent(
                                        activity,
                                        com.uniconnect.activities.SavedPostsActivity.class)))
                        .show();
            }

            @Override
            public void onSwipeLeft(int position) {
                // Swipe left → Upvote
                Post post = getPost(adapter, position);
                if (post == null) return;

                boolean wasVoted = post.isUpvotedByUser();
                viewModel.toggleUpvote(post.getId());

                Snackbar.make(recyclerView,
                        wasVoted ? "Upvote removed" : "Upvoted! 👍",
                        Snackbar.LENGTH_SHORT).show();
            }

            private Post getPost(PostAdapter a, int pos) {
                List<Post> posts = viewModel.getPosts().getValue();
                if (posts == null || pos < 0 || pos >= posts.size()) return null;
                return posts.get(pos);
            }
        });

        gesture.attachTo(recyclerView);
    }
}
