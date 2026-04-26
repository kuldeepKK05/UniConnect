package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("token")
    private String token;

    @SerializedName("refreshToken")
    private String refreshToken;

    @SerializedName("user")
    private UserDto user;

    @SerializedName("expiresIn")
    private long expiresIn;

    public String getToken() { return token; }
    public String getRefreshToken() { return refreshToken; }
    public UserDto getUser() { return user; }
    public long getExpiresIn() { return expiresIn; }

    public static class UserDto {
        public String id, email, name, role, universityName, avatarUrl, city, country;
        public boolean verified;
        public User toUser() {
            User u = new User();
            u.setId(id); u.setEmail(email); u.setName(name); u.setRole(role);
            u.setUniversityName(universityName); u.setAvatarUrl(avatarUrl);
            u.setVerified(verified); u.setCity(city); u.setCountry(country);
            return u;
        }
    }
}
