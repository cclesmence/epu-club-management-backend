package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.dto.response.TeamMemberDTO;
import com.sep490.backendclubmanagement.entity.RoleMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleMemberShipRepository extends JpaRepository<RoleMemberShip, Long> {

    @Query("""
    SELECT COUNT(DISTINCT rm.clubMemberShip.id)
    FROM RoleMemberShip rm
    JOIN rm.clubMemberShip cm
    JOIN rm.semester s
    WHERE cm.club.id = :clubId
      AND COALESCE(rm.isActive, TRUE) = TRUE
      AND s.isCurrent = TRUE
""")
    Long countActiveMembersInCurrentSemester(@Param("clubId") Long clubId);

    @Query("""
    SELECT DISTINCT cm.user.id
    FROM RoleMemberShip rm
    JOIN rm.clubMemberShip cm
    JOIN rm.semester s
    WHERE cm.club.id = :clubId
      AND COALESCE(rm.isActive, TRUE) = TRUE
      AND s.isCurrent = TRUE
      AND cm.status = com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus.ACTIVE
""")
    List<Long> findActiveMemberUserIdsByClubId(@Param("clubId") Long clubId);

    // Trả về system role của user
    @Query(value = "SELECT sr.role_name\n" +
            "FROM users u\n" +
            "         JOIN club_memberships cm ON u.id = cm.user_id\n" +
            "         JOIN role_memberships rm ON cm.id = rm.club_membership_id\n" +
            "         JOIN semesters s ON rm.semester_id = s.id\n" +
            "         JOIN club_roles cr ON rm.clubrole_id = cr.id\n" +
            "         JOIN system_roles sr ON cr.system_role_id = sr.id\n" +
            "WHERE u.id = :userId\n" +
            "  AND s.is_current = true\n" +
            "LIMIT 1",
           nativeQuery = true)
    Optional<String> findSystemRoleByUserId(@Param("userId") Long userId);

    // Trả về system role của user TRONG MỘT CLB CỤ THỂ (current semester)
    @Query(value = "SELECT sr.role_name\n" +
            "FROM users u\n" +
            "         JOIN club_memberships cm ON u.id = cm.user_id\n" +
            "         JOIN clubs c ON cm.club_id = c.id\n" +
            "         JOIN role_memberships rm ON cm.id = rm.club_membership_id\n" +
            "         JOIN semesters s ON rm.semester_id = s.id\n" +
            "         JOIN club_roles cr ON rm.clubrole_id = cr.id\n" +
            "         JOIN system_roles sr ON cr.system_role_id = sr.id\n" +
            "WHERE u.id = :userId\n" +
            "  AND cm.club_id = :clubId\n" +
            "  AND c.status = 'ACTIVE'\n" +
            "  AND s.is_current = true\n" +
            "LIMIT 1",
            nativeQuery = true)
    Optional<String> findSystemRoleByUserIdAndClubId(@Param("userId") Long userId,
                                                     @Param("clubId") Long clubId);

    @Query(value = "SELECT sr.role_name FROM users u " +
            "JOIN system_roles sr ON u.system_role_id = sr.id " +
            "WHERE u.id = :userId",
            nativeQuery = true)
    Optional<String> findSystemRoleStaff(Long userId);

    List<RoleMemberShip> findByClubMemberShipId(Long clubMemberShipId);

    List<RoleMemberShip> findByClubMemberShipIdAndSemesterId(Long clubMemberShipId, Long semesterId);

    Optional<RoleMemberShip> findByClubMemberShipAndSemester(ClubMemberShip clubMemberShip,
                                                             com.sep490.backendclubmanagement.entity.Semester semester);

    List<RoleMemberShip> findByClubMemberShipIdAndSemesterIdAndIsActive(Long clubMemberShipId, Long semesterId, Boolean isActive);

    // Query với fetch join để load team, clubRole và systemRole cùng lúc, tránh lazy loading issues
    @Query("""
        SELECT rm FROM RoleMemberShip rm
        LEFT JOIN FETCH rm.team t
        LEFT JOIN FETCH rm.clubRole cr
        LEFT JOIN FETCH cr.systemRole sr
        WHERE rm.clubMemberShip.id = :clubMemberShipId
          AND rm.semester.id = :semesterId
          AND rm.isActive = :isActive
        """)
    List<RoleMemberShip> findByClubMemberShipIdAndSemesterIdAndIsActiveWithFetch(
            @Param("clubMemberShipId") Long clubMemberShipId,
            @Param("semesterId") Long semesterId,
            @Param("isActive") Boolean isActive);

    List<RoleMemberShip> findByClubMemberShipIdAndIsActive(Long clubMemberShipId, Boolean isActive);

    // == Club admin (Chủ nhiệm + Phó chủ nhiệm) ==
    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'CLUB_PRESIDENT','PRESIDENT',
              'VICE_PRESIDENT','CLUB_VP',
              'CHỦ NHIỆM','CHU NHIEM',
              'PHÓ CHỦ NHIỆM','PHO CHU NHIEM'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean isClubAdmin(@Param("userId") Long userId,
                        @Param("clubId") Long clubId,
                        @Param("semesterId") Long semesterId);

    // == Chủ nhiệm EXACT (reject ở CLB) ==
    @Query("""
    SELECT CASE WHEN EXISTS (
      SELECT 1 FROM RoleMemberShip rm
      JOIN rm.clubMemberShip cm
      LEFT JOIN rm.clubRole cr
      WHERE cm.user.id = :userId
        AND cm.club.id = :clubId
        AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
        AND COALESCE(rm.isActive, TRUE) = TRUE
        AND rm.team IS NULL
        AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
          'CLUB_PRESIDENT','PRESIDENT','CHU NHIEM','CHỦ NHIỆM'
        )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean isClubPresidentExact(@Param("userId") Long userId,
                                 @Param("clubId") Long clubId,
                                 @Param("semesterId") Long semesterId);

    // == Phó chủ nhiệm EXACT (approve & submit) ==
    @Query("""
SELECT CASE WHEN COUNT(rm) > 0 THEN TRUE ELSE FALSE END
FROM RoleMemberShip rm
JOIN rm.clubMemberShip cm
JOIN rm.clubRole cr
JOIN rm.semester s
WHERE cm.user.id = :userId
  AND cm.club.id = :clubId
  AND s.isCurrent = TRUE
  AND rm.isActive = TRUE
  AND UPPER(cr.roleCode) = 'CLUB_VICE_PRESIDENT'
""")
    boolean isClubViceExact(@Param("userId") Long userId,
                            @Param("clubId") Long clubId,
                            @Param("semesterId") Long semesterId);


    // == Trưởng ban (lead) thuộc một team bất kỳ trong CLB ==
    @Query("""
SELECT CASE WHEN EXISTS (
  SELECT 1
  FROM RoleMemberShip rm
  JOIN rm.clubMemberShip cm
  LEFT JOIN rm.clubRole cr
  WHERE cm.user.id = :userId
    AND cm.club.id = :clubId
    AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
    AND COALESCE(rm.isActive, TRUE) = TRUE
    AND rm.team IS NOT NULL
    AND (
         UPPER(TRIM(COALESCE(cr.roleName, ''))) IN ('CLUB_OFFICER','OFFICER','TEAM_LEAD','LEAD','HEAD')
      OR UPPER(TRIM(COALESCE(cr.roleCode, '')))     IN ('TEAM_LEAD','LEAD','HEAD')
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRUONG BAN%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRƯỞNG BAN%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%LEAD%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%HEAD%'
    )
) THEN TRUE ELSE FALSE END
""")
    boolean isAnyTeamLeadInClub(@Param("userId") Long userId,
                                @Param("clubId") Long clubId,
                                @Param("semesterId") Long semesterId);


    // == Các query bạn đã có (giữ nguyên) ==
    @Query("""
    SELECT new com.sep490.backendclubmanagement.dto.response.TeamMemberDTO(
        u.id,
        u.fullName,
        u.avatarUrl,
        COALESCE(cr.roleName, 'Thành viên'),
        u.email,
        u.studentCode
    )
    FROM RoleMemberShip rm
    JOIN rm.clubMemberShip cms
    JOIN cms.user u
    LEFT JOIN rm.clubRole cr
    WHERE rm.team.id = :teamId
      AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
      AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    List<TeamMemberDTO> findMembersByTeamIdAndSemesterId(@Param("teamId") Long teamId,
                                                         @Param("semesterId") Long semesterId);

    @Query("""
        SELECT DISTINCT t.id, t.teamName, t.description
        FROM RoleMemberShip rm
        JOIN rm.team t
        JOIN rm.clubMemberShip cm
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    List<Object[]> findMyTeamsInClub(@Param("userId") Long userId,
                                     @Param("clubId") Long clubId,
                                     @Param("semesterId") Long semesterId);

    @Query("""
        SELECT t.id, COUNT(DISTINCT rm.clubMemberShip.id)
        FROM RoleMemberShip rm
        JOIN rm.team t
        WHERE t.club.id = :clubId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
        GROUP BY t.id
    """)
    List<Object[]> countMembersByTeam(@Param("clubId") Long clubId,
                                      @Param("semesterId") Long semesterId);

    @Query("""
        SELECT t.id, COALESCE(cr.roleName, 'Thành viên')
        FROM RoleMemberShip rm
        JOIN rm.team t
        LEFT JOIN rm.clubRole cr
        JOIN rm.clubMemberShip cm
        WHERE cm.user.id = :userId
          AND t.club.id = :clubId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    List<Object[]> findMyRolesPerTeam(@Param("userId") Long userId,
                                      @Param("clubId") Long clubId,
                                      @Param("semesterId") Long semesterId);

    // ---- User có thuộc team này không? (cho phép xem DETAIL nếu không phải CLUB_PRESIDENT)
    @Query("""
        SELECT CASE WHEN COUNT(rm.id) > 0 THEN TRUE ELSE FALSE END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND rm.team.id = :teamId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    boolean isMyTeam(@Param("userId") Long userId,
                     @Param("clubId") Long clubId,
                     @Param("teamId") Long teamId,
                     @Param("semesterId") Long semesterId);

    // ---- Đếm distinct member của 1 team
    @Query("""
        SELECT COUNT(DISTINCT rm.clubMemberShip.id)
        FROM RoleMemberShip rm
        WHERE rm.team.id = :teamId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    Long countDistinctMembers(@Param("teamId") Long teamId,
                              @Param("semesterId") Long semesterId);

    // ---- Các role của user trong 1 team (để gắn vào myRoles ở DETAIL)
    @Query("""
        SELECT DISTINCT COALESCE(cr.roleName, 'Thành viên')
        FROM RoleMemberShip rm
        LEFT JOIN rm.clubRole cr
        JOIN rm.clubMemberShip cm
        WHERE cm.user.id = :userId
          AND rm.team.id = :teamId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    List<String> findMyRoles(@Param("userId") Long userId,
                             @Param("teamId") Long teamId,
                             @Param("semesterId") Long semesterId);
    @Query("""
SELECT CASE WHEN EXISTS (
  SELECT 1
  FROM RoleMemberShip rm
  JOIN rm.clubMemberShip cm
  LEFT JOIN rm.clubRole cr
  WHERE cm.user.id = :userId
    AND cm.club.id = :clubId
    AND rm.team.id = :teamId
    AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
    AND COALESCE(rm.isActive, TRUE) = TRUE
    AND (
         UPPER(TRIM(COALESCE(cr.roleName, ''))) IN ('CLUB_OFFICER','OFFICER','TEAM_LEAD','LEAD','HEAD')
      OR UPPER(TRIM(COALESCE(cr.roleCode, '')))     IN ('TEAM_LEAD','LEAD','HEAD')
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRUONG BAN%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRƯỞNG BAN%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%LEAD%'
      OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%HEAD%'
    )
) THEN TRUE ELSE FALSE END
""")
    boolean isTeamLeadExact(@Param("userId") Long userId,
                            @Param("clubId") Long clubId,
                            @Param("teamId") Long teamId,
                            @Param("semesterId") Long semesterId);
    @Query("""
    SELECT DISTINCT rm.team.id
    FROM RoleMemberShip rm
    JOIN rm.clubMemberShip cm
    LEFT JOIN rm.clubRole cr
    WHERE cm.user.id = :userId
      AND cm.club.id = :clubId
      AND COALESCE(rm.isActive, TRUE) = TRUE
      AND rm.team IS NOT NULL
      AND (
            UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%LEAD%'
         OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%HEAD%'
         OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRUONG BAN%'
         OR UPPER(TRIM(COALESCE(cr.roleName, ''))) LIKE '%TRƯỞNG BAN%'
      )
""")
    List<Long> findLeadTeamIdsInClub(@Param("userId") Long userId, @Param("clubId") Long clubId);



    @Query("""
        SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        JOIN rm.semester s
        WHERE cm.user.id = :userId
          AND rm.team.id = :teamId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND ( cr.roleCode LIKE %:headSuffix OR cr.roleLevel <= 3 )
          AND s.isCurrent = true
    """)
    boolean existsTeamLeader(@Param("userId") Long userId,
                             @Param("teamId") Long teamId,
                             @Param("headSuffix") String headSuffix);

    // RoleMemberShipRepository.java
    @Query("""
    SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
    FROM RoleMemberShip rm
    JOIN rm.clubMemberShip c
    JOIN rm.clubRole cr
    JOIN rm.semester s
    WHERE c.user.id = :userId
      AND c.club.id = :clubId
      AND COALESCE(rm.isActive, TRUE) = TRUE
      AND cr.roleLevel <= 2
      AND s.isCurrent = true
    """)
    boolean existsClubAdmin(@Param("userId") Long userId,
                            @Param("clubId") Long clubId);

    // ---- Lấy danh sách club roles (roleCode) của user trong một club
    @Query("""
        SELECT DISTINCT cr.roleCode
        FROM RoleMemberShip rm
        JOIN rm.clubRole cr
        JOIN rm.clubMemberShip cm
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
          AND COALESCE(rm.isActive, TRUE) = TRUE
    """)
    List<String> findClubRolesByUserAndClub(@Param("userId") Long userId,
                                            @Param("clubId") Long clubId,
                                            @Param("semesterId") Long semesterId);

    // User có giữ vai trò PRESIDENT ở bất kỳ CLB nào không? (trong học kỳ hiện tại)
    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        JOIN rm.semester s
        WHERE cm.user.id = :userId
          AND s.isCurrent = TRUE
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'CLUB_OFFICER','CLUB_PRESIDENT','PRESIDENT',
              'CHỦ NHIỆM','CHU NHIEM'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean existsPresidentSomewhere(@Param("userId") Long userId);

    // User là OFFICER của CLB cụ thể?
    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'TEAM_OFFICER','CLUB_OFFICER','OFFICER',
              'CÁN BỘ','CAN BO'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean isClubOfficer(@Param("userId") Long userId,
                          @Param("clubId") Long clubId);

    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'CLUB_TREASURE','CLUB_TREASURER','TREASURER',
              'THU QUY','THỦ QUỸ'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean isClubTreasurer(@Param("userId") Long userId,
                            @Param("clubId") Long clubId);

    // User là OFFICER ở bất kỳ CLB nào? (học kỳ hiện tại)
    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        JOIN rm.semester s
        WHERE cm.user.id = :userId
          AND s.isCurrent = TRUE
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'TEAM_OFFICER','CLUB_OFFICER','OFFICER',
              'CÁN BỘ','CAN BO'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean existsOfficerSomewhere(@Param("userId") Long userId);

    @Query("""
    SELECT CASE WHEN EXISTS (
        SELECT 1
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        LEFT JOIN rm.clubRole cr
        JOIN rm.semester s
        WHERE cm.user.id = :userId
          AND s.isCurrent = TRUE
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND rm.team IS NULL
          AND UPPER(TRIM(COALESCE(cr.roleName, ''))) IN (
              'CLUB_TREASURE','CLUB_TREASURER','TREASURER',
              'THU QUY','THỦ QUỸ'
          )
    ) THEN TRUE ELSE FALSE END
    """)
    boolean existsTreasurerSomewhere(@Param("userId") Long userId);
    // Trả về đầy đủ bản ghi RoleMemberShip (nếu cần)
    @Query(value = "SELECT rm.* FROM role_memberships rm " +
           "JOIN club_memberships cms ON rm.club_membership_id = cms.id " +
           "WHERE cms.user_id = :userId AND cms.club_id = :clubId " +
           "AND rm.is_active = true",
           nativeQuery = true)
    List<RoleMemberShip> findActiveRoleMemberships(
            @Param("userId") Long userId,
            @Param("clubId") Long clubId
    );

    // Lấy danh sách club_id mà user là CLUB_OFFICER dựa theo users.system_role
    @Query(value = "SELECT DISTINCT cms.club_id FROM club_memberships cms " +
           "JOIN users u ON cms.user_id = u.id " +
           "JOIN system_roles sr ON u.system_role_id = sr.id " +
           "WHERE cms.user_id = :userId AND sr.role_name = 'CLUB_OFFICER'",
           nativeQuery = true)
    List<Long> findPresidentClubIdsByUserId(@Param("userId") Long userId);



//tao phong ban

    @Query(value = """
        SELECT DISTINCT cm.user_id
        FROM role_memberships rm
        JOIN club_memberships cm ON rm.club_membership_id = cm.id
        WHERE cm.club_id = :clubId
          AND rm.semester_id = :semesterId
          AND rm.is_active = TRUE
          AND rm.team_id IS NOT NULL
          AND cm.user_id IN (:userIds)
        """, nativeQuery = true)
    List<Long> findExistingTeamMembersInSemester(
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId,
            @Param("userIds") List<Long> userIds
    );
    @Query("""
SELECT cm.user.id
FROM ClubMemberShip cm
WHERE cm.club.id = :clubId
  AND cm.status = com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus.ACTIVE
  AND NOT EXISTS (
       SELECT 1
       FROM RoleMemberShip rmTeam
       WHERE rmTeam.clubMemberShip = cm
         AND rmTeam.semester.id = :semesterId
         AND COALESCE(rmTeam.isActive, TRUE) = TRUE
         AND rmTeam.team IS NOT NULL
  )
  AND NOT EXISTS (
       SELECT 1
       FROM RoleMemberShip rmClub
       JOIN rmClub.clubRole cr
       WHERE rmClub.clubMemberShip = cm
         AND rmClub.semester.id = :semesterId
         AND COALESCE(rmClub.isActive, TRUE) = TRUE
         AND rmClub.team IS NULL
         AND UPPER(cr.roleCode) IN ('CLUB_PRESIDENT','CLUB_VICE_PRESIDENT')
  )
""")
    List<Long> findAvailableMemberUserIds(@Param("clubId") Long clubId,
                                          @Param("semesterId") Long semesterId);


    @Query("""
    SELECT DISTINCT COALESCE(cr.roleName, 'Thành viên')
    FROM RoleMemberShip rm
    LEFT JOIN rm.clubRole cr
    JOIN rm.clubMemberShip cm
    WHERE cm.user.id = :userId
      AND cm.club.id = :clubId
      AND rm.team IS NULL
      AND (:semesterId IS NULL OR rm.semester.id = :semesterId)
      AND COALESCE(rm.isActive, TRUE) = TRUE
""")
    List<String> findMyClubRoleNames(@Param("userId") Long userId,
                                     @Param("clubId") Long clubId,
                                     @Param("semesterId") Long semesterId);




    /**
     * Kiểm tra user có phải CLUB_OFFICER trong kỳ hiện tại và đang active không
     */
    @Query("""
        SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) IN ('CLUB_TREASURE', 'TEAM_OFFICER')
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) IN ('CLUB_TREASURE', 'TEAM_OFFICER'))
          )
    """)
    boolean isTeamOfficerOrTreasurerInCurrentSemester(@Param("userId") Long userId,
                                             @Param("clubId") Long clubId,
                                             @Param("semesterId") Long semesterId);

    /**
     * Kiểm tra user có phải TEAM_OFFICER trong kỳ hiện tại và đang active không
     */
    @Query("""
        SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) ='CLUB_OFFICER'
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) = 'CLUB_OFFICER')
          )
    """)
    boolean isClubOfficerInCurrentSemester(@Param("userId") Long userId,
                                           @Param("clubId") Long clubId,
                                           @Param("semesterId") Long semesterId);

    /**
     * Kiểm tra user có club role là CLUB_OFFICER hoặc TEAM_OFFICER trong kỳ hiện tại và đang active không
     * Kiểm tra từ bảng club_roles thông qua RoleMemberShip
     */
    @Query("""
        SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) IN ('CLUB_TREASURE', 'CLUB_OFFICER', 'TEAM_OFFICER')
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) IN ('CLUB_TREASURE', 'CLUB_OFFICER', 'TEAM_OFFICER'))
          )
    """)
    boolean isClubOfficerOrTeamOfficerOrTreasurerInCurrentSemester(@Param("userId") Long userId,
                                           @Param("clubId") Long clubId,
                                           @Param("semesterId") Long semesterId);
    
    /**
     * Find team ID of a team officer in a club for a specific semester
     * Returns the team ID if user is a team officer (not club officer) in the current semester
     */
    @Query("""
        SELECT rm.team.id
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND rm.team IS NOT NULL
          AND cr IS NOT NULL
          AND (
              (UPPER(TRIM(cr.roleCode)) = 'TEAM_OFFICER' AND sr IS NULL)
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) = 'TEAM_OFFICER')
          )
        ORDER BY rm.id DESC
    """)
    Optional<Long> findTeamIdByUserIdAndClubIdAndSemesterId(
            @Param("userId") Long userId,
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId
    );

    /**
     * Get list of user IDs who are Club Officers in a specific club and semester
     * Club Officers are users with CLUB_OFFICER role (not team-specific)
     */
    @Query("""
        SELECT DISTINCT cm.user.id
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.club.id = :clubId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) IN ('CLUB_OFFICER')
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) IN ('CLUB_OFFICER'))
          )
    """)
    List<Long> findClubOfficerUserIdsByClubIdAndSemesterId(
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId
    );

    /**
     * Batch load club officers for multiple clubs in a specific semester
     * Returns Object[] with [clubId, userId] to avoid N+1 queries
     * Used for bulk notifications and other operations
     */
    @Query("""
        SELECT cm.club.id, cm.user.id
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE cm.club.id IN :clubIds
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) IN ('CLUB_OFFICER')
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) IN ('CLUB_OFFICER'))
          )
    """)
    List<Object[]> findClubOfficerUserIdsByClubIdsAndSemesterId(
            @Param("clubIds") List<Long> clubIds,
            @Param("semesterId") Long semesterId
    );

    /**
     * Get list of user IDs who are Club Officers in a specific club and semester
     * Club Officers are users with CLUB_OFFICER role (not team-specific)
     */
    @Query("""
        SELECT DISTINCT cm.user.id
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN rm.team t
        JOIN rm.clubRole cr
        LEFT JOIN cr.systemRole sr
        WHERE t.id = :teamId
          AND rm.semester.id = :semesterId
          AND COALESCE(rm.isActive, TRUE) = TRUE
          AND COALESCE(cm.user.isActive, TRUE) = TRUE
          AND cr IS NOT NULL
          AND (
              UPPER(TRIM(cr.roleCode)) IN ('TEAM_OFFICER','CLUB_TREASURE')
              OR (sr IS NOT NULL AND UPPER(TRIM(sr.roleName)) IN ('TEAM_OFFICER','CLUB_TREASURE'))
          )
    """)
    List<Long> findTeamOfficerOrTreasurerUserIdsByClubIdAndSemesterId(
            @Param("teamId") Long teamId,
            @Param("semesterId") Long semesterId
    );

    @Query("""
        SELECT CASE WHEN COUNT(rm) > 0 THEN true ELSE false END
        FROM RoleMemberShip rm
        JOIN rm.clubMemberShip cm
        JOIN cm.user u
        JOIN cm.club c
        JOIN rm.clubRole cr
        LEFT JOIN rm.semester s
        WHERE u.id = :userId
          AND c.id = :clubId
          AND rm.isActive = true
          AND rm.team IS NULL
          AND (
                rm.semester IS NULL
             OR (s.startDate <= CURRENT_DATE AND s.endDate >= CURRENT_DATE)
          )
          AND cr.roleCode IN ('CLUB_PRESIDENT','CLUB_VICE_PRESIDENT')
    """)
    boolean isUserClubOfficer(@Param("userId") Long userId, @Param("clubId") Long clubId);
    List<RoleMemberShip> findByTeamIdAndIsActiveTrue(Long teamId);
    @Query("""
    SELECT rm.clubMemberShip.user.id
    FROM RoleMemberShip rm
    WHERE rm.team.id = :teamId
      AND rm.isActive = true
""")
    List<Long> findActiveUserIdsByTeamId(@Param("teamId") Long teamId);
    boolean existsByTeamId(Long teamId);
    @Query(value = """
    SELECT DISTINCT cm.user_id
    FROM role_memberships rm
    JOIN club_memberships cm ON rm.club_membership_id = cm.id
    WHERE cm.club_id = :clubId
      AND rm.semester_id = :semesterId
      AND rm.is_active = TRUE         -- chỉ chặn user đang active
      AND rm.team_id IS NOT NULL      -- đang thuộc 1 team nào đó
      AND cm.user_id IN (:userIds)
""", nativeQuery = true)
    List<Long> findActiveTeamMembersInSemester(
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId,
            @Param("userIds") List<Long> userIds
    );
    @Modifying
    @Query("""
    UPDATE RoleMemberShip rm
    SET rm.isActive = false
    WHERE rm.clubMemberShip.id = :clubMembershipId
      AND rm.semester.id = :semesterId
      AND rm.team IS NOT NULL
      AND COALESCE(rm.isActive, TRUE) = TRUE
""")
    void deactivateActiveTeamRoles(@Param("clubMembershipId") Long clubMembershipId,
                                   @Param("semesterId") Long semesterId);

    @Modifying
    @Query("""
    UPDATE RoleMemberShip rm
    SET rm.isActive = false
    WHERE rm.clubMemberShip.user.id IN :userIds
      AND rm.clubMemberShip.club.id = :clubId
      AND rm.semester.id = :semesterId
      AND rm.isActive = true
""")
    void deactivateActiveRolesForUsers(
            @Param("userIds") List<Long> userIds,
            @Param("clubId") Long clubId,
            @Param("semesterId") Long semesterId
    );
    @Query("""
SELECT CASE WHEN COUNT(rm) > 0 THEN TRUE ELSE FALSE END
FROM RoleMemberShip rm
JOIN rm.clubMemberShip cm
JOIN rm.clubRole cr
JOIN rm.semester s
WHERE cm.user.id = :userId
  AND cm.club.id = :clubId
  AND s.isCurrent = TRUE
  AND rm.isActive = TRUE
  AND UPPER(cr.roleCode) = 'CLUB_PRESIDENT'
""")
    boolean isPresidentSimple(@Param("userId") Long userId,
                              @Param("clubId") Long clubId);


    @Query("""
SELECT CASE WHEN COUNT(rm) > 0 THEN TRUE ELSE FALSE END
FROM RoleMemberShip rm
JOIN rm.clubMemberShip cm
JOIN rm.clubRole cr
JOIN rm.semester s
WHERE cm.user.id = :userId
  AND cm.club.id = :clubId
  AND s.isCurrent = TRUE
  AND rm.isActive = TRUE
  AND rm.team IS NULL
  AND UPPER(cr.roleCode) IN ('CLUB_PRESIDENT', 'CLUB_VICE_PRESIDENT')
""")
    boolean isManagerSimple(@Param("userId") Long userId,
                            @Param("clubId") Long clubId);


}
