package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.dto.request.NewsRequest;
import com.sep490.backendclubmanagement.dto.response.ActivityDTO;
import com.sep490.backendclubmanagement.dto.response.LatestNewsDTO;
import com.sep490.backendclubmanagement.entity.club.Club;
import com.sep490.backendclubmanagement.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {

    // Lấy danh sách tin mới nhất, bỏ spotlight
    @Query("SELECT new com.sep490.backendclubmanagement.dto.response.LatestNewsDTO(" +
            "n.id, n.title, n.thumbnailUrl, SUBSTRING(n.content, 1, 150), n.createdAt) " +
            "FROM News n " +
            "WHERE n.isDraft = false AND n.isSpotlight = false " +
            "ORDER BY n.createdAt DESC")
    List<LatestNewsDTO> findLatestNews(Pageable pageable);

    Optional<News> findTopByIsDraftFalseOrderByCreatedAtDesc();
    Optional<News> findTopByIsSpotlightTrueOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE News n SET n.isSpotlight = false WHERE n.isSpotlight = true")
    int clearAllSpotlight();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE News n SET n.isSpotlight = true WHERE n.id = :id")
    int markSpotlight(@Param("id") Long id);

    // Lọc news (giữ nguyên bản native bạn đang dùng)
    @Query(value = """
            SELECT DISTINCT e.*
            FROM news e
            LEFT JOIN clubs c ON e.club_id = c.id
            WHERE (:#{#request.clubId} IS NULL OR e.club_id = :#{#request.clubId})
              AND e.is_draft = false
            """,
            nativeQuery = true, countProjection = "e.id")
    Page<News> getAllNewsByFilter(NewsRequest request, Pageable pageable);

    @Query("SELECT new com.sep490.backendclubmanagement.dto.response.ActivityDTO(" +
            "n.id, 'NEWS', n.title, u.fullName, n.createdAt) " +
            "FROM News n JOIN n.createdBy u " +
            "WHERE u.id IN :authorIds " +
            "ORDER BY n.createdAt DESC")
    List<ActivityDTO> findActivitiesByAuthorIds(@Param("authorIds") List<Long> authorIds);

    List<News> findByIsDraftFalseOrderByCreatedAtDesc();
    List<News> findByClubAndIsDraftFalseOrderByCreatedAtDesc(Club club);
    Page<News> findByIsDraftTrueAndCreatedBy_Id(Long userId, Pageable pageable);

    @Query("""
      SELECT n FROM News n
      WHERE n.createdBy.id = :authorId
        AND n.isDraft = true
      ORDER BY n.updatedAt DESC
    """)
    Page<News> findDraftsByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== STAFF: ẩn/xóa mềm/khôi phục =====
    @Query("select n from News n where n.id = :id")
    Optional<News> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update News n set n.hidden = :hidden where n.id = :id and n.deleted = false")
    int updateHidden(@Param("id") Long id, @Param("hidden") boolean hidden);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update News n
              set n.deleted = true,
                  n.deletedById = :staffId,
                  n.deletedAt = :now
            where n.id = :id and n.deleted = false
           """)
    int softDelete(@Param("id") Long id,
                   @Param("staffId") Long staffId,
                   @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update News n
              set n.deleted = false,
                  n.deletedById = null,
                  n.deletedAt = null
            where n.id = :id and n.deleted = true
           """)
    int restore(@Param("id") Long id);
    @EntityGraph(attributePaths = {"createdBy", "club"})
    @Query("""
        SELECT n
        FROM News n
        WHERE n.isDraft = true
          AND n.createdBy.id = :authorId
          AND (:clubId IS NULL OR n.club.id = :clubId)
        ORDER BY n.updatedAt DESC, n.id DESC
    """)
    Page<News> findDraftsVisibleToUser(@Param("authorId") Long authorId,
                                       @Param("clubId") Long clubId,
                                       Pageable pageable);

    /**
     * Lấy danh sách tin tức đã được publish của một câu lạc bộ với phân trang và tìm kiếm
     */
    @Query("SELECT n FROM News n " +
           "WHERE n.club.id = :clubId " +
           "AND n.isDraft = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.newsType) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.createdAt DESC")
    Page<News> findPublishedNewsByClubId(@Param("clubId") Long clubId, @Param("keyword") String keyword, Pageable pageable);
}


