package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;

public class Post {

    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category; // HACKATHON, INTERNSHIP, SUMMER_COURSE

    @SerializedName("mode")
    private String mode; // ON_GROUND, VIRTUAL, WFH

    @SerializedName("universityName")
    private String universityName;

    @SerializedName("city")
    private String city;

    @SerializedName("state")
    private String state;

    @SerializedName("country")
    private String country;

    @SerializedName("venue")
    private String venue;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("distanceKm")
    private double distanceKm;

    @SerializedName("deadline")
    private String deadline;

    @SerializedName("applyLink")
    private String applyLink;

    @SerializedName("postedDate")
    private String postedDate;

    @SerializedName("upvoteCount")
    private int upvoteCount;

    @SerializedName("commentCount")
    private int commentCount;

    @SerializedName("isUpvotedByUser")
    private boolean isUpvotedByUser;

    @SerializedName("isSavedByUser")
    private boolean isSavedByUser;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("postedBy")
    private String postedBy;

    @SerializedName("trendingScore")
    private double trendingScore;

    // Default constructor
    public Post() {}

    // ---- Getters ----

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getMode() { return mode; }
    public String getUniversityName() { return universityName; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }
    public String getVenue() { return venue; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getDistanceKm() { return distanceKm; }
    public String getDeadline() { return deadline; }
    public String getApplyLink() { return applyLink; }
    public String getPostedDate() { return postedDate; }
    public int getUpvoteCount() { return upvoteCount; }
    public int getCommentCount() { return commentCount; }
    public boolean isUpvotedByUser() { return isUpvotedByUser; }
    public boolean isSavedByUser() { return isSavedByUser; }
    public String getImageUrl() { return imageUrl; }
    public String getPostedBy() { return postedBy; }
    public double getTrendingScore() { return trendingScore; }

    // ---- Setters ----

    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setMode(String mode) { this.mode = mode; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setCountry(String country) { this.country = country; }
    public void setVenue(String venue) { this.venue = venue; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public void setApplyLink(String applyLink) { this.applyLink = applyLink; }
    public void setPostedDate(String postedDate) { this.postedDate = postedDate; }
    public void setUpvoteCount(int upvoteCount) { this.upvoteCount = upvoteCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setUpvotedByUser(boolean upvotedByUser) { isUpvotedByUser = upvotedByUser; }
    public void setSavedByUser(boolean savedByUser) { isSavedByUser = savedByUser; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
    public void setTrendingScore(double trendingScore) { this.trendingScore = trendingScore; }

    // ---- Helpers ----

    public String getFormattedLocation() {
        return city + ", " + state + ", " + country;
    }

    public String getFormattedDistance() {
        if (distanceKm < 1) return "< 1 km away";
        if (distanceKm < 100) return String.format("%.0f km away", distanceKm);
        return String.format("%.0f km away", distanceKm);
    }

    public boolean isVirtual() {
        return "VIRTUAL".equals(mode) || "WFH".equals(mode);
    }
}
