# UniConnect — Complete Production-Ready Platform

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Android App (Java/XML)                    │
│  SplashActivity → LoginActivity / RegisterActivity               │
│         └──► MainActivity (Feed + BottomNav)                     │
│                   ├── PostDetailActivity (Comments, Apply)       │
│                   ├── SearchActivity                             │
│                   ├── SavedPostsActivity                         │
│                   ├── CreatePostActivity (Admin only)            │
│                   └── ProfileActivity                            │
│                                                                  │
│  Architecture: MVVM                                              │
│  FeedViewModel ──► PostRepository ──► RetrofitClient ──► API    │
│  PostDetailViewModel ──► ApiService                              │
└─────────────────────────────────────────────────────────────────┘
                              │ HTTPS / JWT
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Backend (Java 17)                        │
│                                                                  │
│  Controllers: Auth, Feed, Post, User, Admin, Search, Geo         │
│  Services:    AuthService, FeedService, PostService,             │
│               CommentService                                     │
│  Security:    JwtUtils, JwtAuthFilter, SecurityConfig            │
│  Repos:       UserRepo, PostRepo, VoteRepo, SavedRepo,           │
│               CommentRepo                                        │
└─────────────────────────────────────────────────────────────────┘
          │                              │
┌─────────────────────┐   ┌─────────────────────────────────────┐
│  PostgreSQL          │   │  Cassandra                          │
│  (Source of Truth)   │   │  (Feed Optimization Layer)         │
│                      │   │                                     │
│  Tables:             │   │  Tables:                            │
│  • users             │   │  • posts_by_location                │
│  • posts             │   │  • posts_by_trending                │
│  • comments          │   │  • posts_by_user                    │
│  • post_votes        │   │  • posts_by_category                │
│  • comment_likes     │   │  • feed_cursor_state                │
│  • saved_posts       │   │                                     │
│  • eav_entities      │   │  Strategy: Write to PG, async       │
│  • eav_attributes    │   │  sync to Cassandra for fast reads   │
│  • eav_values        │   │                                     │
└─────────────────────┘   └─────────────────────────────────────┘
```

---

## File Structure

```
UniConnect/
├── android/
│   └── app/src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/uniconnect/
│       │   ├── UniConnectApp.java
│       │   ├── activities/
│       │   │   ├── SplashActivity.java
│       │   │   ├── LoginActivity.java
│       │   │   ├── RegisterActivity.java
│       │   │   ├── MainActivity.java          ← Feed + infinite scroll
│       │   │   ├── PostDetailActivity.java    ← Full post + comments
│       │   │   ├── CreatePostActivity.java    ← Admin only
│       │   │   ├── FilterBottomSheetFragment.java
│       │   │   └── LocationPickerActivity.java
│       │   ├── adapters/
│       │   │   ├── PostAdapter.java           ← RecyclerView + DiffUtil + pagination
│       │   │   └── CommentAdapter.java        ← Threaded comments
│       │   ├── models/
│       │   │   ├── Post.java
│       │   │   ├── User.java
│       │   │   ├── Comment.java
│       │   │   └── ApiModels.java
│       │   ├── viewmodels/
│       │   │   ├── FeedViewModel.java
│       │   │   └── PostDetailViewModel.java
│       │   ├── repositories/
│       │   │   └── PostRepository.java
│       │   ├── network/
│       │   │   ├── ApiService.java            ← All Retrofit endpoints
│       │   │   └── RetrofitClient.java        ← JWT interceptor
│       │   └── utils/
│       │       ├── SessionManager.java        ← SharedPreferences + JWT
│       │       └── LocationHelper.java        ← FusedLocationProviderClient
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_login.xml
│           │   ├── activity_register.xml
│           │   ├── activity_post_detail.xml
│           │   ├── activity_create_post.xml
│           │   ├── activity_splash.xml
│           │   ├── fragment_filter_sheet.xml
│           │   ├── item_post_card.xml
│           │   ├── item_comment.xml
│           │   ├── item_loading_footer.xml
│           │   └── item_shimmer_post.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── themes.xml
│           │   └── strings.xml
│           ├── menu/
│           │   └── menu_bottom_nav.xml
│           └── anim/
│               └── item_slide_in.xml
│
└── backend/
    ├── pom.xml
    └── src/main/
        ├── resources/
        │   ├── application.yml
        │   ├── schema.sql          ← PostgreSQL schema
        │   └── cassandra_schema.cql
        └── java/com/uniconnect/
            ├── UniConnectApplication.java
            ├── config/
            │   └── SecurityConfig.java
            ├── security/
            │   ├── JwtUtils.java
            │   └── JwtAuthFilter.java
            ├── models/
            │   ├── UserEntity.java
            │   ├── PostEntity.java
            │   └── SocialEntities.java  (Comment, Vote, Save, Like)
            ├── repositories/
            │   └── Repositories.java    (all Spring Data repos)
            ├── services/
            │   ├── AuthService.java
            │   ├── FeedService.java     ← Cursor pagination + geo
            │   ├── PostService.java     ← CRUD + trending score
            │   └── CommentService.java
            └── controllers/
                ├── ApiResponse.java     ← Envelope + GlobalExceptionHandler
                ├── AuthController.java
                ├── FeedController.java
                ├── PostController.java
                └── AdminAndUserControllers.java (Admin, User, Search, Geo)
```

---

## REST API Reference

### Base URL: `https://api.uniconnect.app/v1`

---

### AUTH

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/auth/login` | None | Email/password login → JWT |
| POST | `/auth/register` | None | Register student or university |
| POST | `/auth/google` | None | Google OAuth token → JWT |
| GET | `/auth/me` | JWT | Get current user profile |
| POST | `/auth/logout` | JWT | Logout (client discards JWT) |
| POST | `/auth/refresh` | None | Refresh access token |

#### POST /auth/login
```json
Request:  { "email": "user@example.com", "password": "securepass" }
Response: {
  "success": true,
  "data": {
    "token": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": 86400000,
    "user": {
      "id": "uuid", "email": "...", "name": "...",
      "role": "STUDENT", "verified": true
    }
  }
}
```

---

### FEED

#### GET /feed
```
Query params:
  sortBy     = LATEST | TRENDING | NEAREST  (default: LATEST)
  scope      = NEAR_ME | SAME_CITY | NEARBY | GLOBAL  (default: NEAR_ME)
  category   = HACKATHON | INTERNSHIP | SUMMER_COURSE
  mode       = ON_GROUND | VIRTUAL | WFH
  radiusKm   = 10 | 50 | 100 | 150  (default: 50)
  city       = "Mumbai"
  university = "IIT"
  keyword    = "machine learning"
  userLat    = 19.076
  userLng    = 72.877
  cursor     = "base64-encoded-cursor"
  limit      = 15  (max 50)

Response: {
  "success": true,
  "data": {
    "posts": [ ...PostDto ],
    "nextCursor": "base64...",
    "hasMore": true,
    "totalCount": 15
  }
}
```

---

### POSTS

#### GET /posts/:id
```json
Response: { "success": true, "data": { PostDto } }
```

#### POST /posts (Admin/University only)
```json
Request: {
  "title": "Summer Hackathon 2025",
  "description": "Build for the future...",
  "category": "HACKATHON",
  "mode": "ON_GROUND",
  "universityName": "IIT Bombay",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "venue": "IITB Main Campus",
  "latitude": 19.133,
  "longitude": 72.913,
  "applyLink": "https://hackathon.iitb.ac.in",
  "deadline": "2025-08-31",
  "imageUrl": "https://cdn.example.com/banner.jpg"
}
Response: { "success": true, "data": { PostDto } }
```

#### POST /posts/:id/upvote (Auth required)
```json
Response: { "success": true, "data": { "upvoted": true, "newCount": 143 } }
```

#### POST /posts/:id/save (Auth required)
```json
Response: { "success": true, "data": { "saved": true } }
```

---

### COMMENTS

#### GET /posts/:id/comments
```json
Response: {
  "success": true,
  "data": [
    {
      "id": "uuid",
      "authorName": "Alice",
      "content": "Great opportunity!",
      "likeCount": 5,
      "createdAt": "2025-01-10T10:00:00Z",
      "replies": [
        { "id": "uuid2", "authorName": "Bob", "content": "Agreed!", "depth": 1 }
      ]
    }
  ]
}
```

#### POST /posts/:id/comments (Auth required)
```json
Request:  { "content": "This looks amazing!", "parentId": null }
Response: { "success": true, "data": { CommentDto } }
```

---

### PostDto Structure
```json
{
  "id": "uuid",
  "title": "Summer Hackathon 2025",
  "description": "Full description...",
  "category": "HACKATHON",
  "mode": "ON_GROUND",
  "universityName": "IIT Bombay",
  "city": "Mumbai",
  "state": "Maharashtra",
  "country": "India",
  "venue": "IITB Main Campus",
  "latitude": 19.133,
  "longitude": 72.913,
  "distanceKm": 12.4,
  "deadline": "2025-08-31",
  "applyLink": "https://hackathon.iitb.ac.in",
  "postedDate": "2025-01-10T08:00:00Z",
  "upvoteCount": 142,
  "commentCount": 27,
  "isUpvotedByUser": false,
  "isSavedByUser": true,
  "imageUrl": "https://...",
  "postedBy": "Admin Name",
  "trendingScore": 8.47
}
```

---

## Infinite Scroll Implementation

```
RecyclerView scroll listener detects:
  lastVisible >= totalItems - 4 (buffer of 4 items)
     └──► viewModel.loadMorePosts()
            └──► repository.loadMorePosts()
                   └──► [guard: isLoading || isLastPage]
                          └──► apiService.getFeed(cursor=currentCursor, limit=15)
                                 └──► append posts to list
                                        └──► update cursor
                                               └──► check hasMore → isLastPage
```

---

## Region Logic

```
NEAR_ME   → Haversine query ≤ radius km + all VIRTUAL/WFH globally
SAME_CITY → city = user.city + all VIRTUAL/WFH
NEARBY    → Haversine query ≤ (radius * 3) km + all VIRTUAL/WFH
GLOBAL    → all posts regardless of location

Sort:
  LATEST   → ORDER BY created_at DESC
  TRENDING → ORDER BY trending_score DESC  (Wilson/Hacker News hybrid)
  NEAREST  → sorted post-query by distance in memory
```

---

## Trending Score Formula

```
score = upvoteCount / (ageHours + 2) ^ 1.8
```
Inspired by Hacker News gravity algorithm. Decays with time, boosted by upvotes.

---

## Setup Instructions

### Android
1. Open in Android Studio (Giraffe+)
2. Update `BASE_URL` in `RetrofitClient.java`
3. Add `google-services.json` for Firebase/Google Sign-In
4. Set `default_web_client_id` in `strings.xml`
5. Run on device (API 24+)

### Backend
1. Install PostgreSQL 15+ and Cassandra 4+
2. Run `schema.sql` against your PostgreSQL instance
3. Run `cassandra_schema.cql` against your Cassandra cluster
4. Set environment variables:
   ```
   DB_USER, DB_PASS, JWT_SECRET (256-bit Base64), GOOGLE_CLIENT_ID
   CASSANDRA_HOST, CASSANDRA_USER, CASSANDRA_PASS
   ```
5. `mvn spring-boot:run`

### Environment Variables
```bash
export DB_USER=uniconnect
export DB_PASS=yourpassword
export JWT_SECRET=<base64-256-bit-random>
export GOOGLE_CLIENT_ID=<from-google-console>
export CASSANDRA_HOST=localhost
```

---

## Security Checklist

- [x] HTTPS only (enforce in production with nginx/load balancer)
- [x] JWT with 24h expiry + refresh token
- [x] BCrypt password hashing (strength 12)
- [x] Role-based API protection (`@PreAuthorize`)
- [x] Input validation on all endpoints (`@Valid`)
- [x] CORS configured (restrict origins in production)
- [x] Soft deletes (no hard data removal)
- [x] SQL injection protection (JPA/prepared statements)
- [x] Domain-based university verification
- [x] Admin manual verification flow

---

## Performance Optimizations

### Android
- DiffUtil in PostAdapter (no full list redraws)
- setHasStableIds(true) on adapter
- setItemViewCacheSize(20) on RecyclerView
- Glide with cross-fade + centerCrop
- ExecutorService for background tasks
- Shimmer loading placeholders

### Backend
- PostgreSQL GIN indexes for full-text search
- Composite indexes on (city, created_at), (category, created_at)
- Cassandra feed layer for sub-millisecond feed reads
- Cursor pagination (no OFFSET — avoids full table scans)
- Connection pooling via HikariCP (pool size: 20)
- Trending score pre-computed on write (not on read)
