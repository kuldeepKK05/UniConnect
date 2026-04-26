package com.uniconnect.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users",
       indexes = {
           @Index(name = "idx_users_email", columnList = "email", unique = true),
           @Index(name = "idx_users_role",  columnList = "role")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 200)
    private String universityName;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String country;

    @Column
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum Role {
        STUDENT, UNIVERSITY, ADMIN
    }

    public boolean isAdmin() {
        return role == Role.ADMIN || role == Role.UNIVERSITY;
    }

    /**
     * Auto-verify based on email domain
     */
    public static boolean isDomainVerified(String email, java.util.List<String> verifiedDomains) {
        if (email == null) return false;
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return verifiedDomains.contains(domain);
    }
}
