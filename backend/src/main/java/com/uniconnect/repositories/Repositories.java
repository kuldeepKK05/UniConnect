package com.uniconnect.repositories;

import com.uniconnect.models.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;

// ─────────────────────────────────────────────────────────────
// UserRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserEntity u WHERE u.role = 'UNIVERSITY' AND u.verified = false")
    List<UserEntity> findPendingUniversities();
}

// ─────────────────────────────────────────────────────────────
// PostRepository  — includes geo (Haversine) JPQL query
// ─────────────────────────────────────────────────────────────
@Repository
interface PostRepository extends JpaRepository<PostEntity, UUID> {

    Optional<PostEntity> findByIdAndActiveTrue(UUID id);

    /**
     * Geo-bounded query using Haversine formula inline.
     * Includes virtual posts globally (isVirtual flag not on entity; handled via mode).
     */
    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.active = true
          AND (
            (6371 * 2 * ASIN(SQRT(
                POWER(SIN((:userLat - p.latitude)  * PI() / 180 / 2), 2) +
                COS(:userLat * PI() / 180) * COS(p.latitude * PI() / 180) *
                POWER(SIN((:userLng - p.longitude) * PI() / 180 / 2), 2)
            ))) <= :radiusKm
            OR p.mode IN ('VIRTUAL', 'WFH')
          )
          AND (:category IS NULL OR CAST(p.category AS string) = :category)
          AND (:mode     IS NULL OR CAST(p.mode     AS string) = :mode)
          AND (:keyword  IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                                 OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:cursor   IS NULL OR p.createdAt < :cursor)
          AND (:cursorId IS NULL OR p.id <> :cursorId)
        """)
    List<PostEntity> findNearbyPosts(
            @Param("userLat")  double  userLat,
            @Param("userLng")  double  userLng,
            @Param("radiusKm") double  radiusKm,
            @Param("category") String  category,
            @Param("mode")     String  mode,
            @Param("keyword")  String  keyword,
            @Param("cursor")   Instant cursor,
            @Param("cursorId") UUID    cursorId,
            Pageable pageable);

    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.active = true
          AND (LOWER(p.city) = LOWER(:city) OR p.mode IN ('VIRTUAL', 'WFH'))
          AND (:category IS NULL OR CAST(p.category AS string) = :category)
          AND (:mode     IS NULL OR CAST(p.mode     AS string) = :mode)
          AND (:keyword  IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:cursor   IS NULL OR p.createdAt < :cursor)
        """)
    List<PostEntity> findByCityPosts(
            @Param("city")     String  city,
            @Param("category") String  category,
            @Param("mode")     String  mode,
            @Param("keyword")  String  keyword,
            @Param("cursor")   Instant cursor,
            Pageable pageable);

    @Query("""
        SELECT p FROM PostEntity p
        WHERE p.active = true
          AND (:category   IS NULL OR CAST(p.category AS string) = :category)
          AND (:mode       IS NULL OR CAST(p.mode     AS string) = :mode)
          AND (:keyword    IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:university IS NULL OR LOWER(p.universityName) LIKE LOWER(CONCAT('%', :university, '%')))
          AND (:cursor     IS NULL OR p.createdAt < :cursor)
          AND (:cursorId   IS NULL OR p.id <> :cursorId)
        """)
    List<PostEntity> findGlobalPosts(
            @Param("category")   String  category,
            @Param("mode")       String  mode,
            @Param("keyword")    String  keyword,
            @Param("university") String  university,
            @Param("cursor")     Instant cursor,
            @Param("cursorId")   UUID    cursorId,
            Pageable pageable);
}

// ─────────────────────────────────────────────────────────────
// PostVoteRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface PostVoteRepository extends JpaRepository<PostVoteEntity, UUID> {
    boolean existsByUserAndPost(UserEntity user, PostEntity post);
    void deleteByUserAndPost(UserEntity user, PostEntity post);

    @Query("SELECT v.post.id FROM PostVoteEntity v WHERE v.user.email = :email AND v.post.id IN :postIds")
    List<UUID> findVotedPostIdsByUserEmail(@Param("email") String email, @Param("postIds") List<UUID> postIds);
}

// ─────────────────────────────────────────────────────────────
// SavedPostRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface SavedPostRepository extends JpaRepository<SavedPostEntity, UUID> {
    boolean existsByUserAndPost(UserEntity user, PostEntity post);
    void deleteByUserAndPost(UserEntity user, PostEntity post);

    @Query("SELECT s.post.id FROM SavedPostEntity s WHERE s.user.email = :email AND s.post.id IN :postIds")
    List<UUID> findSavedPostIdsByUserEmail(@Param("email") String email, @Param("postIds") List<UUID> postIds);

    @Query("SELECT s.post FROM SavedPostEntity s WHERE s.user.email = :email ORDER BY s.savedAt DESC")
    List<PostEntity> findSavedPostsByUserEmail(@Param("email") String email, Pageable pageable);
}

// ─────────────────────────────────────────────────────────────
// CommentRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface CommentRepository extends JpaRepository<CommentEntity, UUID> {
    List<CommentEntity> findByPostIdAndParentIsNullAndActiveTrueOrderByCreatedAtAsc(UUID postId);
    long countByPostIdAndActiveTrue(UUID postId);
}

// ─────────────────────────────────────────────────────────────
// CommentLikeRepository
// ─────────────────────────────────────────────────────────────
@Repository
interface CommentLikeRepository extends JpaRepository<CommentLikeEntity, UUID> {
    boolean existsByUserAndComment(UserEntity user, CommentEntity comment);
    void deleteByUserAndComment(UserEntity user, CommentEntity comment);
}
