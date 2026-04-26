package com.uniconnect.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.uniconnect.R;

/**
 * Adds left/right swipe gestures to the feed RecyclerView.
 *
 * Swipe RIGHT → Save / Unsave (bookmark)
 * Swipe LEFT  → Quick upvote
 *
 * Per spec: must NOT conflict with vertical scrolling.
 * ItemTouchHelper handles the disambiguation automatically.
 */
public class GestureHelper extends ItemTouchHelper.SimpleCallback {

    public interface SwipeListener {
        void onSwipeRight(int position);   // Save
        void onSwipeLeft(int position);    // Upvote
    }

    private final SwipeListener listener;
    private final Paint         paint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int           colorSave;
    private final int           colorVote;
    private final int           iconSave;
    private final int           iconVote;
    private final Context       context;

    // Track which items are currently being swiped (prevent duplicate triggers)
    private final java.util.Set<Integer> pendingSwipes = new java.util.HashSet<>();

    public GestureHelper(Context context, SwipeListener listener) {
        // Only horizontal swipes; vertical scroll is handled by RecyclerView
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        this.listener = listener;
        this.context  = context;
        this.colorSave = ContextCompat.getColor(context, R.color.tertiary);
        this.colorVote = ContextCompat.getColor(context, R.color.upvote_active);
        this.iconSave  = R.drawable.ic_bookmark_filled;
        this.iconVote  = R.drawable.ic_upvote_filled;
    }

    // ── Attach to RecyclerView ────────────────────────────────

    public void attachTo(RecyclerView recyclerView) {
        new ItemTouchHelper(this).attachToRecyclerView(recyclerView);
    }

    // ── Swipe callback ────────────────────────────────────────

    @Override
    public boolean onMove(@NonNull RecyclerView rv,
                          @NonNull RecyclerView.ViewHolder vh,
                          @NonNull RecyclerView.ViewHolder target) {
        return false; // We don't support drag-and-drop
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int pos = viewHolder.getAdapterPosition();
        if (pos == RecyclerView.NO_ID) return;
        if (pendingSwipes.contains(pos)) return;
        pendingSwipes.add(pos);

        if (direction == ItemTouchHelper.RIGHT) {
            listener.onSwipeRight(pos);
        } else {
            listener.onSwipeLeft(pos);
        }

        // Reset item (we animate back, not remove)
        if (viewHolder.itemView.getParent() instanceof RecyclerView) {
            ((RecyclerView) viewHolder.itemView.getParent())
                    .getAdapter().notifyItemChanged(pos);
        }
        pendingSwipes.remove(pos);
    }

    // ── Draw swipe background ─────────────────────────────────

    @Override
    public void onChildDraw(@NonNull Canvas c,
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY,
                            int actionState, boolean isCurrentlyActive) {

        View item = viewHolder.itemView;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            float alpha = Math.min(1f, Math.abs(dX) / (item.getWidth() / 3f));
            item.setAlpha(0.7f + 0.3f * (1f - alpha));

            if (dX > 0) {
                // Swipe RIGHT → Save (green background)
                paint.setColor(colorSave);
                RectF bg = new RectF(item.getLeft(), item.getTop(),
                        item.getLeft() + dX, item.getBottom());
                c.drawRoundRect(bg, 16f, 16f, paint);
                drawIcon(c, item, iconSave, true, dX);
            } else if (dX < 0) {
                // Swipe LEFT → Upvote (orange background)
                paint.setColor(colorVote);
                RectF bg = new RectF(item.getRight() + dX, item.getTop(),
                        item.getRight(), item.getBottom());
                c.drawRoundRect(bg, 16f, 16f, paint);
                drawIcon(c, item, iconVote, false, dX);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    private void drawIcon(Canvas c, View item, int iconRes,
                          boolean isRight, float dX) {
        android.graphics.drawable.Drawable icon =
                ContextCompat.getDrawable(context, iconRes);
        if (icon == null) return;

        int iconSize   = dpToPx(24);
        int iconMargin = dpToPx(20);
        int itemTop    = item.getTop() + (item.getHeight() - iconSize) / 2;
        int iconLeft, iconRight;

        if (isRight) {
            iconLeft  = item.getLeft() + iconMargin;
            iconRight = item.getLeft() + iconMargin + iconSize;
        } else {
            iconLeft  = item.getRight() - iconMargin - iconSize;
            iconRight = item.getRight() - iconMargin;
        }

        icon.setBounds(iconLeft, itemTop, iconRight, itemTop + iconSize);
        icon.setTint(0xFFFFFFFF);
        icon.setAlpha((int) (255 * Math.min(1f, Math.abs(dX) / dpToPx(80))));
        icon.draw(c);
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return 0.35f; // 35% of item width to trigger swipe
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return defaultValue * 4; // Require more velocity to avoid accidental swipes
    }

    private int dpToPx(int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
