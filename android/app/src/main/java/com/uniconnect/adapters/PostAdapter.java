package com.uniconnect.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.uniconnect.R;
import com.uniconnect.activities.PostDetailActivity;
import com.uniconnect.models.Post;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_LOADING = 1;

    private List<Post> posts = new ArrayList<>();
    private boolean isLoadingMore = false;
    private boolean isLastPage = false;

    private final OnPostInteractionListener listener;
    private int lastAnimatedPosition = -1;

    public interface OnPostInteractionListener {
        void onUpvoteClicked(Post post, int position);
        void onSaveClicked(Post post, int position);
        void onPostClicked(Post post);
        void onApplyClicked(Post post);
    }

    public PostAdapter(OnPostInteractionListener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        if (position == posts.size() && isLoadingMore) return TYPE_LOADING;
        return TYPE_POST;
    }

    @Override
    public long getItemId(int position) {
        if (getItemViewType(position) == TYPE_LOADING) return Long.MAX_VALUE;
        return posts.get(position).getId().hashCode();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_LOADING) {
            View view = inflater.inflate(R.layout.item_loading_footer, parent, false);
            return new LoadingViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_post_card, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof PostViewHolder) {
            ((PostViewHolder) holder).bind(posts.get(position));
            animateItem(holder.itemView, position);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size() + (isLoadingMore ? 1 : 0);
    }

    // ---- Data updates ----

    public void setPosts(List<Post> newPosts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PostDiffCallback(posts, newPosts));
        posts = new ArrayList<>(newPosts);
        diffResult.dispatchUpdatesTo(this);
    }

    public void setLoadingMore(boolean loading) {
        boolean was = isLoadingMore;
        isLoadingMore = loading;
        if (was && !loading) {
            notifyItemRemoved(posts.size());
        } else if (!was && loading) {
            notifyItemInserted(posts.size());
        }
    }

    public void setLastPage(boolean lastPage) {
        this.isLastPage = lastPage;
    }

    public void updatePostAt(int position, Post post) {
        if (position >= 0 && position < posts.size()) {
            posts.set(position, post);
            notifyItemChanged(position, "partial");
        }
    }

    // ---- Animation ----

    private void animateItem(View view, int position) {
        if (position > lastAnimatedPosition) {
            view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), R.anim.item_slide_in));
            lastAnimatedPosition = position;
        }
    }

    // ---- ViewHolder ----

    class PostViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardView;
        ImageView ivBanner;
        TextView tvTitle, tvUniversity, tvLocation, tvDistance;
        TextView tvDeadline, tvUpvotes, tvComments, tvPostedDate;
        Chip chipCategory, chipMode;
        ImageButton btnUpvote, btnSave, btnShare;
        View viewUpvoteIndicator;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvUniversity = itemView.findViewById(R.id.tvUniversity);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvDeadline = itemView.findViewById(R.id.tvDeadline);
            tvUpvotes = itemView.findViewById(R.id.tvUpvotes);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvPostedDate = itemView.findViewById(R.id.tvPostedDate);
            chipCategory = itemView.findViewById(R.id.chipCategory);
            chipMode = itemView.findViewById(R.id.chipMode);
            btnUpvote = itemView.findViewById(R.id.btnUpvote);
            btnSave = itemView.findViewById(R.id.btnSave);
            btnShare = itemView.findViewById(R.id.btnShare);
            viewUpvoteIndicator = itemView.findViewById(R.id.viewUpvoteIndicator);
        }

        void bind(Post post) {
            Context ctx = itemView.getContext();

            tvTitle.setText(post.getTitle());
            tvUniversity.setText(post.getUniversityName());
            tvLocation.setText(post.getFormattedLocation());
            tvDistance.setText(post.getFormattedDistance());
            tvUpvotes.setText(formatCount(post.getUpvoteCount()));
            tvComments.setText(formatCount(post.getCommentCount()));
            tvPostedDate.setText(formatDate(post.getPostedDate()));

            if (post.getDeadline() != null) {
                tvDeadline.setText("Deadline: " + post.getDeadline());
                tvDeadline.setVisibility(View.VISIBLE);
            } else {
                tvDeadline.setVisibility(View.GONE);
            }

            // Chips
            chipCategory.setText(formatCategory(post.getCategory()));
            chipCategory.setChipBackgroundColorResource(getCategoryColor(post.getCategory()));

            chipMode.setText(formatMode(post.getMode()));
            chipMode.setChipBackgroundColorResource(getModeColor(post.getMode()));

            // Upvote state
            updateUpvoteButton(post.isUpvotedByUser());

            // Save state
            btnSave.setImageResource(post.isSavedByUser()
                    ? R.drawable.ic_bookmark_filled
                    : R.drawable.ic_bookmark_outline);

            // Banner image
            if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
                Glide.with(ctx)
                        .load(post.getImageUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_banner)
                        .into(ivBanner);
                ivBanner.setVisibility(View.VISIBLE);
            } else {
                ivBanner.setVisibility(View.GONE);
            }

            // Clicks
            cardView.setOnClickListener(v -> {
                if (listener != null) listener.onPostClicked(post);
            });

            btnUpvote.setOnClickListener(v -> {
                animateButton(btnUpvote);
                if (listener != null) listener.onUpvoteClicked(post, getAdapterPosition());
            });

            btnSave.setOnClickListener(v -> {
                animateButton(btnSave);
                if (listener != null) listener.onSaveClicked(post, getAdapterPosition());
            });
        }

        private void updateUpvoteButton(boolean upvoted) {
            btnUpvote.setImageResource(upvoted
                    ? R.drawable.ic_upvote_filled
                    : R.drawable.ic_upvote_outline);
            if (viewUpvoteIndicator != null) {
                viewUpvoteIndicator.setVisibility(upvoted ? View.VISIBLE : View.INVISIBLE);
            }
        }

        private void animateButton(View btn) {
            btn.animate()
                    .scaleX(0.85f).scaleY(0.85f)
                    .setDuration(100)
                    .withEndAction(() -> btn.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
        }

        private String formatCount(int count) {
            if (count >= 1000) return String.format("%.1fk", count / 1000.0);
            return String.valueOf(count);
        }

        private String formatDate(String date) {
            if (date == null) return "";
            // Simplified — in prod use DateUtils
            return date.substring(0, Math.min(10, date.length()));
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

        private int getCategoryColor(String cat) {
            if ("HACKATHON".equals(cat)) return R.color.chip_hackathon;
            if ("INTERNSHIP".equals(cat)) return R.color.chip_internship;
            return R.color.chip_course;
        }

        private int getModeColor(String mode) {
            if ("VIRTUAL".equals(mode) || "WFH".equals(mode)) return R.color.chip_virtual;
            return R.color.chip_onground;
        }
    }

    // ---- Loading ViewHolder ----

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // ---- DiffUtil ----

    static class PostDiffCallback extends DiffUtil.Callback {
        private final List<Post> oldList;
        private final List<Post> newList;

        PostDiffCallback(List<Post> oldList, List<Post> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() { return oldList.size(); }

        @Override
        public int getNewListSize() { return newList.size(); }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Post o = oldList.get(oldPos);
            Post n = newList.get(newPos);
            return o.getUpvoteCount() == n.getUpvoteCount()
                    && o.isUpvotedByUser() == n.isUpvotedByUser()
                    && o.isSavedByUser() == n.isSavedByUser()
                    && o.getCommentCount() == n.getCommentCount();
        }
    }
}
