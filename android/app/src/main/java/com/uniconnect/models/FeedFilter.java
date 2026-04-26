package com.uniconnect.models;

public class FeedFilter {
    private String sortBy;       // NEAREST, TRENDING, LATEST
    private String scope;        // NEAR_ME, SAME_CITY, NEARBY, GLOBAL
    private String category;     // HACKATHON, INTERNSHIP, SUMMER_COURSE
    private String mode;         // ON_GROUND, VIRTUAL, WFH
    private int radiusKm;        // 10, 50, 100, 150
    private String city;
    private String university;
    private String keyword;
    private double userLat;
    private double userLng;
    private String cursor;

    public FeedFilter() {
        this.sortBy = "LATEST";
        this.scope = "NEAR_ME";
        this.radiusKm = 50;
    }

    public String getSortBy() { return sortBy; }
    public String getScope() { return scope; }
    public String getCategory() { return category; }
    public String getMode() { return mode; }
    public int getRadiusKm() { return radiusKm; }
    public String getCity() { return city; }
    public String getUniversity() { return university; }
    public String getKeyword() { return keyword; }
    public double getUserLat() { return userLat; }
    public double getUserLng() { return userLng; }
    public String getCursor() { return cursor; }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setScope(String scope) { this.scope = scope; }
    public void setCategory(String category) { this.category = category; }
    public void setMode(String mode) { this.mode = mode; }
    public void setRadiusKm(int radiusKm) { this.radiusKm = radiusKm; }
    public void setCity(String city) { this.city = city; }
    public void setUniversity(String university) { this.university = university; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public void setUserLat(double userLat) { this.userLat = userLat; }
    public void setUserLng(double userLng) { this.userLng = userLng; }
    public void setCursor(String cursor) { this.cursor = cursor; }
}
