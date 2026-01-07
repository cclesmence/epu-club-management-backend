package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.club.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicClubRepository extends JpaRepository<Club, Long> {

    /* ===================== Projections ===================== */

    interface ClubCardRow {
        Long getId();
        String getClub_code();
        String getClub_name();
        String getShort_description();
        String getBanner_url();
        String getLogo_url();
        Boolean getIs_featured();
        LocalDateTime getCreated_at();
        String getCategory_name();
        String getCampus_name();
        Long getTotal_teams();
        String getTop2_names();
        Long getActive_recruitment_id();
    }

    interface ClubDetailRow {
        Long getId();
        String getClub_code();
        String getClub_name();
        String getDescription();
        String getBanner_url();
        String getLogo_url();
        String getEmail();
        String getPhone();
        String getFb_url();
        String getIg_url();
        String getTt_url();
        String getYt_url();
        String getCampus_name();
        String getCategory_name();
        LocalDateTime getCreated_at();
        LocalDateTime getUpdated_at();
        Boolean getIs_featured();
        Long getDepartments_count();
    }

    /** Team basic (không yêu cầu leader/count) */
    interface TeamRowBasic {
        Long getId();
        String getTeam_name();
        String getDescription();
        String getLink_group_chat();
    }

    /** Team kèm leader_name & member_count (public) */
    interface TeamWithLeaderRow {
        Long getId();
        String getTeam_name();
        String getDescription();
        String getLink_group_chat();
        String getLeader_name();
        Integer getMember_count();
    }

    interface EventRow {
        Long getId();
        String getTitle();
        String getDescription();
        LocalDateTime getStart_time();
        LocalDateTime getEnd_time();
        String getLocation();
    }

    interface NewsRow {
        Long getId();
        String getTitle();
        String getThumbnail_url();
        String getExcerpt();
        LocalDateTime getPublished_at();
        Boolean getIs_spotlight();
    }

    /* ===================== Queries ===================== */

    // 1) LIST clubs (homepage cards)
    @Query(value = """
        SELECT
          c.id,
          c.club_code,
          c.club_name,
          CASE
            WHEN c.description IS NULL THEN NULL
            WHEN CHAR_LENGTH(c.description) <= 160 THEN c.description
            ELSE CONCAT(SUBSTRING(c.description, 1, 157), '...')
          END AS short_description,
          c.banner_url,
          c.logo_url,
          c.is_featured,
          c.created_at,
          cat.category_name,
          cam.campus_name,
          COALESCE(t_tot.total_teams, 0) AS total_teams,
          COALESCE(t2.top2_names, '') AS top2_names,
          r.id AS active_recruitment_id
        FROM clubs c
        LEFT JOIN club_categories cat 
          ON cat.id = c.club_category_id AND cat.deleted_at IS NULL
        LEFT JOIN campuses cam 
          ON cam.id = c.campus_id AND cam.deleted_at IS NULL
        LEFT JOIN (
          SELECT club_id, COUNT(*) AS total_teams
          FROM teams
          WHERE deleted_at IS NULL
          GROUP BY club_id
        ) t_tot ON t_tot.club_id = c.id
        LEFT JOIN (
          SELECT
            t.club_id,
            SUBSTRING_INDEX(
              GROUP_CONCAT(t.team_name ORDER BY t.team_name SEPARATOR ','),
              ',', 2
            ) AS top2_names
          FROM teams t
          WHERE t.deleted_at IS NULL
          GROUP BY t.club_id
        ) t2 ON t2.club_id = c.id
        LEFT JOIN (
          SELECT club_id, MIN(id) AS id
          FROM recruitments
          WHERE deleted_at IS NULL
            AND status = 'OPEN'
          GROUP BY club_id
        ) r ON r.club_id = c.id
        WHERE c.deleted_at IS NULL
          AND c.status = 'ACTIVE'
          AND (:q IS NULL OR
               LOWER(c.club_name) LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(c.club_code) LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(COALESCE(c.description,'')) LIKE CONCAT('%', LOWER(:q), '%'))
          AND (:campusId IS NULL OR c.campus_id = :campusId)
          AND (:categoryId IS NULL OR c.club_category_id = :categoryId)
        ORDER BY c.is_featured DESC, c.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(1)
        FROM clubs c
        WHERE c.deleted_at IS NULL
          AND c.status = 'ACTIVE'
          AND (:q IS NULL OR
               LOWER(c.club_name) LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(c.club_code) LIKE CONCAT('%', LOWER(:q), '%')
            OR LOWER(COALESCE(c.description,'')) LIKE CONCAT('%', LOWER(:q), '%'))
          AND (:campusId IS NULL OR c.campus_id = :campusId)
          AND (:categoryId IS NULL OR c.club_category_id = :categoryId)
        """,
            nativeQuery = true)
    Page<ClubCardRow> findPublicClubs(
            @Param("q") String q,
            @Param("campusId") Long campusId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    // 2) DETAIL 1 club theo id
    @Query(value = """
        SELECT
          c.id,
          c.club_code,
          c.club_name,
          c.description,
          c.banner_url,
          c.logo_url,
          c.email,
          c.phone,
          c.fb_url,
          c.ig_url,
          c.tt_url,
          c.yt_url,
          cam.campus_name,
          cat.category_name,
          c.created_at,
          c.updated_at,
          c.is_featured,
          COALESCE(t_tot.total_teams, 0) AS departments_count
        FROM clubs c
        LEFT JOIN club_categories cat
          ON cat.id = c.club_category_id AND cat.deleted_at IS NULL
        LEFT JOIN campuses cam
          ON cam.id = c.campus_id AND cam.deleted_at IS NULL
        LEFT JOIN (
          SELECT club_id, COUNT(*) AS total_teams
          FROM teams
          WHERE deleted_at IS NULL
          GROUP BY club_id
        ) t_tot ON t_tot.club_id = c.id
        WHERE c.deleted_at IS NULL
          AND c.status = 'ACTIVE'
          AND c.id = :clubId
        LIMIT 1
        """, nativeQuery = true)
    Optional<ClubDetailRow> findPublicClubDetail(@Param("clubId") Long clubId);

    // 3a) TEAMS basic (nếu chỉ cần tên/desc/link)
    @Query(value = """
        SELECT id, team_name, description, link_group_chat
        FROM teams
        WHERE deleted_at IS NULL AND club_id = :clubId
        ORDER BY team_name ASC
        """, nativeQuery = true)
    List<TeamRowBasic> findPublicTeams(@Param("clubId") Long clubId);

    // 3b) TEAMS kèm leader_name & member_count (semester hiện hành)
    @Query(value = """
        SELECT
          t.id AS id,
          t.team_name AS team_name,
          t.description AS description,
          t.link_group_chat AS link_group_chat,
          GROUP_CONCAT(
            DISTINCT IF(
              cr.role_code IN ('TEAM_LEAD','TEAM_LEADER') OR cr.role_name LIKE '%Trưởng ban%',
              u.full_name, NULL
            ) ORDER BY u.full_name SEPARATOR ', '
          ) AS leader_name,
          COUNT(DISTINCT cm.id) AS member_count
        FROM teams t
        LEFT JOIN role_memberships rm
               ON rm.team_id = t.id
        LEFT JOIN club_memberships cm
               ON cm.id = rm.club_membership_id
              AND cm.deleted_at IS NULL
              AND cm.status = 'ACTIVE'
        LEFT JOIN users u
               ON u.id = cm.user_id
        LEFT JOIN club_roles cr
               ON cr.id = rm.clubrole_id
        LEFT JOIN semesters s
               ON s.id = rm.semester_id
        WHERE t.deleted_at IS NULL
          AND t.club_id = :clubId
          AND s.id IS NOT NULL
          AND NOW() BETWEEN s.start_date AND s.end_date
        GROUP BY t.id, t.team_name, t.description, t.link_group_chat
        ORDER BY t.team_name
        """, nativeQuery = true)
    List<TeamWithLeaderRow> findPublicTeamsWithLeaderAndCount(@Param("clubId") Long clubId);

    // 4) EVENTS upcoming (tab)
    @Query(value = """
        SELECT id, title, description, start_time, end_time, location
        FROM events
        WHERE deleted_at IS NULL
          AND is_draft = 0
          AND club_id = :clubId
          AND start_time >= CURRENT_TIMESTAMP
        ORDER BY start_time ASC
        """, nativeQuery = true)
    List<EventRow> findUpcomingEvents(@Param("clubId") Long clubId, Pageable pageable);

    // 5) NEWS latest (tab)
    @Query(value = """
        SELECT
          id,
          title,
          thumbnail_url,
          CASE
            WHEN content IS NULL THEN NULL
            ELSE
              CASE
                WHEN CHAR_LENGTH(REGEXP_REPLACE(content, '<[^>]+>', '')) <= 160
                THEN REGEXP_REPLACE(content, '<[^>]+>', '')
                ELSE CONCAT(SUBSTRING(REGEXP_REPLACE(content, '<[^>]+>', ''), 1, 157), '...')
              END
          END AS excerpt,
          created_at AS published_at,
          is_spotlight
        FROM news
        WHERE deleted_at IS NULL
          AND is_draft = 0
          AND club_id = :clubId
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<NewsRow> findLatestNews(@Param("clubId") Long clubId, Pageable pageable);
}
