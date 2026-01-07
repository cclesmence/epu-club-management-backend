package com.sep490.backendclubmanagement.repository;

import com.sep490.backendclubmanagement.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.sep490.backendclubmanagement.dto.request.StaffFilterRequest;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByIdAndSystemRole_RoleNameIgnoreCase(Long id, String roleName);

    @Query("""
        SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END
        FROM User u
        WHERE u.id = :id
          AND UPPER(u.systemRole.roleName) IN ('STAFF','ADMIN')
    """)
    boolean isStaffOrAdmin(@Param("id") Long id);

    @Query("SELECT u.id FROM User u WHERE LOWER(u.email) = LOWER(:email)")
    Optional<Long> findIdByEmail(@Param("email") String email);

    @Query("SELECT u.systemRole.roleName FROM User u WHERE u.id = :userId AND u.isActive = TRUE")
    Optional<String> findSystemRoleNameByUserId(@Param("userId") Long userId);

    List<User> findByIdIn(List<Long> ids);

    List<User> findBySystemRole_RoleNameIgnoreCase(String roleName);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    Optional<User> findByStudentCode(String studentCode);

    @Query("SELECT u.id FROM User u WHERE UPPER(u.systemRole.roleName) = UPPER(:roleName)")
    List<Long> findIdsBySystemRoleName(@Param("roleName") String roleName);


    @Query(value = """
            SELECT u.*
            FROM users u
            JOIN system_roles sr ON u.system_role_id = sr.id
            WHERE UPPER(sr.role_name) = 'STAFF'
              AND (:#{#req.isActive} IS NULL OR u.is_active = :#{#req.isActive})
              AND (
                   :#{#req.keyword} IS NULL
                OR LOWER(u.full_name)     LIKE LOWER(CONCAT('%', :#{#req.keyword}, '%'))
                OR LOWER(u.email)         LIKE LOWER(CONCAT('%', :#{#req.keyword}, '%'))
              )
            """,
            nativeQuery = true,
            countProjection = "u.id")
    Page<User> getAllStaffByFilter(@Param("req") StaffFilterRequest req, Pageable pageable);

}


