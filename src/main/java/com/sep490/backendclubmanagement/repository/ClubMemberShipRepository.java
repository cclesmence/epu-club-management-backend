package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.dto.response.MemberDTO;
import com.sep490.backendclubmanagement.dto.response.MyClubDTO;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShip;
import com.sep490.backendclubmanagement.entity.club.ClubMemberShipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubMemberShipRepository extends JpaRepository<ClubMemberShip, Long> {

    // CLB mà user đang tham gia trong 1 học kỳ (không phụ thuộc tên collection ở ClubMemberShip)
    @Query("""
        select new com.sep490.backendclubmanagement.dto.response.MyClubDTO(
            c.id, c.clubName, c.logoUrl
        )
        from ClubMemberShip cm
            join cm.club c
        where cm.user.id = :userId
          and exists (
                select 1
                from RoleMemberShip rm
                where rm.clubMemberShip = cm
                  and rm.semester.id = :semesterId
                  and rm.isActive = true
          )
        """)
    List<MyClubDTO> findClubsByUserIdAndSemesterId(@Param("userId") Long userId,
                                                   @Param("semesterId") Long semesterId);

    // Tất cả membership của user (để dùng ở service khác)
    List<ClubMemberShip> findAllByUserId(Long userId);

    // Danh sách thành viên trong 1 CLB (kèm vai trò nếu có trong bảng role_memberships)
    @Query("""
        select new com.sep490.backendclubmanagement.dto.response.MemberDTO(
            u.id,
            u.fullName,
            u.studentCode,
            u.email,
            u.avatarUrl,
            cm.joinDate,
            coalesce(cr.roleName, 'Thành viên')
        )
        from ClubMemberShip cm
            join cm.user u
            left join RoleMemberShip rm
                on rm.clubMemberShip = cm and rm.isActive = true
            left join rm.clubRole cr
        where cm.club.id = :clubId
        """)
    List<MemberDTO> findAllMembersByClubId(@Param("clubId") Long clubId);

    @Query(
            value = """
        SELECT DISTINCT cms.*
        FROM club_memberships cms
        LEFT JOIN role_memberships rm ON rm.club_membership_id = cms.id
        LEFT JOIN semesters s ON s.id = rm.semester_id
        LEFT JOIN club_roles cr ON cr.id = rm.clubrole_id
        LEFT JOIN users u ON u.id = cms.user_id
        WHERE cms.club_id = :clubId
        AND (:status IS NULL OR cms.status = :status)
        AND (:searchTerm IS NULL OR (
            u.full_name LIKE CONCAT('%', :searchTerm, '%') COLLATE utf8mb4_general_ci
            OR u.student_code LIKE CONCAT('%', :searchTerm, '%') COLLATE utf8mb4_general_ci
        ))
    """,
            nativeQuery = true
    )
    List<ClubMemberShip> findMembersWithFiltersList(
            @Param("clubId") Long clubId,
            @Param("status") String status,
            @Param("searchTerm") String searchTerm
    );


    @Query(
            value = """
        SELECT DISTINCT cms.*
        FROM club_memberships cms
        LEFT JOIN role_memberships rm ON rm.club_membership_id = cms.id
        LEFT JOIN semesters s ON s.id = rm.semester_id
        LEFT JOIN club_roles cr ON cr.id = rm.clubrole_id
        LEFT JOIN users u ON u.id = cms.user_id
        WHERE cms.club_id = :clubId
        AND (:searchTerm IS NULL OR (
            u.full_name LIKE CONCAT('%', :searchTerm, '%') COLLATE utf8mb4_general_ci
            OR u.student_code LIKE CONCAT('%', :searchTerm, '%') COLLATE utf8mb4_general_ci
        ))
    """,
            nativeQuery = true
    )
    List<ClubMemberShip> findMembersWithFiltersList(
            @Param("clubId") Long clubId,
            @Param("searchTerm") String searchTerm
    );

    /**
     * Optimized query với JOIN FETCH để tránh N+1 query problem
     * Xử lý tất cả filters trong database thay vì trong Java
     *
     * Logic kiểm tra member còn active trong semester (giống logic ban đầu):
     * - Nếu có semesterId: Chỉ check joinDate/endDate để xác định member có active trong kì đó
     *   + joinDate <= semester.endDate (joined before or during semester)
     *   + endDate IS NULL OR endDate >= semester.startDate (not left before semester starts)
     * - Nếu không có semesterId: Check cms.status theo parameter status
     *
     * Search: Không dùng COLLATE (JPQL không hỗ trợ)
     *         Search không dấu được xử lý trong service layer bằng code
     */
    @Query("""
        SELECT DISTINCT cms 
        FROM ClubMemberShip cms
        LEFT JOIN FETCH cms.user u
        LEFT JOIN FETCH cms.roleMemberships rm
        LEFT JOIN FETCH rm.semester s
        LEFT JOIN FETCH rm.clubRole cr
        LEFT JOIN FETCH rm.team t
        WHERE cms.club.id = :clubId
        AND (
            (:semesterId IS NOT NULL AND 
             cms.joinDate <= (SELECT sem.endDate FROM Semester sem WHERE sem.id = :semesterId) AND
             (cms.endDate IS NULL OR cms.endDate >= (SELECT sem.startDate FROM Semester sem WHERE sem.id = :semesterId)))
            OR
            (:semesterId IS NULL AND (:status IS NULL OR cms.status = :status))
        )
        AND (:roleId IS NULL OR 
             EXISTS (SELECT 1 FROM RoleMemberShip rm2 
                     WHERE rm2.clubMemberShip = cms 
                     AND rm2.clubRole.id = :roleId
                     AND (:semesterId IS NULL OR rm2.semester.id = :semesterId)))
        AND (:isActive IS NULL OR
             (:isActive = true AND 
              EXISTS (SELECT 1 FROM RoleMemberShip rm3 
                      WHERE rm3.clubMemberShip = cms 
                      AND rm3.isActive = true
                      AND (:semesterId IS NULL OR rm3.semester.id = :semesterId)
                      AND (:roleId IS NULL OR rm3.clubRole.id = :roleId))) OR
             (:isActive = false AND 
              NOT EXISTS (SELECT 1 FROM RoleMemberShip rm4 
                          WHERE rm4.clubMemberShip = cms 
                          AND rm4.isActive = true
                          AND (:semesterId IS NULL OR rm4.semester.id = :semesterId))))
        ORDER BY u.fullName ASC
    """)
    org.springframework.data.domain.Page<ClubMemberShip> findMembersWithFiltersOptimized(
            @Param("clubId") Long clubId,
            @Param("status") ClubMemberShipStatus status,
            @Param("semesterId") Long semesterId,
            @Param("roleId") Long roleId,
            @Param("isActive") Boolean isActive,
            org.springframework.data.domain.Pageable pageable
    );

    /**
     * Optimized query for left members với sorting by end_date DESC, full_name ASC
     * Search: Search không dấu được xử lý trong service layer bằng code
     */
    @Query("""
        SELECT DISTINCT cms 
        FROM ClubMemberShip cms
        LEFT JOIN FETCH cms.user u
        LEFT JOIN FETCH cms.roleMemberships rm
        LEFT JOIN FETCH rm.semester s
        LEFT JOIN FETCH rm.clubRole cr
        LEFT JOIN FETCH rm.team t
        WHERE cms.club.id = :clubId
        AND cms.status = 'LEFT'
        ORDER BY cms.endDate DESC NULLS LAST, u.fullName ASC
    """)
    org.springframework.data.domain.Page<ClubMemberShip> findLeftMembersOptimized(
            @Param("clubId") Long clubId,
            org.springframework.data.domain.Pageable pageable
    );
    // Kiểm tra xem user đã là thành viên active của club chưa
    boolean existsByUserIdAndClubIdAndStatus(Long userId, Long clubId, ClubMemberShipStatus status);

    // Kiểm tra xem user có thuộc club hay không (bất kể trạng thái)
    boolean existsByUserIdAndClubId(Long userId, Long clubId);

    // Tìm membership của user trong club với status cụ thể
    java.util.Optional<ClubMemberShip> findByUserIdAndClubIdAndStatus(Long userId, Long clubId, ClubMemberShipStatus status);

    // Kiểm tra xem user có phải là CLUB_PRESIDENT của club trong semester hiện tại không
    @Query("""
        SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END
        FROM ClubMemberShip cm
        JOIN cm.roleMemberships rm
        JOIN rm.clubRole cr
        WHERE cm.user.id = :userId
          AND cm.club.id = :clubId
          AND cm.status = 'ACTIVE'
          AND cr.roleCode = 'CLUB_PRESIDENT'
          AND rm.isActive = true
          AND rm.semester.id = :semesterId
        """)
    boolean isClubPresidentInSemester(@Param("userId") Long userId,
                                      @Param("clubId") Long clubId,
                                      @Param("semesterId") Long semesterId);

//    @Query("""
//    SELECT cms FROM ClubMemberShip cms
//    WHERE cms.club.id = :clubId AND cms.user.id = :userId AND cms.status = 'ACTIVE'
//    ORDER BY cms.joinDate DESC
//    """)
//    List<ClubMemberShip> findByClubIdAndUserIdActive(@Param("clubId") Long clubId, @Param("userId") Long userId);

    @Query("""
    SELECT cms FROM ClubMemberShip cms
    WHERE cms.club.id = :clubId AND cms.user.id = :userId
    """)
    ClubMemberShip findByClubIdAndUserId(@Param("clubId") Long clubId, @Param("userId") Long userId);
    @Query("SELECT COUNT(cms) > 0 FROM ClubMemberShip cms WHERE cms.club.id = :clubId AND cms.user.id = :userId AND cms.status = 'ACTIVE'")
    boolean existsByClubIdAndUserIdAndStatusActive(@Param("clubId") Long clubId, @Param("userId") Long userId);



    @Query("""
        SELECT cm
        FROM ClubMemberShip cm
        WHERE cm.user.id IN :userIds
          AND cm.club.id = :clubId
          AND cm.status = 'ACTIVE'
    """)
    List<ClubMemberShip> findByUserIdInAndClubId(@Param("userIds") List<Long> userIds,
                                                 @Param("clubId") Long clubId);


    @Query("""
        select distinct cms from ClubMemberShip cms
        left join fetch cms.club c
        left join fetch cms.roleMemberships rms
        left join fetch rms.clubRole cr
        left join fetch rms.team t
        where cms.user.id = :userId
          and (:status is null or cms.status = :status)
          and (:semesterId is null or rms.semester.id = :semesterId)
          and (:isActive is null or rms.isActive = :isActive)
        """)
    List<ClubMemberShip> findByUserIdWithRoles(
            @Param("userId") Long userId,
            @Param("status") ClubMemberShipStatus status,
            @Param("semesterId") Long semesterId,
            @Param("isActive") Boolean isActive
    );

    // Get all members by club and status
    @Query("""
        SELECT cms FROM ClubMemberShip cms
        JOIN FETCH cms.user u
        WHERE cms.club.id = :clubId
          AND cms.status = :status
        ORDER BY u.fullName ASC
        """)
    List<ClubMemberShip> findByClubIdAndStatus(
            @Param("clubId") Long clubId,
            @Param("status") ClubMemberShipStatus status
    );

    @Query("""
       select distinct rm.team.id
       from RoleMemberShip rm
       join rm.clubMemberShip cms
       join rm.semester s
       where cms.user.id  = :userId
         and cms.club.id  = :clubId
         and cms.status   = :status
         and rm.isActive  = true
         and rm.team      is not null
         and s.isCurrent  = true
       """)
    List<Long> findTeamIdsByUserAndClubAndStatus(
            @Param("userId") Long userId,
            @Param("clubId") Long clubId,
            @Param("status") ClubMemberShipStatus status
    );

    @Query("""
    select distinct cm.user.id
    from ClubMemberShip cm
      join cm.roleMemberships rm
      join rm.clubRole cr
    where cm.club.id = :clubId
      and cm.status = 'ACTIVE'
      and rm.isActive = true
      and rm.semester.id = :semesterId
      and cr.roleCode in ('CLUB_PRESIDENT', 'CLUB_VICE_PRESIDENT')
""")
    List<Long> findManagerUserIdsByClubAndSemester(Long clubId, Long semesterId);
    @Query("""
    SELECT cm.user.id
    FROM ClubMemberShip cm
    JOIN cm.roleMemberships rm
    JOIN rm.clubRole cr
    JOIN rm.semester s
    WHERE cm.club.id = :clubId
      AND cm.status = 'ACTIVE'
      AND rm.isActive = true
      AND s.isCurrent = true
      AND cr.roleCode NOT IN ('CLUB_PRESIDENT', 'CLUB_VICE')
""")
    List<Long> findAllActiveNonLeadersMemberIds(Long clubId);


}


