package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class FeedResponse {

    @SerializedName("posts")
    private List<Post> posts;

    @SerializedName("nextCursor")
    private String nextCursor;

    @SerializedName("hasMore")
    private boolean hasMore;

    @SerializedName("totalCount")
    private int totalCount;

    public List<Post> getPosts() { return posts; }
    public String getNextCursor() { return nextCursor; }
    public boolean isHasMore() { return hasMore; }
    public int getTotalCount() { return totalCount; }
}
