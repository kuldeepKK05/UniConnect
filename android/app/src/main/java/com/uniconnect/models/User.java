package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("name")
    private String name;

    @SerializedName("role")
    private String role; // STUDENT, ADMIN, UNIVERSITY

    @SerializedName("universityName")
    private String universityName;

    @SerializedName("avatarUrl")
    private String avatarUrl;

    @SerializedName("isVerified")
    private boolean isVerified;

    @SerializedName("city")
    private String city;

    @SerializedName("country")
    private String country;

    public User() {}

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public String getUniversityName() { return universityName; }
    public String getAvatarUrl() { return avatarUrl; }
    public boolean isVerified() { return isVerified; }
    public String getCity() { return city; }
    public String getCountry() { return country; }

    public void setId(String id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setRole(String role) { this.role = role; }
    public void setUniversityName(String universityName) { this.universityName = universityName; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setVerified(boolean verified) { isVerified = verified; }
    public void setCity(String city) { this.city = city; }
    public void setCountry(String country) { this.country = country; }

    public boolean isAdmin() { return "ADMIN".equals(role) || "UNIVERSITY".equals(role); }
    public boolean isStudent() { return "STUDENT".equals(role); }
}
