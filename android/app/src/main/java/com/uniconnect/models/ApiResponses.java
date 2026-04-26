package com.uniconnect.models;

import java.util.List;

public class ApiResponses {
    public static class UpvoteResponse { public boolean upvoted; public int newCount; }
    public static class SaveResponse   { public boolean saved; }
    public static class LikeResponse   { public boolean liked; public int newCount; }
    public static class City           { public String name, state, country; public double latitude, longitude; }
}
