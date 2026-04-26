package com.uniconnect.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("role")
    private String role;

    @SerializedName("universityName")
    private String universityName;

    public RegisterRequest(String name, String email, String password, String role, String universityName) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.universityName = universityName;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
    public String getUniversityName() { return universityName; }
}
