package com.uniconnect.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "posts",
       indexes = {
           @Index(name = "idx_posts_author",   columnList = "author_id"),
           @Index(name = "idx_posts_category", columnList = "category"),
           @Index(name = "idx_posts_mode",     columnList = "mode"),
           @Index(name = "idx_posts_city",     columnList = "city"),
           @Index(name = "idx_posts_created",  columnList = "created_at"),
           @Index(name = "idx_posts_coords",   columnList = "latitude, longitude")
       })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode;

    @Column(nullable = false, length = 200)
    private String universityName;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(nullable = false, length = 100)
    private String country;

    @Column(length = 200)
    private String venue;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false, length = 500)
    private String applyLink;

    @Column
    private LocalDate deadline;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private int upvoteCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int commentCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private double trendingScore = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserEntity author;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public enum Category {
        HACKATHON, INTERNSHIP, SUMMER_COURSE
    }

    public enum Mode {
        ON_GROUND, VIRTUAL, WFH
    }

    public boolean isVirtual() {
        return mode == Mode.VIRTUAL || mode == Mode.WFH;
    }

    /**
     * Haversine distance in km from this post's location to a given point
     */
    public double distanceTo(double userLat, double userLng) {
        final double R = 6371.0;
        double dLat = Math.toRadians(userLat - this.latitude);
        double dLng = Math.toRadians(userLng - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(userLat))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
