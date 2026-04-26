package com.uniconnect.cassandra;

import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────
// posts_by_location  (partition: country + city, cluster: created_at DESC)
// ─────────────────────────────────────────────────────────────
@Table("posts_by_location")
class PostByLocationEntity {

    @PrimaryKeyColumn(name = "country",    type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private String country;

    @PrimaryKeyColumn(name = "city",       type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private String city;

    @PrimaryKeyColumn(name = "created_at", type = PrimaryKeyType.CLUSTERED,
            ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING, ordinal = 2)
    private Instant createdAt;

    @PrimaryKeyColumn(name = "post_id",    type = PrimaryKeyType.CLUSTERED, ordinal = 3)
    private UUID postId;

    @Column private String title;
    @Column("university_name") private String universityName;
    @Column private String category;
    @Column private String mode;
    @Column private double latitude;
    @Column private double longitude;
    @Column("upvote_count")  private int    upvoteCount;
    @Column("comment_count") private int    commentCount;
    @Column("trending_score") private double trendingScore;
    @Column private LocalDate deadline;
    @Column("image_url") private String imageUrl;

    // Getters
    public String    getCountry()       { return country; }
    public String    getCity()          { return city; }
    public Instant   getCreatedAt()     { return createdAt; }
    public UUID      getPostId()        { return postId; }
    public String    getTitle()         { return title; }
    public String    getUniversityName(){ return universityName; }
    public String    getCategory()      { return category; }
    public String    getMode()          { return mode; }
    public double    getLatitude()      { return latitude; }
    public double    getLongitude()     { return longitude; }
    public int       getUpvoteCount()   { return upvoteCount; }
    public int       getCommentCount()  { return commentCount; }
    public double    getTrendingScore() { return trendingScore; }
    public LocalDate getDeadline()      { return deadline; }
    public String    getImageUrl()      { return imageUrl; }

    // Setters
    public void setCountry(String v)        { this.country = v; }
    public void setCity(String v)           { this.city = v; }
    public void setCreatedAt(Instant v)     { this.createdAt = v; }
    public void setPostId(UUID v)           { this.postId = v; }
    public void setTitle(String v)          { this.title = v; }
    public void setUniversityName(String v) { this.universityName = v; }
    public void setCategory(String v)       { this.category = v; }
    public void setMode(String v)           { this.mode = v; }
    public void setLatitude(double v)       { this.latitude = v; }
    public void setLongitude(double v)      { this.longitude = v; }
    public void setUpvoteCount(int v)       { this.upvoteCount = v; }
    public void setCommentCount(int v)      { this.commentCount = v; }
    public void setTrendingScore(double v)  { this.trendingScore = v; }
    public void setDeadline(LocalDate v)    { this.deadline = v; }
    public void setImageUrl(String v)       { this.imageUrl = v; }
}

// ─────────────────────────────────────────────────────────────
// posts_by_trending  (partition: date_bucket, cluster: trending_score DESC)
// ─────────────────────────────────────────────────────────────
@Table("posts_by_trending")
class PostByTrendingEntity {

    @PrimaryKeyColumn(name = "date_bucket",    type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private String dateBucket;

    @PrimaryKeyColumn(name = "trending_score", type = PrimaryKeyType.CLUSTERED,
            ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING, ordinal = 1)
    private double trendingScore;

    @PrimaryKeyColumn(name = "post_id",        type = PrimaryKeyType.CLUSTERED, ordinal = 2)
    private UUID postId;

    @Column private String title;
    @Column("university_name") private String universityName;
    @Column private String category;
    @Column private String mode;
    @Column private String city;
    @Column private String country;
    @Column private double latitude;
    @Column private double longitude;
    @Column("upvote_count")  private int    upvoteCount;
    @Column("comment_count") private int    commentCount;
    @Column("created_at")    private Instant createdAt;
    @Column private LocalDate deadline;
    @Column("image_url") private String imageUrl;

    public String   getDateBucket()     { return dateBucket; }
    public double   getTrendingScore()  { return trendingScore; }
    public UUID     getPostId()         { return postId; }
    public String   getTitle()          { return title; }
    public String   getUniversityName() { return universityName; }
    public String   getCategory()       { return category; }
    public String   getMode()           { return mode; }
    public String   getCity()           { return city; }
    public String   getCountry()        { return country; }
    public double   getLatitude()       { return latitude; }
    public double   getLongitude()      { return longitude; }
    public int      getUpvoteCount()    { return upvoteCount; }
    public int      getCommentCount()   { return commentCount; }
    public Instant  getCreatedAt()      { return createdAt; }
    public LocalDate getDeadline()      { return deadline; }
    public String   getImageUrl()       { return imageUrl; }

    public void setDateBucket(String v)     { this.dateBucket = v; }
    public void setTrendingScore(double v)  { this.trendingScore = v; }
    public void setPostId(UUID v)           { this.postId = v; }
    public void setTitle(String v)          { this.title = v; }
    public void setUniversityName(String v) { this.universityName = v; }
    public void setCategory(String v)       { this.category = v; }
    public void setMode(String v)           { this.mode = v; }
    public void setCity(String v)           { this.city = v; }
    public void setCountry(String v)        { this.country = v; }
    public void setLatitude(double v)       { this.latitude = v; }
    public void setLongitude(double v)      { this.longitude = v; }
    public void setUpvoteCount(int v)       { this.upvoteCount = v; }
    public void setCommentCount(int v)      { this.commentCount = v; }
    public void setCreatedAt(Instant v)     { this.createdAt = v; }
    public void setDeadline(LocalDate v)    { this.deadline = v; }
    public void setImageUrl(String v)       { this.imageUrl = v; }
}

// ─────────────────────────────────────────────────────────────
// posts_by_user  (partition: user_id + list_type, cluster: created_at DESC)
// ─────────────────────────────────────────────────────────────
@Table("posts_by_user")
class PostByUserEntity {

    @PrimaryKeyColumn(name = "user_id",   type = PrimaryKeyType.PARTITIONED, ordinal = 0)
    private UUID userId;

    @PrimaryKeyColumn(name = "list_type", type = PrimaryKeyType.PARTITIONED, ordinal = 1)
    private String listType;  // SAVED | POSTED

    @PrimaryKeyColumn(name = "created_at", type = PrimaryKeyType.CLUSTERED,
            ordering = org.springframework.data.cassandra.core.cql.Ordering.DESCENDING, ordinal = 2)
    private Instant createdAt;

    @PrimaryKeyColumn(name = "post_id",   type = PrimaryKeyType.CLUSTERED, ordinal = 3)
    private UUID postId;

    @Column private String title;
    @Column("university_name") private String universityName;
    @Column private String category;
    @Column private String mode;
    @Column private String city;
    @Column private String country;
    @Column("image_url") private String imageUrl;

    public UUID    getUserId()         { return userId; }
    public String  getListType()       { return listType; }
    public Instant getCreatedAt()      { return createdAt; }
    public UUID    getPostId()         { return postId; }
    public String  getTitle()          { return title; }
    public String  getUniversityName() { return universityName; }
    public String  getCategory()       { return category; }
    public String  getMode()           { return mode; }
    public String  getCity()           { return city; }
    public String  getCountry()        { return country; }
    public String  getImageUrl()       { return imageUrl; }

    public void setUserId(UUID v)          { this.userId = v; }
    public void setListType(String v)      { this.listType = v; }
    public void setCreatedAt(Instant v)    { this.createdAt = v; }
    public void setPostId(UUID v)          { this.postId = v; }
    public void setTitle(String v)         { this.title = v; }
    public void setUniversityName(String v){ this.universityName = v; }
    public void setCategory(String v)      { this.category = v; }
    public void setMode(String v)          { this.mode = v; }
    public void setCity(String v)          { this.city = v; }
    public void setCountry(String v)       { this.country = v; }
    public void setImageUrl(String v)      { this.imageUrl = v; }
}
