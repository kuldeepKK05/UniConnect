package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Comment {

    @SerializedName("id")
    private String id;

    @SerializedName("postId")
    private String postId;

    @SerializedName("parentId")
    private String parentId; // null if top-level

    @SerializedName("authorName")
    private String authorName;

    @SerializedName("authorAvatarUrl")
    private String authorAvatarUrl;

    @SerializedName("authorId")
    private String authorId;

    @SerializedName("content")
    private String content;

    @SerializedName("likeCount")
    private int likeCount;

    @SerializedName("isLikedByUser")
    private boolean isLikedByUser;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("replies")
    private List<Comment> replies;

    // UI state (not from API)
    private boolean isExpanded = true;
    private int depth = 0;

    public Comment() {}

    public String getId() { return id; }
    public String getPostId() { return postId; }
    public String getParentId() { return parentId; }
    public String getAuthorName() { return authorName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public String getAuthorId() { return authorId; }
    public String getContent() { return content; }
    public int getLikeCount() { return likeCount; }
    public boolean isLikedByUser() { return isLikedByUser; }
    public String getCreatedAt() { return createdAt; }
    public List<Comment> getReplies() { return replies; }
    public boolean isExpanded() { return isExpanded; }
    public int getDepth() { return depth; }

    public void setId(String id) { this.id = id; }
    public void setPostId(String postId) { this.postId = postId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    public void setContent(String content) { this.content = content; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setLikedByUser(boolean likedByUser) { isLikedByUser = likedByUser; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setReplies(List<Comment> replies) { this.replies = replies; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public void setDepth(int depth) { this.depth = depth; }

    public boolean isReply() { return parentId != null && !parentId.isEmpty(); }
    public boolean hasReplies() { return replies != null && !replies.isEmpty(); }
}
