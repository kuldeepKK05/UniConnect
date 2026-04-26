package com.uniconnect.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.uniconnect.R;
import com.uniconnect.models.Comment;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnReplyClickListener { void onReply(Comment comment); }
    public interface OnLikeClickListener  { void onLike(Comment comment);  }

    private List<Comment> flatList = new ArrayList<>();
    private final OnReplyClickListener replyListener;
    private final OnLikeClickListener  likeListener;

    public CommentAdapter(OnReplyClickListener replyListener,
                          OnLikeClickListener  likeListener) {
        this.replyListener = replyListener;
        this.likeListener  = likeListener;
    }

    /** Flatten threaded comments into a depth-annotated list for the adapter */
    public void setComments(List<Comment> rootComments) {
        flatList = new ArrayList<>();
        flatten(rootComments, 0);
        notifyDataSetChanged();
    }

    private void flatten(List<Comment> comments, int depth) {
        if (comments == null) return;
        for (Comment c : comments) {
            c.setDepth(depth);
            flatList.add(c);
            if (c.isExpanded() && c.hasReplies()) {
                flatten(c.getReplies(), depth + 1);
            }
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder h, int position) {
        h.bind(flatList.get(position));
    }

    @Override
    public int getItemCount() { return flatList.size(); }

    // ──────────────────────────────────────────

    class CommentViewHolder extends RecyclerView.ViewHolder {

        CircleImageView ivAvatar;
        TextView tvAuthor, tvContent, tvTime, tvLikes, tvReply;
        ImageButton btnLike;
        View indentBar;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar  = itemView.findViewById(R.id.ivAvatar);
            tvAuthor  = itemView.findViewById(R.id.tvAuthor);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvTime    = itemView.findViewById(R.id.tvTime);
            tvLikes   = itemView.findViewById(R.id.tvLikes);
            tvReply   = itemView.findViewById(R.id.tvReply);
            btnLike   = itemView.findViewById(R.id.btnLike);
            indentBar = itemView.findViewById(R.id.indentBar);
        }

        void bind(Comment c) {
            tvAuthor.setText(c.getAuthorName());
            tvContent.setText(c.getContent());
            tvTime.setText(c.getCreatedAt());
            tvLikes.setText(c.getLikeCount() > 0 ? String.valueOf(c.getLikeCount()) : "");

            btnLike.setImageResource(c.isLikedByUser()
                    ? R.drawable.ic_heart_filled
                    : R.drawable.ic_heart_outline);

            // Visual indent for replies
            int indentDp = c.getDepth() * 20;
            int indentPx = (int)(indentDp * itemView.getContext()
                    .getResources().getDisplayMetrics().density);
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
            params.leftMargin = indentPx;
            itemView.setLayoutParams(params);

            // Show indent bar only for replies
            if (indentBar != null) {
                indentBar.setVisibility(c.getDepth() > 0 ? View.VISIBLE : View.GONE);
            }

            // Avatar
            if (c.getAuthorAvatarUrl() != null && !c.getAuthorAvatarUrl().isEmpty()) {
                Glide.with(itemView).load(c.getAuthorAvatarUrl())
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop().into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.ic_default_avatar);
            }

            btnLike.setOnClickListener(v -> { if (likeListener != null) likeListener.onLike(c); });
            tvReply.setOnClickListener(v -> { if (replyListener != null) replyListener.onReply(c); });
        }
    }
}
